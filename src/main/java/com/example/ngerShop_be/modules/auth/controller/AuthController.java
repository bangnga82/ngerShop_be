package com.example.ngerShop_be.modules.auth.controller;

import com.example.ngerShop_be.common.response.GlobalResponse;
import com.example.ngerShop_be.modules.auth.dto.AuthResponse;
import com.example.ngerShop_be.modules.auth.dto.LoginRequest;
import com.example.ngerShop_be.modules.auth.dto.RegisterRequest;
import com.example.ngerShop_be.modules.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final String googleFrontendRedirectUri;

    public AuthController(
            AuthService authService,
            @Value("${app.oauth.google.frontendRedirectUri:http://localhost:3000/auth/callback}")
            String googleFrontendRedirectUri
    ) {
        this.authService = authService;
        this.googleFrontendRedirectUri = googleFrontendRedirectUri;
    }

    @PostMapping("/register")
    public ResponseEntity<GlobalResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(GlobalResponse.ok(authService.register(request)));
    }

    @PostMapping("/login")
    public ResponseEntity<GlobalResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(GlobalResponse.ok(authService.login(request)));
    }

    @GetMapping("/login")
    public ResponseEntity<GlobalResponse<String>> loginWithGoogle() {
        return ResponseEntity.ok(GlobalResponse.ok(authService.getGoogleLoginUrl()));
    }

    @GetMapping("/callback")
    public RedirectView googleCallback(
            @RequestParam("code") String code,
            @RequestParam("state") String state
    ) {
        AuthResponse response = authService.handleGoogleCallback(code, state);
        String encodedToken = URLEncoder.encode(response.getToken(), StandardCharsets.UTF_8);
        String separator = googleFrontendRedirectUri.contains("?") ? "&" : "?";
        String redirectUrl = googleFrontendRedirectUri + separator + "token=" + encodedToken;
        return new RedirectView(redirectUrl);
    }
}
