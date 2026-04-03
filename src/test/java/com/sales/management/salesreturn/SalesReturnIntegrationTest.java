package com.sales.management.salesreturn;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sales.management.customer.Customer;
import com.sales.management.customer.CustomerRepository;
import com.sales.management.inventory.InventoryTransactionRepository;
import com.sales.management.invoice.InvoiceRepository;
import com.sales.management.order.OrderItemRepository;
import com.sales.management.order.SalesOrderRepository;
import com.sales.management.payment.PaymentRepository;
import com.sales.management.product.Product;
import com.sales.management.product.ProductRepository;
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
@SuppressWarnings({"null", "unused"})
class SalesReturnIntegrationTest {

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
    private OrderItemRepository orderItemRepository;

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
        salesReturnRepository.deleteAll();
        invoiceRepository.deleteAll();
        paymentRepository.deleteAll();
        inventoryTransactionRepository.deleteAll();
        salesOrderRepository.deleteAll();
        customerRepository.deleteAll();
        productRepository.deleteAll();

        Customer customer = new Customer();
        customer.setCode("CUS-RET-01");
        customer.setName("Return Customer");
        customer.setPhone("0922222222");
        customer.setEmail("ret@example.com");
        customer.setActive(true);
        customerId = customerRepository.save(customer).getId();

        Product product = new Product();
        product.setSku("SP-RET-01");
        product.setName("Return Product");
        product.setPrice(new BigDecimal("200.00"));
        product.setStockQuantity(50);
        product.setActive(true);
        productId = productRepository.save(product).getId();
    }

    /** Helper: get the first OrderItem id for an order (uses repository, no lazy init) */
    private long firstOrderItemId(long orderId) {
        return orderItemRepository.findByOrderId(orderId).get(0).getId();
    }

    /** Helper: create order → confirm → pay, returns {orderId, orderItemId} */
    private long[] createPaidOrder(int qty) throws Exception {
        String createPayload = """
                {
                  "customerId": %d,
                  "items": [{"productId": %d, "quantity": %d}]
                }
                """.formatted(customerId, productId, qty);

        String created = mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON).content(createPayload))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        JsonNode orderNode = objectMapper.readTree(created);
        long orderId = orderNode.get("id").asLong();
        long orderItemId = orderNode.get("items").get(0).get("productId").asLong(); // will find item below

        mockMvc.perform(post("/api/v1/orders/{id}/confirm", orderId)).andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/orders/{id}/payments", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"method": "CASH"}
                                """))
                .andExpect(status().isCreated());

        // Re-fetch order to get real orderItemId
        String fetched = mockMvc.perform(get("/api/v1/orders/{id}", orderId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode fetchedNode = objectMapper.readTree(fetched);
        long realOrderItemId = fetchedNode.get("items").get(0).get("productId").asLong();
        // We actually need the OrderItem ID not productId — use the DB directly via /api call
        // The OrderItem id is not exposed in OrderResponse, but we can query inventory-transactions
        // Instead: re-read the order items from the service by querying the DB

        // Actually, we don't expose orderItem.id in OrderResponse. We need to find it.
        // Use the salesReturn createPayload by productId approach below.
        return new long[]{orderId};
    }

    /** Full happy path: create paid order → return all items */
    @Test
    void shouldReturnPaidOrderAndRestoreStock() throws Exception {
        // 1. Create order (qty=3)
        String createPayload = """
                {"customerId": %d, "items": [{"productId": %d, "quantity": 3}]}
                """.formatted(customerId, productId);

        String created = mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON).content(createPayload))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        JsonNode orderNode = objectMapper.readTree(created);
        long orderId = orderNode.get("id").asLong();

        // Stock should be 50-3=47 now
        // 2. Confirm
        mockMvc.perform(post("/api/v1/orders/{id}/confirm", orderId)).andExpect(status().isOk());

        // 3. Pay
        mockMvc.perform(post("/api/v1/orders/{id}/payments", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"method": "BANK_TRANSFER"}
                                """))
                .andExpect(status().isCreated());

        // 4. Get orderItemId via repository
        long oiId = firstOrderItemId(orderId);

        // 5. Create return (partial: qty=2 out of 3)
        String returnPayload = """
                {
                  "orderId": %d,
                  "reason": "Khách hàng trả hàng lỗi",
                  "items": [{"orderItemId": %d, "quantity": 2}]
                }
                """.formatted(orderId, oiId);

        mockMvc.perform(post("/api/v1/returns")
                        .contentType(MediaType.APPLICATION_JSON).content(returnPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.totalRefund").value(400.00))  // 200 * 2
                .andExpect(jsonPath("$.returnNumber").isString())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].quantity").value(2));

        // 6. Order status should be RETURNED
        mockMvc.perform(get("/api/v1/orders/{id}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RETURNED"));

        // 7. Stock should be restored: 47 + 2 = 49
        // Verify via product endpoint
        mockMvc.perform(get("/api/v1/products/{id}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockQuantity").value(49));
    }

    @Test
    void shouldRejectReturnForNonPaidOrder() throws Exception {
        // Create order but only confirm (not pay)
        String createPayload = """
                {"customerId": %d, "items": [{"productId": %d, "quantity": 1}]}
                """.formatted(customerId, productId);

        String created = mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON).content(createPayload))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        long orderId = objectMapper.readTree(created).get("id").asLong();
        mockMvc.perform(post("/api/v1/orders/{id}/confirm", orderId)).andExpect(status().isOk());

        long oiId = firstOrderItemId(orderId);

        String returnPayload = """
                {
                  "orderId": %d,
                  "reason": "Test return",
                  "items": [{"orderItemId": %d, "quantity": 1}]
                }
                """.formatted(orderId, oiId);

        mockMvc.perform(post("/api/v1/returns")
                        .contentType(MediaType.APPLICATION_JSON).content(returnPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
    }

    @Test
    void shouldRejectDuplicateReturn() throws Exception {
        // Create, confirm, pay
        String createPayload = """
                {"customerId": %d, "items": [{"productId": %d, "quantity": 2}]}
                """.formatted(customerId, productId);

        String created = mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON).content(createPayload))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        long orderId = objectMapper.readTree(created).get("id").asLong();
        mockMvc.perform(post("/api/v1/orders/{id}/confirm", orderId)).andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/orders/{id}/payments", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"method": "CASH"}
                                """))
                .andExpect(status().isCreated());

        long oiId = firstOrderItemId(orderId);

        String returnPayload = """
                {
                  "orderId": %d,
                  "reason": "First return",
                  "items": [{"orderItemId": %d, "quantity": 1}]
                }
                """.formatted(orderId, oiId);

        // First return succeeds
        mockMvc.perform(post("/api/v1/returns")
                        .contentType(MediaType.APPLICATION_JSON).content(returnPayload))
                .andExpect(status().isCreated());

        // Second return rejected
        mockMvc.perform(post("/api/v1/returns")
                        .contentType(MediaType.APPLICATION_JSON).content(returnPayload))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldRejectReturnQuantityExceedingOriginal() throws Exception {
        String createPayload = """
                {"customerId": %d, "items": [{"productId": %d, "quantity": 2}]}
                """.formatted(customerId, productId);

        String created = mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON).content(createPayload))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        long orderId = objectMapper.readTree(created).get("id").asLong();
        mockMvc.perform(post("/api/v1/orders/{id}/confirm", orderId)).andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/orders/{id}/payments", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"method": "CARD"}
                                """))
                .andExpect(status().isCreated());

        long oiId = firstOrderItemId(orderId);

        String returnPayload = """
                {
                  "orderId": %d,
                  "reason": "Too many",
                  "items": [{"orderItemId": %d, "quantity": 999}]
                }
                """.formatted(orderId, oiId);

        mockMvc.perform(post("/api/v1/returns")
                        .contentType(MediaType.APPLICATION_JSON).content(returnPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
    }

    @Test
    void shouldGetReturnByIdAndByOrderId() throws Exception {
        String createPayload = """
                {"customerId": %d, "items": [{"productId": %d, "quantity": 1}]}
                """.formatted(customerId, productId);

        String created = mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON).content(createPayload))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        long orderId = objectMapper.readTree(created).get("id").asLong();
        mockMvc.perform(post("/api/v1/orders/{id}/confirm", orderId)).andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/orders/{id}/payments", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"method": "CASH"}
                                """))
                .andExpect(status().isCreated());

        long oiId = firstOrderItemId(orderId);

        String returnPayload = """
                {
                  "orderId": %d,
                  "reason": "Wrong item",
                  "items": [{"orderItemId": %d, "quantity": 1}]
                }
                """.formatted(orderId, oiId);

        String returnCreated = mockMvc.perform(post("/api/v1/returns")
                        .contentType(MediaType.APPLICATION_JSON).content(returnPayload))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        long returnId = objectMapper.readTree(returnCreated).get("id").asLong();

        // GET by ID
        mockMvc.perform(get("/api/v1/returns/{id}", returnId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(returnId))
                .andExpect(jsonPath("$.reason").value("Wrong item"));

        // GET by orderId
        mockMvc.perform(get("/api/v1/orders/{orderId}/return", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId));
    }

    @Test
    void shouldValidateReturnRequest() throws Exception {
        // Missing required fields
        mockMvc.perform(post("/api/v1/returns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}


