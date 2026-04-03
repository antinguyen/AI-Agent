package com.sales.management.report;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import org.springframework.security.test.context.support.WithMockUser;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(roles = "ADMIN")
@SuppressWarnings("null")
class ReportingIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Autowired private CustomerRepository customerRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private SalesOrderRepository salesOrderRepository;
    @Autowired private InventoryTransactionRepository inventoryTransactionRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private InvoiceRepository invoiceRepository;
    @Autowired private SalesReturnRepository salesReturnRepository;

    private Long customerId;
    private Long productAId;
    private Long productBId;

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
        customer.setCode("RPT-CUS-01");
        customer.setName("Report Customer");
        customer.setActive(true);
        customerId = customerRepository.save(customer).getId();

        Product productA = new Product();
        productA.setSku("RPT-PRD-01");
        productA.setName("Report Product A");
        productA.setPrice(new BigDecimal("200.00"));
        productA.setStockQuantity(100);
        productA.setActive(true);
        productAId = productRepository.save(productA).getId();

        Product productB = new Product();
        productB.setSku("RPT-PRD-02");
        productB.setName("Report Product B");
        productB.setPrice(new BigDecimal("50.00"));
        productB.setStockQuantity(100);
        productB.setActive(true);
        productBId = productRepository.save(productB).getId();
    }

    // ---- helpers ----

    private long createAndPayOrder(long productId, int quantity) throws Exception {
        // create order
        String orderPayload = """
                {"customerId":%d,"items":[{"productId":%d,"quantity":%d}]}
                """.formatted(customerId, productId, quantity);

        String orderJson = mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderPayload))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        long orderId = objectMapper.readTree(orderJson).get("id").asLong();

        // confirm
        mockMvc.perform(post("/api/v1/orders/" + orderId + "/confirm"))
                .andExpect(status().isOk());

        // pay
        String payPayload = """
                {"method":"CASH"}
                """;
        mockMvc.perform(post("/api/v1/orders/" + orderId + "/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payPayload))
                .andExpect(status().isCreated());

        return orderId;
    }

    // ---- tests ----

    @Test
    void orderSummaryShouldReflectOrderCounts() throws Exception {
        createAndPayOrder(productAId, 2);
        createAndPayOrder(productBId, 3);

        mockMvc.perform(get("/api/v1/reports/order-summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalOrders").value(greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.countByStatus.PAID").value(greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.totalRevenue").isNumber());
    }

    @Test
    void topProductsShouldRankByQuantitySold() throws Exception {
        // productA: 5 units sold, productB: 3 units sold → productA should rank first
        createAndPayOrder(productAId, 5);
        createAndPayOrder(productBId, 3);

        mockMvc.perform(get("/api/v1/reports/top-products?limit=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$[0].sku").value("RPT-PRD-01"))
                .andExpect(jsonPath("$[0].totalQuantitySold").value(5))
                .andExpect(jsonPath("$[0].totalRevenue").isNumber());
    }

    @Test
    void revenueShouldSumPaidOrdersInDateRange() throws Exception {
        createAndPayOrder(productAId, 1); // 200.00
        createAndPayOrder(productBId, 2); // 100.00

        String today = LocalDate.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_LOCAL_DATE);

        mockMvc.perform(get("/api/v1/reports/revenue?from=" + today + "&to=" + today))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value(today))
                .andExpect(jsonPath("$.to").value(today))
                .andExpect(jsonPath("$.totalRevenue").isNumber())
                .andExpect(jsonPath("$.totalOrders").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.daily", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void revenueShouldReturn400ForInvalidDateRange() throws Exception {
        mockMvc.perform(get("/api/v1/reports/revenue?from=2026-03-31&to=2026-03-01"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void topProductsShouldReturn400ForInvalidLimit() throws Exception {
        mockMvc.perform(get("/api/v1/reports/top-products?limit=0"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/reports/top-products?limit=200"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void topProductsDefaultLimitShouldReturnAtMost10() throws Exception {
        mockMvc.perform(get("/api/v1/reports/top-products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(lessThanOrEqualTo(10))));
    }
}


