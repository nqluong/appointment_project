package org.project.appointment_project.user.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.appointment_project.user.dto.request.UpdateMedicalProfileRequest;
import org.project.appointment_project.user.dto.request.UpdateUserProfileRequest;
import org.project.appointment_project.user.dto.response.UpdateMedicalProfileResponse;
import org.project.appointment_project.user.dto.response.UpdateUserProfileResponse;
import org.project.appointment_project.user.service.ProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users/{userId}/profiles")
@RequiredArgsConstructor
@Slf4j
public class ProfileController {
    private final ProfileService profileService;

    @PutMapping("/user")
    public ResponseEntity<UpdateUserProfileResponse> updateUserProfile(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserProfileRequest request) {


        UpdateUserProfileResponse response = profileService.updateUserProfile(userId, request);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/medical")

    public ResponseEntity<UpdateMedicalProfileResponse> updateMedicalProfile(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateMedicalProfileRequest request) {

        log.info("Received request to update medical profile for userId: {}", userId);

        UpdateMedicalProfileResponse response = profileService.updateMedicalProfile(userId, request);


        return ResponseEntity.ok(response);
    }
}
