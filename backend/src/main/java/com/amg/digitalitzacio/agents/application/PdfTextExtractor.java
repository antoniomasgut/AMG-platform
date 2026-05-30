package com.amg.digitalitzacio.agents.application;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
@Slf4j
public class PdfTextExtractor {

    public String extract(MultipartFile file) throws IOException {
        String ct = file.getContentType();
        if ("text/plain".equals(ct)) {
            return new String(file.getBytes(), StandardCharsets.UTF_8);
        }
        try (PDDocument doc = Loader.loadPDF(file.getBytes())) {
            if (doc.isEncrypted()) {
                throw new IllegalArgumentException("El PDF està protegit amb contrasenya");
            }
            var stripper = new PDFTextStripper();
            String text = stripper.getText(doc);
            log.debug("Extracted {} chars from PDF '{}' ({} pages)",
                    text.length(), file.getOriginalFilename(), doc.getNumberOfPages());
            return text.trim();
        }
    }
}
