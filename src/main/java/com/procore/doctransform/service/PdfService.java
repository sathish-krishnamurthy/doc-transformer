package com.procore.doctransform.service;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.*;
import com.pdftron.sdf.SDFDoc;
import com.procore.doctransform.model.PageRange;
import java.io.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PdfService {

  // Utility: Convert MultipartFile to temp File
  private File convertToFile(MultipartFile multipart) throws IOException {
    File convFile = File.createTempFile("upload-", ".pdf");
    try (OutputStream os = new FileOutputStream(convFile)) {
      os.write(multipart.getBytes());
    }
    return convFile;
  }

  // Save PDFDoc to temp File
  private File savePdfDoc(PDFDoc doc, String prefix) throws PDFNetException, IOException {
    File outFile = File.createTempFile(prefix, ".pdf");
    doc.save(outFile.getAbsolutePath(), SDFDoc.SaveMode.LINEARIZED, null);
    return outFile;
  }

  // 1. Split: extract ranges into separate PDFs, return ZIP of files
  public File split(MultipartFile file, List<PageRange> ranges)
      throws IOException, PDFNetException {
    File inputFile = convertToFile(file);
    PDFDoc originalDoc = new PDFDoc(inputFile.getAbsolutePath());
    originalDoc.initSecurityHandler();

    // Prepare ZIP output file
    File zipFile = File.createTempFile("split_output_", ".zip");
    try (FileOutputStream fos = new FileOutputStream(zipFile);
        java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(fos)) {

      for (int i = 0; i < ranges.size(); i++) {
        PageRange range = ranges.get(i);
        PDFDoc splitDoc = new PDFDoc();
        int totalPages = originalDoc.getPageCount();

        // Validate range inside allowed pages
        int startPage = Math.max(1, range.getStart());
        int endPage = Math.min(totalPages, range.getEnd());
        if (startPage > endPage) {
          splitDoc.close();
          continue; // skip invalid range
        }

        // Insert pages from originalDoc to splitDoc
        splitDoc.insertPages(
            0, originalDoc, startPage, endPage, PDFDoc.InsertBookmarkMode.NONE, null);

        // Save to temp File
        File splitPdfFile = savePdfDoc(splitDoc, "split_" + (i + 1));
        splitDoc.close();

        // Add PDF file to ZIP archive
        java.util.zip.ZipEntry entry = new java.util.zip.ZipEntry("split_" + (i + 1) + ".pdf");
        zos.putNextEntry(entry);
        try (FileInputStream fis = new FileInputStream(splitPdfFile)) {
          fis.transferTo(zos);
        }
        zos.closeEntry();

        // Delete splitPdfFile after adding (optional)
        splitPdfFile.delete();
      }
    }
    originalDoc.close();
    inputFile.delete();
    return zipFile;
  }

  // 2. Merge: merge multiple PDFs into one
  public File merge(MultipartFile file1, MultipartFile file2) throws IOException, PDFNetException {
    File f1 = convertToFile(file1);
    File f2 = convertToFile(file2);

    PDFDoc doc1 = new PDFDoc(f1.getAbsolutePath());
    doc1.initSecurityHandler();
    PDFDoc doc2 = new PDFDoc(f2.getAbsolutePath());
    doc2.initSecurityHandler();

    PDFDoc mergedDoc = new PDFDoc();
    mergedDoc.insertPages(0, doc1, 1, doc1.getPageCount(), PDFDoc.InsertBookmarkMode.NONE, null);
    mergedDoc.insertPages(
        mergedDoc.getPageCount(),
        doc2,
        1,
        doc2.getPageCount(),
        PDFDoc.InsertBookmarkMode.NONE,
        null);

    File outFile = savePdfDoc(mergedDoc, "merged");
    mergedDoc.close();
    doc1.close();
    doc2.close();

    f1.delete();
    f2.delete();
    return outFile;
  }

  // 3. Reorder: reorder pages of a single PDF
  public File reorder(MultipartFile file, List<Integer> newOrder)
      throws IOException, PDFNetException {
    File inputFile = convertToFile(file);
    PDFDoc srcDoc = new PDFDoc(inputFile.getAbsolutePath());
    srcDoc.initSecurityHandler();
    int pageCount = srcDoc.getPageCount();
    if (newOrder.size() != pageCount) {
      srcDoc.close();
      throw new IllegalArgumentException("New order length must equal original page count");
    }
    // Validate indices are in range (1-based)
    for (int p : newOrder) {
      if (p < 1 || p > pageCount) {
        srcDoc.close();
        throw new IllegalArgumentException("Page numbers in newOrder out of range");
      }
    }
    PDFDoc reordered = new PDFDoc();
    // Append pages in new order; insert before pageCount+1 always to append
    for (int p : newOrder) {
      int insertBeforePage = reordered.getPageCount() + 1; // append at end
      reordered.insertPages(insertBeforePage, srcDoc, p, p, PDFDoc.InsertBookmarkMode.NONE, null);
    }
    File outFile = savePdfDoc(reordered, "reordered");
    reordered.close();
    srcDoc.close();
    inputFile.delete();
    return outFile;
  }

  // 4. Rotate: rotate specified pages by degrees (90/180/270)
  public File rotate(MultipartFile file, List<Integer> pagesToRotate, int degrees)
      throws IOException, PDFNetException {
    File inputFile = convertToFile(file);
    PDFDoc doc = new PDFDoc(inputFile.getAbsolutePath());
    doc.initSecurityHandler();

    Set<Integer> pagesSet = new HashSet<>(pagesToRotate);
    int pageCount = doc.getPageCount();

    for (int p = 1; p <= pageCount; p++) {
      if (pagesSet.contains(p)) {
        Page page = doc.getPage(p);
        int currentRotation = page.getRotation();
        int newRotation = (currentRotation + degrees) % 360;

        int rotated =
            switch (newRotation) {
              case 0 -> Page.e_0;
              case 90 -> Page.e_90;
              case 180 -> Page.e_180;
              case 270 -> Page.e_270;
              default ->
                  throw new IllegalArgumentException("Invalid rotation degree: " + newRotation);
            };

        page.setRotation(rotated);
      }
    }

    File outFile = savePdfDoc(doc, "rotated");
    doc.close();
    inputFile.delete();
    return outFile;
  }

  // 5. Remove: remove pages specified
  public File remove(MultipartFile file, List<Integer> pagesToRemove)
      throws IOException, PDFNetException {
    File inputFile = convertToFile(file);
    PDFDoc doc = new PDFDoc(inputFile.getAbsolutePath());
    doc.initSecurityHandler();

    // Sort descending to safely remove pages without affecting indexes
    List<Integer> sortedPages = new ArrayList<>(pagesToRemove);
    sortedPages.removeIf(
        p -> {
          try {
            return p < 1 || p > doc.getPageCount();
          } catch (PDFNetException e) {
            throw new RuntimeException(e);
          }
        });
    sortedPages.sort(Collections.reverseOrder());

    for (int p : sortedPages) {
      PageIterator itr = doc.getPageIterator(p);
      doc.pageRemove(itr);
    }

    File outFile = savePdfDoc(doc, "removed");
    doc.close();
    inputFile.delete();
    return outFile;
  }

  // 6. Add: append blank pages and/or external PDFs
  public File add(
      MultipartFile baseFile, int blankPagesB, int blankPageE, MultipartFile[] appendFiles)
      throws IOException, PDFNetException {
    PDFDoc doc;
    if (baseFile != null && !baseFile.isEmpty()) {
      File baseTemp = convertToFile(baseFile);
      doc = new PDFDoc(baseTemp.getAbsolutePath());
      doc.initSecurityHandler();
      baseTemp.delete();
    } else {
      doc = new PDFDoc();
    }

    // Add blank pages at the Start
    for (int i = 0; i < blankPagesB; i++) {
      Page blankPage = doc.pageCreate();
      doc.pagePushFront(blankPage);
    }

    // Add blank pages at the End
    for (int i = 0; i < blankPageE; i++) {
      Page blankPage = doc.pageCreate();
      doc.pagePushBack(blankPage);
    }

    // Append pages from other PDFs
    if (appendFiles != null) {
      for (MultipartFile appendFile : appendFiles) {
        if (appendFile == null || appendFile.isEmpty()) continue;
        File appendTemp = convertToFile(appendFile);
        PDFDoc appendDoc = new PDFDoc(appendTemp.getAbsolutePath());
        appendDoc.initSecurityHandler();
        int appendPageCount = appendDoc.getPageCount();

        doc.insertPages(
            doc.getPageCount(),
            appendDoc,
            1,
            appendPageCount,
            PDFDoc.InsertBookmarkMode.NONE,
            null);

        appendDoc.close();
        appendTemp.delete();
      }
    }

    File outFile = savePdfDoc(doc, "added");
    doc.close();
    return outFile;
  }
}
