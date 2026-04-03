package com.sales.management.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sales.management.customer.Customer;
import com.sales.management.customer.CustomerRepository;
import com.sales.management.inventory.InventoryTransactionRepository;
import com.sales.management.invoice.InvoiceRepository;
import com.sales.management.order.SalesOrderRepository;
import com.sales.management.product.Product;
import com.sales.management.product.ProductRepository;
import com.sales.management.salesreturn.SalesReturnRepository;
import com.sales.management.shipment.ShipmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(roles = "ADMIN")
@SuppressWarnings("null")
class PaymentInvoiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private SalesOrderRepository salesOrderRepository;

        @Autowired
        private ShipmentRepository shipmentRepository;

    @Autowired
    private InventoryTransactionRepository inventoryTransactionRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

        @Autowired
        private SalesReturnRepository salesReturnRepository;

    private Long customerId;
    private Long productId;

    @BeforeEach
    void setUp() {
        invoiceRepository.deleteAll();
        paymentRepository.deleteAll();
        shipmentRepository.deleteAll();
        inventoryTransactionRepository.deleteAll();
        salesReturnRepository.deleteAll();
        salesOrderRepository.deleteAll();
        customerRepository.deleteAll();
        productRepository.deleteAll();

        Customer customer = new Customer();
        customer.setCode("CUS-PAY-01");
        customer.setName("Pay Customer");
        customer.setPhone("0911111111");
        customer.setEmail("pay@example.com");
        customer.setActive(true);
        customerId = customerRepository.save(customer).getId();

        Product product = new Product();
        product.setSku("SP-PAY-01");
        product.setName("Pay Product");
        product.setPrice(new BigDecimal("250.00"));
        product.setStockQuantity(20);
        product.setActive(true);
        productId = productRepository.save(product).getId();
    }

    /** helper: tạo order và trả về orderId */
    private long createConfirmedOrder(int qty) throws Exception {
        String createPayload = """
                {
                  "customerId": %d,
                  "items": [{"productId": %d, "quantity": %d}]
                }
                """.formatted(customerId, productId, qty);

        String created = mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        long orderId = objectMapper.readTree(created).get("id").asLong();

        mockMvc.perform(post("/api/v1/orders/{id}/confirm", orderId))
                .andExpect(status().isOk());

        return orderId;
    }

    @Test
    void shouldPayConfirmedOrderAndCreateInvoice() throws Exception {
        long orderId = createConfirmedOrder(2); // total = 500.00

        String payPayload = """
                {"method": "CASH"}
                """;

        mockMvc.perform(post("/api/v1/orders/{orderId}/payments", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.amount").value(500.00))
                .andExpect(jsonPath("$.method").value("CASH"))
                .andExpect(jsonPath("$.paymentNumber").isString());

        // order status should now be PAID
        mockMvc.perform(get("/api/v1/orders/{id}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));

        // invoice should be auto-created
        mockMvc.perform(get("/api/v1/orders/{orderId}/invoice", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.totalAmount").value(500.00))
                .andExpect(jsonPath("$.paymentMethod").value("CASH"))
                .andExpect(jsonPath("$.invoiceNumber").isString())
                .andExpect(jsonPath("$.items").isArray());
    }

    @Test
    void shouldRejectPaymentForNonConfirmedOrder() throws Exception {
        // Create order but do NOT confirm it (status = CREATED)
        String createPayload = """
                {
                  "customerId": %d,
                  "items": [{"productId": %d, "quantity": 1}]
                }
                """.formatted(customerId, productId);

        String created = mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        long orderId = objectMapper.readTree(created).get("id").asLong();

        String payPayload = """
                {"method": "BANK_TRANSFER"}
                """;

        mockMvc.perform(post("/api/v1/orders/{orderId}/payments", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
    }

    @Test
    void shouldRejectDuplicatePayment() throws Exception {
        long orderId = createConfirmedOrder(1);

        String payPayload = """
                {"method": "CASH"}
                """;

        // First payment — should succeed
        mockMvc.perform(post("/api/v1/orders/{orderId}/payments", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payPayload))
                .andExpect(status().isCreated());

        // Second payment — should be rejected
        mockMvc.perform(post("/api/v1/orders/{orderId}/payments", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payPayload))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldGetPaymentById() throws Exception {
        long orderId = createConfirmedOrder(1);

        String payPayload = """
                {"method": "CARD", "note": "Thanh toán thẻ VISA"}
                """;

        String payResponse = mockMvc.perform(post("/api/v1/orders/{orderId}/payments", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payPayload))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        JsonNode payNode = objectMapper.readTree(payResponse);
        long paymentId = payNode.get("id").asLong();

        mockMvc.perform(get("/api/v1/payments/{id}", paymentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(paymentId))
                .andExpect(jsonPath("$.method").value("CARD"))
                .andExpect(jsonPath("$.note").value("Thanh toán thẻ VISA"));
    }

    @Test
    void shouldGetInvoiceById() throws Exception {
        long orderId = createConfirmedOrder(3); // total = 750.00

        mockMvc.perform(post("/api/v1/orders/{orderId}/payments", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"method": "BANK_TRANSFER"}
                                """))
                .andExpect(status().isCreated());

        String invoiceResponse = mockMvc.perform(get("/api/v1/orders/{orderId}/invoice", orderId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode invoiceNode = objectMapper.readTree(invoiceResponse);
        long invoiceId = invoiceNode.get("id").asLong();

        mockMvc.perform(get("/api/v1/invoices/{id}", invoiceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(invoiceId))
                .andExpect(jsonPath("$.totalAmount").value(750.00))
                .andExpect(jsonPath("$.paymentMethod").value("BANK_TRANSFER"))
                .andExpect(jsonPath("$.customerName").value("Pay Customer"));
    }

    @Test
    void shouldListAllPaymentsAndInvoices() throws Exception {
        long orderId1 = createConfirmedOrder(1);
        long orderId2 = createConfirmedOrder(2);

        mockMvc.perform(post("/api/v1/orders/{orderId}/payments", orderId1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"method": "CASH"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/orders/{orderId}/payments", orderId2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"method": "CARD"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/payments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(get("/api/v1/invoices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void shouldRejectPaymentWithMissingMethod() throws Exception {
        long orderId = createConfirmedOrder(1);

        mockMvc.perform(post("/api/v1/orders/{orderId}/payments", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
