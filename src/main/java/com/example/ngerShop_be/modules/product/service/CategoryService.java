package com.example.ngerShop_be.modules.product.service;

import com.example.ngerShop_be.common.response.GlobalResponse;
import com.example.ngerShop_be.common.response.PageResponse;
import com.example.ngerShop_be.modules.product.dto.CategoryRequest;
import com.example.ngerShop_be.modules.product.dto.CategoryResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface CategoryService {
    GlobalResponse<CategoryResponse> createCategory(CategoryRequest request);

    GlobalResponse<PageResponse<CategoryResponse>> findAllCategories(String sortedBy, String sortDirection, int page, int size, String searchKeyword);
//
    GlobalResponse<CategoryResponse> updateCategory(UUID categoryId, CategoryRequest request);
//
    GlobalResponse<String> deleteCategory(UUID categoryId);
//
    GlobalResponse<CategoryResponse> uploadImage(UUID categoryId, MultipartFile image);
//
    GlobalResponse<CategoryResponse> findCategoryById(UUID categoryId);
}
