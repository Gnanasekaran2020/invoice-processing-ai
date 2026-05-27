package com.invoice.service;

import com.invoice.domain.entity.Invoice;
import com.invoice.domain.entity.User;
import com.invoice.domain.enums.InvoiceStatus;
import com.invoice.domain.enums.UserRole;
import com.invoice.dto.response.ReportPreviewRowResponse;
import com.invoice.repository.InvoiceRepository;
import com.invoice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd-MMM-yyyy");
    private static final String[] HEADERS = {
        "Invoice #", "Invoice Date", "Vendor Name", "Vendor Address", "Amount", "Status", "Uploaded By", "Created At"
    };

    private final InvoiceRepository invoiceRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public byte[] generateReport(String requestorEmail, String format,
                                  LocalDate fromDate, LocalDate toDate,
                                  InvoiceStatus status, Integer targetUserId) {
        User requestor = userRepository.findByEmail(requestorEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Admin can pass a specific userId to filter by one user, or null for all users.
        // Non-admin always sees only their own invoices.
        Integer userId;
        if (requestor.getRole() == UserRole.ADMIN) {
            userId = targetUserId; // null = all users, non-null = specific user
        } else {
            userId = requestor.getId();
        }

        List<Invoice> invoices = invoiceRepository
                .searchInvoices(null, status, fromDate, toDate, userId, PageRequest.of(0, 10_000))
                .getContent();

        log.info("Generating {} report for {} — {} records (userId={})", format, requestorEmail, invoices.size(), userId);

        return switch (format.toLowerCase()) {
            case "excel", "xlsx" -> generateExcel(invoices);
            case "csv"           -> generateCsv(invoices).getBytes(StandardCharsets.UTF_8);
            case "pdf"           -> generatePdf(invoices);
            default -> throw new IllegalArgumentException(
                    "Unsupported format: '" + format + "'. Use pdf, excel, or csv");
        };
    }

    // ── Excel ─────────────────────────────────────────────────────────────────

    private byte[] generateExcel(List<Invoice> invoices) {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Invoice Report");

            CellStyle headerStyle = wb.createCellStyle();
            Font hFont = wb.createFont();
            hFont.setBold(true);
            hFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(hFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell c = headerRow.createCell(i);
                c.setCellValue(HEADERS[i]);
                c.setCellStyle(headerStyle);
            }

            CellStyle altStyle = wb.createCellStyle();
            altStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
            altStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            int row = 1;
            for (Invoice inv : invoices) {
                Row r = sheet.createRow(row);
                if (row % 2 == 0) {
                    for (int c = 0; c < HEADERS.length; c++) r.createCell(c).setCellStyle(altStyle);
                }
                setCellValue(r, 0, inv.getInvoiceNumber());
                setCellValue(r, 1, inv.getInvoiceDate() != null ? inv.getInvoiceDate().format(DATE_FMT) : "");
                setCellValue(r, 2, inv.getVendorName());
                setCellValue(r, 3, inv.getVendorAddress());
                //if (inv.getAmount() != null) r.getCell(4) != null ? r.getCell(4).setCellValue(inv.getAmount().doubleValue())                         : r.createCell(4).setCellValue(inv.getAmount().doubleValue());
                setCellValue(r, 5, inv.getStatus() != null ? inv.getStatus().name() : "");
                setCellValue(r, 6, inv.getUser() != null ? inv.getUser().getEmail() : "");
                setCellValue(r, 7, inv.getCreatedAt() != null ? inv.getCreatedAt().toString() : "");
                row++;
            }
            for (int i = 0; i < HEADERS.length; i++) sheet.autoSizeColumn(i);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate Excel report", e);
        }
    }

    private void setCellValue(Row row, int col, String value) {
        Cell cell = row.getCell(col);
        if (cell == null) cell = row.createCell(col);
        cell.setCellValue(value != null ? value : "");
    }

    // ── CSV ───────────────────────────────────────────────────────────────────

    private String generateCsv(List<Invoice> invoices) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.join(",", HEADERS)).append("\n");
        for (Invoice inv : invoices) {
            sb.append(csv(inv.getInvoiceNumber())).append(",")
              .append(inv.getInvoiceDate() != null ? inv.getInvoiceDate().format(DATE_FMT) : "").append(",")
              .append(csv(inv.getVendorName())).append(",")
              .append(csv(inv.getVendorAddress())).append(",")
              .append(inv.getAmount() != null ? inv.getAmount().toPlainString() : "").append(",")
              .append(inv.getStatus() != null ? inv.getStatus().name() : "").append(",")
              .append(inv.getUser() != null ? csv(inv.getUser().getEmail()) : "").append(",")
              .append(inv.getCreatedAt() != null ? inv.getCreatedAt().toString() : "")
              .append("\n");
        }
        return sb.toString();
    }

    private String csv(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n"))
            return "\"" + s.replace("\"", "\"\"") + "\"";
        return s;
    }

    // ── PDF ───────────────────────────────────────────────────────────────────

    private byte[] generatePdf(List<Invoice> invoices) {
        try (PDDocument doc = new PDDocument()) {
            PDType1Font bold    = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            float margin = 40f;
            float pageW  = PDRectangle.A4.getWidth();
            float pageH  = PDRectangle.A4.getHeight();
            float tableW = pageW - 2 * margin;
            float rowH   = 16f;
            // 7 columns (skip "Created At" for space)
            float[] cw = { tableW*0.12f, tableW*0.10f, tableW*0.18f, tableW*0.18f,
                           tableW*0.10f, tableW*0.12f, tableW*0.20f };
            String[] cols = { "Invoice #", "Date", "Vendor", "Address", "Amount", "Status", "Uploaded By" };

            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            PDPageContentStream cs = new PDPageContentStream(doc, page);
            float y = pageH - margin;

            // Title
            cs.beginText(); cs.setFont(bold, 15);
            cs.newLineAtOffset(margin, y); cs.showText("Invoice Report"); cs.endText();
            y -= 6;
            cs.beginText(); cs.setFont(regular, 8);
            cs.newLineAtOffset(margin, y);
            cs.showText("Generated: " + LocalDate.now().format(DateTimeFormatter.ISO_DATE)
                    + "   Total records: " + invoices.size());
            cs.endText();
            y -= 18;

            cs = drawHeaderRow(cs, doc, bold, margin, y, tableW, rowH, cw, cols);
            y -= rowH;

            boolean alt = false;
            for (Invoice inv : invoices) {
                if (y < margin + rowH + 10) {
                    cs.close();
                    page = new PDPage(PDRectangle.A4);
                    doc.addPage(page);
                    cs = new PDPageContentStream(doc, page);
                    y = pageH - margin;
                    cs = drawHeaderRow(cs, doc, bold, margin, y, tableW, rowH, cw, cols);
                    y -= rowH;
                }
                if (alt) {
                    cs.setNonStrokingColor(0.94f, 0.96f, 1.0f);
                    cs.addRect(margin, y - rowH, tableW, rowH);
                    cs.fill();
                }
                alt = !alt;
                String[] vals = {
                    nvl(inv.getInvoiceNumber()),
                    inv.getInvoiceDate() != null ? inv.getInvoiceDate().format(DateTimeFormatter.ofPattern("dd/MM/yy")) : "",
                    nvl(inv.getVendorName()),
                    nvl(inv.getVendorAddress()),
                    inv.getAmount() != null ? inv.getAmount().toPlainString() : "",
                    inv.getStatus() != null ? inv.getStatus().name() : "",
                    inv.getUser() != null ? nvl(inv.getUser().getEmail()) : ""
                };
                cs.setNonStrokingColor(0f, 0f, 0f);
                float x = margin + 3;
                for (int i = 0; i < vals.length; i++) {
                    cs.beginText(); cs.setFont(regular, 7f);
                    cs.newLineAtOffset(x, y - rowH + 4);
                    cs.showText(trunc(vals[i], cw[i], 7f));
                    cs.endText();
                    x += cw[i];
                }
                cs.setStrokingColor(0.85f, 0.85f, 0.85f);
                cs.moveTo(margin, y - rowH); cs.lineTo(margin + tableW, y - rowH); cs.stroke();
                y -= rowH;
            }
            cs.close();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate PDF report", e);
        }
    }

    private PDPageContentStream drawHeaderRow(PDPageContentStream cs, PDDocument doc,
            PDType1Font bold, float margin, float y, float tableW, float rowH,
            float[] cw, String[] cols) throws IOException {
        cs.setNonStrokingColor(0.18f, 0.38f, 0.72f);
        cs.addRect(margin, y - rowH, tableW, rowH);
        cs.fill();
        cs.setNonStrokingColor(1f, 1f, 1f);
        float x = margin + 3;
        for (int i = 0; i < cols.length; i++) {
            cs.beginText(); cs.setFont(bold, 7.5f);
            cs.newLineAtOffset(x, y - rowH + 4);
            cs.showText(cols[i]);
            cs.endText();
            x += cw[i];
        }
        return cs;
    }

    private String trunc(String s, float colW, float fontSize) {
        if (s == null) return "";
        int max = (int) (colW / (fontSize * 0.56f));
        return s.length() > max ? s.substring(0, Math.max(0, max - 1)) + "." : s;
    }

    private String nvl(String s) { return s != null ? s : ""; }

    @Transactional(readOnly = true)
    public List<ReportPreviewRowResponse> getPreview(String requestorEmail,
                                                      LocalDate fromDate, LocalDate toDate,
                                                      InvoiceStatus status, Integer targetUserId) {
        User requestor = userRepository.findByEmail(requestorEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Integer userId;
        if (requestor.getRole() == UserRole.ADMIN) {
            userId = targetUserId;
        } else {
            userId = requestor.getId();
        }

        List<Invoice> invoices = invoiceRepository
                .searchInvoices(null, status, fromDate, toDate, userId, PageRequest.of(0, 200))
                .getContent();

        return invoices.stream()
                .map(inv -> ReportPreviewRowResponse.builder()
                        .invoiceNumber(inv.getInvoiceNumber() != null ? inv.getInvoiceNumber() : "#" + inv.getInvoiceId())
                        .vendor(inv.getVendorName() != null ? inv.getVendorName() : "—")
                        .date(inv.getInvoiceDate() != null ? inv.getInvoiceDate().format(DATE_FMT) : "")
                        .amount(inv.getAmount() != null ? inv.getAmount() : java.math.BigDecimal.ZERO)
                        .status(inv.getStatus().name())
                        .build())
                .toList();
    }
}
