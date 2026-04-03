package com.sales.management.customer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sales.management.inventory.InventoryTransactionRepository;
import com.sales.management.order.SalesOrderRepository;
import com.sales.management.product.ProductRepository;
import com.sales.management.salesreturn.SalesReturnRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(roles = "ADMIN")
@SuppressWarnings("null")
class CustomerControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private InventoryTransactionRepository inventoryTransactionRepository;

    @Autowired
    private SalesOrderRepository salesOrderRepository;

    @Autowired
    private ProductRepository productRepository;

        @Autowired
        private SalesReturnRepository salesReturnRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        inventoryTransactionRepository.deleteAll();
                salesReturnRepository.deleteAll();
        salesOrderRepository.deleteAll();
        customerRepository.deleteAll();
        productRepository.deleteAll();
    }

    @Test
    void shouldCreateCustomer_andReturn201() throws Exception {
        String payload = """
                {
                  "code": "CUS-TEST-001",
                  "name": "Test Customer",
                  "phone": "0901234567",
                  "email": "test@example.com",
                  "active": true
                }
                """;

        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.code").value("CUS-TEST-001"))
                .andExpect(jsonPath("$.name").value("Test Customer"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void shouldListCustomers() throws Exception {
        String payload = """
                {
                  "code": "CUS-LIST-001",
                  "name": "List Customer",
                  "active": true
                }
                """;

        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].code").value("CUS-LIST-001"));
    }

    @Test
    void shouldGetCustomerById() throws Exception {
        String payload = """
                {
                  "code": "CUS-GET-001",
                  "name": "Get Customer",
                  "active": true
                }
                """;

        String createResponse = mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(createResponse).get("id").asLong();

        mockMvc.perform(get("/api/v1/customers/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("CUS-GET-001"));
    }

    @Test
    void shouldReturn404_whenCustomerNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/customers/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void shouldUpdateCustomer() throws Exception {
        String createPayload = """
                {
                  "code": "CUS-UPD-001",
                  "name": "Original Name",
                  "active": true
                }
                """;

        String createResponse = mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(createResponse).get("id").asLong();

        String updatePayload = """
                {
                  "code": "CUS-UPD-001",
                  "name": "Updated Name",
                  "active": false
                }
                """;

        mockMvc.perform(put("/api/v1/customers/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"))
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void shouldDeleteCustomer() throws Exception {
        String payload = """
                {
                  "code": "CUS-DEL-001",
                  "name": "Delete Customer",
                  "active": true
                }
                """;

        String createResponse = mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(createResponse).get("id").asLong();

        mockMvc.perform(delete("/api/v1/customers/{id}", id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/customers/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn409_whenDuplicateCode() throws Exception {
        String payload = """
                {
                  "code": "CUS-DUP-001",
                  "name": "First Customer",
                  "active": true
                }
                """;

        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_RESOURCE"));
    }

    @Test
    void shouldReturn400_whenValidationFails() throws Exception {
        String payload = """
                {
                  "code": "",
                  "name": "",
                  "active": null
                }
                """;

        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldReturn404_whenDeletingNonExistentCustomer() throws Exception {
        mockMvc.perform(delete("/api/v1/customers/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }
}
