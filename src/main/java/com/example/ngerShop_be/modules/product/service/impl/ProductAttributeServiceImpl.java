package com.example.ngerShop_be.modules.product.service.impl;

import com.example.ngerShop_be.common.constants.AttributeType;
import com.example.ngerShop_be.common.exception.BadRequestException;
import com.example.ngerShop_be.common.exception.NotFoundException;
import com.example.ngerShop_be.common.response.GlobalResponse;
import com.example.ngerShop_be.modules.product.dto.ProductAttributeRequest;
import com.example.ngerShop_be.modules.product.dto.ProductAttributeResponse;
import com.example.ngerShop_be.modules.product.entity.ProductAttribute;
import com.example.ngerShop_be.modules.product.entity.ProductVariant;
import com.example.ngerShop_be.modules.product.repository.ProductAttributeRepository;
import com.example.ngerShop_be.modules.product.repository.ProductVariantRepository;
import com.example.ngerShop_be.modules.product.service.ProductAttributeService;
import com.example.ngerShop_be.modules.product.util.ProductMapperUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductAttributeServiceImpl implements ProductAttributeService {

    private final ProductAttributeRepository attributeRepository;
    private final ProductVariantRepository variantRepository;
    // Đừng quên inject Mapper để chuyển đổi Entity sang Response
    private final ProductMapperUtil productMapperUtil;

    @Override
    @Transactional
    public GlobalResponse<ProductAttributeResponse> createAttribute(UUID variantId, ProductAttributeRequest request) {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy biến thể sản phẩm"));

        ProductAttribute attr = ProductAttribute.builder()
                .type(request.type())
                .value(request.value())
                .productVariant(variant)
                .build();

        attributeRepository.save(attr);
        return GlobalResponse.ok(productMapperUtil.toAttributeResponse(attr));
    }

    @Override
    public GlobalResponse<ProductAttributeResponse> getAttributeById(UUID id) {
        ProductAttribute attr = attributeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy thuộc tính"));
        return GlobalResponse.ok(productMapperUtil.toAttributeResponse(attr));
    }

    @Override
    public GlobalResponse<List<ProductAttributeResponse>> getAttributesByVariantId(UUID variantId) {
        List<ProductAttributeResponse> data = attributeRepository.findByProductVariantId(variantId)
                .stream()
                .map(productMapperUtil::toAttributeResponse)
                .toList();
        return GlobalResponse.ok(data);
    }

    @Override
    @Transactional
    public GlobalResponse<ProductAttributeResponse> updateAttribute(UUID id, ProductAttributeRequest request) {
        ProductAttribute attr = attributeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy thuộc tính để cập nhật"));

        attr.setType(request.type());
        attr.setValue(request.value());
        attributeRepository.save(attr);

        return GlobalResponse.ok(productMapperUtil.toAttributeResponse(attr));
    }

    @Override
    @Transactional
    public GlobalResponse<String> deleteAttribute(UUID id) {
        if (!attributeRepository.existsById(id)) {
            throw new NotFoundException("Không tìm thấy thuộc tính để xóa");
        }
        attributeRepository.deleteById(id);
        return GlobalResponse.ok("Đã xóa thuộc tính thành công");
    }
    @Override
    public GlobalResponse<List<ProductAttributeResponse>> findByTypeAndValue(String type, String value) {
        AttributeType typeEnum = parseType(type);
        List<ProductAttributeResponse> data = attributeRepository.findByTypeAndValue(typeEnum, value)
                .stream()
                .map(productMapperUtil::toAttributeResponse)
                .toList();
        return GlobalResponse.ok(data);
    }

    private AttributeType parseType(String type) {
        if (type == null || type.isBlank()) {
            throw new BadRequestException("Loai thuoc tinh khong duoc de trong");
        }
        try {
            return AttributeType.valueOf(type.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Loai thuoc tinh khong hop le: " + type);
        }
    }
}
