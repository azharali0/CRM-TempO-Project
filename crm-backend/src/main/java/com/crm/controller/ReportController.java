package com.crm.controller;

import com.crm.dto.response.*;
import com.crm.model.entity.User;
import com.crm.service.ReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
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
}
