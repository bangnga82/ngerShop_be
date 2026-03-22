package com.example.ngerShop_be.modules.product.entity;

import com.example.ngerShop_be.common.constants.AttributeType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "product_attributes")
@Builder
@Entity
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductAttribute {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    UUID id;

    @Enumerated(EnumType.STRING)
    AttributeType type;

    @Column(nullable = false)
    String value;

    @ManyToOne
    @JoinColumn(name = "variant_id", nullable = false)
    ProductVariant productVariant;
}