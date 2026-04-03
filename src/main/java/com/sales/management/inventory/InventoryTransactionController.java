package com.sales.management.inventory;

import com.sales.management.common.api.PageResponse;
import com.sales.management.inventory.dto.InventoryTransactionResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/inventory-transactions")
public class InventoryTransactionController {

    private final InventoryTransactionService inventoryTransactionService;

    public InventoryTransactionController(InventoryTransactionService inventoryTransactionService) {
        this.inventoryTransactionService = inventoryTransactionService;
    }

    @GetMapping
    public PageResponse<InventoryTransactionResponse> list(
            @RequestParam(value = "productId", required = false) Long productId,
            @RequestParam(value = "orderId", required = false) Long orderId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return inventoryTransactionService.list(productId, orderId, pageable);
    }
}
