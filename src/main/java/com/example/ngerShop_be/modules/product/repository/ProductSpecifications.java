package com.example.ngerShop_be.modules.product.repository;

import com.example.ngerShop_be.common.constants.AttributeType;
import com.example.ngerShop_be.modules.product.entity.Product;
import com.example.ngerShop_be.modules.product.entity.ProductAttribute;
import com.example.ngerShop_be.modules.product.entity.ProductVariant;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.util.Arrays;

public final class ProductSpecifications {
    private ProductSpecifications() {}

    public static Specification<Product> distinct() {
        return (root, query, cb) -> {
            query.distinct(true);
            return cb.conjunction();
        };
    }

    public static Specification<Product> isActive() {
        return (root, query, cb) -> cb.isTrue(root.get("isActive"));
    }

    public static Specification<Product> categoryContains(String category) {
        if (category == null || category.isBlank()) {
            return Specification.where(null);
        }
        return (root, query, cb) -> cb.like(
                cb.lower(root.get("category").get("name")),
                "%" + category.toLowerCase() + "%"
        );
    }

    public static Specification<Product> nameContains(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return Specification.where(null);
        }
        String[] tokens = Arrays.stream(keyword.toLowerCase().trim().split("\\s+"))
                .filter(token -> !token.isBlank())
                .toArray(String[]::new);

        if (tokens.length == 0) {
            return Specification.where(null);
        }

        return (root, query, cb) -> cb.and(
                Arrays.stream(tokens)
                        .map(token -> cb.like(cb.lower(root.get("name")), "%" + token + "%"))
                        .toArray(jakarta.persistence.criteria.Predicate[]::new)
        );
    }

    public static Specification<Product> colorContains(String color) {
        if (color == null || color.isBlank()) {
            return Specification.where(null);
        }
        return (root, query, cb) -> {
            Join<Product, ProductVariant> variant = root.join("variants", JoinType.LEFT);
            Join<ProductVariant, ProductAttribute> attribute = variant.join("attributes", JoinType.LEFT);
            return cb.and(
                    cb.equal(attribute.get("type"), AttributeType.COLOR),
                    cb.like(cb.lower(attribute.get("value")), "%" + color.toLowerCase() + "%")
            );
        };
    }

    public static Specification<Product> attributeValueContains(String attributeValue) {
        if (attributeValue == null || attributeValue.isBlank()) {
            return Specification.where(null);
        }
        return (root, query, cb) -> {
            Join<Product, ProductVariant> variant = root.join("variants", JoinType.LEFT);
            Join<ProductVariant, ProductAttribute> attribute = variant.join("attributes", JoinType.LEFT);
            return cb.like(
                    cb.lower(attribute.get("value")),
                    "%" + attributeValue.toLowerCase() + "%"
            );
        };
    }

    public static Specification<Product> minVariantPrice(Double minPrice) {
        if (minPrice == null) {
            return Specification.where(null);
        }
        return (root, query, cb) -> {
            Join<Product, ProductVariant> variant = root.join("variants", JoinType.LEFT);
            return cb.greaterThanOrEqualTo(variant.get("price"), minPrice);
        };
    }

    public static Specification<Product> maxVariantPrice(Double maxPrice) {
        if (maxPrice == null) {
            return Specification.where(null);
        }
        return (root, query, cb) -> {
            Join<Product, ProductVariant> variant = root.join("variants", JoinType.LEFT);
            return cb.lessThanOrEqualTo(variant.get("price"), maxPrice);
        };
    }
}
