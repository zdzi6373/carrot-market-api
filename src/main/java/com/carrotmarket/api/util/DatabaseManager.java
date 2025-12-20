package com.carrotmarket.api.util;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.logging.Level;

// DB 연결을 한곳에서 관리.
public class DatabaseManager {

    // DB 정보를 담을 변수, 상수로 선언
    private static final String URL;
    private static final String USERNAME;
    private static final String PASSWORD;
    private static final String DRIVER;

    private static final Logger logger = Logger.getLogger(DatabaseManager.class.getName());


    // static 블록
    // 처음 사용해보는 방식
    // 클래스 로드시, 한번만 실행
    // 1.db.properties 파일 읽어오기
    // 2.변수에 DB연결 정보 세팅
    // 3.드라이버 로드
    static {
        // try-with-resources 사용(inputStream 자동 close 처리 -> 자원 관리)
        // DatabaseUtil.class는 큰 의미 없음
        // 진짜 목적은 ClassLoader의 getResourceAsStream() 메서드를 쓰기 위함.
        // 상대경로로 db.properties 파일을 읽어옴
        try (InputStream input = DatabaseManager.class.getClassLoader().getResourceAsStream("db.properties")) {
            
            // 파일이 존재하지 않을 경우 예외 처리
            if (input == null) {
                throw new RuntimeException("db.properties 파일을 찾을 수 없습니다.");
            }

            // .properties 파일을 읽어오기 위한 객체 생성
            Properties properties = new Properties();

            // properties 객체로 파일 로드
            properties.load(input);

            // properties 객체에서 값 읽어와서 변수에 세팅
            URL = properties.getProperty("db.url");
            USERNAME = properties.getProperty("db.username");
            PASSWORD = properties.getProperty("db.password");
            DRIVER = properties.getProperty("db.driver");

            // 필수 정보 누락 시 예외 처리
            if (URL == null || USERNAME == null || PASSWORD == null || DRIVER == null) {
                throw new RuntimeException("DB 설정 정보가 누락되었습니다.");
            }

            // 드라이버 로드
            Class.forName(DRIVER);

        } catch (Exception e) {
            throw new RuntimeException("DB 설정 로드 실패: " + e.getMessage(), e);
        }
    }

    // DB 연결 메서드
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USERNAME, PASSWORD);
    }
    
    // Connection 닫기 메서드
    public static void close(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                logger.log(Level.WARNING, "Connection close error", e);
            }
        }
    }

    // Statement 닫기 메서드
    public static void close(Statement stmt) {
        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException e) {
                logger.log(Level.WARNING, "Statement close error", e);
            }
        }
    }

    // ResultSet 닫기 메서드
    public static void close(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                logger.log(Level.WARNING, "ResultSet close error", e);
            }
        }
    }

    // 모든 SQL자원 닫기 메서드
    public static void close(Connection conn, Statement stmt, ResultSet rs) {
        close(rs);
        close(stmt);
        close(conn);
    }
    
}
