package com.sales.management.notification;

import com.sales.management.order.dto.OrderResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final boolean mailEnabled;

    public EmailService(JavaMailSender mailSender,
                        @Value("${app.mail.from:noreply@salesapp.com}") String fromAddress,
                        @Value("${app.mail.enabled:true}") boolean mailEnabled) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.mailEnabled = mailEnabled;
    }

    /**
     * Sends an order confirmation email asynchronously.
     * Failures are logged but not propagated — email is best-effort.
     */
    @Async
    public void sendOrderConfirmation(String toEmail, OrderResponse order) {
        String subject = "Order Confirmation — " + order.orderNumber();
        String body = """
                Dear Customer,

                Your order has been placed successfully.

                Order Number : %s
                Total Amount : %.2f
                %s
                Status       : %s

                Thank you for your purchase!

                Sales Management System
                """.formatted(
                order.orderNumber(),
                order.totalAmount().doubleValue(),
                order.discountCode() != null
                        ? "Discount Applied: " + order.discountCode()
                          + " (-" + order.discountAmount() + ")\n"
                        : "",
                order.status());

        sendEmail(toEmail, subject, body);
    }

    /**
     * Sends an order confirmed (CONFIRMED status) notification asynchronously.
     */
    @Async
    public void sendOrderConfirmed(String toEmail, OrderResponse order) {
        String subject = "Order Confirmed — " + order.orderNumber();
        String body = """
                Dear Customer,

                Your order %s has been confirmed and is being processed.

                Thank you!

                Sales Management System
                """.formatted(order.orderNumber());

        sendEmail(toEmail, subject, body);
    }

    /**
     * Sends a low stock alert to the admin email asynchronously.
     */
    @Async
    public void sendLowStockAlert(String adminEmail, String productSku, String productName, int currentStock, int threshold) {
        String subject = "Low Stock Alert — " + productSku;
        String body = """
                Low Stock Alert

                Product  : %s (%s)
                Current  : %d units
                Threshold: %d units

                Please restock as soon as possible.

                Sales Management System
                """.formatted(productName, productSku, currentStock, threshold);

        sendEmail(adminEmail, subject, body);
    }

    private void sendEmail(String to, String subject, String body) {
        if (!mailEnabled) {
            log.debug("Skipping email to {} because app.mail.enabled=false", to);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Email sent to {} — subject: {}", to, subject);
        } catch (MailException ex) {
            log.warn("Failed to send email to {} — {}", to, ex.getMessage());
        }
    }
}
