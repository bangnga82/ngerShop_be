package com.example.ngerShop_be.modules.product.repository;

import com.example.ngerShop_be.common.constants.AttributeType;
import com.example.ngerShop_be.modules.product.entity.ProductAttribute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProductAttributeRepository extends JpaRepository<ProductAttribute, UUID> {

    // Tìm tất cả thuộc tính của 1 biến thể (Size, Màu của 1 cái iPhone cụ thể)
    List<ProductAttribute> findByProductVariantId(UUID variantId);

    // Query tìm kiếm linh hoạt
    @Query("SELECT pa FROM ProductAttribute pa " +
            "WHERE pa.type = :type " +
            "AND LOWER(pa.value) LIKE LOWER(CONCAT('%', :value, '%'))")
    List<ProductAttribute> findByTypeAndValue(@Param("type") AttributeType type, @Param("value") String value);

    // Thêm hàm này nếu bạn muốn xóa nhanh tất cả thuộc tính của 1 Variant khi xóa Variant đó
    void deleteByProductVariantId(UUID variantId);

    @Query("SELECT DISTINCT pa.value FROM ProductAttribute pa " +
            "WHERE pa.type = com.example.ngerShop_be.common.constants.AttributeType.COLOR")
    List<String> findDistinctColorValues();

    @Query("SELECT DISTINCT pa.value FROM ProductAttribute pa")
    List<String> findDistinctValues();
}
