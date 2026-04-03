package com.sales.management.shipment;

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
import com.sales.management.salesreturn.SalesReturnRepository;
import com.sales.management.warehouse.ProductWarehouseStock;
import com.sales.management.warehouse.ProductWarehouseStockRepository;
import com.sales.management.warehouse.Warehouse;
import com.sales.management.warehouse.WarehouseRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Objects;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(roles = "ADMIN")
@SuppressWarnings("null")
class ShipmentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ShipmentRepository shipmentRepository;

    @Autowired
    private SalesOrderRepository salesOrderRepository;

        @Autowired
        private InvoiceRepository invoiceRepository;

        @Autowired
        private PaymentRepository paymentRepository;

        @Autowired
        private OrderItemRepository orderItemRepository;

    @Autowired
    private InventoryTransactionRepository inventoryTransactionRepository;

        @Autowired
        private SalesReturnRepository salesReturnRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ProductWarehouseStockRepository productWarehouseStockRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private ProductRepository productRepository;

    private Long customerId;
    private Long productId;
    private Long warehouseId;

    @BeforeEach
    void setUp() {
                salesReturnRepository.deleteAll();
                invoiceRepository.deleteAll();
                paymentRepository.deleteAll();
        shipmentRepository.deleteAll();
        inventoryTransactionRepository.deleteAll();
                orderItemRepository.deleteAll();
        salesOrderRepository.deleteAll();
        productWarehouseStockRepository.deleteAll();
        customerRepository.deleteAll();
        warehouseRepository.deleteAll();
        productRepository.deleteAll();

        Customer customer = new Customer();
        customer.setCode("CUS-SHP-001");
        customer.setName("Shipment Customer");
        customer.setPhone("0900111222");
        customer.setEmail("ship@example.com");
        customer.setActive(true);
        customerId = customerRepository.save(customer).getId();

        Product product = new Product();
        product.setSku("SP-SHP-01");
        product.setName("Shipment Product");
        product.setPrice(new BigDecimal("150.00"));
        product.setStockQuantity(10);
        product.setActive(true);
        productId = productRepository.save(product).getId();

        Warehouse warehouse = new Warehouse();
        warehouse.setCode("WH-TEST-01");
        warehouse.setName("Kho test shipment");
        warehouse.setAddress("Test address");
        warehouse.setActive(true);
        warehouseId = warehouseRepository.save(warehouse).getId();

        ProductWarehouseStock stock = new ProductWarehouseStock(product, warehouse, 10, 2);
        productWarehouseStockRepository.save(stock);
    }

    @Test
    void shouldCreateShipmentForConfirmedOrder() throws Exception {
        long orderId = createAndConfirmOrder();

        String payload = """
                {
                  "orderId": %d,
                  "note": "Đóng gói ca sáng"
                }
                """.formatted(orderId);

        mockMvc.perform(post("/api/v1/shipments")
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.warehouseId").value(warehouseId))
                .andExpect(jsonPath("$.items[0].productId").value(productId));

        mockMvc.perform(get("/api/v1/orders/{id}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fulfillmentStatus").value("READY_TO_SHIP"));
    }

    @Test
    void shouldMarkShipmentAsShippedAndDeductWarehouseStock() throws Exception {
        long orderId = createAndConfirmOrder();

        String shipmentResponse = mockMvc.perform(post("/api/v1/shipments")
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content("""
                                {
                                  "orderId": %d,
                                  "note": "Giao hàng nội thành"
                                }
                                """.formatted(orderId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        JsonNode shipmentJson = objectMapper.readTree(shipmentResponse);
        long shipmentId = shipmentJson.get("id").asLong();

        mockMvc.perform(post("/api/v1/shipments/{id}/ship", shipmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SHIPPED"));

        mockMvc.perform(get("/api/v1/orders/{id}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fulfillmentStatus").value("SHIPPED"));

        ProductWarehouseStock stock = productWarehouseStockRepository
                .findByProductIdAndWarehouseId(productId, warehouseId)
                .orElseThrow();

        Assertions.assertEquals(8, stock.getQuantity());
        Assertions.assertEquals(0, stock.getReservedQuantity());
        Assertions.assertEquals(8, stock.getAvailableQuantity());
    }

    private long createAndConfirmOrder() throws Exception {
        String createPayload = """
                {
                  "customerId": %d,
                  "warehouseId": %d,
                  "items": [
                    {"productId": %d, "quantity": 2}
                  ]
                }
                """.formatted(customerId, warehouseId, productId);

        String response = mockMvc.perform(post("/api/v1/orders")
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content(createPayload))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        JsonNode jsonNode = objectMapper.readTree(response);
        long orderId = jsonNode.get("id").asLong();

        mockMvc.perform(post("/api/v1/orders/{id}/confirm", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        return orderId;
    }
}

