package com.carrotmarket.api.util;

import java.util.List;

import com.carrotmarket.api.model.Product;

public class JsonUtil {

    // Product 리스트를 JSON 배열 문자열로 변환하는 헬퍼 메서드
    public String productsToJson(List<Product> products) {
        if (products == null || products.isEmpty()) {
            return "[]";
        }

        StringBuilder json = new StringBuilder();
        json.append("[");

        for (int i = 0; i < products.size(); i++) {
            Product p = products.get(i);
            json.append("{");
            json.append("\"id\":").append(p.getId()).append(",");
            json.append("\"title\":\"").append(escapeJson(p.getTitle())).append("\",");
            json.append("\"description\":\"").append(escapeJson(p.getDescription())).append("\",");
            json.append("\"price\":").append(p.getPrice()).append(",");
            json.append("\"location\":\"").append(escapeJson(p.getLocation())).append("\",");
            json.append("\"status\":\"").append(escapeJson(p.getStatus())).append("\",");
            json.append("\"viewCount\":").append(p.getViewCount()).append(",");
            json.append("\"createdAt\":\"").append(p.getCreatedAt()).append("\",");
            json.append("\"updatedAt\":\"").append(p.getUpdatedAt()).append("\"");
            json.append("}");

            if (i < products.size() - 1) {
                json.append(",");
            }
        }

        json.append("]");
        return json.toString();
    }

    // JSON 문자열에 포함될 수 있는 특수문자를 이스케이프 처리하는 헬퍼 메서드
    public String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
}
