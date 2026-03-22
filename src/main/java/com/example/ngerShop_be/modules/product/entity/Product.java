package com.example.ngerShop_be.modules.product.entity;


import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "products")
@Builder
@Entity
@FieldDefaults(level = AccessLevel.PRIVATE)
@EntityListeners(AuditingEntityListener.class)
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(nullable = false, updatable = false, unique = true)
    UUID id;

    @Column(nullable = false)
    String name;

    String description;

    @Column(nullable = false)
    Double price;

    Boolean isActive = true;

    @CreatedBy
    @Column(nullable = false, updatable = false)
    String createdBy;

    @ManyToOne
    @JoinColumn(name = "category_id")
    Category category;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    List<ProductVariant> variants = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    List<ProductImage> images = new ArrayList<>();

    @CreatedDate
    @Column(updatable = false, nullable = false)
    LocalDateTime createdDate;

    @Column(nullable = false, updatable = false)
    String creatorName;

    @LastModifiedDate
    @Column(nullable = false)
    LocalDateTime lastModifiedDate;
}
