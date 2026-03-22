package com.example.ngerShop_be.modules.product.service.impl;

import com.example.ngerShop_be.common.exception.NotFoundException;
import com.example.ngerShop_be.common.response.GlobalResponse;
import com.example.ngerShop_be.common.response.PageResponse;
import com.example.ngerShop_be.modules.product.dto.CategoryRequest;
import com.example.ngerShop_be.modules.product.dto.CategoryResponse;
import com.example.ngerShop_be.modules.product.entity.Category;
import com.example.ngerShop_be.modules.product.repository.CategoryRepository;
import com.example.ngerShop_be.modules.product.service.CategoryService;
import com.example.ngerShop_be.modules.product.util.FileUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final FileUtil fileUtil;

    @Override
    @Transactional
    public GlobalResponse<CategoryResponse> createCategory(CategoryRequest request) {
        Category category = Category.builder()
                .name(request.name())
                .description(request.description())
                .build();
        if (request.image()!=null){
            category.setImageUrl(fileUtil.saveImage(request.image()));
        }

//        Category saved =categoryRepository.save(category);
//        //return new GlobalResponse<>(Status.SUCCESS, toResponse(category));
//        return GlobalResponse.ok(toResponse(saved));
        return GlobalResponse.ok(toResponse(categoryRepository.save(category)));
    }
    @Transactional
    @Override
    public GlobalResponse<CategoryResponse> updateCategory(UUID categoryId, CategoryRequest request){
        Category category = requireCategory(categoryId);
        applyCategoryData(category,request);
        return GlobalResponse.ok(toResponse(categoryRepository.save(category)));
    }
    @Override
    public GlobalResponse<String> deleteCategory(UUID categoryId){
        Category category = requireCategory(categoryId);
        categoryRepository.delete(category);
        return GlobalResponse.ok("Deleted");
    }
    @Override
    public GlobalResponse<CategoryResponse> findCategoryById(UUID categoryId){
        Category category=requireCategory(categoryId);
        return GlobalResponse.ok(toResponse(category));
    }
    @Transactional
    @Override
    public GlobalResponse<CategoryResponse> uploadImage(UUID categoryId, MultipartFile image){
        Category category=requireCategory(categoryId);
        category.setImageUrl(fileUtil.saveImage(image));
        return GlobalResponse.ok(toResponse(categoryRepository.save(category)));
    }
    @Override
    public GlobalResponse<PageResponse<CategoryResponse>> findAllCategories(String sortedBy, String sortDirection, int page, int size, String searchKeyword){
        Sort sort=Sort.by(
                "desc".equalsIgnoreCase(sortDirection) ? Sort.Direction.DESC : Sort.Direction.ASC,
                sortedBy == null ? "createdAt" : sortedBy
        );
        Pageable pageable = PageRequest.of(page,size,sort);
        Page<Category> pageData=(searchKeyword == null || searchKeyword.isBlank())
                ? categoryRepository.findAll(pageable)
                : categoryRepository.findByNameContainingIgnoreCase(searchKeyword, pageable);

        List<CategoryResponse> content=pageData.getContent()
                .stream()
                .map(this::toResponse)
                .toList();
        PageResponse<CategoryResponse> pageResponse=new PageResponse<>(
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

    private CategoryResponse toResponse(Category category){
        return new CategoryResponse(category.getId(), category.getName(), category.getDescription(), category.getImageUrl());
    }
    private Category requireCategory(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy danh mục với ID cung cấp"));
    }
    private void applyCategoryData(Category category, CategoryRequest request) {
        if (request.name() != null) category.setName(request.name());
        if (request.description() != null) category.setDescription(request.description());

        if (request.image() != null && !request.image().isEmpty()) {
            // 1. Xóa ảnh cũ trên ổ đĩa
            fileUtil.deleteImage(category.getImageUrl());
            // 2. Lưu ảnh mới
            category.setImageUrl(fileUtil.saveImage(request.image()));
        }
    }

}