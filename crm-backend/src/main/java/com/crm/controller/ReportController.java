package com.crm.controller;

import com.crm.dto.response.*;
import com.crm.model.entity.User;
import com.crm.service.PdfExportService;
import com.crm.service.ReportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;
    private final PdfExportService pdfExportService;

    public ReportController(ReportService reportService, PdfExportService pdfExportService) {
        this.reportService = reportService;
        this.pdfExportService = pdfExportService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<DashboardDTO>> getDashboard(Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        DashboardDTO dashboard = reportService.getDashboard(currentUser);
        return ResponseEntity.ok(ApiResponse.success("Dashboard retrieved successfully", dashboard));
    }

    @GetMapping("/conversion")
    public ResponseEntity<ApiResponse<ConversionReportDTO>> getConversionReport(Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        ConversionReportDTO report = reportService.getConversionReport(currentUser);
        return ResponseEntity.ok(ApiResponse.success("Conversion report retrieved successfully", report));
    }

    @GetMapping("/sales-by-rep")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<SalesRepReportDTO>>> getSalesByRep(Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        List<SalesRepReportDTO> report = reportService.getSalesByRep(currentUser);
        return ResponseEntity.ok(ApiResponse.success("Sales by rep report retrieved successfully", report));
    }

    @GetMapping("/monthly-trend")
    public ResponseEntity<ApiResponse<List<MonthlyTrendDTO>>> getMonthlyTrend(Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        List<MonthlyTrendDTO> trends = reportService.getMonthlyTrend(currentUser);
        return ResponseEntity.ok(ApiResponse.success("Monthly trend report retrieved successfully", trends));
    }

    @GetMapping("/activity-summary")
    public ResponseEntity<ApiResponse<ActivitySummaryDTO>> getActivitySummary(Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        ActivitySummaryDTO summary = reportService.getActivitySummary(currentUser);
        return ResponseEntity.ok(ApiResponse.success("Activity summary retrieved successfully", summary));
    }

    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exportPdf(
            @RequestParam String type,
            @RequestParam(required = false) UUID customerId,
            Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();

        byte[] pdf;
        String filename;

        if ("monthly".equalsIgnoreCase(type)) {
            pdf = pdfExportService.generateMonthlySalesReport(currentUser);
            filename = "monthly-sales-report.pdf";
        } else if ("customer".equalsIgnoreCase(type) && customerId != null) {
            pdf = pdfExportService.generateCustomerSummaryReport(customerId, currentUser);
            filename = "customer-summary-" + customerId + ".pdf";
        } else {
            return ResponseEntity.badRequest().build();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", filename);

        return ResponseEntity.ok().headers(headers).body(pdf);
    }
}
