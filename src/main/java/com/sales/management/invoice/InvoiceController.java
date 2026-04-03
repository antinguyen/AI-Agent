package com.sales.management.invoice;

import com.sales.management.invoice.dto.InvoiceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name = "Invoice", description = "Invoice management APIs")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @GetMapping("/api/v1/invoices")
    @Operation(summary = "List all invoices")
    public ResponseEntity<List<InvoiceResponse>> list() {
        return ResponseEntity.ok(invoiceService.list());
    }

    @GetMapping("/api/v1/invoices/{id}")
    @Operation(summary = "Get invoice by ID")
    public ResponseEntity<InvoiceResponse> getById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(invoiceService.getById(id));
    }

    @GetMapping("/api/v1/orders/{orderId}/invoice")
    @Operation(summary = "Get invoice for an order")
    public ResponseEntity<InvoiceResponse> getByOrderId(@PathVariable("orderId") Long orderId) {
        return ResponseEntity.ok(invoiceService.getByOrderId(orderId));
    }
}
