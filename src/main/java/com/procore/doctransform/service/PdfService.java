package com.procore.doctransform.service;

import java.io.*;
import java.util.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PdfService {

  public File split(MultipartFile file, int from, int to) throws IOException {
    try (PDDocument src = PDDocument.load(file.getInputStream())) {
      PDDocument dest = new PDDocument();
      int numPages = src.getNumberOfPages();
      for (int i = Math.max(1, from) - 1; i < Math.min(to, numPages); i++) {
        dest.addPage(src.getPage(i));
      }
      File out = File.createTempFile("split-", ".pdf");
      dest.save(out);
      dest.close();
      return out;
    }
  }

  public File merge(MultipartFile file1, MultipartFile file2) throws IOException {
    PDDocument doc1 = PDDocument.load(file1.getInputStream());
    PDDocument doc2 = PDDocument.load(file2.getInputStream());
    PDDocument merged = new PDDocument();
    doc1.getPages().forEach(merged::addPage);
    doc2.getPages().forEach(merged::addPage);
    File out = File.createTempFile("merged-", ".pdf");
    merged.save(out);
    doc1.close();
    doc2.close();
    merged.close();
    return out;
  }

  public File reorder(MultipartFile file, String order) throws IOException {
    try (PDDocument src = PDDocument.load(file.getInputStream())) {
      PDDocument dest = new PDDocument();
      String[] tokens = order.split(",");
      List<Integer> idxs = new ArrayList<>();
      for (String t : tokens) {
        int v = Integer.parseInt(t.trim()) - 1;
        if (v >= 0 && v < src.getNumberOfPages()) idxs.add(v);
      }
      for (Integer idx : idxs) {
        dest.addPage(src.getPage(idx));
      }
      File out = File.createTempFile("reordered-", ".pdf");
      dest.save(out);
      dest.close();
      return out;
    }
  }

  public File rotate(MultipartFile file, String pagesStr, int degrees) throws IOException {
    try (PDDocument doc = PDDocument.load(file.getInputStream())) {
      Set<Integer> pages = new HashSet<>();
      for (String p : pagesStr.split(",")) {
        try {
          pages.add(Integer.parseInt(p.trim()) - 1);
        } catch (Exception ignored) {
        }
      }
      for (int i = 0; i < doc.getNumberOfPages(); i++) {
        if (pages.contains(i)) {
          PDPage page = doc.getPage(i);
          int curr = page.getRotation();
          page.setRotation((curr + degrees) % 360);
        }
      }
      File out = File.createTempFile("rotated-", ".pdf");
      doc.save(out);
      return out;
    }
  }

  public File remove(MultipartFile file, String pagesStr) throws IOException {
    try (PDDocument doc = PDDocument.load(file.getInputStream())) {
      Set<Integer> toRemove = new HashSet<>();
      for (String p : pagesStr.split(",")) {
        try {
          toRemove.add(Integer.parseInt(p.trim()) - 1);
        } catch (Exception ignored) {
        }
      }
      List<PDPage> keep = new ArrayList<>();
      for (int i = 0; i < doc.getNumberOfPages(); i++) {
        if (!toRemove.contains(i)) keep.add(doc.getPage(i));
      }
      PDDocument result = new PDDocument();
      keep.forEach(result::addPage);
      File out = File.createTempFile("removed-", ".pdf");
      result.save(out);
      result.close();
      return out;
    }
  }

  public File add(MultipartFile file, int blanks, MultipartFile[] append) throws IOException {
    PDDocument doc;
    if (file != null && !file.isEmpty()) {
      doc = PDDocument.load(file.getInputStream());
    } else {
      doc = new PDDocument();
    }
    for (int i = 0; i < blanks; i++) {
      doc.addPage(new PDPage());
    }
    if (append != null) {
      for (MultipartFile app : append) {
        if (app != null && !app.isEmpty()) {
          PDDocument ap = PDDocument.load(app.getInputStream());
          ap.getPages().forEach(doc::addPage);
          ap.close();
        }
      }
    }
    File out = File.createTempFile("added-", ".pdf");
    doc.save(out);
    doc.close();
    return out;
  }
}
