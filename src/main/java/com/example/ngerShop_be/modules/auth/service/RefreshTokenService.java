package com.example.ngerShop_be.modules.auth.service;

import com.example.ngerShop_be.common.exception.BadRequestException;
import com.example.ngerShop_be.modules.auth.entity.RefreshToken;
import com.example.ngerShop_be.modules.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${app.security.jwt.refreshExpirationDays:7}")
    private long refreshExpirationDays;

    /**
     * Tạo refresh token mới cho user.
     * Xóa token cũ trước để mỗi user chỉ có 1 token active.
     */
    @Transactional
    public RefreshToken createRefreshToken(Long userId) {
        // Xóa token cũ nếu có (rotation policy)
        refreshTokenRepository.deleteByUserId(userId);

        RefreshToken refreshToken = RefreshToken.builder()
                .userId(userId)
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusSeconds(refreshExpirationDays * 24 * 60 * 60))
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    /**
     * Verify refresh token: tìm trong DB, kiểm tra hết hạn chưa.
     */
    public RefreshToken verifyRefreshToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new BadRequestException("Refresh token không hợp lệ hoặc đã bị thu hồi"));

        if (refreshToken.isExpired()) {
            refreshTokenRepository.delete(refreshToken);
            throw new BadRequestException("Refresh token đã hết hạn, vui lòng đăng nhập lại");
        }

        return refreshToken;
    }

    /**
     * Xóa refresh token khi logout.
     */
    @Transactional
    public void revokeByUserId(Long userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }
}
