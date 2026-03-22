package com.example.ngerShop_be.modules.product.service;

import com.example.ngerShop_be.common.response.GlobalResponse;
import com.example.ngerShop_be.modules.product.dto.ProductAttributeRequest;
import com.example.ngerShop_be.modules.product.dto.ProductAttributeResponse;

import java.util.List;
import java.util.UUID;

public interface ProductAttributeService {
    // Tạo thuộc tính mới cho một biến thể (màu sắc, kích cỡ...)
    GlobalResponse<ProductAttributeResponse> createAttribute(UUID variantId, ProductAttributeRequest request);

    // Lấy chi tiết một thuộc tính theo ID
    GlobalResponse<ProductAttributeResponse> getAttributeById(UUID id);

    // Lấy tất cả thuộc tính của một biến thể (ví dụ: lấy hết Color và Size của iPhone 15 Đỏ)
    GlobalResponse<List<ProductAttributeResponse>> getAttributesByVariantId(UUID variantId);

    // Cập nhật thông tin thuộc tính
    GlobalResponse<ProductAttributeResponse> updateAttribute(UUID id, ProductAttributeRequest request);

    // Xóa thuộc tính
    GlobalResponse<String> deleteAttribute(UUID id);

    // Tìm kiếm thuộc tính theo loại và giá trị (phục vụ bộ lọc)
    GlobalResponse<List<ProductAttributeResponse>> findByTypeAndValue(String type, String value);
}
