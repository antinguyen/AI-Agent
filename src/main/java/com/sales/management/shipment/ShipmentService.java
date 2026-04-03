package com.sales.management.shipment;

import com.sales.management.common.exception.BusinessRuleException;
import com.sales.management.common.exception.ResourceNotFoundException;
import com.sales.management.order.OrderItem;
import com.sales.management.order.OrderStatus;
import com.sales.management.order.SalesOrder;
import com.sales.management.order.SalesOrderRepository;
import com.sales.management.shipment.dto.ShipmentCreateRequest;
import com.sales.management.shipment.dto.ShipmentItemResponse;
import com.sales.management.shipment.dto.ShipmentResponse;
import com.sales.management.warehouse.Warehouse;
import com.sales.management.warehouse.WarehouseService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class ShipmentService {

    private final ShipmentRepository shipmentRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final WarehouseService warehouseService;

    public ShipmentService(
            ShipmentRepository shipmentRepository,
            SalesOrderRepository salesOrderRepository,
            WarehouseService warehouseService
    ) {
        this.shipmentRepository = shipmentRepository;
        this.salesOrderRepository = salesOrderRepository;
        this.warehouseService = warehouseService;
    }

    @Transactional
    public ShipmentResponse create(ShipmentCreateRequest request) {
        Long orderId = Objects.requireNonNull(request.getOrderId(), "orderId is required");

        SalesOrder order = salesOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        if (order.getStatus() != OrderStatus.CONFIRMED) {
            throw new BusinessRuleException("Only CONFIRMED orders can create shipment. Current status: " + order.getStatus());
        }

        if (shipmentRepository.existsByOrderId(orderId)) {
            throw new BusinessRuleException("Shipment already exists for order: " + order.getOrderNumber());
        }

        Shipment shipment = new Shipment();
        shipment.setShipmentNumber(generateShipmentNumber());
        shipment.setOrder(order);
        shipment.setWarehouse(order.getWarehouse());
        shipment.setStatus(ShipmentStatus.CREATED);
        shipment.setNote(request.getNote());

        for (OrderItem orderItem : order.getItems()) {
            ShipmentItem shipmentItem = new ShipmentItem();
            shipmentItem.setOrderItem(orderItem);
            shipmentItem.setProduct(orderItem.getProduct());
            shipmentItem.setQuantity(orderItem.getQuantity());
            shipment.addItem(shipmentItem);
        }

        Shipment saved = shipmentRepository.save(shipment);
        return toResponse(saved);
    }

    @Transactional
    public ShipmentResponse markShipped(Long id) {
        Shipment shipment = shipmentRepository.findById(Objects.requireNonNull(id, "id is required"))
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found: " + id));

        if (shipment.getStatus() != ShipmentStatus.CREATED) {
            throw new BusinessRuleException("Only CREATED shipment can be marked as shipped");
        }

        Warehouse warehouse = shipment.getWarehouse();
        if (warehouse != null) {
            for (ShipmentItem item : shipment.getItems()) {
                warehouseService.deductStock(
                        item.getProduct().getId(),
                        warehouse.getId(),
                        item.getQuantity(),
                        "SHIPMENT",
                        shipment.getId()
                );
            }
        }

        shipment.setStatus(ShipmentStatus.SHIPPED);
        shipment.setShippedAt(Instant.now());

        return toResponse(shipment);
    }

    @Transactional
    public ShipmentResponse cancel(Long id) {
        Shipment shipment = shipmentRepository.findById(Objects.requireNonNull(id, "id is required"))
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found: " + id));

        if (shipment.getStatus() == ShipmentStatus.SHIPPED) {
            throw new BusinessRuleException("Cannot cancel a shipped shipment");
        }

        shipment.setStatus(ShipmentStatus.CANCELLED);
        return toResponse(shipment);
    }

    @Transactional(readOnly = true)
    public ShipmentResponse getById(Long id) {
        Shipment shipment = shipmentRepository.findById(Objects.requireNonNull(id, "id is required"))
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found: " + id));
        return toResponse(shipment);
    }

    @Transactional(readOnly = true)
    public ShipmentResponse getByOrderId(Long orderId) {
        Shipment shipment = shipmentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found for order: " + orderId));
        return toResponse(shipment);
    }

    @Transactional(readOnly = true)
    public List<ShipmentResponse> list() {
        return shipmentRepository.findAll().stream().map(this::toResponse).toList();
    }

    private ShipmentResponse toResponse(Shipment shipment) {
        List<ShipmentItemResponse> itemResponses = shipment.getItems().stream()
                .map(item -> new ShipmentItemResponse(
                        item.getId(),
                        item.getOrderItem().getId(),
                        item.getProduct().getId(),
                        item.getProduct().getSku(),
                        item.getProduct().getName(),
                        item.getQuantity()
                ))
                .toList();

        return new ShipmentResponse(
                shipment.getId(),
                shipment.getShipmentNumber(),
                shipment.getOrder().getId(),
                shipment.getOrder().getOrderNumber(),
                shipment.getWarehouse() != null ? shipment.getWarehouse().getId() : null,
                shipment.getWarehouse() != null ? shipment.getWarehouse().getName() : null,
                shipment.getStatus(),
                shipment.getNote(),
                shipment.getShippedAt(),
                shipment.getCreatedAt(),
                itemResponses
        );
    }

    private String generateShipmentNumber() {
        String date = LocalDate.now(ZoneOffset.UTC).format(DateTimeFormatter.BASIC_ISO_DATE);
        int suffix = ThreadLocalRandom.current().nextInt(10000, 99999);
        return "SHP-" + date + "-" + suffix;
    }
}
