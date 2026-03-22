package com.example.ngerShop_be.modules.product.entity;


import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "product_images")
@Builder
@Entity
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductImage {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    UUID id;

    String imageUrl; // Đường dẫn ảnh sản phẩm

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    Product product;
}