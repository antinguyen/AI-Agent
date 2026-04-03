package com.sales.management.order;

import com.sales.management.common.api.PageResponse;
import com.sales.management.common.exception.BusinessRuleException;
import com.sales.management.common.exception.ResourceNotFoundException;
import com.sales.management.customer.Customer;
import com.sales.management.customer.CustomerRepository;
import com.sales.management.discount.Discount;
import com.sales.management.discount.DiscountService;
import com.sales.management.inventory.InventoryTransaction;
import com.sales.management.inventory.InventoryTransactionRepository;
import com.sales.management.inventory.InventoryTransactionType;
import com.sales.management.notification.EmailService;
import com.sales.management.order.dto.OrderCreateItemRequest;
import com.sales.management.order.dto.OrderCreateRequest;
import com.sales.management.order.dto.OrderItemResponse;
import com.sales.management.order.dto.OrderResponse;
import com.sales.management.order.dto.BulkOrderActionResponse;
import com.sales.management.order.dto.BulkOrderFailureDetail;
import com.sales.management.product.Product;
import com.sales.management.product.ProductRepository;
import com.sales.management.shipment.Shipment;
import com.sales.management.shipment.ShipmentRepository;
import com.sales.management.shipment.ShipmentStatus;
import com.sales.management.warehouse.ProductWarehouseStockRepository;
import com.sales.management.warehouse.WarehouseService;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SalesOrderService {

    private final SalesOrderRepository salesOrderRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final DiscountService discountService;
    private final EmailService emailService;
    private final ShipmentRepository shipmentRepository;
    private final WarehouseService warehouseService;
    private final ProductWarehouseStockRepository productWarehouseStockRepository;

    public SalesOrderService(
            SalesOrderRepository salesOrderRepository,
            CustomerRepository customerRepository,
            ProductRepository productRepository,
            InventoryTransactionRepository inventoryTransactionRepository,
            DiscountService discountService,
            EmailService emailService,
            ShipmentRepository shipmentRepository,
            WarehouseService warehouseService,
                ProductWarehouseStockRepository productWarehouseStockRepository
    ) {
        this.salesOrderRepository = salesOrderRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.inventoryTransactionRepository = inventoryTransactionRepository;
        this.discountService = discountService;
        this.emailService = emailService;
        this.shipmentRepository = shipmentRepository;
        this.warehouseService = warehouseService;
        this.productWarehouseStockRepository = productWarehouseStockRepository;
    }

    @Transactional
    public OrderResponse create(OrderCreateRequest request) {
        Long customerId = Objects.requireNonNull(request.getCustomerId(), "customerId is required");
        Customer customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + customerId));

        if (!Boolean.TRUE.equals(customer.getActive())) {
            throw new BusinessRuleException("Customer is inactive: " + customer.getId());
        }

        // Only apply warehouse-specific stock checks when user explicitly selects a warehouse.
        // If warehouse is omitted, keep legacy global stock behavior for broader product compatibility.
        var warehouse = request.getWarehouseId() != null
            ? warehouseService.getWarehouseEntity(request.getWarehouseId())
            : null;

        SalesOrder order = new SalesOrder();
        order.setOrderNumber(generateOrderNumber());
        order.setCustomer(customer);
        order.setStatus(OrderStatus.CREATED);
        order.setWarehouse(warehouse);

        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalVnd = BigDecimal.ZERO;
        Map<Long, Integer> aggregatedItems = aggregateRequestedItems(request.getItems());

        for (Map.Entry<Long, Integer> entry : aggregatedItems.entrySet()) {
            Long productId = entry.getKey();
            Integer requestedQuantity = entry.getValue();
            Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));

            if (!Boolean.TRUE.equals(product.getActive())) {
                throw new BusinessRuleException("Product is inactive: " + product.getId());
            }

            // If warehouse is specified, check warehouse-specific stock
            if (warehouse != null) {
                var warehouseStock = productWarehouseStockRepository
                    .findByProductIdAndWarehouseId(productId, warehouse.getId())
                    .orElseThrow(() -> new BusinessRuleException("Product not available in selected warehouse: " + product.getSku()));

                if (warehouseStock.getAvailableQuantity() < requestedQuantity) {
                    throw new BusinessRuleException("Insufficient stock in warehouse for product: " + product.getSku());
                }

                // Reserve stock in warehouse (not deduct yet - only deduct when shipped)
                warehouseStock.reserve(requestedQuantity);
                productWarehouseStockRepository.save(warehouseStock);
            } else {
                // Fallback to old logic: check product.stockQuantity directly
                if (product.getStockQuantity() < requestedQuantity) {
                    throw new BusinessRuleException("Insufficient stock for product: " + product.getSku());
                }
            }

            // Also update old product.stockQuantity for backward compatibility
            product.setStockQuantity(product.getStockQuantity() - requestedQuantity);

            BigDecimal vatRate = product.getVatRate() != null ? product.getVatRate() : BigDecimal.ZERO;
            BigDecimal vatMultiplier = BigDecimal.ONE.add(vatRate.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
            BigDecimal lineTotal = product.getPrice().multiply(BigDecimal.valueOf(requestedQuantity)).multiply(vatMultiplier);
            BigDecimal rate = product.getExchangeRate() != null ? product.getExchangeRate() : BigDecimal.ONE;
            BigDecimal lineTotalVnd = product.getPrice().multiply(rate).multiply(BigDecimal.valueOf(requestedQuantity)).multiply(vatMultiplier);

            OrderItem item = new OrderItem();
            item.setProduct(product);
            item.setQuantity(requestedQuantity);
            item.setUnitPrice(product.getPrice());
            item.setLineTotal(lineTotal);

            order.addItem(item);
            totalAmount = totalAmount.add(lineTotal);
            totalVnd = totalVnd.add(lineTotalVnd);
        }

        order.setTotalAmount(totalAmount);
        order.setTotalVnd(totalVnd.setScale(2, RoundingMode.HALF_UP));

        // Apply discount if provided
        if (request.getDiscountCode() != null && !request.getDiscountCode().isBlank()) {
            Discount discount = discountService.findByCodeOrThrow(request.getDiscountCode());
            java.math.BigDecimal discountAmount = discountService.calculateDiscount(discount, totalAmount);
            order.setDiscount(discount);
            order.setDiscountAmount(discountAmount);
            order.setTotalAmount(totalAmount.subtract(discountAmount));
            // Apply proportional discount to VND total
            if (totalAmount.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal ratio = order.getTotalAmount().divide(totalAmount, 10, RoundingMode.HALF_UP);
                order.setTotalVnd(totalVnd.multiply(ratio).setScale(2, RoundingMode.HALF_UP));
            }
        }

        SalesOrder saved = salesOrderRepository.save(order);

        for (OrderItem item : saved.getItems()) {
            recordInventoryTransaction(
                    item.getProduct(),
                    saved,
                    InventoryTransactionType.OUT,
                    item.getQuantity(),
                    "ORDER_CREATED"
            );
        }

        OrderResponse response = toResponse(saved, null);

        if (customer.getEmail() != null && !customer.getEmail().isBlank()) {
            emailService.sendOrderConfirmation(customer.getEmail(), response);
        }

        return response;
    }

    private Map<Long, Integer> aggregateRequestedItems(List<OrderCreateItemRequest> items) {
        Map<Long, Integer> aggregatedItems = new LinkedHashMap<>();
        for (OrderCreateItemRequest item : items) {
            Long productId = Objects.requireNonNull(item.getProductId(), "productId is required");
            Integer quantity = Objects.requireNonNull(item.getQuantity(), "quantity is required");
            aggregatedItems.merge(productId, quantity, Integer::sum);
        }
        return aggregatedItems;
    }

    @Transactional
    public OrderResponse confirm(Long id) {
        Long orderId = Objects.requireNonNull(id, "id is required");
        SalesOrder order = salesOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));

        if (order.getStatus() != OrderStatus.CREATED) {
            throw new BusinessRuleException("Only CREATED orders can be confirmed");
        }

        order.setStatus(OrderStatus.CONFIRMED);
        OrderResponse response = toResponse(order, shipmentRepository.findByOrderId(order.getId()).orElse(null));

        String email = order.getCustomer().getEmail();
        if (email != null && !email.isBlank()) {
            emailService.sendOrderConfirmed(email, response);
        }

        return response;
    }

    @Transactional
    public OrderResponse cancel(Long id) {
        Long orderId = Objects.requireNonNull(id, "id is required");
        SalesOrder order = salesOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));

        if (order.getStatus() != OrderStatus.CREATED) {
            throw new BusinessRuleException("Only CREATED orders can be cancelled");
        }

        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            product.setStockQuantity(product.getStockQuantity() + item.getQuantity());

            // Release reserved stock in warehouse
            if (order.getWarehouse() != null) {
                productWarehouseStockRepository
                    .findByProductIdAndWarehouseId(item.getProduct().getId(), order.getWarehouse().getId())
                    .ifPresent(warehouseStock -> {
                        warehouseStock.release(item.getQuantity());
                        productWarehouseStockRepository.save(warehouseStock);
                    });
            }

            recordInventoryTransaction(
                    product,
                    order,
                    InventoryTransactionType.IN,
                    item.getQuantity(),
                    "ORDER_CANCELLED"
            );
        }

        order.setStatus(OrderStatus.CANCELLED);
        return toResponse(order, shipmentRepository.findByOrderId(order.getId()).orElse(null));
    }

    @Transactional
    public BulkOrderActionResponse bulkConfirm(List<Long> orderIds) {
        return bulkProcess(orderIds, this::confirm);
    }

    @Transactional
    public BulkOrderActionResponse bulkCancel(List<Long> orderIds) {
        return bulkProcess(orderIds, this::cancel);
    }

    @Transactional(readOnly = true)
    public OrderResponse getById(Long id) {
        Long orderId = Objects.requireNonNull(id, "id is required");
        SalesOrder order = salesOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
        return toResponse(order, shipmentRepository.findByOrderId(order.getId()).orElse(null));
    }

    @Transactional(readOnly = true)
    @SuppressWarnings("null")
    public PageResponse<OrderResponse> list(
            OrderStatus status,
            OrderFulfillmentStatus fulfillmentStatus,
            Long customerId,
            LocalDate from,
            LocalDate to,
            Pageable pageable) {
        Specification<SalesOrder> spec = buildSpecification(status, fulfillmentStatus, customerId, from, to);
        var pageResult = salesOrderRepository.findAll(spec, pageable);
        List<Long> orderIds = pageResult.getContent().stream().map(SalesOrder::getId).toList();
        Map<Long, Shipment> shipmentByOrderId;
        if (orderIds.isEmpty()) {
            shipmentByOrderId = Collections.emptyMap();
        } else {
            shipmentByOrderId = shipmentRepository.findAllByOrderIdIn(orderIds).stream()
                    .collect(Collectors.toMap(s -> s.getOrder().getId(), s -> s, (left, right) -> left, HashMap::new));
        }

        return PageResponse.from(pageResult.map(order -> toResponse(order, shipmentByOrderId.get(order.getId()))));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> listAll(
            OrderStatus status,
            OrderFulfillmentStatus fulfillmentStatus,
            Long customerId,
            LocalDate from,
            LocalDate to) {
        Specification<SalesOrder> spec = buildSpecification(status, fulfillmentStatus, customerId, from, to);
        List<SalesOrder> orders = salesOrderRepository.findAll(spec);
        List<Long> orderIds = orders.stream().map(SalesOrder::getId).toList();
        Map<Long, Shipment> shipmentByOrderId;
        if (orderIds.isEmpty()) {
            shipmentByOrderId = Collections.emptyMap();
        } else {
            shipmentByOrderId = shipmentRepository.findAllByOrderIdIn(orderIds).stream()
                    .collect(Collectors.toMap(s -> s.getOrder().getId(), s -> s, (left, right) -> left, HashMap::new));
        }
        return orders.stream()
                .map(order -> toResponse(order, shipmentByOrderId.get(order.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Long> listIds(
            OrderStatus status,
            OrderFulfillmentStatus fulfillmentStatus,
            Long customerId,
            LocalDate from,
            LocalDate to) {
        Specification<SalesOrder> spec = buildSpecification(status, fulfillmentStatus, customerId, from, to);
        return salesOrderRepository.findAll(spec).stream()
                .map(SalesOrder::getId)
                .toList();
    }

    private Specification<SalesOrder> buildSpecification(
            OrderStatus status,
            OrderFulfillmentStatus fulfillmentStatus,
            Long customerId,
            LocalDate from,
            LocalDate to) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (fulfillmentStatus != null) {
                predicates.add(buildFulfillmentPredicate(root, query, cb, fulfillmentStatus));
            }
            if (customerId != null) {
                predicates.add(cb.equal(root.get("customer").get("id"), customerId));
            }
            if (from != null) {
                Instant fromInstant = from.atStartOfDay().toInstant(ZoneOffset.UTC);
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), fromInstant));
            }
            if (to != null) {
                Instant toExclusive = to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
                predicates.add(cb.lessThan(root.get("createdAt"), toExclusive));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private String generateOrderNumber() {
        String date = java.time.LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        int random = ThreadLocalRandom.current().nextInt(10000, 99999);
        return "SO-" + date + "-" + random;
    }

    private BulkOrderActionResponse bulkProcess(List<Long> orderIds, Function<Long, OrderResponse> action) {
        List<Long> safeOrderIds = orderIds == null ? List.of() : orderIds;
        List<Long> failedOrderIds = new ArrayList<>();
        List<BulkOrderFailureDetail> failureDetails = new ArrayList<>();

        for (Long orderId : safeOrderIds) {
            try {
                action.apply(orderId);
            } catch (RuntimeException ex) {
                failedOrderIds.add(orderId);
                failureDetails.add(new BulkOrderFailureDetail(orderId, resolveOrderStatusLabel(orderId), ex.getMessage()));
            }
        }

        int requested = safeOrderIds.size();
        int failed = failedOrderIds.size();
        int succeeded = requested - failed;
        return new BulkOrderActionResponse(requested, succeeded, failed, failedOrderIds, failureDetails);
    }

    private String resolveOrderStatusLabel(Long orderId) {
        if (orderId == null) {
            return "UNKNOWN";
        }
        return salesOrderRepository.findById(orderId)
                .map(order -> order.getStatus().name())
                .orElse("NOT_FOUND");
    }

    private OrderResponse toResponse(SalesOrder order, Shipment shipment) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(item -> new OrderItemResponse(
                item.getId(),
                        item.getProduct().getId(),
                        item.getProduct().getSku(),
                        item.getProduct().getName(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getLineTotal()
                ))
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getCustomer().getId(),
                order.getCustomer().getCode(),
                order.getCustomer().getName(),
                order.getStatus(),
                resolveFulfillmentStatus(order.getStatus(), shipment),
                order.getTotalAmount(),
                order.getDiscountAmount(),
                order.getDiscount() != null ? order.getDiscount().getCode() : null,
                order.getCreatedAt(),
                itemResponses,
                order.getCreatedBy()
        );
    }

    private OrderFulfillmentStatus resolveFulfillmentStatus(OrderStatus orderStatus, Shipment shipment) {
        if (orderStatus == OrderStatus.CANCELLED) {
            return OrderFulfillmentStatus.CANCELLED;
        }
        if (shipment == null) {
            return OrderFulfillmentStatus.PENDING;
        }
        if (shipment.getStatus() == ShipmentStatus.SHIPPED) {
            return OrderFulfillmentStatus.SHIPPED;
        }
        if (shipment.getStatus() == ShipmentStatus.CANCELLED) {
            return OrderFulfillmentStatus.SHIPMENT_CANCELLED;
        }
        return OrderFulfillmentStatus.READY_TO_SHIP;
    }

    private Predicate buildFulfillmentPredicate(
            Root<SalesOrder> orderRoot,
            CriteriaQuery<?> query,
            jakarta.persistence.criteria.CriteriaBuilder cb,
            OrderFulfillmentStatus fulfillmentStatus
    ) {
        Subquery<Long> shipmentExistsQuery = query.subquery(Long.class);
        Root<Shipment> shipmentRoot = shipmentExistsQuery.from(Shipment.class);
        shipmentExistsQuery.select(shipmentRoot.get("id"));
        shipmentExistsQuery.where(cb.equal(shipmentRoot.get("order").get("id"), orderRoot.get("id")));

        if (fulfillmentStatus == OrderFulfillmentStatus.CANCELLED) {
            return cb.equal(orderRoot.get("status"), OrderStatus.CANCELLED);
        }

        if (fulfillmentStatus == OrderFulfillmentStatus.PENDING) {
            return cb.and(
                    cb.notEqual(orderRoot.get("status"), OrderStatus.CANCELLED),
                    cb.not(cb.exists(shipmentExistsQuery))
            );
        }

        Subquery<Long> shipmentWithStatusQuery = query.subquery(Long.class);
        Root<Shipment> shipmentWithStatusRoot = shipmentWithStatusQuery.from(Shipment.class);
        shipmentWithStatusQuery.select(shipmentWithStatusRoot.get("id"));

        ShipmentStatus targetStatus = switch (fulfillmentStatus) {
            case READY_TO_SHIP -> ShipmentStatus.CREATED;
            case SHIPPED -> ShipmentStatus.SHIPPED;
            case SHIPMENT_CANCELLED -> ShipmentStatus.CANCELLED;
            default -> null;
        };

        if (targetStatus == null) {
            return cb.conjunction();
        }

        shipmentWithStatusQuery.where(
                cb.equal(shipmentWithStatusRoot.get("order").get("id"), orderRoot.get("id")),
                cb.equal(shipmentWithStatusRoot.get("status"), targetStatus)
        );

        return cb.and(
                cb.notEqual(orderRoot.get("status"), OrderStatus.CANCELLED),
                cb.exists(shipmentWithStatusQuery)
        );
    }

    private void recordInventoryTransaction(
            Product product,
            SalesOrder order,
            InventoryTransactionType type,
            Integer quantity,
            String reason
    ) {
        InventoryTransaction tx = new InventoryTransaction();
        tx.setProduct(product);
        tx.setOrder(order);
        tx.setType(type);
        tx.setQuantity(quantity);
        tx.setReason(reason);
        inventoryTransactionRepository.save(tx);
    }
}
