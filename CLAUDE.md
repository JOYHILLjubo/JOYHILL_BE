# JOYHILL_BE

기쁨의동산교회 청년부(비전교구) 주보/출석/조직 관리 앱의 백엔드. Spring Boot. 실사용자 약 200명이 쓰는 라이브 서비스(`https://joyhill.kro.kr`) — **`main`에 push하면 GitHub Actions가 즉시 빌드·배포한다.**

## 스택 / 구조

- Spring Boot + Spring Security(JWT) + Spring Data JPA
- 운영 DB: EC2 인스턴스 내 로컬 PostgreSQL(DB명 `joy`, 앱 접속 계정 `joy_user`)
- 로컬 개발 DB: H2 in-memory (`application-local.yml`, `ddl-auto: update`)
- 패키지 구조: `domain`(엔티티) / `repository` / `service` / `web`(컨트롤러) / `web.dto.AuthDtos`(요청 DTO record 전부 한 파일에 모음) / `security` / `common.api`(BaseResponse, ErrorCode)

## 코드 컨벤션

- 컨트롤러는 얇게: `@AuthenticationPrincipal AuthUser`를 받아 서비스에 위임, 응답은 항상 `BaseResponse<T>`로 감쌈.
- 응답 DTO를 별도 클래스로 안 만들고 서비스의 `private Map<String,Object> toMap(Entity e)`로 즉석 변환하는 패턴이 전역적으로 일관됨(`SermonService`, `CommunityPrayerService`, `SermonNoteService` 등). 새 기능 추가 시 이 패턴을 따를 것 — 갑자기 record 기반 응답 DTO를 새로 도입하지 말 것.
- 요청 DTO는 전부 `AuthDtos.java`에 `record`로 모아둠(파일 하나, 섹션별 주석 구분).
- 권한 체크는 `AccessGuard`에 메서드로 모아둠(`requirePastorOrAdmin`, `requireNoticeWriter` 등). 컨트롤러/서비스에서 role을 직접 비교하지 말고 여기 추가.
- 에러는 `ApiException(ErrorCode, message)`로 던짐. `ErrorCode`에 없는 상태코드가 필요하면 enum에 추가.
- 완전히 개인 소유 데이터(예: 설교노트)는 타인 데이터 접근 시 `FORBIDDEN`이 아니라 `NOT_FOUND`로 응답해서 존재 자체를 숨김(`SermonNoteService.getOwnedNote` 참고).

## DB 스키마 변경 시 필수 절차 (중요)

운영은 `ddl-auto: validate`라 **엔티티만 추가하면 배포 시 앱이 기동 실패**한다(스키마 불일치 검증 실패). 새 테이블/컬럼을 추가할 때마다:

1. `.github/workflows/deploy.yml`의 "Run pending DB migrations" 스텝(JAR 배포 후 `Restart service` 전)에 멱등 SQL(`CREATE TABLE IF NOT EXISTS`, `ADD COLUMN IF NOT EXISTS` 등)을 추가한다.
2. 마이그레이션은 `sudo -u postgres psql -d "${{ vars.DB_NAME }}"`로 실행되므로 **새 테이블 소유자가 `postgres`가 됨**. 실제 앱은 `joy_user`로 접속하므로 반드시 `ALTER TABLE ... OWNER TO joy_user;` (+ identity 컬럼이 있으면 `ALTER SEQUENCE ..._id_seq OWNER TO joy_user;`)를 같이 넣을 것. 빠뜨리면 `permission denied for table` 500 에러가 남.
3. 로컬(H2, `ddl-auto: update`)에서는 이 스텝 없이도 자동 생성되므로 로컬 테스트만으로는 이 문제를 못 잡는다 — 운영 배포 전 이 스텝을 반드시 챙길 것.

## 운영 서버 확인/조작이 필요할 때

**직접 SSH로 들어가지 않는다.** 이 프로젝트는 2026-07-15에 SSH 키 유출로 인스턴스 삭제 + DB 전체 유실 사고를 겪었고, 이후 에이전트의 직접 SSH 시도는 안전장치가 차단한다. 대신 기존 배포 시크릿(`EC2_HOST`/`EC2_USER`/`EC2_KEY`)을 재사용하는 `workflow_dispatch` 워크플로우를 만들어 사용자가 GitHub Actions에서 직접 실행 버튼을 누르게 하고, 결과는 `gh run view --log`로 읽는다. 예시: `.github/workflows/list-databases.yml`(DB 목록 조회), `check-backend-logs.yml`(에러 로그 확인).

## 로컬 개발

```
export JAVA_HOME=/opt/homebrew/opt/openjdk@21   # JDK 21 필요
export PATH="$JAVA_HOME/bin:$PATH"
sh gradlew bootRun --args=--spring.profiles.active=local
```
`DataInitializer`가 최초 기동 시 테스트 계정을 시드함(예: `010-9999-0000` / 초기 비번 `001225`, member 역할). H2 콘솔: `/h2-console`.

**알려진 이슈 — 로컬 H2에서 `prayers` 테이블이 안 생김 (2026-07-23 확인, 미해결)**: `Prayer.month` 컬럼이 매핑하는 `month`가 H2(MODE=PostgreSQL)의 예약어라 `ddl-auto: update`의 CREATE TABLE DDL이 파싱 에러로 조용히 실패함(`Started DemoApplication`은 뜨지만 `prayers` 테이블만 없음) — `/api/prayers` 관련 로컬 테스트가 전부 500으로 막힘. **`application-local.yml`에 `hibernate.globally_quoted_identifiers: true`를 넣는 방식으로 고쳐보려 했지만, 다른 테이블(`teams` 등) 조회 SQL이 언쿼팅 상태로 생성돼서 "Table not found"가 나며 앱 전체가 깨짐 — 이 방법은 쓰지 말 것.** 운영 Postgres는 이 문제가 없음(`month`가 예약어 아님, `ddl-auto: validate`라 이미 존재하는 스키마 그대로 씀). 로컬에서 기도 기능을 테스트해야 하면 엔티티 컬럼명 자체를 바꾸는 방법(운영 스키마와의 매핑 확인 필요)부터 검토할 것.

프론트(JOYHILL_FE)와 함께 띄울 때 프론트는 **반드시 5173 포트**로 실행할 것 — CORS 허용 origin이 `localhost:5173`/`localhost:3000`/운영 도메인뿐이라 다른 포트는 403으로 막힘(`SecurityConfig.corsConfigurationSource()`).
