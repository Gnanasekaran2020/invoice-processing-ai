package com.invoice.controller;

import com.invoice.domain.enums.InvoiceStatus;
import com.invoice.dto.response.ApiResponse;
import com.invoice.dto.response.ReportPreviewRowResponse;
import com.invoice.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Invoice report generation (PDF / Excel / CSV)")
@SecurityRequirement(name = "bearerAuth")
public class ReportController {

    private final ReportService reportService;

    @GetMapping
    @Operation(summary = "Generate invoice report — ?format=pdf|excel|csv (Admin: all or by userId, User: own)")
    public ResponseEntity<byte[]> generateReport(
            @RequestParam(defaultValue = "pdf") String format,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) InvoiceStatus status,
            @RequestParam(required = false) Integer userId,
            @AuthenticationPrincipal UserDetails userDetails) {

        byte[] data = reportService.generateReport(
                userDetails.getUsername(), format, fromDate, toDate, status, userId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(contentType(format));
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("invoice-report." + extension(format)).build());
        headers.setContentLength(data.length);

        return ResponseEntity.ok().headers(headers).body(data);
    }

    @GetMapping("/preview")
    @Operation(summary = "Preview invoices matching the report filters (Admin: all or by userId, User: own)")
    public ResponseEntity<ApiResponse<List<ReportPreviewRowResponse>>> preview(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) InvoiceStatus status,
            @RequestParam(required = false) Integer userId,
            @AuthenticationPrincipal UserDetails userDetails) {

        List<ReportPreviewRowResponse> rows = reportService.getPreview(
                userDetails.getUsername(), fromDate, toDate, status, userId);
        return ResponseEntity.ok(ApiResponse.success(rows));
    }

    private String extension(String format) {
        return switch (format.toLowerCase()) {
            case "excel", "xlsx" -> "xlsx";
            case "csv"           -> "csv";
            default              -> "pdf";
        };
    }

    private MediaType contentType(String format) {
        return switch (format.toLowerCase()) {
            case "excel", "xlsx" ->
                MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            case "csv" -> MediaType.parseMediaType("text/csv");
            default    -> MediaType.APPLICATION_PDF;
        };
    }
}
