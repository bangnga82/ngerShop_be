package com.example.ngerShop_be.modules.product.repository;

import com.example.ngerShop_be.modules.product.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
    Page<Category> findByNameContainingIgnoreCase(String keyword, Pageable pageable);

    @Query("""
        select c.name, count(p.id)
        from Category c
        left join c.products p
        group by c.id, c.name
        order by count(p.id) desc, c.name asc
        """)
    List<Object[]> countProductsByCategory();
}
