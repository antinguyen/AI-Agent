package com.sales.management.order;

import com.sales.management.common.api.PageResponse;
import com.sales.management.order.dto.BulkOrderActionRequest;
import com.sales.management.order.dto.BulkOrderActionResponse;
import com.sales.management.order.dto.OrderCreateRequest;
import com.sales.management.order.dto.OrderResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
public class SalesOrderController {

    private final SalesOrderService salesOrderService;

    public SalesOrderController(SalesOrderService salesOrderService) {
        this.salesOrderService = salesOrderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse create(@Valid @RequestBody OrderCreateRequest request) {
        return salesOrderService.create(request);
    }

    @GetMapping
    public PageResponse<OrderResponse> list(
            @RequestParam(value = "status", required = false) OrderStatus status,
            @RequestParam(value = "fulfillmentStatus", required = false) OrderFulfillmentStatus fulfillmentStatus,
            @RequestParam(value = "customerId", required = false) Long customerId,
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return salesOrderService.list(status, fulfillmentStatus, customerId, from, to, pageable);
    }

    @GetMapping("/ids")
    public List<Long> listIds(
            @RequestParam(value = "status", required = false) OrderStatus status,
            @RequestParam(value = "fulfillmentStatus", required = false) OrderFulfillmentStatus fulfillmentStatus,
            @RequestParam(value = "customerId", required = false) Long customerId,
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return salesOrderService.listIds(status, fulfillmentStatus, customerId, from, to);
    }

    @GetMapping("/{id}")
    public OrderResponse getById(@PathVariable("id") Long id) {
        return salesOrderService.getById(id);
    }

    @PostMapping("/{id}/confirm")
    public OrderResponse confirm(@PathVariable("id") Long id) {
        return salesOrderService.confirm(id);
    }

    @PostMapping("/{id}/cancel")
    public OrderResponse cancel(@PathVariable("id") Long id) {
        return salesOrderService.cancel(id);
    }

    @PostMapping("/bulk/confirm")
    public BulkOrderActionResponse bulkConfirm(@Valid @RequestBody BulkOrderActionRequest request) {
        return salesOrderService.bulkConfirm(request.getOrderIds());
    }

    @PostMapping("/bulk/cancel")
    public BulkOrderActionResponse bulkCancel(@Valid @RequestBody BulkOrderActionRequest request) {
        return salesOrderService.bulkCancel(request.getOrderIds());
    }
}
