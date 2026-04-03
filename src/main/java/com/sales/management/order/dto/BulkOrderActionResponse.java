package com.sales.management.order.dto;

import java.util.List;

public record BulkOrderActionResponse(
        int requested,
        int succeeded,
        int failed,
        List<Long> failedOrderIds,
        List<BulkOrderFailureDetail> failureDetails
) {
}
