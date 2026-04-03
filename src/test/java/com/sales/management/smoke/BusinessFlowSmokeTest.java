package com.sales.management.smoke;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sales.management.auth.UserRepository;
import com.sales.management.customer.CustomerRepository;
import com.sales.management.inventory.InventoryTransactionRepository;
import com.sales.management.invoice.InvoiceRepository;
import com.sales.management.order.SalesOrderRepository;
import com.sales.management.payment.PaymentRepository;
import com.sales.management.preference.UserPreferenceRepository;
import com.sales.management.product.ProductRepository;
import com.sales.management.salesreturn.SalesReturnRepository;
import com.sales.management.shipment.ShipmentRepository;
import com.sales.management.warehouse.ProductWarehouseStockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end smoke test covering the critical business flow:
 * Register → Login → Create product/customer → Place order
 * → Confirm order → Pay order → Return item → Verify low-stock alert
 */
@SpringBootTest
@AutoConfigureMockMvc
@SuppressWarnings("null")
class BusinessFlowSmokeTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Autowired private UserRepository userRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private SalesReturnRepository salesReturnRepository;
        @Autowired private ShipmentRepository shipmentRepository;
    @Autowired private SalesOrderRepository salesOrderRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private InvoiceRepository invoiceRepository;
    @Autowired private InventoryTransactionRepository inventoryTransactionRepository;
        @Autowired private ProductWarehouseStockRepository productWarehouseStockRepository;
        @Autowired private UserPreferenceRepository userPreferenceRepository;

    @BeforeEach
    void cleanDatabase() {
                salesReturnRepository.deleteAll();
                shipmentRepository.deleteAll();
        invoiceRepository.deleteAll();
        paymentRepository.deleteAll();
        inventoryTransactionRepository.deleteAll();
        salesOrderRepository.deleteAll();
        productWarehouseStockRepository.deleteAll();
        customerRepository.deleteAll();
        productRepository.deleteAll();
                userPreferenceRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private String registerAndLogin(String username, String password) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s","role":"ADMIN"}
                                """.formatted(username, password)))
                .andExpect(status().isCreated());

        String loginJson = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return "Bearer " + objectMapper.readTree(loginJson).get("token").asText();
    }

    private long createCustomer(String auth) throws Exception {
        String json = mockMvc.perform(post("/api/v1/customers")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"CUS-SMOKE","name":"Smoke Customer",
                                 "phone":"0900000001","email":"smoke@test.com","active":true}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("id").asLong();
    }

    /**
     * Creates a product with stockQuantity=3 and lowStockThreshold=5,
     * so after buying 2 units the remaining stock (1) is below threshold.
     */
    private long createProduct(String auth) throws Exception {
        String json = mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sku":"SKU-SMOKE","name":"Smoke Product","price":200.00,
                                 "stockQuantity":3,"lowStockThreshold":5,"active":true}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("id").asLong();
    }

    private long placeAndConfirmOrder(String auth, long customerId, long productId) throws Exception {
        // Place order (2 units of the product)
        String orderJson = mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerId":%d,"items":[{"productId":%d,"quantity":2}]}
                                """.formatted(customerId, productId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalAmount").value(400.00))
                .andReturn().getResponse().getContentAsString();

        JsonNode order = objectMapper.readTree(orderJson);
        long orderId = order.get("id").asLong();

        // Confirm the order
        mockMvc.perform(post("/api/v1/orders/{id}/confirm", orderId)
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        return orderId;
    }

    // ── test ─────────────────────────────────────────────────────────────────

    @Test
        void fullBusinessFlow_registerLoginOrderPayReturnLowStock() throws Exception {
        // 1. Register and login – get real JWT
        String auth = registerAndLogin("smoke_admin", "SmokePwd99!");

        // 2. Verify token works on a protected endpoint
        mockMvc.perform(get("/api/v1/products").header("Authorization", auth))
                .andExpect(status().isOk());

        // 3. Create master data
        long customerId = createCustomer(auth);
        long productId  = createProduct(auth);

        // 4. Place + confirm order
        long orderId = placeAndConfirmOrder(auth, customerId, productId);

        // 5. Pay the order
        mockMvc.perform(post("/api/v1/orders/{orderId}/payments", orderId)
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"method":"CASH"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.amount").value(400.00))
                .andExpect(jsonPath("$.method").value("CASH"));

        // 6. Order status should be PAID
        mockMvc.perform(get("/api/v1/orders/{id}", orderId)
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));

        // 7. Invoice must exist
        mockMvc.perform(get("/api/v1/orders/{orderId}/invoice", orderId)
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount").value(400.00))
                .andExpect(jsonPath("$.paymentMethod").value("CASH"));

        // 8. Order detail must expose orderItemId so the return flow can be driven from the UI
        String orderDetailJson = mockMvc.perform(get("/api/v1/orders/{id}", orderId)
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].orderItemId").isNumber())
                .andReturn().getResponse().getContentAsString();

        long orderItemId = objectMapper.readTree(orderDetailJson)
                .path("items")
                .get(0)
                .path("orderItemId")
                .asLong();

        // 9. Return one unit using the exposed orderItemId
        mockMvc.perform(post("/api/v1/returns")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderId":%d,"reason":"Smoke return","items":[{"orderItemId":%d,"quantity":1}]}
                                """.formatted(orderId, orderItemId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.totalRefund").value(200.00))
                .andExpect(jsonPath("$.items[0].orderItemId").value(orderItemId));

        // 10. Order status should switch to RETURNED after the return is recorded
        mockMvc.perform(get("/api/v1/orders/{id}", orderId)
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RETURNED"));

        // 11. Remaining stock (3 - 2 + 1 = 2) is still below threshold (5) → low-stock alert remains visible
        mockMvc.perform(get("/api/v1/products/low-stock")
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == %d)].sku".formatted(productId)).value("SKU-SMOKE"));

        mockMvc.perform(get("/api/v1/products/{id}", productId)
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockQuantity").value(2));
    }
}
