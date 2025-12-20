package com.carrotmarket.api.controller;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    
    private static final Logger logger = Logger.getLogger(ProductServlet.class.getName());

    // 전체 조회
    // 일부 조회(제목 부분일치 검색)
    // Exception은 HttpServlet 규약에 따라 throws 선언
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        // 응답 콘텐츠 타입 설정(JSON, UTF-8)
        res.setContentType("application/json; charset=UTF-8");
        // 응답 스트림 생성
        PrintWriter pw = res.getWriter();
        try {
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
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error in doGet", e);
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            pw.print("{\"error\":\"서버 오류 발생\"}");
        }
    }

    // 등록
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        // 요청 콘텐츠 타입 설정(JSON, UTF-8)
        req.setCharacterEncoding("UTF-8");
        // 응답 콘텐츠 타입 설정(JSON, UTF-8)
        res.setContentType("application/json; charset=UTF-8");
        // 응답 스트림 생성
        PrintWriter pw = res.getWriter();

        try {
            // 요청 본문에서 JSON 데이터 읽기
            String bodyData = jsonUtil.getBody(req);
            // JSON 문자열을 Product 객체로 변환
            Product product = jsonUtil.parseProduct(bodyData);

            // 제품 저장
            Product saveProduct = productService.save(product);
            
            // 저장된 제품을 JSON 문자열로 변환하여 응답
            String productJson = jsonUtil.productToJson(saveProduct);
            pw.print(productJson);
            
        } catch (IllegalArgumentException e) {
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            pw.print("{\"error\":\"" + e.getMessage() + "\"}");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error in doPost", e);
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            pw.print("{\"error\":\"서버 오류 발생\"}");
        }
    }

    // 수정(id 값으로 특정 제품 수정)
    // req에는 변경되지 않은 값도 NULL이 아닌 수정을 위해 조회한 값이 그대로 들어온다고 가정 함.
    protected void doPut(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        res.setContentType("application/json; charset=UTF-8");
        PrintWriter pw = res.getWriter();

        try {
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

            Product updatedProduct = productService.update(id, product);

            // 객체를 JSON으로 변환해서 리턴
            pw.print(jsonUtil.productToJson(updatedProduct));

        } catch(IllegalArgumentException e) {
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            pw.print("{\"error\":\"" + e.getMessage() + "\"}");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error in doPut", e);
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            pw.print("{\"error\":\"서버 오류 발생\"}");
        }
    }

    // 삭제(id 값으로 특정 제품 삭제)
    protected void doDelete(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        res.setContentType("application/json; charset=UTF-8");
        PrintWriter pw = res.getWriter();

        try {
            // URL에서 ID 추출
            String pathInfo = req.getPathInfo();
            if (pathInfo == null || pathInfo.equals("/")) {
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                pw.print("{\"error\":\"제품 ID가 필요합니다.\"}");
                return;
            }

            // pathInfo는 "/{id}" 형태이므로, 앞의 '/'를 제거하고 정수로 변환
            int id = Integer.parseInt(pathInfo.substring(1));

            Integer result = productService.delete(id);

            if (result == 1) {
                // 명확한 JSON 메시지 반환
                pw.print("{\"message\":\"삭제되었습니다.\", \"id\":" + id + "}");
            } else {
                res.setStatus(HttpServletResponse.SC_NOT_FOUND);
                pw.print("{\"error\":\"해당 상품이 없습니다.\"}");
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error in doDelete", e);
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            pw.print("{\"error\":\"서버 오류 발생\"}");
        }
    }

}
