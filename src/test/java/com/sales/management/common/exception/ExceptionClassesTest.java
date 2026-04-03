package com.sales.management.common.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("null")

class ExceptionClassesTest {

    @Test
    void businessRuleException_shouldStoreMessage() {
        BusinessRuleException ex = new BusinessRuleException("business rule violated");

        assertThat(ex.getMessage()).isEqualTo("business rule violated");
        assertThat(ex).isInstanceOf(RuntimeException.class);
    }

    @Test
    void resourceNotFoundException_shouldStoreMessage() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Resource not found: 42");

        assertThat(ex.getMessage()).isEqualTo("Resource not found: 42");
        assertThat(ex).isInstanceOf(RuntimeException.class);
    }

    @Test
    void duplicateResourceException_shouldStoreMessage() {
        DuplicateResourceException ex = new DuplicateResourceException("Duplicate code: ABC");

        assertThat(ex.getMessage()).isEqualTo("Duplicate code: ABC");
        assertThat(ex).isInstanceOf(RuntimeException.class);
    }
}


