package com.carrotmarket.api.repository;

import com.carrotmarket.api.util.DatabaseManager;
import com.carrotmarket.api.model.Product;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// 제품에 대한 데이터 접근을 처리하는 클래스
public class ProductRepository {

    // 제품을 데이터베이스에 저장
    public Product save(Product product) {
        String sql = "INSERT INTO products" + 
                    "(title, description, price, location, status, view_count, created_at, updated_at)" +
                     " VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())";

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseManager.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, product.getTitle());
            pstmt.setString(2, product.getDescription());
            pstmt.setInt(3, product.getPrice());
            pstmt.setString(4, product.getLocation());
            pstmt.setString(5, product.getStatus());
            pstmt.setInt(6, product.getViewCount());
            pstmt.executeUpdate();

            // 생성된 제품의 ID 가져오기
            rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                int generatedId = rs.getInt(1);
                product.setId(generatedId);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("제품 등록 실패", e);
        } finally {
            DatabaseManager.close(conn, pstmt, rs);
        }

        return product;
    }

    // 제목으로 제품을 찾기
    public List<Product> findByTitle(String title) {
        String sql = "SELECT * FROM products WHERE title LIKE ?";

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        List<Product> products = new ArrayList<>();

        try {
            conn = DatabaseManager.getConnection();
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
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("제품 조회 실패", e);
        } finally {
            DatabaseManager.close(conn, pstmt, rs);
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
            conn = DatabaseManager.getConnection();
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
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("제품 조회 실패", e);
        } finally {
            // 리소스 정리
            DatabaseManager.close(conn, pstmt, rs);
        }
        // 제품 목록 반환
        return products;
    }

    public Product findById(int id) {
        String sql = "SELECT * FROM products WHERE id = ?";
        Product product = null;

        // JDBC 객체 선언
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            // 데이터베이스 연결
            conn = DatabaseManager.getConnection();
            // SQL 준비
            pstmt = conn.prepareStatement(sql);
            // 쿼리 실행
            pstmt.setInt(1, id);
            rs = pstmt.executeQuery();

            // 결과 처리
            if (rs.next()) {
                product = new Product();
                product.setId(rs.getInt("id"));
                product.setTitle(rs.getString("title"));
                product.setDescription(rs.getString("description"));
                product.setPrice(rs.getInt("price"));
                product.setLocation(rs.getString("location"));
                product.setStatus(rs.getString("status"));
                product.setViewCount(rs.getInt("view_count"));
                product.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                product.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
                return product;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("제품 조회 실패", e);
        } finally {
            // 리소스 정리
            DatabaseManager.close(conn, pstmt, rs);
        }

        return product;
    }

    // 제품을 업데이트
    public Integer update(int id, Product product) {
        String sql = "UPDATE products SET title = ?, description = ?, price = ?, location = ?, status = ?, view_count = ?, updated_at = NOW() WHERE id = ?";

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            // 데이터베이스 연결
            conn = DatabaseManager.getConnection();
            // SQL 준비
            pstmt = conn.prepareStatement(sql);
            // 바인딩
            pstmt.setString(1, product.getTitle());
            pstmt.setString(2, product.getDescription());
            pstmt.setInt(3, product.getPrice());
            pstmt.setString(4, product.getLocation());
            pstmt.setString(5, product.getStatus());
            pstmt.setInt(6, product.getViewCount());
            pstmt.setInt(7, id);
            // 결과 바로 리턴(0 아니면 1)
            return pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("제품 수정 실패", e);
        } finally {
            DatabaseManager.close(conn, pstmt, rs);
        }
    }

    // 제품을 삭제
    public Integer delete(int id) {
        String sql = "DELETE FROM products WHERE id = ?";

        Connection conn = null;
        PreparedStatement pstmt = null;
        int result = 0;

        try {
            // 데이터베이스 연결
            conn = DatabaseManager.getConnection();
            // SQL 준비
            pstmt = conn.prepareStatement(sql);
            // 바인딩
            pstmt.setInt(1, id);
            // 쿼리 실행(반드시 1 또는 0 일 것임)
            result = pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("제품 삭제 실패", e);
        } finally {
            DatabaseManager.close(conn, pstmt, null);
        }

        return result;
    }
}