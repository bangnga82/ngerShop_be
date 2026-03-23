package com.example.ngerShop_be.modules.product.controller;

import com.example.ngerShop_be.common.response.GlobalResponse;
import com.example.ngerShop_be.modules.product.dto.*;
import com.example.ngerShop_be.modules.product.service.ProductVariantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

//
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/variants")
public class ProductVariantController {
    private final ProductVariantService variantService;

    // --- CÁC API CHO ADMIN QUẢN LÝ ---

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GlobalResponse<ProductResponse>> createProductVariantToProduct(
            @ModelAttribute @Valid ProductVariantRequest variantRequest
    ) {
        return ResponseEntity.ok(variantService.createVariantToProduct(variantRequest));
    }

    @PutMapping(value = "/{variantId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GlobalResponse<ProductResponse>> updateProductVariant(
            @PathVariable(name = "variantId") UUID variantId,
            @Valid @ModelAttribute ProductVariantRequest variantRequest
    ) {
        return ResponseEntity.ok(variantService.updateVariantProduct(variantId, variantRequest));
    }

    @DeleteMapping("/{variantId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GlobalResponse<String>> deleteProductVariantById(
            @PathVariable(name = "variantId") UUID variantId
    ) {
        return ResponseEntity.ok(variantService.deleteProductVariantById(variantId));
    }

    @PostMapping("/{variantId}/upload")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GlobalResponse<String>> uploadImageToVariant(
            @PathVariable(name = "variantId") UUID variantId,
            @RequestParam(name = "image") MultipartFile image
    ) {
        return ResponseEntity.ok(variantService.uploadImageToVariant(variantId, image));
    }

    // --- CÁC API CHO NGƯỜI DÙNG XEM ---

    @GetMapping("/{variantId}")
    public ResponseEntity<GlobalResponse<ProductVariantResponse>> getProductVariantById(
            @PathVariable(name = "variantId") UUID variantId
    ) {
        return ResponseEntity.ok(variantService.getProductVariantById(variantId));
    }

    // ĐÃ XÓA: check-stock, get-prices, update-stock
    // Vì OrderServiceImpl sẽ gọi trực tiếp các hàm này thông qua variantService (Java call)
}
