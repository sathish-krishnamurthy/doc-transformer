package com.procore.doctransform.controller;

import com.procore.doctransform.model.PageRange;
import com.procore.doctransform.service.PdfService;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class PdfController {
  private final PdfService pdfService;

  public PdfController(PdfService pdfService) {
    this.pdfService = pdfService;
  }

  @GetMapping("/")
  public String index() {
    return "index";
  }

  // -------- Split -------------
  // Accepts multiple page ranges, returns ZIP (multi-split)
  @PostMapping("/split")
  public ResponseEntity<InputStreamResource> split(
      @RequestParam MultipartFile file, @RequestParam String ranges // e.g. "1-3,5-5,7-10"
      ) throws Exception {
    List<PageRange> parsedRanges = parseRanges(ranges);
    File zipFile = pdfService.split(file, parsedRanges);
    return fileResponse(zipFile, "split_output.zip", MediaType.APPLICATION_OCTET_STREAM);
  }

  // -------- Merge -------------
  @PostMapping("/merge")
  public ResponseEntity<InputStreamResource> merge(
      @RequestParam MultipartFile file1, @RequestParam MultipartFile file2) throws Exception {
    File mergedPdf = pdfService.merge(file1, file2);
    return fileResponse(mergedPdf, "merged.pdf", MediaType.APPLICATION_PDF);
  }

  // -------- Reorder -------------
  @PostMapping("/reorder")
  public ResponseEntity<InputStreamResource> reorder(
      @RequestParam MultipartFile file, @RequestParam String order // e.g. "3,1,2,4"
      ) throws Exception {
    List<Integer> newOrder = parseIntegerList(order);
    File reorderedPdf = pdfService.reorder(file, newOrder);
    return fileResponse(reorderedPdf, "reordered.pdf", MediaType.APPLICATION_PDF);
  }

  // -------- Rotate -------------
  @PostMapping("/rotate")
  public ResponseEntity<InputStreamResource> rotate(
      @RequestParam MultipartFile file,
      @RequestParam String pages, // e.g. "1,2,5"
      @RequestParam int degrees // 90,180,270
      ) throws Exception {
    List<Integer> pageList = parseIntegerList(pages);
    File rotatedPdf = pdfService.rotate(file, pageList, degrees);
    return fileResponse(rotatedPdf, "rotated.pdf", MediaType.APPLICATION_PDF);
  }

  // -------- Remove -------------
  @PostMapping("/remove")
  public ResponseEntity<InputStreamResource> remove(
      @RequestParam MultipartFile file, @RequestParam String pages // e.g. "2,5"
      ) throws Exception {
    List<Integer> pagesToRemove = parseIntegerList(pages);
    File removedPdf = pdfService.remove(file, pagesToRemove);
    return fileResponse(removedPdf, "removed.pdf", MediaType.APPLICATION_PDF);
  }

  // -------- Add -------------
  // Accepts optional base file, blank pages number, optional appended PDFs
  @PostMapping("/add")
  public ResponseEntity<InputStreamResource> add(
      @RequestParam(required = false) MultipartFile baseFile,
      @RequestParam(required = false, defaultValue = "0") int start,
      @RequestParam(required = false, defaultValue = "0") int end,
      @RequestParam(required = false) MultipartFile[] append)
      throws Exception {
    File addedPdf = pdfService.add(baseFile, start, end, append);
    return fileResponse(addedPdf, "added.pdf", MediaType.APPLICATION_PDF);
  }

  // Utility to send file as response
  private ResponseEntity<InputStreamResource> fileResponse(
      File file, String filename, MediaType mediaType) throws IOException {
    InputStreamResource resource = new InputStreamResource(new FileInputStream(file));
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
        .contentType(mediaType)
        .contentLength(file.length())
        .body(resource);
  }

  // Parse page ranges string (e.g. "1-3,5-7,10-10") to List<PageRange>
  private List<PageRange> parseRanges(String input) {
    if (input == null || input.isBlank()) return Collections.emptyList();
    return Arrays.stream(input.split(","))
        .map(String::trim)
        .map(
            s -> {
              String[] parts = s.split("-");
              int start = Integer.parseInt(parts[0].trim());
              int end = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : start;
              return new PageRange(start, end);
            })
        .collect(Collectors.toList());
  }

  // Parse comma-separated string of integers (e.g. "1,3,5")
  private List<Integer> parseIntegerList(String input) {
    if (input == null || input.isBlank()) return Collections.emptyList();
    return Arrays.stream(input.split(","))
        .map(String::trim)
        .map(Integer::parseInt)
        .collect(Collectors.toList());
  }
}
