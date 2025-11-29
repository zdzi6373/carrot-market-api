package com.carrotmarket.api.model;

import java.time.LocalDateTime;

public class Product {
    private Integer id;
    private String title;
    private String description;
    private Integer price;
    private String location;
    private String status;
    private Integer viewCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 기본생성자
    public Product() {}

    public Product(String title, String description, Integer price, String location) {
        this.title = title;
        this.description = description;
        this.price = price;
        this.location = location;
        this.status = "SALE";
        this.viewCount = 0;
    }

    // Getters
    public Integer getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Integer getPrice() { return price; }
    public String getLocation() { return location; }
    public String getStatus() { return status; }
    public Integer getViewCount() { return viewCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // Setters
    void setId(Integer id) { this.id = id; }
    public void setTitle(String title) { this.title = title; } 
    public void setDescription(String description) { this.description = description; }
    public void setPrice(Integer price) { this.price = price; }
    public void setLocation(String location) { this.location = location; }
    public void setStatus(String status) { this.status = status; }
    public void setViewCount(Integer viewCount) { this.viewCount = viewCount; }
    void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}