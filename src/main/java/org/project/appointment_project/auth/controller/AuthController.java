package org.project.appointment_project.auth.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.project.appointment_project.auth.dto.LoginRequest;
import org.project.appointment_project.auth.dto.LoginResponse;
import org.project.appointment_project.auth.dto.RefreshTokenRequest;
import org.project.appointment_project.auth.dto.TokenResponse;
import org.project.appointment_project.auth.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<TokenResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        TokenResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(response);
    }
}
