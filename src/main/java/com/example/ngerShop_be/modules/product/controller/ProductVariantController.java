package com.example.ngerShop_be.modules.product.controller;

import com.example.ngerShop_be.common.response.GlobalResponse;
import com.example.ngerShop_be.modules.product.dto.ProductResponse;
import com.example.ngerShop_be.modules.product.dto.ProductVariantRequest;
import com.example.ngerShop_be.modules.product.dto.ProductVariantResponse;
import com.example.ngerShop_be.modules.product.service.ProductVariantService;
import com.example.ngerShop_be.common.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
            @RequestParam("productId") UUID productId,
            @RequestParam("price") String price,
            @RequestParam("stock") String stock,
            @RequestParam(name = "image", required = false) MultipartFile image
    ) {
        Double parsedPrice = parseNonNegativeDouble(price, "price");
        Integer parsedStock = parseNonNegativeInt(stock, "stock");
        ProductVariantRequest variantRequest = new ProductVariantRequest(productId, parsedPrice, parsedStock, image);
        return ResponseEntity.ok(variantService.createVariantToProduct(variantRequest));
    }

    @PutMapping(value = "/{variantId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GlobalResponse<ProductResponse>> updateProductVariant(
            @PathVariable(name = "variantId") UUID variantId,
            @RequestParam("productId") UUID productId,
            @RequestParam("price") String price,
            @RequestParam("stock") String stock,
            @RequestParam(name = "image", required = false) MultipartFile image
    ) {
        Double parsedPrice = parseNonNegativeDouble(price, "price");
        Integer parsedStock = parseNonNegativeInt(stock, "stock");
        ProductVariantRequest variantRequest = new ProductVariantRequest(productId, parsedPrice, parsedStock, image);
        return ResponseEntity.ok(variantService.updateVariantProduct(variantId, variantRequest));
    }

    private Integer parseNonNegativeInt(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new BadRequestException(field + " cannot be null");
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            if (parsed < 0) {
                throw new BadRequestException(field + " must be >= 0");
            }
            return parsed;
        } catch (NumberFormatException ex) {
            throw new BadRequestException(field + " is invalid");
        }
    }

    private Double parseNonNegativeDouble(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new BadRequestException(field + " cannot be null");
        }
        try {
            double parsed = Double.parseDouble(value.trim());
            if (parsed < 0) {
                throw new BadRequestException(field + " must be >= 0");
            }
            return parsed;
        } catch (NumberFormatException ex) {
            throw new BadRequestException(field + " is invalid");
        }
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
