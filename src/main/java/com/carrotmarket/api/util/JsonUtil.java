package com.carrotmarket.api.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

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

        return products.stream() // 스트림 생성
                .map(this::productToJson) // 중간 연산: 각 Product 객체를 JSON 문자열로 변환
                .collect(Collectors.joining(",", "[", "]")); // 최종 연산: 쉼표로 연결하고 대괄호로 감싸기
    }

    // 단일 Product 객체를 JSON 문자열로 변환하는 헬퍼 메서드
    public String productToJson(Product p) {
        if (p == null) return "{}";

        // entrySet 이용을 위해 product 객체를 Map 으로 변환
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", p.getId());
        map.put("title", p.getTitle());
        map.put("description", p.getDescription());
        map.put("price", p.getPrice());
        map.put("location", p.getLocation());
        map.put("status", p.getStatus());
        map.put("viewCount", p.getViewCount());
        map.put("createdAt", p.getCreatedAt());
        map.put("updatedAt", p.getUpdatedAt());

        return map.entrySet().stream() // entrySet은 Map에만 사용가능(Key : Value 를 하나씩 stream함)
                .map(entry -> {
                    String key = "\"" + entry.getKey() + "\""; // Key 값을 ""로 감싸기 위한 장치
                    Object value = entry.getValue(); // Value 값 취득
                    // Value값의 타입에 따라 처리 String 인 경우 ""로 감싸 줌
                    String valueStr = (value instanceof Number) ? String.valueOf(value) : "\"" + escapeJson(value != null ? String.valueOf(value) : "") + "\"";
                    return key + ":" + valueStr; // Key : Value 형태로 리턴
                })
                // 상기 entry들을 , 로 구분하여 collect 해줌 가장 앞부분과 뒷부분을 중괄호 처리해줌 == JSON 형태으로 만들어 줌
                .collect(Collectors.joining(",", "{", "}"));
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
