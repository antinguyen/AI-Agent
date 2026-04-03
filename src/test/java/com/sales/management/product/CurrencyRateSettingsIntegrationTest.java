package com.sales.management.product;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(roles = "ADMIN")
@SuppressWarnings("null")
class CurrencyRateSettingsIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private CurrencyExchangeRateRepository rateRepository;

    @BeforeEach
    void setUp() {
        rateRepository.deleteAll();
    }

    // ---- GET list ----

    @Test
    void listReturnsDefaultsWhenTableEmpty() throws Exception {
        mockMvc.perform(get("/api/v1/settings/currency-rates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[*].currencyCode", hasItems("EUR", "JPY", "USD", "VND")));
    }

    @Test
    void listReturnsPersistedRatesWhenPresent() throws Exception {
        // insert one rate so that DB is not empty → should come from DB
        String payload = """
                {"rates":[
                  {"currencyCode":"USD","bankName":"BIDV","rateToVnd":26000},
                  {"currencyCode":"EUR","bankName":"BIDV","rateToVnd":28000},
                  {"currencyCode":"VND","bankName":"BIDV","rateToVnd":1},
                  {"currencyCode":"JPY","bankName":"BIDV","rateToVnd":180}
                ]}
                """;
        mockMvc.perform(put("/api/v1/settings/currency-rates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/settings/currency-rates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[?(@.currencyCode=='USD')].bankName", contains("BIDV")))
                .andExpect(jsonPath("$[?(@.currencyCode=='USD')].rateToVnd", contains(26000.0)));
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void staffCannotAccessSettingsEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/settings/currency-rates"))
                .andExpect(status().isForbidden());

        String payload = """
                {"rates":[{"currencyCode":"USD","bankName":"X","rateToVnd":25000}]}
                """;
        mockMvc.perform(put("/api/v1/settings/currency-rates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/settings/currency-rates/reset-defaults"))
                .andExpect(status().isForbidden());
    }

    // ---- PUT upsert ----

    @Test
    void upsertUpdatesRatesSuccessfully() throws Exception {
        String payload = """
                {"rates":[
                  {"currencyCode":"USD","bankName":"TECHCOMBANK","rateToVnd":26500},
                  {"currencyCode":"EUR","bankName":"TECHCOMBANK","rateToVnd":29000},
                  {"currencyCode":"VND","bankName":"TECHCOMBANK","rateToVnd":1},
                  {"currencyCode":"JPY","bankName":"TECHCOMBANK","rateToVnd":175}
                ]}
                """;

        mockMvc.perform(put("/api/v1/settings/currency-rates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[?(@.currencyCode=='USD')].rateToVnd", contains(26500)))
                .andExpect(jsonPath("$[?(@.currencyCode=='USD')].bankName", contains("TECHCOMBANK")));
    }

    @Test
    void upsertVndRateAlwaysStaysAtOne() throws Exception {
        String payload = """
                {"rates":[
                  {"currencyCode":"VND","bankName":"CUSTOM","rateToVnd":9999}
                ]}
                """;

        String response = mockMvc.perform(put("/api/v1/settings/currency-rates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode arr = objectMapper.readTree(response);
        BigDecimal vndRate = null;
        for (JsonNode node : arr) {
            if ("VND".equals(node.get("currencyCode").asText())) {
                vndRate = new BigDecimal(node.get("rateToVnd").asText());
                break;
            }
        }
        assert vndRate != null : "VND not found in response";
        assert vndRate.compareTo(BigDecimal.ONE) == 0 : "VND rate must always be 1, got: " + vndRate;
    }

    @Test
    void upsertVndBankNameAlwaysUsesDefault() throws Exception {
        String payload = """
                {"rates":[
                  {"currencyCode":"VND","bankName":"CUSTOM_BANK","rateToVnd":1}
                ]}
                """;

        mockMvc.perform(put("/api/v1/settings/currency-rates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.currencyCode=='VND')].bankName", contains("VIETCOMBANK")));
    }

    @Test
    void upsertRejectsInvalidCurrencyCode() throws Exception {
        String payload = """
                {"rates":[
                  {"currencyCode":"us","bankName":"TEST","rateToVnd":25000}
                ]}
                """;

        mockMvc.perform(put("/api/v1/settings/currency-rates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void upsertRejectsZeroRate() throws Exception {
        String payload = """
                {"rates":[
                  {"currencyCode":"USD","bankName":"TEST","rateToVnd":0}
                ]}
                """;

        mockMvc.perform(put("/api/v1/settings/currency-rates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void upsertRejectsEmptyRatesList() throws Exception {
        String payload = """
                {"rates":[]}
                """;

        mockMvc.perform(put("/api/v1/settings/currency-rates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    // ---- POST reset-defaults ----

    @Test
    void resetDefaultsRestoresOriginalRates() throws Exception {
        // First, set custom rates
        String customPayload = """
                {"rates":[
                  {"currencyCode":"USD","bankName":"ACB","rateToVnd":99999},
                  {"currencyCode":"EUR","bankName":"ACB","rateToVnd":99999},
                  {"currencyCode":"VND","bankName":"ACB","rateToVnd":1},
                  {"currencyCode":"JPY","bankName":"ACB","rateToVnd":99999}
                ]}
                """;
        mockMvc.perform(put("/api/v1/settings/currency-rates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(customPayload))
                .andExpect(status().isOk());

        // Then reset to defaults
        mockMvc.perform(post("/api/v1/settings/currency-rates/reset-defaults"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[?(@.currencyCode=='USD')].rateToVnd", contains(25450)))
                .andExpect(jsonPath("$[?(@.currencyCode=='EUR')].rateToVnd", contains(27600)))
                .andExpect(jsonPath("$[?(@.currencyCode=='JPY')].rateToVnd", contains(171)))
                .andExpect(jsonPath("$[?(@.currencyCode=='VND')].rateToVnd", contains(1)))
                .andExpect(jsonPath("$[*].bankName", everyItem(equalTo("VIETCOMBANK"))));
    }

    @Test
    void resetDefaultsReturns4Currencies() throws Exception {
        mockMvc.perform(post("/api/v1/settings/currency-rates/reset-defaults"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[*].currencyCode", hasItems("EUR", "JPY", "USD", "VND")));
    }
}


