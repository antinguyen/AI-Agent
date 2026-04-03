package com.sales.management.order;

import com.sales.management.customer.Customer;
import com.sales.management.customer.CustomerRepository;
import com.sales.management.product.Product;
import com.sales.management.product.ProductRepository;
import com.sales.management.salesreturn.SalesReturnRepository;
import com.sales.management.inventory.InventoryTransactionRepository;
import com.sales.management.invoice.InvoiceRepository;
import com.sales.management.payment.PaymentRepository;
import com.sales.management.shipment.ShipmentRepository;
import com.sales.management.warehouse.Warehouse;
import com.sales.management.warehouse.WarehouseRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Objects;

import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(roles = "ADMIN")
@SuppressWarnings("null")
class SalesOrderIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ProductRepository productRepository;

  @Autowired
  private InventoryTransactionRepository inventoryTransactionRepository;

  @Autowired
  private SalesOrderRepository salesOrderRepository;

  @Autowired
  private InvoiceRepository invoiceRepository;

  @Autowired
  private PaymentRepository paymentRepository;

  @Autowired
  private ShipmentRepository shipmentRepository;

  @Autowired
  private WarehouseRepository warehouseRepository;

  @Autowired
  private SalesReturnRepository salesReturnRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Long customerId;
    private Long productId;
    private Long secondProductId;

    @BeforeEach
    void setUp() {
        invoiceRepository.deleteAll();
        paymentRepository.deleteAll();
        shipmentRepository.deleteAll();
        inventoryTransactionRepository.deleteAll();
        salesReturnRepository.deleteAll();
        salesOrderRepository.deleteAll();
        warehouseRepository.deleteAll();
        customerRepository.deleteAll();
        productRepository.deleteAll();

        Customer customer = new Customer();
        customer.setCode("CUS-001");
        customer.setName("Customer A");
        customer.setPhone("0900000000");
        customer.setEmail("cus-a@example.com");
        customer.setActive(true);
        customerId = customerRepository.save(customer).getId();

        Product product = new Product();
        product.setSku("SP-ORDER-01");
        product.setName("Order Product");
        product.setPrice(new BigDecimal("100.00"));
        product.setStockQuantity(10);
        product.setActive(true);
        productId = productRepository.save(product).getId();

        Product secondProduct = new Product();
        secondProduct.setSku("SP-ORDER-02");
        secondProduct.setName("Order Product 2");
        secondProduct.setPrice(new BigDecimal("50.00"));
        secondProduct.setStockQuantity(20);
        secondProduct.setActive(true);
        secondProductId = productRepository.save(secondProduct).getId();
    }

    @Test
    void shouldCreateOrderAndReduceStock() throws Exception {
        String payload = """
                {
                  "customerId": %d,
                  "items": [
                    {"productId": %d, "quantity": 2}
                  ]
                }
                """.formatted(customerId, productId);

        mockMvc.perform(post("/api/v1/orders")
            .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
            .content(Objects.requireNonNull(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerId").value(customerId))
            .andExpect(jsonPath("$.totalAmount").value(200.00))
            .andExpect(jsonPath("$.fulfillmentStatus").value("PENDING"));
    }

    @Test
    void shouldCreateOrderWithMultipleProducts() throws Exception {
        String payload = """
                {
                  "customerId": %d,
                  "items": [
                    {"productId": %d, "quantity": 2},
                    {"productId": %d, "quantity": 3}
                  ]
                }
                """.formatted(customerId, productId, secondProductId);

        mockMvc.perform(post("/api/v1/orders")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .content(Objects.requireNonNull(payload)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.customerId").value(customerId))
            .andExpect(jsonPath("$.items.length()").value(2))
            .andExpect(jsonPath("$.totalAmount").value(350.00));

        mockMvc.perform(get("/api/v1/products/{id}", productId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.stockQuantity").value(8));

        mockMvc.perform(get("/api/v1/products/{id}", secondProductId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.stockQuantity").value(17));
    }

    @Test
    void shouldAggregateDuplicateProductLinesOnCreate() throws Exception {
        String payload = """
                {
                  "customerId": %d,
                  "items": [
                    {"productId": %d, "quantity": 2},
                    {"productId": %d, "quantity": 3}
                  ]
                }
                """.formatted(customerId, productId, productId);

        mockMvc.perform(post("/api/v1/orders")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .content(Objects.requireNonNull(payload)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].productId").value(productId))
            .andExpect(jsonPath("$.items[0].quantity").value(5))
            .andExpect(jsonPath("$.totalAmount").value(500.00));

        mockMvc.perform(get("/api/v1/products/{id}", productId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.stockQuantity").value(5));
    }

    @Test
    void shouldRejectOrderWhenStockInsufficient() throws Exception {
        String payload = """
                {
                  "customerId": %d,
                  "items": [
                    {"productId": %d, "quantity": 999}
                  ]
                }
                """.formatted(customerId, productId);

        mockMvc.perform(post("/api/v1/orders")
            .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
            .content(Objects.requireNonNull(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
    }

    @Test
    void shouldCreateOrderWithoutWarehouseEvenWhenDefaultWarehouseExists() throws Exception {
        Warehouse defaultWarehouse = new Warehouse();
        defaultWarehouse.setCode("WH-DEFAULT");
        defaultWarehouse.setName("Default Warehouse");
        defaultWarehouse.setAddress("Default");
        defaultWarehouse.setActive(true);
        warehouseRepository.save(defaultWarehouse);

        String payload = """
                {
                  "customerId": %d,
                  "items": [
                    {"productId": %d, "quantity": 1}
                  ]
                }
                """.formatted(customerId, productId);

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content(Objects.requireNonNull(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerId").value(customerId))
                .andExpect(jsonPath("$.items[0].productId").value(productId));
    }

                @Test
                void shouldConfirmCreatedOrder() throws Exception {
              String createPayload = """
                {
                  "customerId": %d,
                  "items": [
                    {"productId": %d, "quantity": 1}
                  ]
                }
                """.formatted(customerId, productId);

              String response = mockMvc.perform(post("/api/v1/orders")
                  .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                  .content(Objects.requireNonNull(createPayload)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

              JsonNode jsonNode = objectMapper.readTree(response);
              long orderId = jsonNode.get("id").asLong();

              mockMvc.perform(post("/api/v1/orders/{id}/confirm", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
                }

                @Test
                void shouldCancelCreatedOrderAndRestoreStock() throws Exception {
              String createPayload = """
                {
                  "customerId": %d,
                  "items": [
                    {"productId": %d, "quantity": 3}
                  ]
                }
                """.formatted(customerId, productId);

              String response = mockMvc.perform(post("/api/v1/orders")
                  .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                  .content(Objects.requireNonNull(createPayload)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

              JsonNode jsonNode = objectMapper.readTree(response);
              long orderId = jsonNode.get("id").asLong();

              mockMvc.perform(post("/api/v1/orders/{id}/cancel", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.fulfillmentStatus").value("CANCELLED"));

              mockMvc.perform(get("/api/v1/products/{id}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockQuantity").value(10));
                }

                @Test
                void shouldListInventoryTransactionsByOrderId() throws Exception {
              String createPayload = """
                {
                  "customerId": %d,
                  "items": [
                    {"productId": %d, "quantity": 2}
                  ]
                }
                """.formatted(customerId, productId);

              String response = mockMvc.perform(post("/api/v1/orders")
                  .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                  .content(Objects.requireNonNull(createPayload)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

              JsonNode jsonNode = objectMapper.readTree(response);
              long orderId = jsonNode.get("id").asLong();

              mockMvc.perform(get("/api/v1/inventory-transactions").param("orderId", String.valueOf(orderId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].reason").value("ORDER_CREATED"));
                }

                @Test
                void shouldFilterOrdersByFulfillmentStatus() throws Exception {
              long pendingOrderId = createOrder(1);

              long readyOrderId = createOrder(1);
              confirmOrder(readyOrderId);
              createShipment(readyOrderId);

              long shippedOrderId = createOrder(1);
              confirmOrder(shippedOrderId);
              long shippedShipmentId = createShipment(shippedOrderId);
              markShipmentShipped(shippedShipmentId);

              long cancelledOrderId = createOrder(1);
              cancelOrder(cancelledOrderId);

              mockMvc.perform(get("/api/v1/orders").param("size", "50").param("fulfillmentStatus", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id==%d)]", pendingOrderId).exists())
                .andExpect(jsonPath("$.content[?(@.id==%d)]", readyOrderId).doesNotExist());

              mockMvc.perform(get("/api/v1/orders").param("size", "50").param("fulfillmentStatus", "READY_TO_SHIP"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id==%d)]", readyOrderId).exists())
                .andExpect(jsonPath("$.content[?(@.id==%d)]", shippedOrderId).doesNotExist());

              mockMvc.perform(get("/api/v1/orders").param("size", "50").param("fulfillmentStatus", "SHIPPED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id==%d)]", shippedOrderId).exists())
                .andExpect(jsonPath("$.content[?(@.id==%d)]", cancelledOrderId).doesNotExist());

              mockMvc.perform(get("/api/v1/orders").param("size", "50").param("fulfillmentStatus", "CANCELLED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id==%d)]", cancelledOrderId).exists())
                .andExpect(jsonPath("$.content[?(@.id==%d)]", shippedOrderId).doesNotExist());
                }

                @Test
                void shouldListOrderIdsByStatusFilter() throws Exception {
              long createdOrderId = createOrder(1);
              long confirmedOrderId = createOrder(1);
              confirmOrder(confirmedOrderId);

              mockMvc.perform(get("/api/v1/orders/ids").param("status", "CREATED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@==%d)]", createdOrderId).exists())
                .andExpect(jsonPath("$[?(@==%d)]", confirmedOrderId).doesNotExist());
                }

                @Test
                void shouldBulkConfirmOrdersWithPartialFailure() throws Exception {
              long createdOrderId = createOrder(1);
              long confirmedOrderId = createOrder(1);
              confirmOrder(confirmedOrderId);

              String payload = """
                {
                  "orderIds": [%d, %d]
                }
                """.formatted(createdOrderId, confirmedOrderId);

              mockMvc.perform(post("/api/v1/orders/bulk/confirm")
                  .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                  .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requested").value(2))
                .andExpect(jsonPath("$.succeeded").value(1))
                .andExpect(jsonPath("$.failed").value(1))
                .andExpect(jsonPath("$.failedOrderIds[0]").value(confirmedOrderId))
                .andExpect(jsonPath("$.failureDetails[0].orderId").value(confirmedOrderId))
                .andExpect(jsonPath("$.failureDetails[0].orderStatus").value("CONFIRMED"))
                .andExpect(jsonPath("$.failureDetails[0].reason").value("Only CREATED orders can be confirmed"));
                }

                private long createOrder(int quantity) throws Exception {
              String createPayload = """
                {
                  "customerId": %d,
                  "items": [
                    {"productId": %d, "quantity": %d}
                  ]
                }
                """.formatted(customerId, productId, quantity);

              String response = mockMvc.perform(post("/api/v1/orders")
                  .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                  .content(Objects.requireNonNull(createPayload)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

              return objectMapper.readTree(response).get("id").asLong();
                }

                private void confirmOrder(long orderId) throws Exception {
              mockMvc.perform(post("/api/v1/orders/{id}/confirm", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
                }

                private void cancelOrder(long orderId) throws Exception {
              mockMvc.perform(post("/api/v1/orders/{id}/cancel", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
                }

                private long createShipment(long orderId) throws Exception {
              String response = mockMvc.perform(post("/api/v1/shipments")
                  .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                  .content("""
                    {
                      "orderId": %d,
                      "note": "shipment for integration test"
                    }
                    """.formatted(orderId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

              return objectMapper.readTree(response).get("id").asLong();
                }

                private void markShipmentShipped(long shipmentId) throws Exception {
              mockMvc.perform(post("/api/v1/shipments/{id}/ship", shipmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SHIPPED"));
                }
}


