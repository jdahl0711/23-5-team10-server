# 1gram
와플스튜디오 23.5기 10조 server repository입니다.

## **Project Introduction**
본 프로젝트는 인스타그램을 클론한 1gram입니다.

기존 인스타그램과 같이 다른 사람들과 상호작용을 통해 서로의 소식을 나누는 것이 목표입니다.

제외된 주요 기능 : 게시글 동영상 업로드, 릴스, DM

추가된 기능 : 앨범을 통해 게시글을 그룹화하여 정리할 수 있는 기능

---------

### **server**

|이름| 담당 역할 |
|--------|-------------------------------------|
| 김성헌 | 프로젝트 설정, 팔로우, 스토리, 앨범 |
| 서주원 | 게시글, 댓글, 좋아요, 북마크        |
| 이정달 | 회원가입, 로그인, 검색              |

### **web**

김남희

천준영

------------

### 기술 스택

**CORE** : Kotlin + Spring Boot

**DB** : MySQL, Flyway, JPA, JdbcTemplate 

**Infra** : AWS EC2, S3, GitHub Actions (CI/CD)

**Security** : Spring Security, JWT

**Docs** : Swagger (SpringDoc)

**Environment**

JDK : 17

Framework : spring boot 4.0.1

Database : MYSQL 8.4

-------

### 서비스

https://www.wfinstaclone.shop

### API

https://api.wfinstaclone.shop/swagger-ui/index.html

---------

## 주요 기능

### 1. 회원가입 및 로그인

### 개요
- 이메일 또는 닉네임 기반 회원가입 및 로그인 지원
- OAuth 2.0 로그인 (Kakao)
- JWT Access Token과 Refresh Token을 사용한 인증 관리

### 회원가입
- 사용자가 이메일, 비밀번호, 닉네임을 입력하여 회원가입
- 이메일/닉네임 중복 여부 확인
- 회원가입 후 자동 로그인되어 Access Token 발급

### 로그인
- 이메일 또는 닉네임과 비밀번호로 로그인
- 로그인 성공 시 Access Token과 Refresh Token 발급

### Refresh Token 재발급
- Access Token 만료 시 Refresh Token을 제출하면 새로운 Access Token 발급
- Refresh Token Rotation(RTR) 적용: 이전 Refresh Token 폐기 후 새 토큰 발급
- 클라이언트는 Refresh Token을 HttpOnly 쿠키로 안전하게 관리

### 로그아웃
- 현재 Access Token 무효화

### 회원 탈퇴
- 로그인한 사용자는 본인 계정 삭제 가능

### 2. 유저 검색 

### 개요
- 인증된 사용자는 닉네임 기반으로 다른 사용자를 검색 가능
- 검색 시 최근 검색 기록 저장 및 조회 가능

### 검색 기능
- 닉네임 부분 검색 지원, 대소문자 구분 없음
- 검색 결과는 최근 검색 순으로 정렬
- 페이징 처리로 효율적인 데이터 반환

### 검색 기록 관리
- 최근 검색 기록 저장, 조회, 삭제 가능
- 전체 검색 기록 삭제 가능

### 3. 랜덤 게시글 검색

### 4. 게시글 업로드

### 5. 스토리 업로드

### 6. 개인 프로필 

### 조회
- 로그인한 사용자는 본인 및 다른 사용자의 프로필 정보를 조회 가능
- 프로필 화면에는 닉네임, bio, 게시글 등 포함

### 수정
- 사용자는 본인 프로필을 수정 가능 (닉네임 등 공개 정보)

### 추가 기능 : 앨범
