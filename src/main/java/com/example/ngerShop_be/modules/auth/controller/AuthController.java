package com.example.ngerShop_be.modules.auth.controller;

import com.example.ngerShop_be.common.response.ApiResponse;
import com.example.ngerShop_be.modules.auth.dto.AuthResponse;
import com.example.ngerShop_be.modules.auth.dto.LoginRequest;
import com.example.ngerShop_be.modules.auth.dto.RegisterRequest;
import com.example.ngerShop_be.modules.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.register(request)));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.login(request)));
    }

    @GetMapping("/login")
    public ResponseEntity<ApiResponse<String>> loginWithGoogle() {
        return ResponseEntity.ok(ApiResponse.ok(authService.getGoogleLoginUrl()));
    }

    @GetMapping("/callback")
    public ResponseEntity<ApiResponse<AuthResponse>> googleCallback(
            @RequestParam("code") String code,
            @RequestParam("state") String state
    ) {
        return ResponseEntity.ok(ApiResponse.ok(authService.handleGoogleCallback(code, state)));
    }
}