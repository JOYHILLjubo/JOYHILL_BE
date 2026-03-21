# JOYHILL Backend

JOY 교회 청년부 앱 백엔드 프로젝트입니다.

Spring Boot, Spring Security, JWT, JPA, PostgreSQL 기반으로 구성했고, 로컬 개발용 H2 프로필도 함께 제공합니다.

## Tech Stack

- Java 21
- Spring Boot 4
- Spring Security
- JWT
- Spring Data JPA
- PostgreSQL
- H2
- Gradle

## Implemented Features

- 인증
  - 로그인
  - 토큰 재발급
  - 로그아웃
  - 비밀번호 변경
- 조직 관리
  - 마을 / 팸 / 팸원 조회 및 관리
  - 역할 승급 / 강등 규칙 반영
- 사용자 관리
  - 관리자 전용 사용자 CRUD
- 출석 관리
  - 출석 저장
  - 월별 이력 조회
  - 통계 조회
- 공지사항
  - 목록 / 상세 / 등록 / 수정 / 삭제
- 기도제목
  - 개인 / 공동 기도제목 관리
- 설교
  - 최신 설교 조회
  - 설교 등록
- 새가족
  - 등록 / 조회 / 팸 배정 / 삭제

## Response Format

모든 API는 아래 응답 구조를 따릅니다.

```json
{
  "success": true,
  "data": {}
}
```

```json
{
  "success": false,
  "error": {
    "code": "ERROR_CODE",
    "message": "설명"
  }
}
```

## Profiles

### default

PostgreSQL 실행용 프로필입니다.

설정 파일:
- `src/main/resources/application.yml`

### local

H2 메모리 DB 실행용 프로필입니다.

설정 파일:
- `src/main/resources/application-local.yml`

로컬에서 PostgreSQL 없이 바로 실행할 때 사용합니다.

## Run

### 1. 로컬 H2로 실행

```powershell
.\gradlew.bat bootRun --args="--spring.profiles.active=local"
```

### 2. PostgreSQL로 실행

`src/main/resources/application.yml`의 DB 정보와 JWT secret을 실제 값으로 바꾼 뒤 실행합니다.

```powershell
.\gradlew.bat bootRun
```

## Build

```powershell
.\gradlew.bat compileJava
```

## Seed Accounts

`local` 프로필 실행 시 아래 기본 계정이 등록됩니다.

| 역할 | 이름 | 전화번호 | 비밀번호 |
|---|---|---|---|
| leader | 김민준 | 010-1111-2222 | 950315 |
| member | 박청년 | 010-9999-0000 | 001225 |
| village_leader | 홍성인 | 010-3333-4444 | 881020 |
| pastor | 정교역자 | 010-5555-6666 | 750601 |
| admin | 관리자 | 010-7777-8888 | 700101 |

전화번호는 하이픈 포함/제거 모두 허용하도록 정규화합니다.

## Main Packages

- `common`
  - 공통 응답 / 예외 처리
- `config`
  - 초기 데이터 시드
- `domain`
  - JPA 엔티티
- `repository`
  - JPA 리포지토리
- `security`
  - JWT / 인증 필터 / 보안 설정
- `service`
  - 비즈니스 로직
- `web`
  - REST API 컨트롤러

## Main Endpoints

### Auth

- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `POST /api/auth/logout`
- `PATCH /api/auth/change-password`

### Organization

- `GET /api/org/structure`
- `GET /api/villages`
- `DELETE /api/villages/{villageName}`
- `GET /api/fams`
- `PATCH /api/fams/{famName}/village`
- `DELETE /api/fams/{famName}`
- `GET /api/fams/{famName}/members`
- `POST /api/fams/{famName}/members`
- `PUT /api/fam-members/{id}`
- `PATCH /api/fam-members/{id}/role`
- `DELETE /api/fam-members/{id}`

### Users

- `GET /api/users`
- `POST /api/users`
- `PUT /api/users/{id}`
- `PATCH /api/users/{id}/role`
- `DELETE /api/users/{id}`

### Attendance

- `GET /api/attendance`
- `POST /api/attendance`
- `GET /api/attendance/history`
- `GET /api/attendance/stats`

### Notices

- `GET /api/notices`
- `GET /api/notices/{id}`
- `POST /api/notices`
- `PUT /api/notices/{id}`
- `DELETE /api/notices/{id}`

### Prayers

- `GET /api/prayers`
- `POST /api/prayers`
- `PUT /api/prayers/{id}`
- `POST /api/prayers/common`

### Sermon

- `GET /api/sermon/latest`
- `POST /api/sermon`

### Newcomers

- `GET /api/newcomers`
- `POST /api/newcomers`
- `PATCH /api/newcomers/{id}/fam`
- `DELETE /api/newcomers/{id}`

## Notes

- 기본 PostgreSQL 설정은 예시 값입니다.
- 실제 운영 전에는 `application.yml`의 DB 계정, JWT secret, AWS 설정을 실제 환경 변수 또는 비공개 설정으로 분리하는 것을 권장합니다.
- 현재 테스트는 기본 스모크 수준만 포함되어 있으며, 실제 API 통합 테스트는 추가 작업이 필요합니다.
