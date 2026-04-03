package com.sales.management.preference;

import com.sales.management.auth.AppUser;
import com.sales.management.auth.UserRepository;
import com.sales.management.auth.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@SuppressWarnings("null")
class UserPreferenceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserPreferenceRepository preferenceRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        preferenceRepository.deleteAll();
        userRepository.deleteAll();

        AppUser admin = new AppUser();
        admin.setUsername("admin1");
        admin.setPassword(passwordEncoder.encode("password123"));
        admin.setRole(UserRole.ADMIN);
        admin.setActive(true);
        userRepository.save(admin);
    }

    @Test
    @WithMockUser(username = "admin1", roles = "ADMIN")
    void getMineReturnsDefaultWhenMissing() throws Exception {
        mockMvc.perform(get("/api/v1/preferences/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.locale", is("vi-VN")))
                .andExpect(jsonPath("$.currencyCode", is("VND")))
                .andExpect(jsonPath("$.defaultLandingPage", is("/orders")))
            .andExpect(jsonPath("$.tablePageSize", is(15)))
            .andExpect(jsonPath("$.orderListPresetKey", is("ALL")))
            .andExpect(jsonPath("$.orderListStatusFilter", is("")))
            .andExpect(jsonPath("$.orderListFulfillmentFilter", is("ALL")));
    }

    @Test
    @WithMockUser(username = "admin1", roles = "ADMIN")
    void updateMinePersistsNewValues() throws Exception {
        String payload = """
                {
                  "locale":"en-US",
                  "currencyCode":"USD",
                  "reducedMotion":true,
                  "defaultLandingPage":"/products",
                                    "tablePageSize":25,
                                    "orderListPresetKey":"READY_TO_SHIP",
                                    "orderListStatusFilter":"",
                                    "orderListFulfillmentFilter":"READY_TO_SHIP"
                }
                """;

        mockMvc.perform(put("/api/v1/preferences/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.locale", is("en-US")))
                .andExpect(jsonPath("$.currencyCode", is("USD")))
                .andExpect(jsonPath("$.reducedMotion", is(true)))
                .andExpect(jsonPath("$.defaultLandingPage", is("/products")))
                .andExpect(jsonPath("$.tablePageSize", is(25)))
                .andExpect(jsonPath("$.orderListPresetKey", is("READY_TO_SHIP")))
                .andExpect(jsonPath("$.orderListStatusFilter", is("")))
                .andExpect(jsonPath("$.orderListFulfillmentFilter", is("READY_TO_SHIP")));
    }

    @Test
    void unauthenticatedCannotAccessPreferences() throws Exception {
        mockMvc.perform(get("/api/v1/preferences/me"))
                .andExpect(status().isUnauthorized());
    }
}


