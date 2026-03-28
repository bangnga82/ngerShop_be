package com.example.ngerShop_be.modules.review.repository;

import com.example.ngerShop_be.modules.review.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    
    // Optimized JOIN query to fetch Review and User info together
    @org.springframework.data.jpa.repository.Query(
        value = "SELECT r.id as id, r.userId as userId, r.productId as productId, " +
                "r.rating as rating, r.comment as comment, r.createdDate as createdDate, " +
                "r.lastModifiedDate as lastModifiedDate, u.fullName as fullName, u.username as username " +
                "FROM Review r JOIN User u ON r.userId = u.id WHERE r.productId = :productId",
        countQuery = "SELECT count(r) FROM Review r WHERE r.productId = :productId"
    )
    Page<com.example.ngerShop_be.modules.review.dto.ReviewProjection> findReviewsWithUserInfo(
            @org.springframework.data.repository.query.Param("productId") UUID productId, 
            Pageable pageable
    );
    
    // Check if user already reviewed the product
    boolean existsByUserIdAndProductId(Long userId, UUID productId);
    
    // Get the exact review a user made for a product
    Optional<Review> findByUserIdAndProductId(Long userId, UUID productId);
}
