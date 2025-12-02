package com.carrotmarket.api.controller;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import com.carrotmarket.api.model.Product;
import com.carrotmarket.api.service.ProductService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// Rest API Servlet
@WebServlet("/api/products/*")
public class ProductServlet extends HttpServlet {

    // Service 객체 생성
    private ProductService productService = new ProductService();

    // 전체 조회
    // 일부 조회(제목 부분일치 검색)
    // Exception은 HttpServlet 규약에 따라 throws 선언
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        // 응답 콘텐츠 타입 설정(JSON, UTF-8)
        res.setContentType("application/json; charset=UTF-8");
        // 응답 스트림 생성
        PrintWriter pw = res.getWriter();
        
        List<Product> products;

        // 제품 검색
        // 제품 검색 방식 추가시, 로직 수정 필요
        String title = req.getParameter("title");
        if (title != null && !title.isEmpty()) {
            // 제목으로 상품 검색
            products = productService.findByTitle(title);
        } else {
            // 모든 상품 조회
            products = productService.findAll();
        }
        
        // Product 리스트를 수동으로 JSON 문자열로 변환
        // GSON, Jackson 같은 라이브러리의 편리함을 느낌
        String productsJson = productsToJson(products);
        pw.print(productsJson);
    }

    // 등록
    protected void doPost(HttpServletRequest req, HttpServletResponse res) {
        // 구현 필요
    }

    // 수정(id 값으로 특정 제품 수정)
    protected void doPut(HttpServletRequest req, HttpServletResponse res) {
        // 구현 필요
    }

    // 삭제(id 값으로 특정 제품 삭제)
    protected void doDelete(HttpServletRequest req, HttpServletResponse res) {
        // 구현 필요
    }

    // Product 리스트를 JSON 배열 문자열로 변환하는 헬퍼 메서드
    private String productsToJson(List<Product> products) {
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
    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
}
