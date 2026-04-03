package com.sales.management.product;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/settings/currency-rates")
public class CurrencyRateSettingsController {

    private final CurrencyRateService currencyRateService;
    private final CurrencyRateAuditLogRepository auditLogRepository;

    public CurrencyRateSettingsController(CurrencyRateService currencyRateService,
                                           CurrencyRateAuditLogRepository auditLogRepository) {
        this.currencyRateService = currencyRateService;
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping
    public List<CurrencyRateOptionResponse> list() {
        return currencyRateService.listRates();
    }

    @PutMapping
    public List<CurrencyRateOptionResponse> update(@Valid @RequestBody CurrencyRateSettingsUpdateRequest request) {
        return currencyRateService.upsertRates(request.rates());
    }

    @PostMapping("/reset-defaults")
    public List<CurrencyRateOptionResponse> resetDefaults() {
        return currencyRateService.resetToDefaults();
    }

    @GetMapping("/audit-log")
    public Page<CurrencyRateAuditLogResponse> auditLog(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "30") int size) {
        PageRequest pageRequest = PageRequest.of(page, Math.min(size, 100),
                Sort.by(Sort.Direction.DESC, "changedAt"));
        return auditLogRepository.findAllByOrderByChangedAtDesc(pageRequest)
                .map(CurrencyRateAuditLogResponse::from);
    }
}
