package com.example.ngerShop_be.modules.product.service.impl;

import com.example.ngerShop_be.common.exception.BusinessException;
import com.example.ngerShop_be.common.exception.NotFoundException;
import com.example.ngerShop_be.common.response.GlobalResponse;
import com.example.ngerShop_be.modules.order.dto.OrderItemRequest;
import com.example.ngerShop_be.modules.product.dto.*;
import com.example.ngerShop_be.modules.product.entity.Product;
import com.example.ngerShop_be.modules.product.entity.ProductVariant;
import com.example.ngerShop_be.modules.product.repository.ProductRepository;
import com.example.ngerShop_be.modules.product.repository.ProductVariantRepository;
import com.example.ngerShop_be.modules.product.service.ProductVariantService;
import com.example.ngerShop_be.modules.product.util.FileUtil;
import com.example.ngerShop_be.modules.product.util.ProductMapperUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductVariantServiceImpl implements ProductVariantService {
    private final ProductVariantRepository variantRepository;
    private final ProductRepository productRepository;
    private final FileUtil fileUtil;
    private final ProductMapperUtil productMapperUtil;

    @Override
    @Transactional
    public GlobalResponse<ProductResponse> createVariantToProduct(ProductVariantRequest variantRequest) {
        Product product = requireProduct(variantRequest.productId());
        ProductVariant variant = ProductVariant.builder()
                .product(product)
                .price(variantRequest.price())
                .stock(variantRequest.stock())
                .build();
        if (variantRequest.image() != null) {
            variant.setImageUrl(fileUtil.saveImage(variantRequest.image()));
        }
        variantRepository.save(variant);
        product.getVariants().add(variant);
        return GlobalResponse.ok(productMapperUtil.toProductResponse(product));
    }

    @Override
    @Transactional
    public GlobalResponse<ProductResponse> updateVariantProduct(UUID variantId, ProductVariantRequest variantRequest) {
        ProductVariant variant = requireVariant(variantId);
        variant.setPrice(variantRequest.price());
        variant.setStock(variantRequest.stock());
        if (variantRequest.image() != null && !variantRequest.image().isEmpty()) {
            if (variant.getImageUrl() != null) {
                fileUtil.deleteImage(variant.getImageUrl());
            }
            variant.setImageUrl(fileUtil.saveImage(variantRequest.image()));
        }
        variantRepository.save(variant);
        return GlobalResponse.ok(productMapperUtil.toProductResponse(variant.getProduct()));
    }

    @Override
    @Transactional
    public GlobalResponse<String> deleteProductVariantById(UUID variantId) {
        ProductVariant variant = requireVariant(variantId);
        if (variant.getImageUrl() != null) {
            fileUtil.deleteImage(variant.getImageUrl());
        }
        variant.getProduct().getVariants().remove(variant);
        variantRepository.delete(variant);
        return GlobalResponse.ok("Deleted");
    }

    @Override
    @Transactional
    public GlobalResponse<String> uploadImageToVariant(UUID variantId, MultipartFile image) {
        ProductVariant variant = requireVariant(variantId);
        variant.setImageUrl(fileUtil.saveImage(image));
        variantRepository.save(variant);
        return GlobalResponse.ok("Uploaded");
    }

    @Override
    public GlobalResponse<ProductVariantResponse> getProductVariantById(UUID variantId) {
        ProductVariant v = requireVariant(variantId);
        ProductVariantResponse response = ProductVariantResponse.builder()
                .id(v.getId())
                .productId(v.getProduct() != null ? v.getProduct().getId() : null)
                .productName(v.getProduct().getName())
                .stock(v.getStock())
                .price(v.getPrice())
                .imageUrl(v.getImageUrl())
                .attributes(v.getAttributes().stream()
                        .map(a -> ProductAttributeResponse.builder()
                                .id(a.getId())
                                .type(a.getType() != null ? a.getType().name() : null)
                                .value(a.getValue())
                                .build())
                        .toList())
                .build();
        return GlobalResponse.ok(response);
    }

    @Override
    public boolean checkStock(List<OrderItemRequest> requests) {
        for (OrderItemRequest req : requests) {
            ProductVariant v = variantRepository.findById(req.variantId())
                    .orElseThrow(() -> new NotFoundException("Khong tim thay bien the ID: " + req.variantId()));
            if (v.getStock() < req.quantity()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public List<ProductPriceResponse> getProductPrices(List<UUID> variantIds) {
        return variantIds.stream().map(id -> {
            ProductVariant v = variantRepository.findById(id)
                    .orElseThrow(() -> new NotFoundException("Khong tim thay bien the ID: " + id));
            return new ProductPriceResponse(
                    v.getId(),
                    v.getProduct().getName(),
                    0,
                    v.getPrice()
            );
        }).toList();
    }

    @Override
    @Transactional
    public void updateStock(List<OrderItemRequest> requests) {
        for (OrderItemRequest req : requests) {
            ProductVariant v = variantRepository.findById(req.variantId())
                    .orElseThrow(() -> new NotFoundException("Khong tim thay bien the"));

            int updated = variantRepository.updateStock(v.getId(), req.quantity());
            if (updated == 0) {
                throw new BusinessException("San pham " + v.getProduct().getName() + " da het hang!");
            }
        }
    }

    private Product requireProduct(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Khong tim thay san pham voi ID cung cap"));
    }

    private ProductVariant requireVariant(UUID id) {
        return variantRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Khong tim thay san pham bien the voi ID cung cap"));
    }
}
