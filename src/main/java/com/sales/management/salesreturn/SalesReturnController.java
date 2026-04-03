package com.sales.management.salesreturn;

import com.sales.management.salesreturn.dto.SalesReturnRequest;
import com.sales.management.salesreturn.dto.SalesReturnResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Tag(name = "Sales Return", description = "Sales return management APIs")
public class SalesReturnController {

    private final SalesReturnService salesReturnService;

    public SalesReturnController(SalesReturnService salesReturnService) {
        this.salesReturnService = salesReturnService;
    }

    @PostMapping("/api/v1/returns")
    @Operation(summary = "Create a sales return for a paid order")
    public ResponseEntity<SalesReturnResponse> create(@Valid @RequestBody SalesReturnRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(salesReturnService.create(request));
    }

    @GetMapping("/api/v1/returns")
    @Operation(summary = "List all sales returns")
    public ResponseEntity<List<SalesReturnResponse>> list() {
        return ResponseEntity.ok(salesReturnService.list());
    }

    @GetMapping("/api/v1/returns/{id}")
    @Operation(summary = "Get a sales return by ID")
    public ResponseEntity<SalesReturnResponse> getById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(salesReturnService.getById(id));
    }

    @GetMapping("/api/v1/orders/{orderId}/return")
    @Operation(summary = "Get the sales return for an order")
    public ResponseEntity<SalesReturnResponse> getByOrderId(@PathVariable("orderId") Long orderId) {
        return ResponseEntity.ok(salesReturnService.getByOrderId(orderId));
    }
}
