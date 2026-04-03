package com.sales.management.inventory;

import com.sales.management.common.api.PageResponse;
import com.sales.management.inventory.dto.InventoryTransactionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryTransactionService {

    private final InventoryTransactionRepository inventoryTransactionRepository;

    public InventoryTransactionService(InventoryTransactionRepository inventoryTransactionRepository) {
        this.inventoryTransactionRepository = inventoryTransactionRepository;
    }

    @Transactional(readOnly = true)
    @SuppressWarnings("null")
    public PageResponse<InventoryTransactionResponse> list(Long productId, Long orderId, Pageable pageable) {
        Page<InventoryTransaction> page;

        if (productId != null) {
            page = inventoryTransactionRepository.findByProductId(productId, pageable);
        } else if (orderId != null) {
            page = inventoryTransactionRepository.findByOrderId(orderId, pageable);
        } else {
            page = inventoryTransactionRepository.findAll(pageable);
        }

        return PageResponse.from(page.map(this::toResponse));
    }

    private InventoryTransactionResponse toResponse(InventoryTransaction tx) {
        return new InventoryTransactionResponse(
                tx.getId(),
                tx.getProduct().getId(),
                tx.getProduct().getSku(),
                tx.getOrder() == null ? null : tx.getOrder().getId(),
                tx.getType(),
                tx.getQuantity(),
                tx.getReason(),
                tx.getCreatedAt()
        );
    }
}
