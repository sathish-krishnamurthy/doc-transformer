package com.procore.doctransform.service;

import com.pdftron.pdf.Convert;
import com.pdftron.pdf.ConvertPrinter;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.sdf.SDFDoc;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.ImageIO;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ConvertService {

  public File imageToPdf(MultipartFile file) throws IOException {
    BufferedImage img = ImageIO.read(file.getInputStream());
    float width = img.getWidth();
    float height = img.getHeight();

    PDDocument doc = new PDDocument();
    PDPage page = new PDPage(new PDRectangle(width, height));
    doc.addPage(page);

    PDPageContentStream cs = new PDPageContentStream(doc, page);
    cs.drawImage(LosslessFactory.createFromImage(doc, img), 0, 0, width, height);
    cs.close();

    File out = File.createTempFile("img2pdf-", ".pdf");
    doc.save(out);
    doc.close();
    return out;
  }

  public File officeToPdf(MultipartFile file) throws IOException, InterruptedException {
    File input = File.createTempFile("office-", "-" + file.getOriginalFilename());
    file.transferTo(input);
    File dir = input.getParentFile();

    Process proc =
        new ProcessBuilder(
                "soffice",
                "--headless",
                "--convert-to",
                "pdf",
                "--outdir",
                dir.getAbsolutePath(),
                input.getAbsolutePath())
            .redirectErrorStream(true)
            .start();
    int status = proc.waitFor();

    String pdfName = input.getName().replaceAll("\\.[^.]+$", "") + ".pdf";
    File out = new File(dir, pdfName);
    if (!out.exists() || status != 0)
      throw new IOException("LibreOffice failed to convert input. See container logs for details.");
    return out;
  }

  public File cadToPdf(MultipartFile file) throws IOException {
    File input = File.createTempFile("cad-", "-" + file.getOriginalFilename());
    file.transferTo(input);

    // Use Apryse SDK for CAD to PDF conversion
    PDFDoc pdfDoc = null;
    try {
      pdfDoc = new PDFDoc();
      ConvertPrinter.setMode(ConvertPrinter.e_convert_printer_prefer_builtin_converter);
      Convert.toPdf(pdfDoc, input.getAbsolutePath());
      File out = File.createTempFile("cad2pdf-", ".pdf");
      pdfDoc.save(out.getAbsolutePath(), SDFDoc.SaveMode.LINEARIZED, null);
      pdfDoc.close();
      return out;
    } catch (Exception e) {
      if (pdfDoc != null)
        try {
          pdfDoc.close();
        } catch (Exception ignore) {
        }
      throw new IOException("Apryse CAD-to-PDF conversion failed: " + e.getMessage(), e);
    }
  }
}
