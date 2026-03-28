package com.example.ngerShop_be.modules.review.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "reviews")
@EntityListeners(AuditingEntityListener.class) // Phải có cái này để nó "nghe" sự kiện Save
@Getter @Setter
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId; // Kiểu Long như User.id

    @Column(name = "product_id", nullable = false)
    private UUID productId; // Kiểu UUID như Product.id

    @Min(1) @Max(5)
    private int rating;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @CreatedDate
    @Column(updatable = false) // Chỉ tạo lúc insert, không cho sửa lúc update
    private LocalDateTime createdDate;

    @LastModifiedDate
    private LocalDateTime lastModifiedDate;
}