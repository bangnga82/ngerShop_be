package com.example.ngerShop_be.modules.auth.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    /** Thời gian hết hạn access token (giây) */
    private long expiresIn;
}
