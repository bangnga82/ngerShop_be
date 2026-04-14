package com.example.ngerShop_be.modules.product.repository;

import com.example.ngerShop_be.modules.product.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, UUID> {
    @Query("""
            select distinct pv
            from ProductVariant pv
            left join fetch pv.product
            left join fetch pv.attributes
            where pv.id = :id
            """)
    Optional<ProductVariant> findDetailedById(@Param("id") UUID id);

    @Modifying
    @Query("UPDATE ProductVariant pv SET pv.stock = pv.stock - :quantity " +
            "WHERE pv.id = :id AND pv.stock >= :quantity")
    int updateStock(@Param("id") UUID id, @Param("quantity") Integer quantity);
}
