package com.carrotmarket.api.service;

import java.util.List;

import com.carrotmarket.api.model.Product;
import com.carrotmarket.api.repository.ProductRepository;

public class ProductService {

    // Repository 객체 생성
    private ProductRepository productRepository = new ProductRepository();

    public List<Product> findByTitle(String title) throws Exception {
        return productRepository.findByTitle(title);
    }

    public List<Product> findAll() throws Exception {
        return productRepository.findAll();
    }
    
    public Product save(Product product) throws Exception {
        return productRepository.save(product);
    }

    public Integer update(int id, Product product) throws Exception {
        return productRepository.update(id, product);
    }

    public Integer delete(int id) throws Exception {
        return productRepository.delete(id);
    }
}
