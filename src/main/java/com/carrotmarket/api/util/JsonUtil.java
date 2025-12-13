package com.carrotmarket.api.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;

import com.carrotmarket.api.model.Product;

import jakarta.servlet.http.HttpServletRequest;

public class JsonUtil {

    // Product 리스트를 JSON 배열 문자열로 변환하는 헬퍼 메서드
    // Spring 에선 @ResponseBody, Jackson 라이브러리로 간단히 처리 가능
    // 이 메서드가 너무 길어지는 경향이 있어 단일 객체 변환 메서드를 분리함
    public String productsToJson(List<Product> products) {
        if (products == null || products.isEmpty()) {
            return "[]";
        }

        StringBuilder json = new StringBuilder();
        json.append("[");

        for (int i = 0; i < products.size(); i++) {
            
            json.append(productToJson(products.get(i)));

            if (i < products.size() - 1) {
                json.append(",");
            }
        }

        json.append("]");
        return json.toString();
    }

    // 단일 Product 객체를 JSON 문자열로 변환하는 헬퍼 메서드
    public String productToJson(Product p) {
        if (p == null) return "{}";

        StringBuilder json = new StringBuilder();
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
        return json.toString();
    }

    // JSON 문자열에 포함될 수 있는 특수문자를 이스케이프 처리하는 헬퍼 메서드
    public String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    // Request Body를 문자열로 읽어오는 헬퍼 메서드
    public String getBody(HttpServletRequest req) throws IOException{
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = req.getReader();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        reader.close();

        return sb.toString();
    }

    // JSON 문자열을 Product 객체로 변환하는 헬퍼 메서드
    public Product parseProduct(String json) {
        Product product = new Product();
        product.setTitle(extractString(json, "title"));
        product.setDescription(extractString(json, "description"));
        product.setLocation(extractString(json, "location"));
        product.setPrice(extractInt(json, "price"));

        return product;
    }

    // JSON 에서 문자열 추출 (역 직렬화)
    // Spring 에선 @RequestBody, Jackson 라이브러리로 간단히 처리 가능
    public String extractString(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int start = json.indexOf(searchKey);
        if (start == -1) return null;
        
        int colon = json.indexOf(":", start);
        int firstQuote = json.indexOf("\"", colon);
        int secondQuote = json.indexOf("\"", firstQuote + 1);
        
        if (firstQuote == -1 || secondQuote == -1) return null;
        return json.substring(firstQuote + 1, secondQuote);
    }

    // JSON 에서 정수 추출 (역 직렬화)
    // Spring 에선 @RequestBody, Jackson 라이브러리로 간단히 처리 가능
    public Integer extractInt(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int start = json.indexOf(searchKey);
        if (start == -1) return null;
        
        int colon = json.indexOf(":", start);
        int valueStart = colon + 1;
        while (valueStart < json.length() && !Character.isDigit(json.charAt(valueStart))) {
            valueStart++;
        }
        int valueEnd = valueStart;
        while (valueEnd < json.length() && Character.isDigit(json.charAt(valueEnd))) {
            valueEnd++;
        }
        
        if (valueStart >= valueEnd) return null;
        return Integer.parseInt(json.substring(valueStart, valueEnd));
    }
}
