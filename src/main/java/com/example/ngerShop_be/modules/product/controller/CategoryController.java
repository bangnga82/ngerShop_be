package com.example.ngerShop_be.modules.product.controller;

import com.example.ngerShop_be.common.response.GlobalResponse;
import com.example.ngerShop_be.common.response.PageResponse;
import com.example.ngerShop_be.modules.product.dto.CategoryRequest;
import com.example.ngerShop_be.modules.product.dto.CategoryResponse;
import com.example.ngerShop_be.modules.product.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/categories")
public class CategoryController {
    private final CategoryService categoryService;
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GlobalResponse<CategoryResponse>> createCategory(
            @ModelAttribute @Valid CategoryRequest request
    ){
        return ResponseEntity.ok(categoryService.createCategory(request));
    }
    @PutMapping(value = "/{categoryId}",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GlobalResponse<CategoryResponse>> updateCategory(
            @PathVariable(name = "categoryId") UUID categoryId,
            @ModelAttribute @Valid CategoryRequest request
    ) {
        return ResponseEntity.ok(categoryService.updateCategory(categoryId, request));
    }
    @DeleteMapping("/{categoryId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GlobalResponse<String>> deleteCategory(
            @PathVariable(name = "categoryId") UUID categoryId
    ) {
        return ResponseEntity.ok(categoryService.deleteCategory(categoryId));
    }

    @GetMapping("/{categoryId}")
    public ResponseEntity<GlobalResponse<CategoryResponse>> findCategoryById(
            @PathVariable(name = "categoryId") UUID categoryId
    ) {
        return ResponseEntity.ok(categoryService.findCategoryById(categoryId));
    }

    @GetMapping
    public ResponseEntity<GlobalResponse<PageResponse<CategoryResponse>>> findAllCategories(
            @RequestParam(name = "sortedBy", required = false) String sortedBy,
            @RequestParam(name = "sortDirection", required = false, defaultValue = "asc") String sortDirection,
            @RequestParam(name = "page", required = false, defaultValue = "0") int page,
            @RequestParam(name = "size", required = false, defaultValue = "10") int size,
            @RequestParam(name = "searchKeyword", required = false) String searchKeyword
    ){
        return ResponseEntity.ok(categoryService.findAllCategories(sortedBy, sortDirection, page, size, searchKeyword));
    }
    @PutMapping("/{categoryId}/upload")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GlobalResponse<CategoryResponse>> uploadImage(
            @PathVariable(name = "categoryId") UUID categoryId,
            @RequestParam(name = "image") MultipartFile image
    ) {
        return ResponseEntity.ok(categoryService.uploadImage(categoryId, image));
    }
}
