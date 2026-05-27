package com.invoice.service;

import com.invoice.exception.FileStorageException;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles text extraction from PDF and image files.
 * - PDFs: try text layer first, fall back to OCR if scanned.
 * - Images: run Tesseract OCR directly.
 */
@Slf4j
@Service
public class DocumentExtractionService {

    private static final float PDF_RENDER_DPI = 300f;

    /**
     * Extract text from a PDF byte array.
     * First attempts native text extraction; falls back to OCR if text layer is empty.
     */
    public String extractTextFromPdf(byte[] pdfBytes) {
        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(doc).trim();

            if (text.length() > 50) {
                log.info("PDF text layer extracted: {} chars", text.length());
                return text;
            }

            // Scanned PDF — render pages to images and OCR
            log.info("PDF text layer empty — falling back to OCR");
            return ocrPdfPages(doc);

        } catch (IOException ex) {
            throw new FileStorageException("Failed to read PDF document: " + ex.getMessage(), ex);
        }
    }

    /**
     * Render each PDF page as an image and run OCR on them.
     */
    private String ocrPdfPages(PDDocument doc) throws IOException {
        PDFRenderer renderer = new PDFRenderer(doc);
        List<String> pageTexts = new ArrayList<>();

        for (int page = 0; page < doc.getNumberOfPages(); page++) {
            BufferedImage image = renderer.renderImageWithDPI(page, PDF_RENDER_DPI, ImageType.RGB);
            byte[] imageBytes = bufferedImageToBytes(image, "png");
            String pageText = runOcr(imageBytes);
            pageTexts.add(pageText);
            log.debug("Page {} OCR result: {} chars", page + 1, pageText.length());
        }

        return String.join("\n\n--- PAGE BREAK ---\n\n", pageTexts);
    }

    /**
     * Extract text from an image byte array using Tesseract OCR.
     */
    public String extractTextFromImage(byte[] imageBytes) {
        return runOcr(imageBytes);
    }

    /**
     * Get raw image bytes from the first page of a PDF (used for vision API).
     */
    public byte[] renderPdfFirstPageAsImage(byte[] pdfBytes) {
        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            PDFRenderer renderer = new PDFRenderer(doc);
            BufferedImage image = renderer.renderImageWithDPI(0, PDF_RENDER_DPI, ImageType.RGB);
            return bufferedImageToBytes(image, "png");
        } catch (IOException ex) {
            throw new FileStorageException("Failed to render PDF page as image: " + ex.getMessage(), ex);
        }
    }

    private String runOcr(byte[] imageBytes) {
        try {
            Tesseract tesseract = new Tesseract();
            tesseract.setLanguage("eng");
            tesseract.setPageSegMode(1);
            tesseract.setOcrEngineMode(1);

            try (ByteArrayInputStream bis = new ByteArrayInputStream(imageBytes)) {
                BufferedImage img = ImageIO.read(bis);
                String result = tesseract.doOCR(img);
                log.info("OCR extracted {} chars", result.length());
                return result;
            }
        } catch (Exception ex) {
            log.warn("OCR failed: {} — returning empty string", ex.getMessage());
            return "";
        }
    }

    private byte[] bufferedImageToBytes(BufferedImage image, String format) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageIO.write(image, format, bos);
        return bos.toByteArray();
    }
}
