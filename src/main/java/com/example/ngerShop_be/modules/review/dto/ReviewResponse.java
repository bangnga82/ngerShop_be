package com.example.ngerShop_be.modules.review.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReviewResponse {
    Long id;
    Long userId;
    String userName; // Frontend will need this to display who wrote the review
    UUID productId;
    int rating;
    String comment;
    LocalDateTime createdDate;
    LocalDateTime lastModifiedDate;
}
