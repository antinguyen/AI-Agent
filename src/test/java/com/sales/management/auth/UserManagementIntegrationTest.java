package com.sales.management.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(roles = "ADMIN")
@SuppressWarnings("null")
class UserManagementIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    private Long createUserAndGetId(String username, String role) throws Exception {
        String payload = """
                {"username":"%s","password":"password123","role":"%s"}
                """.formatted(username, role);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());

        return userRepository.findByUsername(username).orElseThrow().getId();
    }

    @Test
    void shouldCreateUserViaUserManagementEndpoint() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"created-by-admin","password":"password123","role":"STAFF"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("created-by-admin"))
                .andExpect(jsonPath("$.role").value("STAFF"));
    }

    @Test
    void shouldListUsers() throws Exception {
        createUserAndGetId("user-a", "ADMIN");
        createUserAndGetId("user-b", "STAFF");

        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content", hasSize(2)));
    }

            @Test
            void shouldFilterUsersByUsernameRoleAndActive() throws Exception {
            createUserAndGetId("admin-find-me", "ADMIN");
            Long staffId = createUserAndGetId("staff-hide", "STAFF");

            mockMvc.perform(put("/api/v1/users/{id}/deactivate", staffId))
                .andExpect(status().isOk());

            mockMvc.perform(get("/api/v1/users")
                    .param("username", "find")
                    .param("role", "ADMIN")
                    .param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].username").value("admin-find-me"));
            }

    @Test
    void shouldDeactivateUser() throws Exception {
        Long id = createUserAndGetId("deact-user", "STAFF");

        mockMvc.perform(put("/api/v1/users/{id}/deactivate", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void shouldActivateUser() throws Exception {
        Long id = createUserAndGetId("act-user", "STAFF");

        // Deactivate first
        mockMvc.perform(put("/api/v1/users/{id}/deactivate", id))
                .andExpect(status().isOk());

        // Then reactivate
        mockMvc.perform(put("/api/v1/users/{id}/activate", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void shouldChangeUserRole() throws Exception {
        Long id = createUserAndGetId("role-user", "STAFF");

        mockMvc.perform(put("/api/v1/users/{id}/role", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"role":"ADMIN"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

            @Test
            void shouldDeleteUser() throws Exception {
            Long id = createUserAndGetId("delete-user", "STAFF");

            mockMvc.perform(delete("/api/v1/users/{id}", id))
                .andExpect(status().isOk());

            mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
            }

    @Test
    void shouldResetPassword() throws Exception {
        Long id = createUserAndGetId("reset-pass-user", "STAFF");

        mockMvc.perform(put("/api/v1/users/{id}/password", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"newPassword":"NewStrongPass123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("reset-pass-user"));

        AppUser saved = userRepository.findById(id).orElseThrow();
        org.junit.jupiter.api.Assertions.assertTrue(passwordEncoder.matches("NewStrongPass123", saved.getPassword()));
    }

    @Test
    void shouldReturn404ForNonExistentUser() throws Exception {
        mockMvc.perform(put("/api/v1/users/99999/deactivate"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn400ForInvalidRole() throws Exception {
        Long id = createUserAndGetId("bad-role-user", "STAFF");

        mockMvc.perform(put("/api/v1/users/{id}/role", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"role":"SUPERUSER"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn403ForStaffAccessingUsers() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                        .with(org.springframework.security.test.web.servlet.request
                                .SecurityMockMvcRequestPostProcessors.user("staff").roles("STAFF")))
                .andExpect(status().isForbidden());
    }
}


