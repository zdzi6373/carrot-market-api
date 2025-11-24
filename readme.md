# Carrot Market API - Core Java Clone

> Spring과 ORM 없이, **순수 Java**로 구현하는 당근마켓 백엔드 API

## 프로젝트를 시작한 이유

### 해결하고 싶은 문제
1. Java를 어떤 상황에서 써야 하는지, 장점이 무엇인지 모름
2. Spring Framework가 왜 강력한지, 내부 동작 원리를 모름
3. MyBatis/JPA 같은 ORM 프레임워크의 지속 사용으로 원리 이해 부족

### 접근 방법
**프레임워크가 처리해주는 복잡한 영역을 직접 구현하며 원리 이해**

## 기대 효과

- **Java 언어 이해도 상승**: 자바의 특징과 장점을 고려하며 사용
- **HTTP 프로토콜 이해**: Method, Header, Session/Cookie 등 웹 통신 원리
- **DB 통신 이해**: JDBC만으로 데이터 접근 계층 구현
- **프레임워크 가치 인식**: 왜 필요한지, 어디까지 숨겨져 있는지 체감

## 기술 스택

| 항목 | 선택 | 이유 |
|------|------|------|
| **언어** | Java 17 | Java 8 경험 기반, 최신 기능 비교 |
| **빌드** | Maven | JDBC, Servlet API 의존성 관리 |
| **서버** | Tomcat 10 | Jakarta EE 지원 |
| **DB** | PostgreSQL | 익숙한 DB로 학습 집중 |
| **IDE** | VS Code + Java Extension | (선택 사항) |