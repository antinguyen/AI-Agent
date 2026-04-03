package com.sales.management.notification;

import com.sales.management.customer.Customer;
import com.sales.management.customer.CustomerRepository;
import com.sales.management.discount.DiscountRepository;
import com.sales.management.inventory.InventoryTransactionRepository;
import com.sales.management.invoice.InvoiceRepository;
import com.sales.management.order.SalesOrderRepository;
import com.sales.management.payment.PaymentRepository;
import com.sales.management.product.Product;
import com.sales.management.product.ProductRepository;
import com.sales.management.salesreturn.SalesReturnRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.mail.enabled=true")
@AutoConfigureMockMvc
@WithMockUser(roles = "ADMIN")
@SuppressWarnings("null")
class EmailNotificationIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private SalesOrderRepository salesOrderRepository;
    @Autowired private InventoryTransactionRepository inventoryTransactionRepository;
    @Autowired private DiscountRepository discountRepository;
    @Autowired private InvoiceRepository invoiceRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private SalesReturnRepository salesReturnRepository;

    @MockitoBean
    private JavaMailSender mailSender;

    private Long customerId;
    private Long productId;

    @BeforeEach
    void setUp() {
        invoiceRepository.deleteAll();
        paymentRepository.deleteAll();
        inventoryTransactionRepository.deleteAll();
        salesReturnRepository.deleteAll();
        salesOrderRepository.deleteAll();
        discountRepository.deleteAll();
        customerRepository.deleteAll();
        productRepository.deleteAll();

        Customer customer = new Customer();
        customer.setCode("CUS-EMAIL-01");
        customer.setName("Email Customer");
        customer.setPhone("0900111222");
        customer.setEmail("customer@example.com");
        customer.setActive(true);
        customerId = customerRepository.save(customer).getId();

        Product product = new Product();
        product.setSku("SP-EMAIL-01");
        product.setName("Email Product");
        product.setPrice(new BigDecimal("200.00"));
        product.setStockQuantity(100);
        product.setLowStockThreshold(5);
        product.setActive(true);
        productId = productRepository.save(product).getId();
    }

    @Test
    void shouldSendEmailWhenOrderCreated() throws Exception {
        String payload = """
                {
                  "customerId": %d,
                  "items": [{"productId": %d, "quantity": 1}]
                }
                """.formatted(customerId, productId);

        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isCreated());

        // Wait briefly for @Async to complete
        Thread.sleep(200);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(captor.capture());

        SimpleMailMessage sent = captor.getValue();
        assertThat(sent.getTo()).containsExactly("customer@example.com");
        assertThat(sent.getSubject()).contains("Order Confirmation");
    }

    @Test
    void shouldNotSendEmailWhenCustomerHasNoEmail() throws Exception {
        // Create customer without email
        Customer noEmailCustomer = new Customer();
        noEmailCustomer.setCode("CUS-NOEMAIL");
        noEmailCustomer.setName("No Email Customer");
        noEmailCustomer.setPhone("0900333444");
        noEmailCustomer.setActive(true);
        Long noEmailId = customerRepository.save(noEmailCustomer).getId();

        String payload = """
                {
                  "customerId": %d,
                  "items": [{"productId": %d, "quantity": 1}]
                }
                """.formatted(noEmailId, productId);

        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isCreated());

        Thread.sleep(200);
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }
}
