package com.sales.management.dashboard;

import com.sales.management.customer.CustomerRepository;
import com.sales.management.order.OrderStatus;
import com.sales.management.order.SalesOrderRepository;
import com.sales.management.product.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

@Service
public class DashboardService {

    private final SalesOrderRepository salesOrderRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;

    public DashboardService(SalesOrderRepository salesOrderRepository,
                            CustomerRepository customerRepository,
                            ProductRepository productRepository) {
        this.salesOrderRepository = salesOrderRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public DashboardKpiResponse getKpi() {
        Instant todayStart = LocalDate.now(ZoneOffset.UTC).atStartOfDay(ZoneOffset.UTC).toInstant();

        return new DashboardKpiResponse(
                salesOrderRepository.sumTotalAmountByStatusSince(OrderStatus.PAID, todayStart),
                salesOrderRepository.countCreatedSince(todayStart),
                salesOrderRepository.countByStatus(OrderStatus.CREATED)
                        + salesOrderRepository.countByStatus(OrderStatus.CONFIRMED),
                productRepository.findLowStockProducts().size(),
                customerRepository.countActive()
        );
    }
}
