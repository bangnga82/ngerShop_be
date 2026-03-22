package com.example.ngerShop_be.modules.product.controller;

import com.example.ngerShop_be.common.response.GlobalResponse;
import com.example.ngerShop_be.common.response.PageResponse;
import com.example.ngerShop_be.modules.product.dto.ProductRequest;
import com.example.ngerShop_be.modules.product.dto.ProductResponse;
import com.example.ngerShop_be.modules.product.dto.UpdateProductRequest;
import com.example.ngerShop_be.modules.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/products")
public class ProductController {
    private final ProductService productService;
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GlobalResponse<ProductResponse>> createProduct(
            @ModelAttribute @Valid ProductRequest request
    ) {
        return ResponseEntity.ok(productService.createProduct(request));
    }

    @GetMapping
    public ResponseEntity<GlobalResponse<PageResponse<ProductResponse>>> findAllProducts(
            @RequestParam(name = "sortedBy", required = false) String sortedBy,
            @RequestParam(name = "sortDirection", required = false, defaultValue = "asc") String sortDirection,
            @RequestParam(name = "page", required = false, defaultValue = "0") int page,
            @RequestParam(name = "size", required = false, defaultValue = "10") int size,
            @RequestParam(name = "searchKeyword", required = false) String searchKeyword,
            @RequestParam(name = "category", required = false) String category,
            @RequestParam(name = "minPrice", required = false) Double minPrice,
            @RequestParam(name = "maxPrice", required = false) Double maxPrice,
            @RequestParam(name = "status", required = false, defaultValue = "true") boolean status
    ) {
        return ResponseEntity.ok(productService.findAllProducts(
                sortedBy, sortDirection, page, size, searchKeyword, category, minPrice, maxPrice, status
        ));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<GlobalResponse<ProductResponse>> getProductById(
            @PathVariable(name = "productId") UUID productId
    ) {
        return ResponseEntity.ok(productService.getProductById(productId));
    }

    @GetMapping("/search")
    public ResponseEntity<GlobalResponse<List<ProductResponse>>> findByKeyword(
            @RequestParam(name = "keyword") String keyword
    ) {
        return ResponseEntity.ok(productService.searchByKeyword(keyword));
    }

    @PutMapping(value = "/{productId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GlobalResponse<ProductResponse>> updateProduct(
            @PathVariable(name = "productId") UUID productId,
            @Valid
            @ModelAttribute UpdateProductRequest request
    ) {
        return ResponseEntity.ok(productService.updateProduct(productId, request));
    }

    @PutMapping("/{productId}/upload")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GlobalResponse<ProductResponse>> uploadImage(
            @PathVariable(name = "productId") UUID productId,
            @RequestParam("images") List<MultipartFile> images
    ) {
        return ResponseEntity.ok(productService.uploadImage(productId, images));
    }

    @PutMapping("/{productId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GlobalResponse<ProductResponse>> changeStatus(
            @PathVariable(name = "productId") UUID productId
    ) {
        return ResponseEntity.ok(productService.changeStatusForProduct(productId));
    }
}
