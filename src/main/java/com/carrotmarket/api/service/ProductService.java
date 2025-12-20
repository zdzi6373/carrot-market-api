package com.carrotmarket.api.service;

import java.util.List;

import com.carrotmarket.api.model.Product;
import com.carrotmarket.api.repository.ProductRepository;

public class ProductService {

    // Repository 객체 생성
    private ProductRepository productRepository = new ProductRepository();

        private void validateProduct(Product product) {
        if (product.getTitle() == null || product.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("제목은 필수 입력값입니다.");
        }
        if (product.getPrice() == null) {
            throw new IllegalArgumentException("가격은 필수 입력값입니다.");
        }
        if (product.getPrice() < 0) {
            throw new IllegalArgumentException("가격은 0원 이상이어야 합니다.");
        }
        // viewCount는 null이면 0으로 초기화해주는 센스 (선택사항)
        if (product.getViewCount() == null) {
            product.setViewCount(0);
        }
    }

    public List<Product> findByTitle(String title) throws Exception {
        return productRepository.findByTitle(title);
    }

    public List<Product> findAll() throws Exception {
        return productRepository.findAll();
    }
    
    public Product save(Product product) throws Exception {
        validateProduct(product);
        return productRepository.save(product);
    }

    public Product update(int id, Product product) throws Exception {
        validateProduct(product);

        Integer result = productRepository.update(id, product);

        // 수정된 행이 없으면 예외 발생
        // 수정된 행이 있으면 수정된 제품 정보 취득 후 반환
        if (result == 0) {
            throw new IllegalArgumentException("해당 ID의 제품이 존재하지 않습니다.");
        } else {
            return productRepository.findById(id);
        }
    }

    public Integer delete(int id) throws Exception {
        return productRepository.delete(id);
    }
}
