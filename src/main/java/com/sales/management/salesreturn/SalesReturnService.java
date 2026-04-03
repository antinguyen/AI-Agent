package com.sales.management.salesreturn;

import com.sales.management.common.exception.BusinessRuleException;
import com.sales.management.common.exception.DuplicateResourceException;
import com.sales.management.common.exception.ResourceNotFoundException;
import com.sales.management.inventory.InventoryTransaction;
import com.sales.management.inventory.InventoryTransactionRepository;
import com.sales.management.inventory.InventoryTransactionType;
import com.sales.management.order.OrderItem;
import com.sales.management.order.OrderItemRepository;
import com.sales.management.order.OrderStatus;
import com.sales.management.order.SalesOrder;
import com.sales.management.order.SalesOrderRepository;
import com.sales.management.salesreturn.dto.SalesReturnItemRequest;
import com.sales.management.salesreturn.dto.SalesReturnItemResponse;
import com.sales.management.salesreturn.dto.SalesReturnRequest;
import com.sales.management.salesreturn.dto.SalesReturnResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class SalesReturnService {

    private final SalesReturnRepository salesReturnRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final OrderItemRepository orderItemRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;

    public SalesReturnService(
            SalesReturnRepository salesReturnRepository,
            SalesOrderRepository salesOrderRepository,
            OrderItemRepository orderItemRepository,
            InventoryTransactionRepository inventoryTransactionRepository
    ) {
        this.salesReturnRepository = salesReturnRepository;
        this.salesOrderRepository = salesOrderRepository;
        this.orderItemRepository = orderItemRepository;
        this.inventoryTransactionRepository = inventoryTransactionRepository;
    }

    @Transactional
    public SalesReturnResponse create(SalesReturnRequest request) {
        Long orderId = Objects.requireNonNull(request.getOrderId(), "orderId is required");

        SalesOrder order = salesOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        if (salesReturnRepository.existsByOrderId(orderId)) {
            throw new DuplicateResourceException("Order already has a return: " + order.getOrderNumber());
        }

        if (order.getStatus() != OrderStatus.PAID) {
            throw new BusinessRuleException(
                    "Only PAID orders can be returned. Current status: " + order.getStatus());
        }

        SalesReturn salesReturn = new SalesReturn();
        salesReturn.setReturnNumber(generateReturnNumber());
        salesReturn.setOrder(order);
        salesReturn.setReason(request.getReason());
        salesReturn.setStatus(SalesReturnStatus.PENDING);

        BigDecimal totalRefund = BigDecimal.ZERO;

        for (SalesReturnItemRequest itemReq : request.getItems()) {
            Long orderItemId = Objects.requireNonNull(itemReq.getOrderItemId(), "orderItemId is required");

            OrderItem orderItem = orderItemRepository.findById(orderItemId)
                    .orElseThrow(() -> new ResourceNotFoundException("OrderItem not found: " + orderItemId));

            // Ensure the item belongs to this order
            if (!orderItem.getOrder().getId().equals(orderId)) {
                throw new BusinessRuleException(
                        "OrderItem " + orderItemId + " does not belong to order " + orderId);
            }

            if (itemReq.getQuantity() > orderItem.getQuantity()) {
                throw new BusinessRuleException(
                        "Return quantity (" + itemReq.getQuantity() + ") exceeds original quantity ("
                                + orderItem.getQuantity() + ") for item " + orderItemId);
            }

            BigDecimal lineTotal = orderItem.getUnitPrice()
                    .multiply(BigDecimal.valueOf(itemReq.getQuantity()));

            SalesReturnItem returnItem = new SalesReturnItem();
            returnItem.setOrderItem(orderItem);
            returnItem.setQuantity(itemReq.getQuantity());
            returnItem.setUnitPrice(orderItem.getUnitPrice());
            returnItem.setLineTotal(lineTotal);
            salesReturn.addItem(returnItem);

            totalRefund = totalRefund.add(lineTotal);

            // Restore stock
            orderItem.getProduct().setStockQuantity(
                    orderItem.getProduct().getStockQuantity() + itemReq.getQuantity());

            InventoryTransaction txn = new InventoryTransaction();
            txn.setProduct(orderItem.getProduct());
            txn.setOrder(order);
            txn.setType(InventoryTransactionType.IN);
            txn.setQuantity(itemReq.getQuantity());
            txn.setReason("RETURN");
            inventoryTransactionRepository.save(txn);
        }

        salesReturn.setTotalRefund(totalRefund);
        order.setStatus(OrderStatus.RETURNED);

        SalesReturn saved = salesReturnRepository.save(salesReturn);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<SalesReturnResponse> list() {
        return salesReturnRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public SalesReturnResponse getById(Long id) {
        Long returnId = Objects.requireNonNull(id, "id is required");
        SalesReturn salesReturn = salesReturnRepository.findById(returnId)
                .orElseThrow(() -> new ResourceNotFoundException("SalesReturn not found: " + id));
        return toResponse(salesReturn);
    }

    @Transactional(readOnly = true)
    public SalesReturnResponse getByOrderId(Long orderId) {
        SalesReturn salesReturn = salesReturnRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("SalesReturn not found for order: " + orderId));
        return toResponse(salesReturn);
    }

    private SalesReturnResponse toResponse(SalesReturn salesReturn) {
        List<SalesReturnItemResponse> items = salesReturn.getItems().stream()
                .map(item -> new SalesReturnItemResponse(
                        item.getOrderItem().getId(),
                        item.getOrderItem().getProduct().getId(),
                        item.getOrderItem().getProduct().getSku(),
                        item.getOrderItem().getProduct().getName(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getLineTotal()))
                .toList();

        return new SalesReturnResponse(
                salesReturn.getId(),
                salesReturn.getReturnNumber(),
                salesReturn.getOrder().getId(),
                salesReturn.getOrder().getOrderNumber(),
                salesReturn.getReason(),
                salesReturn.getStatus(),
                salesReturn.getTotalRefund(),
                salesReturn.getCreatedAt(),
                items
        );
    }

    private String generateReturnNumber() {
        String date = LocalDate.now(ZoneOffset.UTC).format(DateTimeFormatter.BASIC_ISO_DATE);
        int suffix = ThreadLocalRandom.current().nextInt(10000, 99999);
        return "RET-" + date + "-" + suffix;
    }
}
