package com.example.ngerShop_be.modules.review.service;

import com.example.ngerShop_be.common.response.PageResponse;
import com.example.ngerShop_be.modules.review.dto.ReviewRequest;
import com.example.ngerShop_be.modules.review.dto.ReviewResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ReviewService {
    ReviewResponse createReview(Long userId, ReviewRequest request);
    
    PageResponse<ReviewResponse> getReviewsByProduct(UUID productId, Pageable pageable);
    
    void deleteReview(Long userId, Long reviewId);
}
