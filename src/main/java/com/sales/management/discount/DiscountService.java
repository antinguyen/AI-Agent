package com.sales.management.discount;

import com.sales.management.common.exception.BusinessRuleException;
import com.sales.management.common.exception.ResourceNotFoundException;
import com.sales.management.discount.dto.DiscountRequest;
import com.sales.management.discount.dto.DiscountResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;

@Service
public class DiscountService {

    private final DiscountRepository discountRepository;

    public DiscountService(DiscountRepository discountRepository) {
        this.discountRepository = discountRepository;
    }

    @Transactional
    public DiscountResponse create(DiscountRequest request) {
        if (discountRepository.existsByCode(request.getCode().toUpperCase())) {
            throw new BusinessRuleException("Discount code already exists: " + request.getCode());
        }
        Discount discount = new Discount();
        discount.setCode(request.getCode().toUpperCase());
        applyFields(discount, request);
        return toResponse(discountRepository.save(discount));
    }

    @Transactional(readOnly = true)
    public List<DiscountResponse> list() {
        return discountRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public DiscountResponse getByCode(String code) {
        return toResponse(findByCodeOrThrow(code));
    }

    @Transactional
    public DiscountResponse update(String code, DiscountRequest request) {
        Discount discount = findByCodeOrThrow(code);
        applyFields(discount, request);
        return toResponse(discount);
    }

    @Transactional
    public void deactivate(String code) {
        Discount discount = findByCodeOrThrow(code);
        discount.setActive(false);
    }

    /**
     * Validates and calculates the discount amount for a given order total.
     * Called internally by SalesOrderService.
     */
    public BigDecimal calculateDiscount(Discount discount, BigDecimal orderTotal) {
        if (!Boolean.TRUE.equals(discount.getActive())) {
            throw new BusinessRuleException("Discount is inactive: " + discount.getCode());
        }
        if (discount.getExpiresAt() != null && Instant.now().isAfter(discount.getExpiresAt())) {
            throw new BusinessRuleException("Discount has expired: " + discount.getCode());
        }
        if (discount.getMinOrderAmount() != null
                && orderTotal.compareTo(discount.getMinOrderAmount()) < 0) {
            throw new BusinessRuleException(
                    "Order total does not meet the minimum amount required for discount: " + discount.getCode());
        }

        if (discount.getType() == DiscountType.FIXED) {
            return discount.getValue().min(orderTotal);
        } else {
            BigDecimal amount = orderTotal
                    .multiply(discount.getValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            if (discount.getMaxDiscountAmount() != null) {
                amount = amount.min(discount.getMaxDiscountAmount());
            }
            return amount;
        }
    }

    public Discount findByCodeOrThrow(String code) {
        return discountRepository.findByCode(code.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Discount not found: " + code));
    }

    private void applyFields(Discount discount, DiscountRequest req) {
        discount.setType(req.getType());
        discount.setValue(req.getValue());
        discount.setMinOrderAmount(req.getMinOrderAmount());
        discount.setMaxDiscountAmount(req.getMaxDiscountAmount());
        discount.setActive(req.getActive() != null ? req.getActive() : true);
        discount.setExpiresAt(req.getExpiresAt());
    }

    private DiscountResponse toResponse(Discount d) {
        return new DiscountResponse(
                d.getId(), d.getCode(), d.getType(), d.getValue(),
                d.getMinOrderAmount(), d.getMaxDiscountAmount(),
                d.getActive(), d.getExpiresAt(), d.getCreatedAt(), d.getUpdatedAt());
    }
}
