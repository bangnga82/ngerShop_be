package com.example.ngerShop_be.modules.product.service.impl;

import com.example.ngerShop_be.common.exception.NotFoundException;
import com.example.ngerShop_be.common.response.GlobalResponse;
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
        Product product=requireProduct(variantRequest.productId());
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

        ProductVariant variant=requireVariant(variantId);
        variant.setPrice(variantRequest.price());
        variant.setStock(variantRequest.stock());
        if (variantRequest.image() != null && !variantRequest.image().isEmpty()) {
            // Xóa ảnh cũ trước khi lưu ảnh mới để tránh rác server
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
        ProductVariant variant=requireVariant(variantId);
        // 1. Xóa file ảnh vật lý của biến thể này
        if (variant.getImageUrl() != null) {
            fileUtil.deleteImage(variant.getImageUrl());
        }

        // 2. Gỡ nó ra khỏi danh sách của Product cha (để tránh lỗi cache Hibernate)
        variant.getProduct().getVariants().remove(variant);
        variantRepository.delete(variant);
        return GlobalResponse.ok( "Deleted");
    }
    @Override
    @Transactional
    public GlobalResponse<String> uploadImageToVariant(UUID variantId, MultipartFile image) {
        ProductVariant variant=requireVariant(variantId);

        variant.setImageUrl(fileUtil.saveImage(image));
        variantRepository.save(variant);
        return GlobalResponse.ok("Uploaded");
    }

    @Override
    public Boolean checkStock(List<OrderItemRequest> requests) {
        for (OrderItemRequest req : requests) {
            ProductVariant v=requireVariant(req.variantId());

            if (v.getStock() < req.quantity()) return false;
        }
        return true;
    }

    @Override
    public List<ProductPriceResponse> getPrices(PurchaseRequest request) {
        return request.variantIds().stream().map(id -> {
            ProductVariant v = variantRepository.findById(id).orElseThrow();
            Integer qty = request.orderedQuantities().get(id);
            return new ProductPriceResponse(id, v.getProduct().getName(), qty, v.getPrice());
        }).toList();
    }

//    @Override
//    @Transactional
//    public Void updateStock(List<OrderItemRequest> requests) {
//        for (OrderItemRequest req : requests) {
//            ProductVariant v = variantRepository.findById(req.variantId()).orElseThrow();
//            v.setStock(v.getStock() - req.quantity());
//        }
//        return null;
//    }
@Override
@Transactional
public Void updateStock(List<OrderItemRequest> requests) {
    for (OrderItemRequest req : requests) {
        ProductVariant v = variantRepository.findById(req.variantId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy biến thể"));

        int newStock = v.getStock() - req.quantity();
        if (newStock < 0) {
            throw new RuntimeException("Sản phẩm " + v.getProduct().getName() + " đã hết hàng!");
        }
        v.setStock(newStock);
        variantRepository.save(v); // Đảm bảo lưu lại thay đổi
    }
    return null;
}
    @Override
    public GlobalResponse<ProductVariantResponse> getProductVariantById(UUID variantId) {
        ProductVariant v=requireVariant(variantId);


        ProductVariantResponse response = ProductVariantResponse.builder()
                .id(v.getId())
                .productName(v.getProduct().getName())
                .stock(v.getStock())
                .price(v.getPrice())
                .imageUrl(v.getImageUrl())
                .attributes(v.getAttributes().stream()
                        .map(a -> com.example.ngerShop_be.modules.product.dto.ProductAttributeResponse.builder()
                                .id(a.getId())
                                .type(a.getType() != null ? a.getType().name() : null)
                                .value(a.getValue())
                                .build())
                        .toList())
                .build();

        return  GlobalResponse.ok(response);
    }
    private Product requireProduct(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy sản phẩm với ID cung cấp"));
    }
    private ProductVariant requireVariant(UUID id) {
        return variantRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy sản phẩm biến thể với ID cung cấp"));
    }
}
