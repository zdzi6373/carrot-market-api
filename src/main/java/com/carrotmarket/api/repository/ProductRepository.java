package com.carrotmarket.api.repository;

import com.carrotmarket.api.util.DatabaseUtil;
import com.carrotmarket.api.model.Product;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

// 제품에 대한 데이터 접근을 처리하는 클래스
public class ProductRepository {

    // 제품을 데이터베이스에 저장
    public void save() {
        // 구현 필요
    }

    // 제목으로 제품을 찾기
    public List<Product> findByTitle(String title) {
        String sql = "SELECT * FROM products WHERE title LIKE ?";

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        List<Product> products = new ArrayList<>();

        try {
            conn = DatabaseUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            // SQL Injection 방지를 위해 PreparedStatement의 파라미터 바인딩 사용
            pstmt.setString(1, "%" + title + "%");
            rs = pstmt.executeQuery();

            while (rs.next()) {
                Product product = new Product();
                product.setId(rs.getInt("id"));
                product.setTitle(rs.getString("title"));
                product.setDescription(rs.getString("description"));
                product.setPrice(rs.getInt("price"));
                product.setLocation(rs.getString("location"));
                product.setStatus(rs.getString("status"));
                product.setViewCount(rs.getInt("view_count"));
                product.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                product.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
                products.add(product);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            DatabaseUtil.close(conn, pstmt, rs);
        }
        return products;
    }

    // 모든 제품을 가져오기
    public List<Product> findAll() {
        String sql = "SELECT * FROM products";

        // JDBC 객체 선언
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        // 제품 목록을 저장할 리스트
        List<Product> products = new ArrayList<>();

        try {
            // 데이터베이스 연결
            conn = DatabaseUtil.getConnection();
            // SQL 준비
            pstmt = conn.prepareStatement(sql);
            // 쿼리 실행
            rs = pstmt.executeQuery();

            // 결과 처리
            while (rs.next()) {
                Product product = new Product();
                product.setId(rs.getInt("id"));
                product.setTitle(rs.getString("title"));
                product.setDescription(rs.getString("description"));
                product.setPrice(rs.getInt("price"));
                product.setLocation(rs.getString("location"));
                product.setStatus(rs.getString("status"));
                product.setViewCount(rs.getInt("view_count"));
                product.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                product.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
                products.add(product);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 리소스 정리
            DatabaseUtil.close(conn, pstmt, rs);
        }
        // 제품 목록 반환
        return products;
    }

    // 제품을 업데이트
    public void update() {
        // 구현 필요
    }

    // 제품을 삭제
    public void delete() {
        // 구현 필요
    }
}
