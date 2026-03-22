package com.example.ngerShop_be.modules.product.service.impl;


import com.example.ngerShop_be.common.exception.NotFoundException;
import com.example.ngerShop_be.common.response.GlobalResponse;
import com.example.ngerShop_be.common.response.PageResponse;
import com.example.ngerShop_be.modules.product.dto.ProductRequest;
import com.example.ngerShop_be.modules.product.dto.ProductResponse;
import com.example.ngerShop_be.modules.product.dto.UpdateProductRequest;
import com.example.ngerShop_be.modules.product.entity.Category;
import com.example.ngerShop_be.modules.product.entity.Product;
import com.example.ngerShop_be.modules.product.entity.ProductImage;
import com.example.ngerShop_be.modules.product.repository.CategoryRepository;
import com.example.ngerShop_be.modules.product.repository.ProductImageRepository;
import com.example.ngerShop_be.modules.product.repository.ProductRepository;
import com.example.ngerShop_be.modules.product.service.ProductService;
import com.example.ngerShop_be.modules.product.util.FileUtil;
import com.example.ngerShop_be.modules.product.util.ProductMapperUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor

public class ProductServiceImpl implements ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductImageRepository productImageRepository;
    private final FileUtil fileUtil;

    private final ProductMapperUtil productMapperUtil;

    @Override
    @Transactional
    public GlobalResponse<ProductResponse> createProduct(ProductRequest request){
        Category category=requireCategory(request.categoryId());
        Product product =Product.builder()
                .name(request.name())
                .description(request.description())
                .price(request.price())
                .isActive(true)
                .category(category)
                .build();
        String username= SecurityContextHolder.getContext().getAuthentication().getName();
        product.setCreatedBy(username);
        product.setCreatorName(username);
        productRepository.save(product);
        saveProductImages(request.images(), product); // Gọi hàm helper
        return GlobalResponse.ok(productMapperUtil.toProductResponse(product));
    }
    @Override
    @Transactional
    public GlobalResponse<ProductResponse> updateProduct(UUID productId, UpdateProductRequest request){
        Product product=requireProduct(productId);
        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        if (request.categoryId() != null) {
            Category category = requireCategory(request.categoryId());
            product.setCategory(category); // <--- QUAN TRỌNG: Phải set vào đây!
        }

        if (request.existingImageUrls() != null) {
            List<ProductImage> toDelete = product.getImages().stream()
                    .filter(img -> !request.existingImageUrls().contains(img.getImageUrl()))
                    .toList();
            toDelete.forEach(img -> {
                fileUtil.deleteImage(img.getImageUrl());
                product.getImages().remove(img);
                productImageRepository.delete(img);
            });
        }
        saveProductImages(request.images(), product);
        return GlobalResponse.ok(productMapperUtil.toProductResponse(product));

    }
    @Override
    public GlobalResponse<ProductResponse> getProductById(UUID productId){
        Product product=requireProduct(productId);
        return GlobalResponse.ok(productMapperUtil.toProductResponse(product));
    }
    @Override
    public GlobalResponse<PageResponse<ProductResponse>> findAllProducts(String sortedBy, String sortDirection, int page, int size, String searchKeyword, String category, Double minPrice, Double maxPrice, boolean status){
        Sort sort = Sort.by(sortDirection.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC,
                sortedBy == null ? "createdDate" : sortedBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Product> pageData = productRepository.findAllWithFilters(searchKeyword, category, minPrice, maxPrice, status, pageable);
        List<ProductResponse> content = pageData.getContent().stream()
                .map(productMapperUtil::toProductResponse)
                .toList();

        PageResponse<ProductResponse> pageResponse = new PageResponse<>(
                content,
                pageData.getTotalPages(),
                pageData.getTotalElements(),
                pageData.getNumber(),
                pageData.getSize(),
                pageData.getNumberOfElements(),
                pageData.isFirst(),
                pageData.isLast(),
                pageData.hasNext(),
                pageData.hasPrevious()
        );
        return GlobalResponse.ok(pageResponse);

    }
    @Override
    @Transactional
    public GlobalResponse<ProductResponse> uploadImage(UUID productId, List<MultipartFile> images){
        Product product=requireProduct(productId);
        for (MultipartFile file : images) {
            String url = fileUtil.saveImage(file);
            ProductImage image = ProductImage.builder()
                    .imageUrl(url)
                    .product(product)
                    .build();
            productImageRepository.save(image);
            product.getImages().add(image);
        }
        return GlobalResponse.ok(productMapperUtil.toProductResponse(product));
    }
    @Override
    public GlobalResponse<List<ProductResponse>> searchByKeyword(String keyword){
        List<ProductResponse> data = productRepository.findByNameContainingIgnoreCase(keyword)
                .stream()
                .map(productMapperUtil::toProductResponse)
                .toList();
        return GlobalResponse.ok(data);
    }
    @Override
    @Transactional
    public GlobalResponse<ProductResponse> changeStatusForProduct(UUID productId){
        Product product=requireProduct(productId);
        product.setIsActive(!product.getIsActive());
        productRepository.save(product);
        return GlobalResponse.ok(productMapperUtil.toProductResponse(product));
    }


    private Category requireCategory(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy danh mục với ID cung cấp"));
    }
    private Product requireProduct(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy sản phẩm với ID cung cấp"));
    }
    private void saveProductImages(List<MultipartFile> files, Product product) {
        if (files == null || files.isEmpty()) {
            return;
        }

        for (MultipartFile file : files) {
            // 1. Lưu file vật lý và lấy URL
            String url = fileUtil.saveImage(file);

            // 2. Tạo đối tượng Image gắn với Product
            ProductImage image = ProductImage.builder()
                    .imageUrl(url)
                    .product(product)
                    .build();

            // 3. Lưu vào DB và cập nhật danh sách của Product
            productImageRepository.save(image);
            product.getImages().add(image);
        }
    }
}
