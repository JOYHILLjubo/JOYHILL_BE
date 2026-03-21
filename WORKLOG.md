# WORKLOG

## 2026-03-21

### 작업 내용
- JOY 교회 앱 개발문서를 기준으로 Spring Boot 백엔드 골격 구성
- `build.gradle`에 Web, Validation, JPA, Security, JWT, PostgreSQL, H2 의존성 추가
- 공통 응답 래퍼 및 예외 처리 추가
- JWT 기반 인증/인가 구조 추가
- 아래 도메인 엔티티, 리포지토리, 서비스, 컨트롤러 구현
  - 인증
  - 조직/마을/팸/팸원
  - 사용자 계정
  - 출석
  - 공지사항
  - 기도제목
  - 설교
  - 새가족
- 문서 기준 더미 계정 시드 데이터 추가
- `application.yml`에 PostgreSQL 기본 설정 반영
- `application-local.yml`에 H2 로컬 실행 프로필 추가

### 현재 상태
- `compileJava` 성공
- 기본 실행 실패 원인은 PostgreSQL 미기동
- `local` 프로필로 H2 기반 실행 가능하도록 설정 완료

### 다음 작업
- `local` 프로필로 실제 서버 실행 확인
- 로그인/조직/공지/기도/설교 API 우선 점검
- 프론트엔드 더미 데이터를 실제 API 호출로 교체

### 실행 메모
- PostgreSQL 실행: `application.yml`의 DB 계정 정보 수정 후 사용
- 로컬 H2 실행:

```powershell
.\gradlew.bat bootRun --args="--spring.profiles.active=local"
```
