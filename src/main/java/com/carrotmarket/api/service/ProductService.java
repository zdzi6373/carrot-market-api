package com.carrotmarket.api.service;

import java.util.List;

import com.carrotmarket.api.model.Product;
import com.carrotmarket.api.repository.ProductRepository;

public class ProductService {

    // Repository 객체 생성
    private ProductRepository productRepository = new ProductRepository();

    public List<Product> findByTitle(String title) {
        return productRepository.findByTitle(title);
    }

    public List<Product> findAll() {
        return productRepository.findAll();
    }
    
}
