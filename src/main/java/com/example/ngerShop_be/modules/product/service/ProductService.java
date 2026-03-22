package com.example.ngerShop_be.modules.product.service;


import com.example.ngerShop_be.common.response.GlobalResponse;
import com.example.ngerShop_be.common.response.PageResponse;
import com.example.ngerShop_be.modules.product.dto.ProductRequest;
import com.example.ngerShop_be.modules.product.dto.ProductResponse;
import com.example.ngerShop_be.modules.product.dto.UpdateProductRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface ProductService {
    GlobalResponse<ProductResponse> createProduct(ProductRequest request);
    //
    GlobalResponse<PageResponse<ProductResponse>> findAllProducts(String sortedBy, String sortDirection, int page, int size, String searchKeyword, String category, Double minPrice, Double maxPrice, boolean status);
    //
    GlobalResponse<ProductResponse> getProductById(UUID productId);
    //
    GlobalResponse<ProductResponse> updateProduct(UUID productId, UpdateProductRequest request);
    //
    GlobalResponse<ProductResponse> uploadImage(UUID productId, List<MultipartFile> images);
    //
    GlobalResponse<List<ProductResponse>> searchByKeyword(String keyword);
    //
    GlobalResponse<ProductResponse> changeStatusForProduct(UUID productId);
}
