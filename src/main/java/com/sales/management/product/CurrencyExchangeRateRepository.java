package com.sales.management.product;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CurrencyExchangeRateRepository extends JpaRepository<CurrencyExchangeRate, Long> {
    Optional<CurrencyExchangeRate> findByCurrencyCode(String currencyCode);
}
