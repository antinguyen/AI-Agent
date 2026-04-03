package com.sales.management.customer;

import com.sales.management.common.api.PageResponse;
import com.sales.management.common.exception.DuplicateResourceException;
import com.sales.management.common.exception.ResourceNotFoundException;
import com.sales.management.customer.dto.CustomerRequest;
import com.sales.management.customer.dto.CustomerResponse;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@SuppressWarnings("null")
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Transactional
    public CustomerResponse create(CustomerRequest request) {
        if (customerRepository.existsByCode(request.getCode().trim())) {
            throw new DuplicateResourceException("Customer code already exists: " + request.getCode());
        }

        Customer customer = new Customer();
        applyRequest(customer, request);
        return toResponse(customerRepository.save(customer));
    }

    @Transactional(readOnly = true)
    public PageResponse<CustomerResponse> list(String name, String code, Boolean active, Pageable pageable) {
        Specification<Customer> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (name != null && !name.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%"));
            }
            if (code != null && !code.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("code")), "%" + code.toLowerCase() + "%"));
            }
            if (active != null) {
                predicates.add(cb.equal(root.get("active"), active));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return PageResponse.from(customerRepository.findAll(spec, pageable).map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public CustomerResponse getById(Long id) {
        Long customerId = Objects.requireNonNull(id, "id is required");
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + id));
        return toResponse(customer);
    }

    @Transactional
    public CustomerResponse update(Long id, CustomerRequest request) {
        Long customerId = Objects.requireNonNull(id, "id is required");
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + id));

        if (customerRepository.existsByCodeAndIdNot(request.getCode().trim(), customerId)) {
            throw new DuplicateResourceException("Customer code already exists: " + request.getCode());
        }

        applyRequest(customer, request);
        return toResponse(customerRepository.save(customer));
    }

    @Transactional
    public void delete(Long id) {
        Long customerId = Objects.requireNonNull(id, "id is required");
        if (!customerRepository.existsById(customerId)) {
            throw new ResourceNotFoundException("Customer not found: " + id);
        }
        customerRepository.deleteById(customerId);
    }

    private void applyRequest(Customer customer, CustomerRequest request) {
        customer.setCode(request.getCode().trim());
        customer.setName(request.getName().trim());
        customer.setPhone(request.getPhone() == null ? null : request.getPhone().trim());
        customer.setEmail(request.getEmail() == null ? null : request.getEmail().trim());
        customer.setActive(request.getActive());
    }

    private CustomerResponse toResponse(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getCode(),
                customer.getName(),
                customer.getPhone(),
                customer.getEmail(),
                customer.getActive(),
                customer.getCreatedAt(),
                customer.getUpdatedAt(),
                customer.getCreatedBy(),
                customer.getUpdatedBy()
        );
    }
}
