package com.example.ngerShop_be.modules.product.controller;


import com.example.ngerShop_be.common.response.GlobalResponse;
import com.example.ngerShop_be.modules.product.dto.ProductAttributeRequest;
import com.example.ngerShop_be.modules.product.dto.ProductAttributeResponse;
import com.example.ngerShop_be.modules.product.service.ProductAttributeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/attributes")
public class ProductAttributeController {

    private final ProductAttributeService attributeService;

    // 1. Thêm thuộc tính cho Variant (Admin)
    @PostMapping("/{variantId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GlobalResponse<ProductAttributeResponse>> createAttribute(
            @PathVariable UUID variantId,
            @RequestBody @Valid ProductAttributeRequest request) {
        return ResponseEntity.ok(attributeService.createAttribute(variantId, request));
    }

    // 2. Lấy danh sách thuộc tính của 1 Variant (Public)
    @GetMapping("/variant/{variantId}")
    public ResponseEntity<GlobalResponse<List<ProductAttributeResponse>>> getByVariant(@PathVariable UUID variantId) {
        return ResponseEntity.ok(attributeService.getAttributesByVariantId(variantId));
    }

    // 3. Cập nhật thuộc tính (Admin)
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GlobalResponse<ProductAttributeResponse>> updateAttribute(
            @PathVariable UUID id,
            @RequestBody @Valid ProductAttributeRequest request) {
        return ResponseEntity.ok(attributeService.updateAttribute(id, request));
    }

    // 4. Xóa thuộc tính (Admin)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GlobalResponse<String>> deleteAttribute(@PathVariable UUID id) {
        return ResponseEntity.ok(attributeService.deleteAttribute(id));
    }

    // 5. Tìm kiếm theo Type và Value (Ví dụ: Tìm tất cả màu 'Red')
    @GetMapping("/search")
    public ResponseEntity<GlobalResponse<List<ProductAttributeResponse>>> search(
            @RequestParam String type,
            @RequestParam String value) {
        return ResponseEntity.ok(attributeService.findByTypeAndValue(type, value));
    }
    @GetMapping("/{id}")
    public ResponseEntity<GlobalResponse<ProductAttributeResponse>> getAttributeById(@PathVariable UUID id) {
        return ResponseEntity.ok(attributeService.getAttributeById(id));
    }
}