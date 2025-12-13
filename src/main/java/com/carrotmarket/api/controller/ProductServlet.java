package com.carrotmarket.api.controller;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import com.carrotmarket.api.model.Product;
import com.carrotmarket.api.service.ProductService;
import com.carrotmarket.api.util.JsonUtil;

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

    private JsonUtil jsonUtil = new JsonUtil();

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
        String productsJson = jsonUtil.productsToJson(products);
        pw.print(productsJson);
    }

    // 등록
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        // 요청 콘텐츠 타입 설정(JSON, UTF-8)
        req.setCharacterEncoding("UTF-8");
        // 응답 콘텐츠 타입 설정(JSON, UTF-8)
        res.setContentType("application/json; charset=UTF-8");
        // 응답 스트림 생성
        PrintWriter pw = res.getWriter();

        // 요청 본문에서 JSON 데이터 읽기
        String bodyData = jsonUtil.getBody(req);
        // JSON 문자열을 Product 객체로 변환
        Product product = jsonUtil.parseProduct(bodyData);

        // 제품 저장
        Product saveProduct = productService.save(product);
        
        // 저장된 제품을 JSON 문자열로 변환하여 응답
        String productJson = jsonUtil.productsToJson(List.of(saveProduct));
        pw.print(productJson);

    }

    // 수정(id 값으로 특정 제품 수정)
    protected void doPut(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        res.setContentType("application/json; charset=UTF-8");
        PrintWriter pw = res.getWriter();

        // URL에서 ID 추출
        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) {
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            pw.print("{\"error\":\"제품 ID가 필요합니다.\"}");
            return;
        }

        // pathInfo는 "/{id}" 형태이므로, 앞의 '/'를 제거하고 정수로 변환
        int id = Integer.parseInt(pathInfo.substring(1));
        // 요청 본문에서 JSON 데이터 읽기
        String bodyData = jsonUtil.getBody(req);
        // JSON 문자열을 Product 객체로 변환
        Product product = jsonUtil.parseProduct(bodyData);

        Integer result = productService.update(id, product);

        pw.print(result);
    }

    // 삭제(id 값으로 특정 제품 삭제)
    protected void doDelete(HttpServletRequest req, HttpServletResponse res) {
        // 구현 필요
    }

}
