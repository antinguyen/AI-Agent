package com.sales.management.report;

import com.sales.management.common.exception.BusinessRuleException;
import com.sales.management.order.OrderStatus;
import com.sales.management.report.dto.OrderSummaryResponse;
import com.sales.management.report.dto.RevenueReportResponse;
import com.sales.management.report.dto.TopProductResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportService {

    private final ReportRepository reportRepository;

    public ReportService(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    @Transactional(readOnly = true)
    public RevenueReportResponse revenue(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new BusinessRuleException("'from' date must not be after 'to' date");
        }

        Instant fromInstant = from.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant toInstant = to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        List<Object[]> rows = reportRepository.dailyRevenue(fromInstant, toInstant);

        List<RevenueReportResponse.DailyRevenue> daily = rows.stream()
                .map(r -> new RevenueReportResponse.DailyRevenue(
                        toLocalDate(r[0]),
                        toBigDecimal(r[1]),
                        toLong(r[2])
                ))
                .toList();

        BigDecimal totalRevenue = daily.stream()
                .map(RevenueReportResponse.DailyRevenue::revenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalOrders = daily.stream()
                .mapToLong(RevenueReportResponse.DailyRevenue::orders)
                .sum();

        return new RevenueReportResponse(from, to, totalRevenue, totalOrders, daily);
    }

    @Transactional(readOnly = true)
    public List<TopProductResponse> topProducts(int limit) {
        if (limit < 1 || limit > 100) {
            throw new BusinessRuleException("limit must be between 1 and 100");
        }
        return reportRepository.topProducts(limit).stream()
                .map(r -> new TopProductResponse(
                        (Long) r[0],
                        (String) r[1],
                        (String) r[2],
                        toLong(r[3]),
                        toBigDecimal(r[4])
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderSummaryResponse orderSummary() {
        List<Object[]> statusRows = reportRepository.orderCountByStatus();
        Object[] totals = reportRepository.orderTotals();

        Map<String, Long> countByStatus = new LinkedHashMap<>();
        // initialise all statuses with 0 for consistent response shape
        Arrays.stream(OrderStatus.values())
                .forEach(s -> countByStatus.put(s.name(), 0L));
        statusRows.forEach(r -> countByStatus.put(r[0].toString(), toLong(r[1])));

        BigDecimal totalRevenue = toBigDecimal(totals[0]);
        long totalOrders = toLong(totals[1]);

        return new OrderSummaryResponse(totalOrders, totalRevenue, countByStatus);
    }

    // ---- helpers ----

    private LocalDate toLocalDate(Object o) {
        if (o instanceof LocalDate d) return d;
        // H2 may return java.sql.Date
        if (o instanceof java.sql.Date d) return d.toLocalDate();
        return LocalDate.parse(o.toString());
    }

    private BigDecimal toBigDecimal(Object o) {
        if (o == null) return BigDecimal.ZERO;
        if (o instanceof BigDecimal bd) return bd;
        return new BigDecimal(o.toString());
    }

    private long toLong(Object o) {
        if (o == null) return 0L;
        if (o instanceof Number n) return n.longValue();
        return Long.parseLong(o.toString());
    }
}
