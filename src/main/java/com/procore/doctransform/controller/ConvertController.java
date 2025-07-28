package com.procore.doctransform.controller;

import com.procore.doctransform.service.ConvertService;
import java.io.*;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequestMapping("/convert")
public class ConvertController {
  private final ConvertService service;

  public ConvertController(ConvertService service) {
    this.service = service;
  }

  @PostMapping("/image-to-pdf")
  public ResponseEntity<InputStreamResource> imageToPdf(@RequestParam MultipartFile file)
      throws IOException {
    File result = service.imageToPdf(file);
    return fileResponse(result, "converted.pdf");
  }

  @PostMapping("/office-to-pdf")
  public ResponseEntity<InputStreamResource> officeToPdf(@RequestParam MultipartFile file)
      throws IOException, InterruptedException {
    File result = service.officeToPdf(file);
    return fileResponse(result, "converted.pdf");
  }

  @PostMapping("/cad-to-pdf")
  public ResponseEntity<InputStreamResource> cadToPdf(@RequestParam MultipartFile file)
      throws IOException {
    File result = service.cadToPdf(file);
    return fileResponse(result, "converted.pdf");
  }

  private ResponseEntity<InputStreamResource> fileResponse(File file, String filename)
      throws IOException {
    InputStreamResource resource = new InputStreamResource(new FileInputStream(file));
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
        .contentType(MediaType.APPLICATION_PDF)
        .contentLength(file.length())
        .body(resource);
  }
}
