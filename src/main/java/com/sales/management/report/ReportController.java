package com.sales.management.report;

import com.sales.management.report.dto.OrderSummaryResponse;
import com.sales.management.report.dto.RevenueReportResponse;
import com.sales.management.report.dto.TopProductResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/revenue")
    public RevenueReportResponse revenue(
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reportService.revenue(from, to);
    }

    @GetMapping("/top-products")
    public List<TopProductResponse> topProducts(
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        return reportService.topProducts(limit);
    }

    @GetMapping("/order-summary")
    public OrderSummaryResponse orderSummary() {
        return reportService.orderSummary();
    }
}
