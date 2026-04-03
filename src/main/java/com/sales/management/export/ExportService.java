package com.sales.management.export;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;
import com.sales.management.order.OrderFulfillmentStatus;
import com.sales.management.order.OrderStatus;
import com.sales.management.order.SalesOrderService;
import com.sales.management.order.dto.OrderItemResponse;
import com.sales.management.order.dto.OrderResponse;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ExportService {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.of("UTC"));

    private final SalesOrderService salesOrderService;

    public ExportService(SalesOrderService salesOrderService) {
        this.salesOrderService = salesOrderService;
    }

    /**
     * Generates a PDF invoice for the given order.
     */
    @Transactional(readOnly = true)
    public byte[] generateInvoicePdf(Long orderId) {
        OrderResponse order = salesOrderService.getById(orderId);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc);

        document.add(new Paragraph("INVOICE")
                .setFontSize(20)
                .setBold());
        document.add(new Paragraph(" "));
        document.add(new Paragraph("Order Number : " + order.orderNumber()));
        document.add(new Paragraph("Customer     : " + order.customerName()));
        document.add(new Paragraph("Date         : " + DATE_FMT.format(order.createdAt())));
        document.add(new Paragraph("Status       : " + order.status()));
        if (order.discountCode() != null) {
            document.add(new Paragraph("Discount     : " + order.discountCode()
                    + " (-" + order.discountAmount() + ")"));
        }
        document.add(new Paragraph(" "));

        // Items table
        Table table = new Table(UnitValue.createPercentArray(new float[]{3, 1, 2, 2}))
                .useAllAvailableWidth();
        table.addHeaderCell(new Cell().add(new Paragraph("Product").setBold()));
        table.addHeaderCell(new Cell().add(new Paragraph("Qty").setBold()));
        table.addHeaderCell(new Cell().add(new Paragraph("Unit Price").setBold()));
        table.addHeaderCell(new Cell().add(new Paragraph("Line Total").setBold()));

        for (OrderItemResponse item : order.items()) {
            table.addCell(item.productName() + " (" + item.productSku() + ")");
            table.addCell(String.valueOf(item.quantity()));
            table.addCell(item.unitPrice().toString());
            table.addCell(item.lineTotal().toString());
        }
        document.add(table);

        document.add(new Paragraph(" "));
        document.add(new Paragraph("TOTAL: " + order.totalAmount()).setBold());

        document.close();
        return baos.toByteArray();
    }

    /**
     * Exports orders to an Excel spreadsheet.
     */
    @Transactional(readOnly = true)
    public byte[] exportOrdersExcel(
            OrderStatus status,
            OrderFulfillmentStatus fulfillmentStatus,
            LocalDate from,
            LocalDate to,
            List<Long> orderIds) throws IOException {
        List<OrderResponse> orders = salesOrderService.listAll(status, fulfillmentStatus, null, from, to);
        if (orderIds != null && !orderIds.isEmpty()) {
            Set<Long> selectedIds = new HashSet<>(orderIds);
            orders = orders.stream()
                    .filter(order -> selectedIds.contains(order.id()))
                    .collect(Collectors.toList());
        }

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Orders");

            // Header row
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Order #");
            header.createCell(1).setCellValue("Customer");
            header.createCell(2).setCellValue("Status");
            header.createCell(3).setCellValue("Total Amount");
            header.createCell(4).setCellValue("Discount");
            header.createCell(5).setCellValue("Fulfillment");
            header.createCell(6).setCellValue("Items");
            header.createCell(7).setCellValue("Created At");

            // Data rows
            int rowIdx = 1;
            for (OrderResponse order : orders) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(order.orderNumber());
                row.createCell(1).setCellValue(order.customerName());
                row.createCell(2).setCellValue(order.status().name());
                row.createCell(3).setCellValue(
                    order.totalAmount() != null ? order.totalAmount().doubleValue() : 0.0);
                row.createCell(4).setCellValue(
                    order.discountAmount() != null ? order.discountAmount().doubleValue() : 0.0);
                row.createCell(5).setCellValue(
                    order.fulfillmentStatus() != null ? order.fulfillmentStatus().name() : "PENDING");
                row.createCell(6).setCellValue(order.items().stream()
                    .map(this::formatItemSummary)
                    .collect(Collectors.joining(" | ")));
                row.createCell(7).setCellValue(
                    order.createdAt() != null ? DATE_FMT.format(order.createdAt()) : "");
            }

            for (int i = 0; i <= 7; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(baos);
            return baos.toByteArray();
        }
    }

    private String formatItemSummary(OrderItemResponse item) {
        return item.productSku() + " x" + item.quantity();
    }
}
