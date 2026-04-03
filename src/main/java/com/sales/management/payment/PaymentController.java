package com.sales.management.payment;

import com.sales.management.payment.dto.PaymentRequest;
import com.sales.management.payment.dto.PaymentResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/api/v1/orders/{orderId}/payments")
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse pay(
            @PathVariable("orderId") Long orderId,
            @Valid @RequestBody PaymentRequest request) {
        return paymentService.pay(orderId, request);
    }

    @GetMapping("/api/v1/orders/{orderId}/payments")
    public PaymentResponse getByOrderId(@PathVariable("orderId") Long orderId) {
        return paymentService.getByOrderId(orderId);
    }

    @GetMapping("/api/v1/payments")
    public List<PaymentResponse> list() {
        return paymentService.list();
    }

    @GetMapping("/api/v1/payments/{id}")
    public PaymentResponse getById(@PathVariable("id") Long id) {
        return paymentService.getById(id);
    }
}
