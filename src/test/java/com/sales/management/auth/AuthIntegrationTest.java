package com.sales.management.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@SuppressWarnings("null")
class AuthIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    // ---- helpers ----

    private String registerAndGetToken(String username, String password, String role) throws Exception {
        String payload = """
                {"username":"%s","password":"%s","role":"%s"}
                """.formatted(username, password, role);

        String json = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(json).get("token").asText();
    }

    // ---- tests ----

    @Test
    void shouldRegisterUserAndReturnToken() throws Exception {
        String payload = """
                {"username":"admin1","password":"password123","role":"ADMIN"}
                """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.username").value("admin1"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void shouldLoginAndReturnToken() throws Exception {
        registerAndGetToken("staffuser", "mypassword1", "STAFF");

        String loginPayload = """
                {"username":"staffuser","password":"mypassword1"}
                """;

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.role").value("STAFF"));
    }

    @Test
    void shouldAccessProtectedEndpointWithValidToken() throws Exception {
        String token = registerAndGetToken("admin2", "securePass1", "ADMIN");

        mockMvc.perform(get("/api/v1/products")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturn401WithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void shouldReturn403ForStaffAccessingReports() throws Exception {
        String token = registerAndGetToken("staffmember", "staffPass1", "STAFF");

        mockMvc.perform(get("/api/v1/reports/order-summary")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

        @Test
        void shouldReturn403ForStaffCreatingProduct() throws Exception {
                String token = registerAndGetToken("staffproduct", "staffPass1", "STAFF");

                mockMvc.perform(post("/api/v1/products")
                                                .header("Authorization", "Bearer " + token)
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content("""
                                                                {"sku":"STF-001","name":"Staff Product","price":100.00,"stockQuantity":5,"active":true}
                                                                """))
                                .andExpect(status().isForbidden())
                                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        }

        @Test
        void shouldReturn403ForStaffAccessingLowStock() throws Exception {
                String token = registerAndGetToken("staffstock", "staffPass1", "STAFF");

                mockMvc.perform(get("/api/v1/products/low-stock")
                                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isForbidden())
                                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        }

    @Test
    void shouldReturn401WithInvalidToken() throws Exception {
        mockMvc.perform(get("/api/v1/products")
                        .header("Authorization", "Bearer invalid.jwt.token"))
                .andExpect(status().isUnauthorized());
    }

        @Test
        void shouldReturn404ForUnknownEndpointWhenAuthenticated() throws Exception {
                String token = registerAndGetToken("route-check-user", "password123", "ADMIN");

                mockMvc.perform(get("/api/v1/not-existing-endpoint")
                                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
        }

    @Test
    void shouldRejectDuplicateUsername() throws Exception {
        registerAndGetToken("dupuser", "password123", "ADMIN");

        String payload = """
                {"username":"dupuser","password":"password456","role":"STAFF"}
                """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldReturn401ForWrongPassword() throws Exception {
        registerAndGetToken("user1", "correctPass1", "STAFF");

        String loginPayload = """
                {"username":"user1","password":"wrongPassword"}
                """;

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400ForInactiveUserLogin() throws Exception {
        registerAndGetToken("inactive-user", "correctPass1", "STAFF");

        AppUser user = userRepository.findByUsername("inactive-user").orElseThrow();
        user.setActive(false);
        userRepository.save(user);

        String loginPayload = """
                {"username":"inactive-user","password":"correctPass1"}
                """;

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    void shouldReturn400ForInvalidRegisterPayload() throws Exception {
        String payload = """
                {"username":"ab","password":"short","role":"INVALID"}
                """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldAdminAccessReports() throws Exception {
        String token = registerAndGetToken("reportadmin", "adminPass1", "ADMIN");

        mockMvc.perform(get("/api/v1/reports/order-summary")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

        @Test
        void shouldAdminAccessLowStock() throws Exception {
                String token = registerAndGetToken("lowstockadmin", "adminPass1", "ADMIN");

                mockMvc.perform(get("/api/v1/products/low-stock")
                                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isOk());
        }

    @Test
    void shouldReturnRefreshTokenOnLogin() throws Exception {
        registerAndGetToken("rtuser", "password123", "ADMIN");

        String loginPayload = """
                {"username":"rtuser","password":"password123"}
                """;

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    @Test
    void shouldRefreshAccessToken() throws Exception {
        String payload = """
                {"username":"refresh1","password":"password123","role":"ADMIN"}
                """;

        String registerJson = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String refreshToken = objectMapper.readTree(registerJson).get("refreshToken").asText();

        String refreshPayload = """
                {"refreshToken":"%s"}
                """.formatted(refreshToken);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    @Test
    void shouldRejectInvalidRefreshToken() throws Exception {
        String refreshPayload = """
                {"refreshToken":"notavalidtoken12345678901234567890123456789012"}
                """;

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshPayload))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn429AfterExceedingLoginRateLimit() throws Exception {
        // Register a user first
        registerAndGetToken("ratelimituser", "password123", "ADMIN");

        String loginPayload = """
                {"username":"ratelimituser","password":"wrongpassword"}
                """;

        // Use a unique IP via X-Forwarded-For to avoid interfering with other tests
        String testIp = "10.99.99.99";

                // Exhaust the configured 10-request bucket
                for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .header("X-Forwarded-For", testIp)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginPayload));
        }

                // 11th request must be rate-limited
        mockMvc.perform(post("/api/v1/auth/login")
                        .header("X-Forwarded-For", testIp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("TOO_MANY_REQUESTS"));
    }

    @Test
    void shouldLogoutAndInvalidateRefreshToken() throws Exception {
        String payload = """
                {"username":"logoutuser","password":"password123","role":"ADMIN"}
                """;

        String registerJson = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String refreshToken = objectMapper.readTree(registerJson).get("refreshToken").asText();

        // Logout
        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(refreshToken)))
                .andExpect(status().isNoContent());

        // Refresh should now fail
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(refreshToken)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldLogoutGracefullyWithInvalidToken() throws Exception {
        // Logout with unknown token — should not throw error
        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"unknowntoken12345678901234567890123"}
                                """))
                .andExpect(status().isNoContent());
    }
}
