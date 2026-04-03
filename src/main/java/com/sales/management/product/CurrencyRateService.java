package com.sales.management.product;

import com.sales.management.common.exception.BusinessRuleException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class CurrencyRateService {

    private static final String DEFAULT_BANK_NAME = "VIETCOMBANK";

    private static final Map<String, BigDecimal> DEFAULT_BANK_RATES = Map.of(
            "VND", BigDecimal.ONE,
            "USD", new BigDecimal("25450"),
            "EUR", new BigDecimal("27600"),
            "JPY", new BigDecimal("171")
    );

    private final CurrencyExchangeRateRepository rateRepository;
    private final CurrencyRateAuditLogRepository auditLogRepository;

    public CurrencyRateService(CurrencyExchangeRateRepository rateRepository,
                                CurrencyRateAuditLogRepository auditLogRepository) {
        this.rateRepository = rateRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(readOnly = true)
    public BigDecimal resolveRateToVnd(String currencyCode) {
        String normalized = normalizeCurrency(currencyCode);
        if (normalized.isBlank() || "VND".equals(normalized)) {
            return BigDecimal.ONE;
        }
        return rateRepository.findByCurrencyCode(normalized)
                .map(CurrencyExchangeRate::getRateToVnd)
                .orElseGet(() -> {
                    BigDecimal fallback = DEFAULT_BANK_RATES.get(normalized);
                    if (fallback == null) {
                        throw new BusinessRuleException("Bank exchange rate not configured for currency: " + normalized);
                    }
                    return fallback;
                });
    }

    @Transactional(readOnly = true)
    public List<CurrencyRateOptionResponse> listRates() {
        List<CurrencyRateOptionResponse> fromDb = rateRepository.findAll().stream()
                .sorted(Comparator.comparing(CurrencyExchangeRate::getCurrencyCode))
                .map(rate -> new CurrencyRateOptionResponse(
                        rate.getCurrencyCode(),
                        rate.getBankName(),
                        rate.getRateToVnd(),
                        rate.getUpdatedAt()
                ))
                .toList();

                if (!fromDb.isEmpty()) {
                    return fromDb;
                }

                Instant now = Instant.now();
                return DEFAULT_BANK_RATES.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(entry -> new CurrencyRateOptionResponse(entry.getKey(), DEFAULT_BANK_NAME, entry.getValue(), now))
                    .toList();
    }

    @Transactional
    public List<CurrencyRateOptionResponse> upsertRates(List<CurrencyRateSettingRequest> rates) {
        String changedBy = currentUser();
        for (CurrencyRateSettingRequest request : rates) {
            String currency = normalizeCurrency(request.currencyCode());
            BigDecimal rate = request.rateToVnd();
            if (rate == null) {
                throw new BusinessRuleException("rateToVnd is required for currency: " + currency);
            }
            if ("VND".equals(currency)) {
                rate = BigDecimal.ONE;
            }

            CurrencyExchangeRate entity = rateRepository.findByCurrencyCode(currency)
                    .orElseGet(CurrencyExchangeRate::new);

            BigDecimal oldRate = entity.getRateToVnd();
            String oldBankName = entity.getBankName();

            entity.setCurrencyCode(currency);
            entity.setBankName("VND".equals(currency) ? DEFAULT_BANK_NAME : request.bankName().trim());
            entity.setRateToVnd(rate);
            rateRepository.save(entity);

            saveAuditLog(currency, oldBankName, entity.getBankName(), oldRate, rate, "UPSERT", changedBy);
        }
        return listRates();
    }

    @Transactional
    public List<CurrencyRateOptionResponse> resetToDefaults() {
        String changedBy = currentUser();
        for (Map.Entry<String, BigDecimal> entry : DEFAULT_BANK_RATES.entrySet()) {
            CurrencyExchangeRate entity = rateRepository.findByCurrencyCode(entry.getKey())
                    .orElseGet(CurrencyExchangeRate::new);

            BigDecimal oldRate = entity.getRateToVnd();
            String oldBankName = entity.getBankName();

            entity.setCurrencyCode(entry.getKey());
            entity.setBankName(DEFAULT_BANK_NAME);
            entity.setRateToVnd(entry.getValue());
            rateRepository.save(entity);

            saveAuditLog(entry.getKey(), oldBankName, DEFAULT_BANK_NAME, oldRate, entry.getValue(), "RESET", changedBy);
        }
        return listRates();
    }

    private String normalizeCurrency(String currencyCode) {
        if (currencyCode == null) {
            return "VND";
        }
        return currencyCode.trim().toUpperCase(Locale.ROOT);
    }

    private String currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return "system";
        }
        return auth.getName();
    }

    private void saveAuditLog(String currencyCode, String oldBankName, String newBankName,
                               BigDecimal oldRate, BigDecimal newRate, String action, String changedBy) {
        CurrencyRateAuditLog log = new CurrencyRateAuditLog();
        log.setCurrencyCode(currencyCode);
        log.setOldBankName(oldBankName);
        log.setNewBankName(newBankName);
        log.setOldRate(oldRate);
        log.setNewRate(newRate);
        log.setAction(action);
        log.setChangedBy(changedBy);
        auditLogRepository.save(log);
    }
}
