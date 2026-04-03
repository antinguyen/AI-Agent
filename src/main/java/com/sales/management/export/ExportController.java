package com.sales.management.export;

import com.sales.management.order.OrderFulfillmentStatus;
import com.sales.management.order.OrderStatus;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@SuppressWarnings("null")
public class ExportController {

    private final ExportService exportService;

    public ExportController(ExportService exportService) {
        this.exportService = exportService;
    }

    @GetMapping("/orders/{id}/invoice/pdf")
    public ResponseEntity<byte[]> exportInvoicePdf(@PathVariable("id") Long id) {
        byte[] bytes = exportService.generateInvoicePdf(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename("invoice-" + id + ".pdf")
                                .build()
                                .toString())
                .contentType(MediaType.APPLICATION_PDF)
                .body(bytes);
    }

    @GetMapping("/reports/export")
    public ResponseEntity<byte[]> exportReport(
            @RequestParam(value = "format", defaultValue = "xlsx") String format,
            @RequestParam(value = "status", required = false) OrderStatus status,
                        @RequestParam(value = "fulfillmentStatus", required = false) OrderFulfillmentStatus fulfillmentStatus,
                        @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                        @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                        @RequestParam(value = "orderIds", required = false) List<Long> orderIds) throws IOException {
        if (!"xlsx".equalsIgnoreCase(format)) {
            return ResponseEntity.badRequest().build();
        }

                byte[] bytes = exportService.exportOrdersExcel(status, fulfillmentStatus, from, to, orderIds);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename("orders-report.xlsx")
                                .build()
                                .toString())
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }
}
