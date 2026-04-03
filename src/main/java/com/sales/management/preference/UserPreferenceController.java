package com.sales.management.preference;

import com.sales.management.preference.dto.UserPreferenceResponse;
import com.sales.management.preference.dto.UserPreferenceUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/preferences/me")
public class UserPreferenceController {

    private final UserPreferenceService userPreferenceService;

    public UserPreferenceController(UserPreferenceService userPreferenceService) {
        this.userPreferenceService = userPreferenceService;
    }

    @GetMapping
    public UserPreferenceResponse getMine() {
        return userPreferenceService.getMine();
    }

    @PutMapping
    public UserPreferenceResponse updateMine(@Valid @RequestBody UserPreferenceUpdateRequest request) {
        return userPreferenceService.updateMine(request);
    }
}
