package com.example.ngerShop_be.modules.review.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public interface ReviewProjection {
    Long getId();
    Long getUserId();
    UUID getProductId();
    int getRating();
    String getComment();
    LocalDateTime getCreatedDate();
    LocalDateTime getLastModifiedDate();
    String getFullName();
    String getUsername();
    // You can add String getAvatarUrl(); here later if you want to display avatars!
}
