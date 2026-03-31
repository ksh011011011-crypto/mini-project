# [PRD] 세현 시네마 (SEHYUN CINEMA) 통합 구축 기획서

본 문서는 대표이사 김세현의 '세현 시네마' 웹 서비스 구축을 위한 상세 요구사항 및 1인 개발 MVP(Minimum Viable Product) 계획을 포함합니다.

---

## 1. 프로젝트 개요

- **서비스명:** 세현 시네마 (SEHYUN CINEMA)
- **대표이사:** 김세현 (Kim Se-hyeon)
- **슬로건:** "세현 시네마 앱에서는 다양한 혜택을 드리고 있어요"
- **핵심 목표:** - 직관적인 영화 예매 시스템 구축
  - 2026년 기준 VIP 멤버십 등급 및 혜택 관리
  - 타임리프(Thymeleaf)를 활용한 동적 웹 페이지 구현
- **기술 스택:**
  - **Backend:** Java 17, Spring Boot 3.x, Spring Data JPA
  - **Frontend:** HTML5, Thymeleaf, Bootstrap 5, JavaScript
  - **Database:** PostgreSQL (Supabase 연동)
  - **연동:** Supabase Auth 및 Database 연동

---

## 2. 주요 기능 상세 (Feature List)

### 2.1 메인 및 네비게이션

- **헤더(GNB):** - 세현 시네마 로고 및 SNS(유튜브, 페이스북, 인스타그램) 아이콘.
  - 멤버십, 고객센터, 단체관/대관문의, 로그인/회원가입 버튼.
- **팝업:** 앱 혜택 안내 및 QR코드 접속 팝업 노출.
- **영화 리스트:** TOP 10 영화 노출, '더보기' 클릭 시 10개씩 추가 (최대 100개).
- **검색:** 영화명 검색 기능.

### 2.2 예매 및 영화 정보

- **상세 페이지:** 영화 예고편, 줄거리, 평점/리뷰(CRUD), 하트 좋아요 기능.
- **예매 로직:** 영화/상영관 선택 -> 인원/좌석 선택 -> 쿠폰 적용 -> 가상 결제 완료.
- **상영관:** 일반관 및 스페셜관(샤롯데, IMAX, 수퍼플렉스, 4DX, 리클라이너 등) 정보 제공.

### 2.3 VIP 멤버십 (2026년 운영안 반영)

- **등급 산정 (연간):**
  - **VIP:** 26만 원 이상 결제.
  - **VVIP:** 30만 원 이상 결제.
  - **LVIP:** 36만 원 이상 결제.
- **혜택:** 등급별 무료 관람권(주중/주말), 매점 할인권, 생일 쿠폰(스위트 콤보) 제공.
- **반영 기준:** 관람일 2~3일 후 반영, 1일 최대 3건 인정, 매점 금액 제외.

### 2.4 고객센터 및 지원

- **FAQ:** 8개 카테고리(영화관, 멤버십, 결제 등)별 아코디언 UI.
- **공지사항:** 전체 및 지역별(서울, 경기 등) 공지 필터링.
- **AI 상담톡:** - 욕설 필터링 및 경고 시스템 (3회 위반 시 차단).
  - 자동 예매 지원 및 실시간 질의응답.
- **1:1 문의:** 이메일/SMS 알림 수신 선택 및 첨부파일 지원.

---

## 3. 데이터베이스 설계 (ERD)

```sql
-- 1. 회원 테이블 (VIP 등급 포함)
CREATE TABLE members (
    id SERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(50), -- 김세현
    grade VARCHAR(20) DEFAULT '일반',
    total_spent INT DEFAULT 0, -- 누적 결제액
    birth_date DATE,
    phone VARCHAR(20),
    email VARCHAR(100)
);

-- 2. 영화 테이블
CREATE TABLE movies (
    id SERIAL PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    description TEXT,
    poster_url VARCHAR(255),
    trailer_url VARCHAR(255),
    rating_avg DECIMAL(3,2) DEFAULT 0.0,
    heart_count INT DEFAULT 0
);

-- 3. 예매 테이블
CREATE TABLE bookings (
    id SERIAL PRIMARY KEY,
    member_id INT REFERENCES members(id),
    movie_id INT REFERENCES movies(id),
    seat_info VARCHAR(10), -- 예: A-12
    payment_method VARCHAR(20),
    total_price INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 4. 공지사항 및 FAQ
CREATE TABLE notices (
    id SERIAL PRIMARY KEY,
    category VARCHAR(50), -- 전체, 서울, 경기 등
    title VARCHAR(200),
    content TEXT,
    view_count INT DEFAULT 0,
    is_important BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```
