package com.sales.management.product;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.sales.management.inventory.InventoryTransactionRepository;
import com.sales.management.invoice.InvoiceRepository;
import com.sales.management.order.SalesOrderRepository;
import com.sales.management.customer.CustomerRepository;
import com.sales.management.payment.PaymentRepository;
import com.sales.management.salesreturn.SalesReturnRepository;
import com.sales.management.shipment.ShipmentRepository;

import java.util.Objects;

import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(roles = "ADMIN")
@SuppressWarnings("null")
class ProductControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

        @Autowired
        private InventoryTransactionRepository inventoryTransactionRepository;

        @Autowired
        private InvoiceRepository invoiceRepository;

        @Autowired
        private PaymentRepository paymentRepository;

        @Autowired
        private SalesOrderRepository salesOrderRepository;

        @Autowired
        private CustomerRepository customerRepository;

        @Autowired
        private ProductRepository productRepository;

        @Autowired
        private SalesReturnRepository salesReturnRepository;

        @Autowired
        private ShipmentRepository shipmentRepository;

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
        }

    @Test
    void shouldCreateAndListProducts() throws Exception {
        String payload = """
                {
                  "sku": "SP-001",
                  "name": "San pham A",
                                                                        "description": "Mo ta san pham A",
                  "price": 150000,
                  "stockQuantity": 20,
                  "active": true
                }
                """;

        mockMvc.perform(post("/api/v1/products")
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.sku").value("SP-001"))
                .andExpect(jsonPath("$.description").value("Mo ta san pham A"))
                .andExpect(jsonPath("$.exchangeRate").value(1));

        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("San pham A"));

        String usdPayload = """
                {
                  "sku": "SP-002",
                  "name": "San pham USD",
                  "price": 100,
                  "currencyCode": "USD",
                  "stockQuantity": 5,
                  "active": true
                }
                """;

        mockMvc.perform(post("/api/v1/products")
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content(usdPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.currencyCode").value("USD"))
                .andExpect(jsonPath("$.exchangeRate").isNumber());

        mockMvc.perform(get("/api/v1/products/options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currencies").isArray())
                .andExpect(jsonPath("$.currencies[0].currencyCode").exists());
    }
}


