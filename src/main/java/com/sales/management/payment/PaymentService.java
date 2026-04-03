package com.sales.management.payment;

import com.sales.management.common.exception.BusinessRuleException;
import com.sales.management.common.exception.DuplicateResourceException;
import com.sales.management.common.exception.ResourceNotFoundException;
import com.sales.management.invoice.Invoice;
import com.sales.management.invoice.InvoiceRepository;
import com.sales.management.order.OrderStatus;
import com.sales.management.order.SalesOrder;
import com.sales.management.order.SalesOrderRepository;
import com.sales.management.payment.dto.PaymentRequest;
import com.sales.management.payment.dto.PaymentResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final InvoiceRepository invoiceRepository;

    public PaymentService(
            PaymentRepository paymentRepository,
            SalesOrderRepository salesOrderRepository,
            InvoiceRepository invoiceRepository
    ) {
        this.paymentRepository = paymentRepository;
        this.salesOrderRepository = salesOrderRepository;
        this.invoiceRepository = invoiceRepository;
    }

    @Transactional
    public PaymentResponse pay(Long orderId, PaymentRequest request) {
        Long id = Objects.requireNonNull(orderId, "orderId is required");
        SalesOrder order = salesOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        if (paymentRepository.existsByOrderId(orderId)) {
            throw new DuplicateResourceException("Order already has a payment: " + order.getOrderNumber());
        }

        if (order.getStatus() != OrderStatus.CONFIRMED) {
            throw new BusinessRuleException("Only CONFIRMED orders can be paid. Current status: " + order.getStatus());
        }

        Payment payment = new Payment();
        payment.setPaymentNumber(generatePaymentNumber());
        payment.setOrder(order);
        payment.setAmount(order.getTotalAmount());
        payment.setVndAmount(order.getTotalVnd() != null ? order.getTotalVnd() : order.getTotalAmount());
        payment.setMethod(request.getMethod());
        payment.setNote(request.getNote());
        Payment savedPayment = paymentRepository.save(payment);

        order.setStatus(OrderStatus.PAID);

        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber(generateInvoiceNumber());
        invoice.setOrder(order);
        invoice.setPayment(savedPayment);
        invoice.setTotalAmount(order.getTotalAmount());
        invoiceRepository.save(invoice);

        return toResponse(savedPayment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getByOrderId(Long orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for order: " + orderId));
        return toResponse(payment);
    }

    @Transactional(readOnly = true)
    @SuppressWarnings("null")
    public PaymentResponse getById(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + id));
        return toResponse(payment);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> list() {
        return paymentRepository.findAll().stream().map(this::toResponse).toList();
    }

    private PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getPaymentNumber(),
                payment.getOrder().getId(),
                payment.getOrder().getOrderNumber(),
                payment.getAmount(),
                payment.getMethod(),
                payment.getNote(),
                payment.getPaidAt()
        );
    }

    private String generatePaymentNumber() {
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        int random = ThreadLocalRandom.current().nextInt(10000, 99999);
        return "PAY-" + date + "-" + random;
    }

    private String generateInvoiceNumber() {
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        int random = ThreadLocalRandom.current().nextInt(10000, 99999);
        return "INV-" + date + "-" + random;
    }
}
