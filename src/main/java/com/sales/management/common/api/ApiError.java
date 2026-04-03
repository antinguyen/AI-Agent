package com.sales.management.common.api;

import java.time.Instant;
import java.util.Map;

public record ApiError(
        String code,
        String message,
        Map<String, String> details,
        String path,
        Instant timestamp
) {
}
