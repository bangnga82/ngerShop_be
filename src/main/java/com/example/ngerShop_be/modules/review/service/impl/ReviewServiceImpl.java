package com.example.ngerShop_be.modules.review.service.impl;

import com.example.ngerShop_be.common.exception.BadRequestException;
import com.example.ngerShop_be.common.exception.NotFoundException;
import com.example.ngerShop_be.common.response.PageResponse;
import com.example.ngerShop_be.modules.order.repository.OrderRepository;
import com.example.ngerShop_be.modules.product.entity.Product;
import com.example.ngerShop_be.modules.product.repository.ProductRepository;
import com.example.ngerShop_be.modules.review.dto.ReviewRequest;
import com.example.ngerShop_be.modules.review.dto.ReviewResponse;
import com.example.ngerShop_be.modules.review.entity.Review;
import com.example.ngerShop_be.modules.review.repository.ReviewRepository;
import com.example.ngerShop_be.modules.review.service.ReviewService;
import com.example.ngerShop_be.modules.user.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ReviewServiceImpl implements ReviewService {

    ReviewRepository reviewRepository;
    ProductRepository productRepository;
    OrderRepository orderRepository;
    UserRepository userRepository;

    @Override
    @Transactional
    public ReviewResponse createReview(Long userId, ReviewRequest request) {
        // 1. Check if product exists
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new NotFoundException("Product not found"));

        // 2. Check if user already reviewed this product
        if (reviewRepository.existsByUserIdAndProductId(userId, request.getProductId())) {
            throw new BadRequestException("You have already reviewed this product.");
        }

        // 3. Check if user has purchased this product and order status is DELIVERED
        boolean hasPurchased = orderRepository.hasUserPurchasedProduct(userId, request.getProductId());
        if (!hasPurchased) {
            throw new BadRequestException("You can only review products you have successfully purchased and received.");
        }

        // 4. Create and save review
        Review review = new Review();
        review.setUserId(userId);
        review.setProductId(request.getProductId());
        review.setRating(request.getRating());
        review.setComment(request.getComment());

        Review savedReview = reviewRepository.save(review);

        // Map to Response
        return mapToResponse(savedReview);
    }

    @Override
    public PageResponse<ReviewResponse> getReviewsByProduct(UUID productId, Pageable pageable) {
        // Check if product exists
        if (!productRepository.existsById(productId)) {
            throw new NotFoundException("Product not found");
        }

        // Fetch exactly 1 query to get all reviews + user names
        Page<com.example.ngerShop_be.modules.review.dto.ReviewProjection> reviewPage =
                reviewRepository.findReviewsWithUserInfo(productId, pageable);

        List<ReviewResponse> responseList = reviewPage.getContent().stream()
                .map(proj -> ReviewResponse.builder()
                        .id(proj.getId())
                        .userId(proj.getUserId())
                        // Favor fullName, fallback to username, fallback to "Unknown User"
                        .userName(proj.getFullName() != null && !proj.getFullName().isEmpty() ?
                                proj.getFullName() : (proj.getUsername() != null ? proj.getUsername() : "Unknown User"))
                        .productId(proj.getProductId())
                        .rating(proj.getRating())
                        .comment(proj.getComment())
                        .createdDate(proj.getCreatedDate())
                        .lastModifiedDate(proj.getLastModifiedDate())
                        .build())
                .collect(Collectors.toList());

        return new PageResponse<>(
                responseList,
                reviewPage.getTotalPages(),
                reviewPage.getTotalElements(),
                reviewPage.getNumber(),
                reviewPage.getSize(),
                reviewPage.getNumberOfElements(),
                reviewPage.isFirst(),
                reviewPage.isLast(),
                reviewPage.hasNext(),
                reviewPage.hasPrevious()
        );
    }

    @Override
    @Transactional
    public void deleteReview(Long userId, Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new NotFoundException("Review not found"));

        // Ensure only the owner can delete their review (or an admin, but that logic goes to controller roles usually)
        if (!review.getUserId().equals(userId)) {
            throw new org.springframework.security.access.AccessDeniedException("You don't have permission to delete this review.");
        }

        reviewRepository.delete(review);
    }

    private ReviewResponse mapToResponse(Review review) {
        String userName = userRepository.findById(review.getUserId())
                .map(user -> user.getFullName() != null ? user.getFullName() : user.getUsername())
                .orElse("Unknown User");

        return ReviewResponse.builder()
                .id(review.getId())
                .userId(review.getUserId())
                .userName(userName)
                .productId(review.getProductId())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdDate(review.getCreatedDate())
                .lastModifiedDate(review.getLastModifiedDate())
                .build();
    }
}
