package com.sales.management.discount;

import com.sales.management.discount.dto.DiscountRequest;
import com.sales.management.discount.dto.DiscountResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/discounts")
public class DiscountController {

    private final DiscountService discountService;

    public DiscountController(DiscountService discountService) {
        this.discountService = discountService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DiscountResponse create(@RequestBody @Valid DiscountRequest request) {
        return discountService.create(request);
    }

    @GetMapping
    public List<DiscountResponse> list() {
        return discountService.list();
    }

    @GetMapping("/{code}")
    public DiscountResponse getByCode(@PathVariable("code") String code) {
        return discountService.getByCode(code);
    }

    @PutMapping("/{code}")
    public DiscountResponse update(@PathVariable("code") String code,
                                   @RequestBody @Valid DiscountRequest request) {
        return discountService.update(code, request);
    }

    @DeleteMapping("/{code}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable("code") String code) {
        discountService.deactivate(code);
    }
}
