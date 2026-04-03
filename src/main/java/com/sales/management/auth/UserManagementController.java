package com.sales.management.auth;

import com.sales.management.auth.dto.ChangeRoleRequest;
import com.sales.management.auth.dto.RegisterRequest;
import com.sales.management.auth.dto.ResetPasswordRequest;
import com.sales.management.auth.dto.UserResponse;
import com.sales.management.common.api.PageResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserManagementController {

    private final UserManagementService userManagementService;

    public UserManagementController(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createUser(@Valid @RequestBody RegisterRequest request) {
        return userManagementService.createUser(request);
    }

    @GetMapping
    public PageResponse<UserResponse> listUsers(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "username", required = false) String username,
            @RequestParam(value = "role", required = false) UserRole role,
            @RequestParam(value = "active", required = false) Boolean active) {
        return PageResponse.from(userManagementService.listUsers(
                PageRequest.of(page, size, Sort.by("createdAt").descending()),
                username,
                role,
                active));
    }

    @PutMapping("/{id}/deactivate")
    public UserResponse deactivate(@PathVariable("id") Long id) {
        return userManagementService.deactivateUser(id);
    }

    @PutMapping("/{id}/activate")
    public UserResponse activate(@PathVariable("id") Long id) {
        return userManagementService.activateUser(id);
    }

    @PutMapping("/{id}/role")
    public UserResponse changeRole(@PathVariable("id") Long id,
                                   @Valid @RequestBody ChangeRoleRequest request) {
        return userManagementService.changeRole(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable("id") Long id) {
        userManagementService.deleteUser(id);
    }

    @PutMapping("/{id}/password")
    public UserResponse resetPassword(@PathVariable("id") Long id,
                                      @Valid @RequestBody ResetPasswordRequest request) {
        return userManagementService.resetPassword(id, request);
    }
}
