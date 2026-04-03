package com.sales.management.export;

import com.sales.management.customer.Customer;
import com.sales.management.customer.CustomerRepository;
import com.sales.management.inventory.InventoryTransactionRepository;
import com.sales.management.invoice.InvoiceRepository;
import com.sales.management.order.OrderStatus;
import com.sales.management.order.SalesOrderRepository;
import com.sales.management.payment.PaymentRepository;
import com.sales.management.product.Product;
import com.sales.management.product.ProductRepository;
import com.sales.management.salesreturn.SalesReturnRepository;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(roles = "ADMIN")
@SuppressWarnings("null")
class ExportIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private SalesOrderRepository salesOrderRepository;
    @Autowired private InventoryTransactionRepository inventoryTransactionRepository;
    @Autowired private InvoiceRepository invoiceRepository;
    @Autowired private PaymentRepository paymentRepository;
        @Autowired private SalesReturnRepository salesReturnRepository;

    private Long customerId;
    private Long productId;

    @BeforeEach
    void setUp() {
        invoiceRepository.deleteAll();
        paymentRepository.deleteAll();
        inventoryTransactionRepository.deleteAll();
        salesReturnRepository.deleteAll();
        salesOrderRepository.deleteAll();
        customerRepository.deleteAll();
        productRepository.deleteAll();

        Customer customer = new Customer();
        customer.setCode("CUS-EXPORT-01");
        customer.setName("Export Customer");
        customer.setPhone("0900123000");
        customer.setEmail("export@example.com");
        customer.setActive(true);
        customerId = customerRepository.save(customer).getId();

        Product product = new Product();
        product.setSku("SP-EXPORT-01");
        product.setName("Export Product");
        product.setPrice(new BigDecimal("120.00"));
        product.setStockQuantity(200);
        product.setLowStockThreshold(5);
        product.setActive(true);
        productId = productRepository.save(product).getId();
    }

    private Long createOrder() throws Exception {
        String payload = """
                {
                  "customerId": %d,
                  "items": [{"productId": %d, "quantity": 2}]
                }
                """.formatted(customerId, productId);

        String response = mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return Long.parseLong(response.replaceAll(".*\"id\":(\\d+).*", "$1"));
    }

    @Test
    void shouldExportOrderInvoicePdf() throws Exception {
        Long orderId = createOrder();

        mockMvc.perform(get("/api/v1/orders/{id}/invoice/pdf", orderId))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/pdf"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, org.hamcrest.Matchers.containsString("invoice-" + orderId + ".pdf")));
    }

    @Test
    void shouldExportReportAsExcel() throws Exception {
        createOrder();

        mockMvc.perform(get("/api/v1/reports/export").param("format", "xlsx"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE,
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        org.hamcrest.Matchers.containsString("orders-report.xlsx")));
    }

    @Test
    void shouldRejectUnsupportedFormat() throws Exception {
        mockMvc.perform(get("/api/v1/reports/export").param("format", "pdf"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldExportOnlyMatchingOrdersForStatusFilter() throws Exception {
        Long createdOrderId = createOrder();
        Long confirmedOrderId = createOrder();

        mockMvc.perform(post("/api/v1/orders/{id}/confirm", confirmedOrderId))
                .andExpect(status().isOk());

        byte[] bytes = mockMvc.perform(get("/api/v1/reports/export")
                        .param("format", "xlsx")
                        .param("status", OrderStatus.CREATED.name()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            var sheet = workbook.getSheet("Orders");
            assertThat(sheet).isNotNull();
            assertThat(sheet.getPhysicalNumberOfRows()).isEqualTo(2);
            assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).contains("SO-");
            assertThat(sheet.getRow(1).getCell(2).getStringCellValue()).isEqualTo(OrderStatus.CREATED.name());
            assertThat(sheet.getRow(1).getCell(5).getStringCellValue()).isEqualTo("PENDING");
            assertThat(sheet.getRow(1).getCell(6).getStringCellValue()).contains("SP-EXPORT-01 x2");
        }

        assertThat(salesOrderRepository.findById(createdOrderId)).isPresent();
        assertThat(salesOrderRepository.findById(confirmedOrderId)).isPresent();
    }

    @Test
    void shouldReturnHeaderOnlyWhenDateRangeHasNoOrders() throws Exception {
        createOrder();

        byte[] bytes = mockMvc.perform(get("/api/v1/reports/export")
                        .param("format", "xlsx")
                        .param("from", "2099-01-01")
                        .param("to", "2099-01-31"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            var sheet = workbook.getSheet("Orders");
            assertThat(sheet).isNotNull();
            assertThat(sheet.getPhysicalNumberOfRows()).isEqualTo(1);
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Order #");
        }
    }

    @Test
    void shouldExportOnlySelectedOrderIds() throws Exception {
        Long firstOrderId = createOrder();
        createOrder();

        byte[] bytes = mockMvc.perform(get("/api/v1/reports/export")
                        .param("format", "xlsx")
                        .param("orderIds", String.valueOf(firstOrderId)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            var sheet = workbook.getSheet("Orders");
            assertThat(sheet).isNotNull();
            assertThat(sheet.getPhysicalNumberOfRows()).isEqualTo(2);
        }
    }
}


