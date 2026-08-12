# JOYHILL_BE

기쁨의동산교회 청년부(비전교구) 주보/출석/조직 관리 앱의 백엔드. Spring Boot. 실사용자 약 200명이 쓰는 라이브 서비스(`https://joyhill.kro.kr`) — **`main`에 push하면 GitHub Actions가 즉시 빌드·배포한다.**

## 스택 / 구조

- Spring Boot + Spring Security(JWT) + Spring Data JPA
- 운영 DB: EC2 인스턴스 내 로컬 PostgreSQL(DB명 `joy`, 앱 접속 계정 `joy_user`)
- 로컬 개발 DB: H2 in-memory (`application-local.yml`, `ddl-auto: update`)
- 패키지 구조: `domain`(엔티티) / `repository` / `service` / `web`(컨트롤러) / `web.dto.AuthDtos`(요청 DTO record 전부 한 파일에 모음) / `security` / `common.api`(BaseResponse, ErrorCode)
- 회원 정보/출석 통계를 전용 구글시트로 내보내는 백업 기능(`GoogleSheetsConfig`/`GoogleSheetsSyncService`)이 있음 — 서비스 계정 키/시트 ID가 `google.sheets.*`(`GOOGLE_SHEETS_CREDENTIALS_PATH`/`GOOGLE_SHEETS_SPREADSHEET_ID` env var)로 설정되기 전까진 조용히 비활성화됨. **2026-07-31부터 회원 정보/출석 통계 두 탭으로 분리**: `POST /api/users/sync-sheet`(청년부 전체 관리 페이지, `google.sheets.tab-name` 기본 `회원백업`)와 `POST /api/attendance/sync-sheet`(출석 통계 페이지, `google.sheets.attendance-tab-name` 기본 `출석통계`)를 각각 트리거, 매일 새벽 3시 자동 동기화는 둘 다 독립적으로 실행(하나 실패해도 다른 하나는 진행). `values.clear`/`values.update`는 이미 존재하는 탭만 다룰 수 있어서, `writeRows()`가 쓰기 전에 `ensureTabExists()`로 대상 탭이 없으면 `batchUpdate(AddSheetRequest)`로 자동 생성함 — 새 탭을 스프레드시트에 미리 만들어둘 필요 없음. 마지막 백업 시각은 `sheet_sync_logs` 테이블(백업 종류당 1행)에 남기고 `GET /api/{users,attendance}/sync-sheet`로 조회하며, **시트 쓰기 성공 이후에만 기록**해서 실패한 백업이 성공으로 남지 않게 함.

**시각을 프론트로 내려줄 땐 `LocalDateTime.toString()`을 그대로 쓰지 말 것** — 타임존 정보가 없어서 브라우저의 `new Date()`가 자기 로컬 시간으로 해석해버린다. 서버가 UTC로 돌면 사용자(KST)에게 9시간 어긋난 시각이 보인다. `atZone(ZoneId.systemDefault()).toOffsetDateTime().toString()`으로 오프셋을 붙여 보낼 것(`2026-08-01T15:30:12+09:00`) — 그러면 서버 타임존이 뭐든 클라이언트가 정확히 변환한다.

## 코드 컨벤션

- 컨트롤러는 얇게: `@AuthenticationPrincipal AuthUser`를 받아 서비스에 위임, 응답은 항상 `BaseResponse<T>`로 감쌈.
- 응답 DTO를 별도 클래스로 안 만들고 서비스의 `private Map<String,Object> toMap(Entity e)`로 즉석 변환하는 패턴이 전역적으로 일관됨(`SermonService`, `CommunityPrayerService`, `SermonNoteService` 등). 새 기능 추가 시 이 패턴을 따를 것 — 갑자기 record 기반 응답 DTO를 새로 도입하지 말 것.
- 요청 DTO는 전부 `AuthDtos.java`에 `record`로 모아둠(파일 하나, 섹션별 주석 구분).
- 권한 체크는 `AccessGuard`에 메서드로 모아둠(`requirePastorOrAdmin`, `requireNoticeWriter` 등). 컨트롤러/서비스에서 role을 직접 비교하지 말고 여기 추가.
- 에러는 반드시 `ApiException(ErrorCode, message)`로 던질 것 — `IllegalArgumentException`/`RuntimeException` 등 다른 타입을 던지면 `GlobalExceptionHandler`의 catch-all(`@ExceptionHandler(Exception.class)`)이 그 메시지를 무시하고 항상 "서버 오류가 발생했습니다"로 덮어써서 500이 됨(2026-07-31, `S3Service`/`CommunityPrayerService`/`GoogleSheetsSyncService`에 있던 멀쩡한 한글 에러 메시지가 이렇게 묻혀서 응답에 안 실리고 있었음 — 셋 다 `ApiException`으로 교체해 수정). `ErrorCode`에 없는 상태코드가 필요하면 enum에 추가.
- 완전히 개인 소유 데이터(예: 설교노트)는 타인 데이터 접근 시 `FORBIDDEN`이 아니라 `NOT_FOUND`로 응답해서 존재 자체를 숨김(`SermonNoteService.getOwnedNote` 참고).

## "값을 지운다"를 표현해야 하는 API는 null과 빈 문자열을 구분해서 쓸 것

`OrganizationService.updateFamMember()`의 `famName` 필드가 이 패턴의 예시이자 함정: `request.famName() != null`을 "필드를 건드리지 않음(null)" vs "명시적으로 값을 지정함(빈 문자열 포함)"을 구분하는 게이트로 씀. 즉 `famName: ""`을 보내면 `isBlank() ? null : ...`을 타고 실제로 팸/마을을 지우고(미배정 처리), `famName: null`을 보내면(또는 필드 자체를 생략하면) 아무 일도 안 일어나고 조용히 무시됨 — **에러도 안 나고 다른 필드(이름/전화번호 등)는 정상 저장되기 때문에 프론트에서 실수로 `null`을 보내면 발견하기 매우 어려운 조용한 버그가 됨**(2026-07-28 확인: `VillageManagePageConnected.jsx`가 "미배정" 선택 시 `nullIfBlank(form.fam) || null`로 항상 `null`을 보내고 있어서 저장해도 실제로는 팸이 안 바뀌는 버그가 있었음, 프론트에서 빈 문자열을 그대로 보내도록 수정함). 이런 "지우기 vs 안 건드리기"를 구분해야 하는 필드를 새로 추가할 땐 이 패턴을 그대로 따르고, 프론트 쪽에서 값이 `null`로 뭉개지지 않는지 반드시 확인할 것.

## unique 필드는 생성 경로뿐 아니라 수정 경로에도 중복 체크가 있어야 함

`User.phone`은 `@Column(unique = true)`인데, `addFamMember()`(신규 등록)는 저장 전 `existsByPhone()`으로 중복 체크 후 `ApiException(DUPLICATE_PHONE)`을 던지지만, `OrganizationService.updateFamMember()`/`UserService.update()`(수정)는 이 체크 없이 바로 `setPhone()`하고 있었음(2026-07-31 발견) — 그래서 이미 등록된 번호로 수정하면 DB unique 제약 위반(`DataIntegrityViolationException`)이 위 catch-all 500으로 그대로 샘. `existsByPhoneAndIdNot(phone, id)`로 본인 제외 중복 체크를 추가해 수정. **unique 제약이 있는 필드를 다루는 API를 새로 추가/수정할 때, 생성 경로에 중복 체크가 있으면 수정 경로에도 반드시 대칭으로 넣을 것.**

같은 점검에서 함께 발견/수정한 것들:
- `AccessGuard.requireFamScope`/`requireVillageScope`가 `famName.equals(user.famName())` 식으로 **파라미터가 null이면 NPE**(500)가 났음 — 팸/마을 미배정 상태의 대상에 접근할 때 실제로 null이 들어올 수 있는 케이스였음. null 체크 추가해 정상적으로 403이 나가도록 수정.
- `UserService.create()`/`update()`가 `birth`를 검증 없이 그대로 저장 — 형식이 이상한 값이 하나라도 들어가면 공개 API `GET /api/users/birthdays`가 **전체 회원을 한 스트림으로 조회하면서 `Integer.parseInt`에서 통째로 500**이 나서(한 사람 데이터가 전체 목록 조회를 깨뜨림) 영향 범위가 큼. `OrganizationService`가 이미 쓰던 "숫자만 추출 후 YYMMDD로 정규화" 로직을 그대로 가져와 적용.

## DB 스키마 변경 시 필수 절차 (중요)

운영은 `ddl-auto: validate`라 **엔티티만 추가하면 배포 시 앱이 기동 실패**한다(스키마 불일치 검증 실패). 새 테이블/컬럼을 추가할 때마다:

1. `.github/workflows/deploy.yml`의 "Run pending DB migrations" 스텝(JAR 배포 후 `Restart service` 전)에 멱등 SQL(`CREATE TABLE IF NOT EXISTS`, `ADD COLUMN IF NOT EXISTS` 등)을 추가한다.
2. 마이그레이션은 `sudo -u postgres psql -d "${{ vars.DB_NAME }}"`로 실행되므로 **새 테이블 소유자가 `postgres`가 됨**. 실제 앱은 `joy_user`로 접속하므로 반드시 `ALTER TABLE ... OWNER TO joy_user;` (+ identity 컬럼이 있으면 `ALTER SEQUENCE ..._id_seq OWNER TO joy_user;`)를 같이 넣을 것. 빠뜨리면 `permission denied for table` 500 에러가 남.
3. 로컬(H2, `ddl-auto: update`)에서는 이 스텝 없이도 자동 생성되므로 로컬 테스트만으로는 이 문제를 못 잡는다 — 운영 배포 전 이 스텝을 반드시 챙길 것.

## 파일 업로드(S3)가 실패할 때 — 세 겹으로 막힐 수 있다 (2026-08-01 프로필 사진 기능 붙이며 전부 겪음)

업로드 경로는 `브라우저 → nginx → Spring → S3`인데, 각 구간마다 별개의 원인으로 막힐 수 있고 **증상(HTTP 코드)으로 구간을 구분할 수 있다**:

1. **413 Payload Too Large = nginx 구간.** `client_max_body_size`가 설정돼 있지 않으면 기본값 **1MB**라 요청이 백엔드에 닿기도 전에 잘린다. `application.yml`의 `spring.servlet.multipart.max-file-size: 30MB`는 이 단계에서 아무 소용이 없다. 2026-08-01에 `.github/workflows/set-nginx-upload-limit.yml`로 32M으로 올림(`/etc/nginx/sites-available/joyhill`). **로컬에서는 절대 재현되지 않는다** — 로컬은 vite proxy가 8080으로 직접 붙어서 nginx를 안 거침. 413 응답은 본문이 JSON이 아니라 nginx HTML이라 프론트에서 `res.json()` 파싱이 실패하니, 상태코드로 따로 분기해야 사용자에게 원인을 보여줄 수 있다.
2. **500 + `SdkClientException: Unable to load credentials ... InstanceProfileCredentialsProvider(): Failed to load credentials from IMDS` = EC2에 IAM Role이 안 붙어있음.** 코드 문제가 아니라 인프라 설정 문제다. **2026-07-15에 인스턴스를 새로 만들면서 S3 권한 Role이 재연결되지 않았고, 2026-08-01까지 그 상태였다**(그동안 새 인스턴스에서 아무도 파일 업로드를 안 해봐서 발견이 늦었음 — 공지 이미지 업로드도 같은 코드 경로라 동일하게 깨져 있었을 것). 해결은 AWS 콘솔에서 EC2 인스턴스에 S3 쓰기 권한 Role 연결(에이전트는 AWS API 권한이 없어 못 함). **AWS SDK는 자격증명 조회 실패를 프로세스 수명 동안 물고 갈 수 있으므로 Role 연결 후 백엔드 재시작이 필요하다** — `.github/workflows/restart-backend.yml` 사용.
3. **그 외 500** = Spring 구간. `S3Service`의 검증/압축 단계는 전부 `ApiException`으로 400을 주므로(아래 ApiException 규칙 참고), 여기서 500이 나면 대개 S3 호출 자체가 실패한 것 — `check-backend-logs.yml`로 스택트레이스를 확인할 것.

**이 인스턴스는 RAM이 908Mi로 매우 작아 백엔드 기동에 수십 초~수 분이 걸린다.** 재시작 직후 헬스체크가 502를 반환하는 건 정상적인 기동 지연일 수 있으니, 재시작 스크립트에서 고정 `sleep`으로 판정하지 말고 **폴링**할 것 — `restart-backend.yml`이 5초 간격 최대 3분 폴링 방식으로 되어 있으니 새로 만들 때 이걸 복사해서 쓸 것. 위의 "정확히 200으로 판정하지 말 것"과 같은 계열의 오탐이지만, 그건 "상태코드를 잘못 봄"이고 이건 "너무 일찍 봄"이라는 차이가 있다. 둘 다 빨간 X가 반복되면 진짜 장애를 놓치게 되므로 판정 기준을 정확히 잡을 것.

## 운영 서버 확인/조작이 필요할 때

**직접 SSH로 들어가지 않는다.** 이 프로젝트는 2026-07-15에 SSH 키 유출로 인스턴스 삭제 + DB 전체 유실 사고를 겪었고, 이후 에이전트의 직접 SSH 시도는 안전장치가 차단한다. 대신 기존 배포 시크릿(`EC2_HOST`/`EC2_USER`/`EC2_KEY`)을 재사용하는 `workflow_dispatch` 워크플로우를 만들어 사용자가 GitHub Actions에서 직접 실행 버튼을 누르게 하고, 결과는 `gh run view --log`로 읽는다. 워크플로우를 새로 만들면 **default 브랜치(main)에 머지되어야 dispatch가 가능**하다. 기존 워크플로우: `list-databases.yml`(DB 목록), `check-backend-logs.yml`(에러 로그 — 최근 20분 journal에서 ERROR/Exception 컨텍스트), `capture-server-config.yml`(nginx/systemd/cron 덤프), `check-disk-memory.yml`, `rotate-secrets.yml`, `setup-google-sheets.yml`, `set-nginx-upload-limit.yml`(업로드 크기 제한), `restart-backend.yml`(배포 없이 재시작).

**`gh run view --log`로 결과를 읽을 때**, 로그 각 줄이 `<job>\t<step>\t<타임스탬프> <내용>` 형태라 스크립트 본문(에코된 명령어)과 실제 출력이 섞여 나온다. `sed 's/^<job>\t<step>\t//'`로 접두어를 벗기고 실제 출력 구간만 잘라 보는 게 편하다.

**민감한 환경변수(DB_PASSWORD, JWT_SECRET, GOOGLE_SHEETS_* 등)는 GitHub Actions secrets/`deploy.yml`이 아니라 EC2의 `/etc/systemd/system/joy-backend.service` 파일에 `Environment=KEY=value` 줄로 직접 박혀있다.** `deploy.yml`엔 애초에 env var 주입 로직이 없음 — 새 환경변수가 필요하면 `rotate-secrets.yml`/`setup-google-sheets.yml`처럼 systemd 유닛 파일을 `sed`로 고치고 `daemon-reload` + `restart`하는 workflow_dispatch 워크플로우를 새로 만드는 패턴을 따를 것.

**workflow_dispatch 스크립트에서 "정상 기동" 헬스체크를 `curl ... | grep 200`처럼 정확히 200으로만 판정하지 말 것 (반복 발견된 오탐 패턴)**: `/api/users/birthdays` 같은 흔히 쓰는 헬스체크 대상 엔드포인트가 실제로는 인증이 필요해서 토큰 없이 호출하면 항상 401이 나옴 — `rotate-secrets.yml`과 `setup-google-sheets.yml` 둘 다 이 패턴으로 오탐 실패를 겪었음(서버는 정상 기동했는데 워크플로우만 실패로 표시됨). 올바른 판정: `CODE != "000"`(연결 자체는 됐음) `&& ${CODE:0:1} != "5"`(서버 에러 아님) — 401/403/404는 "서버가 응답은 하고 있다"는 뜻이라 정상. `setup-google-sheets.yml`에 이 방식으로 이미 수정 반영해뒀고, `rotate-secrets.yml`은 아직 예전 방식(`= "200"` 고정 체크)이라 재사용 전 같은 방식으로 고칠 것.

## 로컬 개발

```
export JAVA_HOME=/opt/homebrew/opt/openjdk@21   # JDK 21 필요
export PATH="$JAVA_HOME/bin:$PATH"
sh gradlew bootRun --args=--spring.profiles.active=local
```
`DataInitializer`가 최초 기동 시 테스트 계정을 시드함(예: `010-9999-0000` / 초기 비번 `001225`, member 역할). H2 콘솔: `/h2-console`.

**알려진 이슈 — 로컬 H2에서 `prayers` 테이블이 안 생김 (2026-07-23 확인, 미해결)**: `Prayer.month` 컬럼이 매핑하는 `month`가 H2(MODE=PostgreSQL)의 예약어라 `ddl-auto: update`의 CREATE TABLE DDL이 파싱 에러로 조용히 실패함(`Started DemoApplication`은 뜨지만 `prayers` 테이블만 없음) — `/api/prayers` 관련 로컬 테스트가 전부 500으로 막힘. **`application-local.yml`에 `hibernate.globally_quoted_identifiers: true`를 넣는 방식으로 고쳐보려 했지만, 다른 테이블(`teams` 등) 조회 SQL이 언쿼팅 상태로 생성돼서 "Table not found"가 나며 앱 전체가 깨짐 — 이 방법은 쓰지 말 것.** 운영 Postgres는 이 문제가 없음(`month`가 예약어 아님, `ddl-auto: validate`라 이미 존재하는 스키마 그대로 씀). 로컬에서 기도 기능을 테스트해야 하면 엔티티 컬럼명 자체를 바꾸는 방법(운영 스키마와의 매핑 확인 필요)부터 검토할 것.

프론트(JOYHILL_FE)와 함께 띄울 때 프론트는 **반드시 5173 포트**로 실행할 것 — CORS 허용 origin이 `localhost:5173`/`localhost:3000`/운영 도메인뿐이라 다른 포트는 403으로 막힘(`SecurityConfig.corsConfigurationSource()`).
