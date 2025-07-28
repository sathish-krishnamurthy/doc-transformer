package com.procore.doctransform.config;

import com.pdftron.pdf.PDFNet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Config {

  private static final Logger log = LoggerFactory.getLogger(Config.class);

  @Value("${pdfnet.license.key}")
  private String pdfnetLicenseKey;

  @Bean
  public boolean pdfNetInitialized() {
    try {
      PDFNet.initialize(pdfnetLicenseKey);
      PDFNet.addResourceSearchPath("/opt/apryse-sdk/Lib");
      return true;
    } catch (Exception e) {
      log.error(e.getMessage());
    }
    return false;
  }
}
