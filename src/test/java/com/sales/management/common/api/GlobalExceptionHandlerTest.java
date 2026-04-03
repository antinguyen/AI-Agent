package com.sales.management.common.api;

import com.sales.management.common.exception.BusinessRuleException;
import com.sales.management.common.exception.DuplicateResourceException;
import com.sales.management.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SuppressWarnings("null")
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    // Stub controller that throws each type of exception on demand
    @RestController
    static class StubController {

        @GetMapping("/test/not-found")
        public void throwNotFound() {
            throw new ResourceNotFoundException("stub resource not found");
        }

        @GetMapping("/test/duplicate")
        public void throwDuplicate() {
            throw new DuplicateResourceException("stub duplicate resource");
        }

        @GetMapping("/test/business-rule")
        public void throwBusinessRule() {
            throw new BusinessRuleException("stub business rule violated");
        }

        @GetMapping("/test/unknown")
        public void throwUnknown() {
            throw new RuntimeException("stub unexpected error");
        }

        @PostMapping("/test/validation")
        public void validationEndpoint(@Valid @RequestBody ValidatedBody body) {
        }

        static class ValidatedBody {
            @NotBlank(message = "field is required")
            private String field;

            public String getField() {
                return field;
            }

            public void setField(String field) {
                this.field = field;
            }
        }
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new StubController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldReturn404_forResourceNotFoundException() throws Exception {
        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("stub resource not found"))
                .andExpect(jsonPath("$.path").value("/test/not-found"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void shouldReturn409_forDuplicateResourceException() throws Exception {
        mockMvc.perform(get("/test/duplicate"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_RESOURCE"))
                .andExpect(jsonPath("$.message").value("stub duplicate resource"));
    }

    @Test
    void shouldReturn400_forBusinessRuleException() throws Exception {
        mockMvc.perform(get("/test/business-rule"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"))
                .andExpect(jsonPath("$.message").value("stub business rule violated"));
    }

    @Test
    void shouldReturn500_forUnknownException() throws Exception {
        mockMvc.perform(get("/test/unknown"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("Unexpected server error"));
    }

    @Test
    void shouldReturn400_forValidationError() throws Exception {
        // Sending empty field to trigger @NotBlank validation
        String payload = """
                {"field": ""}
                """;

        mockMvc.perform(post("/test/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details.field").value("field is required"));
    }
}
