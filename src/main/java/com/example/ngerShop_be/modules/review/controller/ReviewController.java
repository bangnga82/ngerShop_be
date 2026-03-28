package com.example.ngerShop_be.modules.review.controller;

import com.example.ngerShop_be.common.response.GlobalResponse;
import com.example.ngerShop_be.common.response.PageResponse;
import com.example.ngerShop_be.modules.review.dto.ReviewRequest;
import com.example.ngerShop_be.modules.review.dto.ReviewResponse;
import com.example.ngerShop_be.modules.review.service.ReviewService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ReviewController {

    ReviewService reviewService;

    @PostMapping
    public GlobalResponse<ReviewResponse> createReview(@RequestBody @Valid ReviewRequest request, Authentication auth) {
        Long userId = requireUserId(auth);
        ReviewResponse response = reviewService.createReview(userId, request);
        return GlobalResponse.ok(response);
    }

    @GetMapping("/product/{productId}")
    public GlobalResponse<PageResponse<ReviewResponse>> getReviewsByProduct(
            @PathVariable UUID productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdDate,desc") String sort) {
        
        String[] sortParams = sort.split(",", 2);
        String sortField = sortParams[0].trim();
        
        if (!sortField.equals("createdDate") && !sortField.equals("rating")) {
            throw new IllegalArgumentException("Invalid sort field. Allowed fields are: createdDate, rating");
        }
        
        Sort.Direction direction = (sortParams.length > 1 && sortParams[1].trim().equalsIgnoreCase("asc"))
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(direction, sortField));
        
        PageResponse<ReviewResponse> response = reviewService.getReviewsByProduct(productId, pageRequest);
        return GlobalResponse.ok(response);
    }

    @DeleteMapping("/{reviewId}")
    public GlobalResponse<String> deleteReview(@PathVariable Long reviewId, Authentication auth) {
        Long userId = requireUserId(auth);
        reviewService.deleteReview(userId, reviewId);
        return GlobalResponse.ok("Review deleted successfully");
    }

    private Long requireUserId(Authentication auth) {
        if (auth == null || auth.getName() == null) {
            throw new IllegalArgumentException("Unauthorized");
        }
        try {
            return Long.parseLong(auth.getName());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid user id");
        }
    }
}
