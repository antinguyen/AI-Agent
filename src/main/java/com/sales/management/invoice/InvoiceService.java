package com.sales.management.invoice;

import com.sales.management.common.exception.ResourceNotFoundException;
import com.sales.management.invoice.dto.InvoiceResponse;
import com.sales.management.order.OrderItem;
import com.sales.management.order.dto.OrderItemResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;

    public InvoiceService(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getById(Long id) {
        Long invoiceId = Objects.requireNonNull(id, "id is required");
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + id));
        return toResponse(invoice);
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getByOrderId(Long orderId) {
        Invoice invoice = invoiceRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found for order: " + orderId));
        return toResponse(invoice);
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> list() {
        return invoiceRepository.findAll().stream().map(this::toResponse).toList();
    }

    private InvoiceResponse toResponse(Invoice invoice) {
        List<OrderItemResponse> items = invoice.getOrder().getItems().stream()
                .map(this::toItemResponse)
                .toList();

        return new InvoiceResponse(
                invoice.getId(),
                invoice.getInvoiceNumber(),
                invoice.getOrder().getId(),
                invoice.getOrder().getOrderNumber(),
                invoice.getOrder().getCustomer().getId(),
                invoice.getOrder().getCustomer().getCode(),
                invoice.getOrder().getCustomer().getName(),
                invoice.getTotalAmount(),
                invoice.getPayment().getMethod(),
                invoice.getIssuedAt(),
                items
        );
    }

    private OrderItemResponse toItemResponse(OrderItem item) {
        return new OrderItemResponse(
                item.getId(),
                item.getProduct().getId(),
                item.getProduct().getSku(),
                item.getProduct().getName(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getLineTotal()
        );
    }
}
