package com.sales.management.customer;

import com.sales.management.common.api.PageResponse;
import com.sales.management.common.exception.DuplicateResourceException;
import com.sales.management.common.exception.ResourceNotFoundException;
import com.sales.management.customer.dto.CustomerRequest;
import com.sales.management.customer.dto.CustomerResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"null", "unchecked"})
class CustomerServiceUnitTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerService customerService;

    private Customer savedCustomer;
    private CustomerRequest request;

    @BeforeEach
    void setUp() {
        savedCustomer = new Customer();
        savedCustomer.setId(1L);
        savedCustomer.setCode("CUS-001");
        savedCustomer.setName("Test Customer");
        savedCustomer.setPhone("0901234567");
        savedCustomer.setEmail("test@example.com");
        savedCustomer.setActive(true);

        request = new CustomerRequest();
        request.setCode("CUS-001");
        request.setName("Test Customer");
        request.setPhone("0901234567");
        request.setEmail("test@example.com");
        request.setActive(true);
    }

    // --- create() ---

    @Test
    void create_shouldReturnResponse_whenCodeIsUnique() {
        when(customerRepository.existsByCode("CUS-001")).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenReturn(savedCustomer);

        CustomerResponse response = customerService.create(request);

        assertThat(response.code()).isEqualTo("CUS-001");
        assertThat(response.name()).isEqualTo("Test Customer");
        assertThat(response.active()).isTrue();
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    void create_shouldTrimWhitespace() {
        request.setCode("  CUS-001  ");
        request.setName("  Test Customer  ");

        when(customerRepository.existsByCode("CUS-001")).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> {
            Customer c = inv.getArgument(0);
            c.setId(1L);
            return c;
        });

        CustomerResponse response = customerService.create(request);

        assertThat(response.code()).isEqualTo("CUS-001");
        assertThat(response.name()).isEqualTo("Test Customer");
    }

    @Test
    void create_shouldThrowDuplicateException_whenCodeAlreadyExists() {
        when(customerRepository.existsByCode("CUS-001")).thenReturn(true);

        assertThatThrownBy(() -> customerService.create(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("CUS-001");

        verify(customerRepository, never()).save(any());
    }

    @Test
    void create_shouldHandleNullPhone_andNullEmail() {
        request.setPhone(null);
        request.setEmail(null);

        when(customerRepository.existsByCode("CUS-001")).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> {
            Customer c = inv.getArgument(0);
            c.setId(1L);
            return c;
        });

        CustomerResponse response = customerService.create(request);

        assertThat(response.phone()).isNull();
        assertThat(response.email()).isNull();
    }

    // --- list() ---

    @Test
    void list_shouldReturnAllCustomers() {
        Customer second = new Customer();
        second.setId(2L);
        second.setCode("CUS-002");
        second.setName("Second");
        second.setActive(false);

        Pageable pageable = PageRequest.of(0, 20);
        when(customerRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(savedCustomer, second), pageable, 2));

        PageResponse<CustomerResponse> result = customerService.list(null, null, null, pageable);

        assertThat(result.content()).hasSize(2);
        assertThat(result.content().get(0).code()).isEqualTo("CUS-001");
        assertThat(result.content().get(1).code()).isEqualTo("CUS-002");
        assertThat(result.totalElements()).isEqualTo(2);
    }

    @Test
    void list_shouldReturnEmptyList_whenNoCustomers() {
        Pageable pageable = PageRequest.of(0, 20);
        when(customerRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        PageResponse<CustomerResponse> result = customerService.list(null, null, null, pageable);

        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isEqualTo(0);
    }

    // --- getById() ---

    @Test
    void getById_shouldReturnResponse_whenCustomerExists() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(savedCustomer));

        CustomerResponse response = customerService.getById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.code()).isEqualTo("CUS-001");
    }

    @Test
    void getById_shouldThrowNotFoundException_whenCustomerDoesNotExist() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // --- update() ---

    @Test
    void update_shouldReturnUpdatedResponse_whenValid() {
        CustomerRequest updateRequest = new CustomerRequest();
        updateRequest.setCode("CUS-001-UPD");
        updateRequest.setName("Updated Name");
        updateRequest.setActive(false);

        when(customerRepository.findById(1L)).thenReturn(Optional.of(savedCustomer));
        when(customerRepository.existsByCodeAndIdNot("CUS-001-UPD", 1L)).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

        CustomerResponse response = customerService.update(1L, updateRequest);

        assertThat(response.code()).isEqualTo("CUS-001-UPD");
        assertThat(response.name()).isEqualTo("Updated Name");
        assertThat(response.active()).isFalse();
    }

    @Test
    void update_shouldThrowNotFoundException_whenCustomerDoesNotExist() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.update(99L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void update_shouldThrowDuplicateException_whenCodeConflictsWithAnotherCustomer() {
        CustomerRequest updateRequest = new CustomerRequest();
        updateRequest.setCode("CUS-EXISTING");
        updateRequest.setName("Updated");
        updateRequest.setActive(true);

        when(customerRepository.findById(1L)).thenReturn(Optional.of(savedCustomer));
        when(customerRepository.existsByCodeAndIdNot("CUS-EXISTING", 1L)).thenReturn(true);

        assertThatThrownBy(() -> customerService.update(1L, updateRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("CUS-EXISTING");

        verify(customerRepository, never()).save(any());
    }

    // --- delete() ---

    @Test
    void delete_shouldDeleteSuccessfully_whenCustomerExists() {
        when(customerRepository.existsById(1L)).thenReturn(true);
        doNothing().when(customerRepository).deleteById(1L);

        customerService.delete(1L);

        verify(customerRepository).deleteById(1L);
    }

    @Test
    void delete_shouldThrowNotFoundException_whenCustomerDoesNotExist() {
        when(customerRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> customerService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");

        verify(customerRepository, never()).deleteById(any());
    }
}


