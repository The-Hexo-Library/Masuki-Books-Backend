// package com.masukibooks.repository;

// import com.masukibooks.entity.ProductImage;
// import org.springframework.data.jpa.repository.JpaRepository;
// import org.springframework.stereotype.Repository;

// import java.util.List;
// import java.util.UUID;

// @Repository
// public interface ProductImageRepository extends JpaRepository<ProductImage,
// UUID> {
// List<ProductImage> findByProductProductIdOrderByDisplayOrder(UUID productId);
// void deleteByProductProductId(UUID productId);
// }
