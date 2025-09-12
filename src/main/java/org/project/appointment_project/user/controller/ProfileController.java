package org.project.appointment_project.user.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.appointment_project.user.dto.request.PhotoUploadRequest;
import org.project.appointment_project.user.dto.request.UpdateCompleteProfileRequest;

import org.project.appointment_project.user.dto.request.UpdateMedicalProfileRequest;
import org.project.appointment_project.user.dto.request.UpdateUserProfileRequest;
import org.project.appointment_project.user.dto.response.CompleteProfileResponse;
import org.project.appointment_project.user.dto.response.PhotoUploadResponse;
import org.project.appointment_project.user.dto.response.UpdateMedicalProfileResponse;
import org.project.appointment_project.user.dto.response.UpdateUserProfileResponse;
import org.project.appointment_project.user.service.PhotoUploadService;
import org.project.appointment_project.user.service.ProfileService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/users/{userId}/profiles")
@RequiredArgsConstructor
@Slf4j
public class ProfileController {
    private final ProfileService profileService;
    private final PhotoUploadService photoUploadService;

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

    @PutMapping("/complete")
    public ResponseEntity<CompleteProfileResponse> updateCompleteProfile(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateCompleteProfileRequest request) {

        CompleteProfileResponse response = profileService.updateCompleteProfile(userId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/complete")
    public ResponseEntity<CompleteProfileResponse> getCompleteProfile(
            @PathVariable UUID userId) {

        CompleteProfileResponse response = profileService.getCompleteProfile(userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PhotoUploadResponse> uploadUserPhoto(
            @PathVariable UUID userId,
            @RequestParam("photo") MultipartFile photo) {


        PhotoUploadRequest request = PhotoUploadRequest.builder()
                .photo(photo)
                .build();

        PhotoUploadResponse response = photoUploadService.uploadUserPhoto(userId, request);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
}
