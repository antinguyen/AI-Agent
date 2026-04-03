package com.sales.management.product;

import com.sales.management.inventory.InventoryTransactionRepository;
import com.sales.management.invoice.InvoiceRepository;
import com.sales.management.order.SalesOrderRepository;
import com.sales.management.payment.PaymentRepository;
import com.sales.management.salesreturn.SalesReturnRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(roles = "ADMIN")
@SuppressWarnings("null")
class LowStockIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ProductRepository productRepository;
    @Autowired private InventoryTransactionRepository inventoryTransactionRepository;
    @Autowired private InvoiceRepository invoiceRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private SalesOrderRepository salesOrderRepository;
    @Autowired private SalesReturnRepository salesReturnRepository;

    @BeforeEach
    void setUp() {
        invoiceRepository.deleteAll();
        paymentRepository.deleteAll();
        inventoryTransactionRepository.deleteAll();
        salesReturnRepository.deleteAll();
        salesOrderRepository.deleteAll();
        productRepository.deleteAll();
    }

    private void createProduct(String sku, int stock, int threshold) throws Exception {
        String payload = """
                {
                  "sku": "%s",
                  "name": "Product %s",
                  "price": 10.00,
                  "stockQuantity": %d,
                  "lowStockThreshold": %d,
                  "active": true
                }
                """.formatted(sku, sku, stock, threshold);

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());
    }

    @Test
    void shouldReturnLowStockProducts() throws Exception {
        createProduct("BELOW-THRESHOLD", 3, 10);   // low stock: 3 <= 10
        createProduct("AT-THRESHOLD", 10, 10);      // low stock: 10 <= 10
        createProduct("ABOVE-THRESHOLD", 50, 10);   // ok: 50 > 10

        mockMvc.perform(get("/api/v1/products/low-stock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].sku", hasItems("BELOW-THRESHOLD", "AT-THRESHOLD")));
    }

    @Test
    void shouldReturnEmptyWhenNoLowStockProducts() throws Exception {
        createProduct("PLENTY", 100, 5);

        mockMvc.perform(get("/api/v1/products/low-stock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void shouldIncludeLowStockThresholdInProductResponse() throws Exception {
        createProduct("THRESHOLD-CHECK", 5, 20);

        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].lowStockThreshold").value(20));
    }

    @Test
    void shouldDefaultLowStockThresholdToTen() throws Exception {
        // Create without specifying lowStockThreshold
        String payload = """
                {
                  "sku": "DEFAULT-THRESHOLD",
                  "name": "Default Threshold Product",
                  "price": 5.00,
                  "stockQuantity": 100,
                  "active": true
                }
                """;

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.lowStockThreshold").value(10));
    }
}


