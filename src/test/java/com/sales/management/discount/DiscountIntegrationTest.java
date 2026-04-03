package com.sales.management.discount;

import com.sales.management.customer.Customer;
import com.sales.management.customer.CustomerRepository;
import com.sales.management.inventory.InventoryTransactionRepository;
import com.sales.management.invoice.InvoiceRepository;
import com.sales.management.order.SalesOrderRepository;
import com.sales.management.payment.PaymentRepository;
import com.sales.management.product.Product;
import com.sales.management.product.ProductRepository;
import com.sales.management.salesreturn.SalesReturnRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(roles = "ADMIN")
@SuppressWarnings("null")
class DiscountIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private DiscountRepository discountRepository;
    @Autowired private SalesOrderRepository salesOrderRepository;
    @Autowired private InventoryTransactionRepository inventoryTransactionRepository;
    @Autowired private InvoiceRepository invoiceRepository;
    @Autowired private PaymentRepository paymentRepository;
        @Autowired private SalesReturnRepository salesReturnRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private ProductRepository productRepository;

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
        customer.setCode("CUS-DISC-01");
        customer.setName("Discount Customer");
        customer.setPhone("0900000001");
        customer.setEmail("disc@example.com");
        customer.setActive(true);
        customerId = customerRepository.save(customer).getId();

        Product product = new Product();
        product.setSku("SP-DISC-01");
        product.setName("Discount Product");
        product.setPrice(new BigDecimal("500.00"));
        product.setStockQuantity(20);
        product.setActive(true);
        productId = productRepository.save(product).getId();
    }

    @Test
    void shouldCreateDiscountAndRetrieveByCode() throws Exception {
        String payload = """
                {"code":"SAVE10","type":"PERCENT","value":10}
                """;

        mockMvc.perform(post("/api/v1/discounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("SAVE10"))
                .andExpect(jsonPath("$.type").value("PERCENT"))
                .andExpect(jsonPath("$.active").value(true));

        mockMvc.perform(get("/api/v1/discounts/SAVE10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SAVE10"));
    }

    @Test
    void shouldRejectDuplicateDiscountCode() throws Exception {
        String payload = """
                {"code":"DUP10","type":"FIXED","value":50}
                """;
        mockMvc.perform(post("/api/v1/discounts")
                .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/discounts")
                .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
    }

    @Test
    void shouldListDiscounts() throws Exception {
        String p1 = """
                {"code":"LIST1","type":"FIXED","value":20}
                """;
        String p2 = """
                {"code":"LIST2","type":"PERCENT","value":5}
                """;
        mockMvc.perform(post("/api/v1/discounts").contentType(MediaType.APPLICATION_JSON).content(p1))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/discounts").contentType(MediaType.APPLICATION_JSON).content(p2))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/discounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void shouldDeactivateDiscount() throws Exception {
        mockMvc.perform(post("/api/v1/discounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"code":"DEACT","type":"FIXED","value":30}
                        """))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/v1/discounts/DEACT"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/discounts/DEACT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void shouldApplyPercentDiscountToOrder() throws Exception {
        // Create 10% discount
        mockMvc.perform(post("/api/v1/discounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"code":"PCTOFF","type":"PERCENT","value":10}
                        """))
                .andExpect(status().isCreated());

        // Create order with discount — product price 500, qty 2 = 1000, 10% off = 100, total = 900
        String orderPayload = """
                {
                  "customerId": %d,
                  "items": [{"productId": %d, "quantity": 2}],
                  "discountCode": "PCTOFF"
                }
                """.formatted(customerId, productId);

        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(orderPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalAmount").value(900.00))
                .andExpect(jsonPath("$.discountAmount").value(100.00))
                .andExpect(jsonPath("$.discountCode").value("PCTOFF"));
    }

    @Test
    void shouldApplyFixedDiscountToOrder() throws Exception {
        // Create fixed $50 discount
        mockMvc.perform(post("/api/v1/discounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"code":"FLAT50","type":"FIXED","value":50}
                        """))
                .andExpect(status().isCreated());

        // product 500 x 1 = 500, fixed 50 off = 450
        String orderPayload = """
                {
                  "customerId": %d,
                  "items": [{"productId": %d, "quantity": 1}],
                  "discountCode": "FLAT50"
                }
                """.formatted(customerId, productId);

        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(orderPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalAmount").value(450.00))
                .andExpect(jsonPath("$.discountAmount").value(50.00));
    }

    @Test
    void shouldRejectOrderWithInactiveDiscount() throws Exception {
        mockMvc.perform(post("/api/v1/discounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"code":"DEAD","type":"FIXED","value":10}
                        """))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/v1/discounts/DEAD"))
                .andExpect(status().isNoContent());

        String orderPayload = """
                {
                  "customerId": %d,
                  "items": [{"productId": %d, "quantity": 1}],
                  "discountCode": "DEAD"
                }
                """.formatted(customerId, productId);

        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(orderPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
    }

    @Test
    void shouldRejectOrderWithNonExistentDiscountCode() throws Exception {
        String orderPayload = """
                {
                  "customerId": %d,
                  "items": [{"productId": %d, "quantity": 1}],
                  "discountCode": "NOTEXIST"
                }
                """.formatted(customerId, productId);

        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(orderPayload))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectDiscountBelowMinOrderAmount() throws Exception {
        mockMvc.perform(post("/api/v1/discounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"code":"MIN1000","type":"FIXED","value":100,"minOrderAmount":1000}
                        """))
                .andExpect(status().isCreated());

        // product 500 x 1 = 500 < minOrderAmount 1000
        String orderPayload = """
                {
                  "customerId": %d,
                  "items": [{"productId": %d, "quantity": 1}],
                  "discountCode": "MIN1000"
                }
                """.formatted(customerId, productId);

        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(orderPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
    }

    @Test
    void shouldEnforceMaxDiscountAmountOnPercentDiscount() throws Exception {
        // 50% discount capped at 100
        mockMvc.perform(post("/api/v1/discounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"code":"CAP100","type":"PERCENT","value":50,"maxDiscountAmount":100}
                        """))
                .andExpect(status().isCreated());

        // product 500 x 1 = 500, 50% = 250 but capped at 100 → total = 400
        String orderPayload = """
                {
                  "customerId": %d,
                  "items": [{"productId": %d, "quantity": 1}],
                  "discountCode": "CAP100"
                }
                """.formatted(customerId, productId);

        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(orderPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.discountAmount").value(100.00))
                .andExpect(jsonPath("$.totalAmount").value(400.00));
    }

    @Test
    void shouldRejectCreatingDiscountAsStaff() throws Exception {
        // Override to STAFF role
        mockMvc.perform(post("/api/v1/discounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"code":"STAFFCODE","type":"FIXED","value":10}
                        """)
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                        .user("staff").roles("STAFF")))
                .andExpect(status().isForbidden());
    }
}


