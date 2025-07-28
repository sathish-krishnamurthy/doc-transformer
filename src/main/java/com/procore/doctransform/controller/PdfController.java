package com.procore.doctransform.controller;

import com.procore.doctransform.service.PdfService;
import java.io.*;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class PdfController {
  private final PdfService service;

  public PdfController(PdfService service) {
    this.service = service;
  }

  @GetMapping("/")
  public String index() {
    return "redirect:/index.html";
  }

  @PostMapping("/split")
  public ResponseEntity<InputStreamResource> split(
      @RequestParam MultipartFile file, @RequestParam int from, @RequestParam int to)
      throws IOException {
    File result = service.split(file, from, to);
    return fileResponse(result, "split.pdf");
  }

  @PostMapping("/merge")
  public ResponseEntity<InputStreamResource> merge(
      @RequestParam MultipartFile file1, @RequestParam MultipartFile file2) throws IOException {
    File result = service.merge(file1, file2);
    return fileResponse(result, "merged.pdf");
  }

  @PostMapping("/reorder")
  public ResponseEntity<InputStreamResource> reorder(
      @RequestParam MultipartFile file, @RequestParam String order // e.g. "3,1,2"
      ) throws IOException {
    File result = service.reorder(file, order);
    return fileResponse(result, "reordered.pdf");
  }

  @PostMapping("/rotate")
  public ResponseEntity<InputStreamResource> rotate(
      @RequestParam MultipartFile file,
      @RequestParam String pages, // e.g. "1,2,3"
      @RequestParam int degrees // 90, 180, 270
      ) throws IOException {
    File result = service.rotate(file, pages, degrees);
    return fileResponse(result, "rotated.pdf");
  }

  @PostMapping("/remove")
  public ResponseEntity<InputStreamResource> remove(
      @RequestParam MultipartFile file, @RequestParam String pages // e.g. "2,4,7"
      ) throws IOException {
    File result = service.remove(file, pages);
    return fileResponse(result, "removed.pdf");
  }

  @PostMapping("/add")
  public ResponseEntity<InputStreamResource> add(
      @RequestParam(required = false) MultipartFile file,
      @RequestParam(required = false, defaultValue = "0") int blanks,
      @RequestParam(required = false) MultipartFile[] append)
      throws IOException {
    File result = service.add(file, blanks, append);
    return fileResponse(result, "added.pdf");
  }

  private ResponseEntity<InputStreamResource> fileResponse(File file, String downloadName)
      throws IOException {
    InputStreamResource resource = new InputStreamResource(new FileInputStream(file));
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + downloadName)
        .contentType(MediaType.APPLICATION_PDF)
        .contentLength(file.length())
        .body(resource);
  }
}
