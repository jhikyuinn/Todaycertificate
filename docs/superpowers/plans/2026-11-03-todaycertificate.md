# 오늘의 자격증 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 국가자격 필기 기출 학습 앱(공부/시험 모드, 운영자 CSV 업로드, 오답·별표, 오늘의 문제)을 Spring Boot 서버와 Expo 웹 앱으로 만들고 배포한다.

**Architecture:** `server/`는 기능별 패키지(member, certificate, exam, solve, attempt, study)로 나눈 Spring Boot REST API이며 JWT로 인증한다. `client/`는 최신 Expo 템플릿(expo-router)으로 새로 만든 앱이고 웹으로 빌드해 Vercel에 올린다. 서버는 Render(Docker), DB는 Neon PostgreSQL.

**Tech Stack:** Java 25, Spring Boot 4.1 (Web MVC, Data JPA, Validation, Security, OAuth2 Resource Server for JWT), PostgreSQL 17, Flyway, Apache Commons CSV, Testcontainers / Expo (latest SDK, TypeScript, expo-router), AsyncStorage

**Spec:** `docs/superpowers/specs/2026-11-03-todaycertificate-design.md`

## Global Constraints

- 비밀값(DB 비밀번호, `JWT_SECRET`)은 `server/.env` 또는 배포 환경변수에만 둔다. 레포에 커밋하지 않는다.
- 커밋 메시지는 영어, `feat:`/`fix:`/`test:`/`docs:`/`refactor:`/`chore:` 접두어. 브랜치 `feature/*`.
- 모든 API는 `/api` 아래 JSON. 오류 응답은 `{"code": "...", "message": "...", "details": [...]}` 하나의 형식.
- `/api/auth/**`만 로그인 없이 호출할 수 있고, `/api/admin/**`은 ADMIN만, 나머지는 로그인한 사용자만 호출한다.
- 문제 텍스트(문제, 보기, 해설)는 줄바꿈과 공백까지 그대로 저장하고, ` ``` ` 안쪽은 화면에서 고정폭 글꼴과 회색 박스로 보여준다. 문법 강조는 하지 않는다.
- 과목 점수 = 과목 정답 수 × 100 / 과목 문항 수 (정수, 버림). 과락 = 과목 점수 < `failScore`. 합격 = 과락 없음 AND 평균 ≥ `passAverage`. 평균은 과목 점수의 평균(소수 첫째 자리 반올림).
- 기본값: `failScore` 40, `passAverage` 60.
- 서버 테스트는 Testcontainers PostgreSQL로 돌린다. 로컬 `.env`가 없어도 Docker만 있으면 `./gradlew test`가 통과해야 한다.
- 화면은 기능 위주로 단순하게 만든다 (UI 개편은 범위 밖).

## Review Focus

1. **엑셀에서 저장한 CSV:** UTF-8 BOM이 붙거나 CP949로 저장된 파일. BOM은 무시하고, UTF-8이 아니면 깨진 글자로 저장하지 않고 "UTF-8로 저장해 주세요" 오류를 낸다. 테스트는 Task 5.
2. **엑셀의 CRLF 줄바꿈:** 코드 칸 안의 `\r\n`은 `\n`으로 바꿔서 저장한다. 들여쓰기는 그대로 둔다. 테스트는 Task 5.
3. **시험 제출 이상 상황:** 이미 제출한 응시를 다시 제출하면 409, 남의 응시를 제출하면 403, 그 회차에 없는 문제 ID가 섞이면 400. 어떤 경우에도 풀이 기록이 중복 저장되지 않는다. 테스트는 Task 13.
4. **문제가 달린 과목 삭제:** 운영자가 자격증을 수정하면서 문제가 있는 과목을 빼려고 하면 409를 내고 아무것도 바꾸지 않는다. 테스트는 Task 4.
5. **잘못되거나 만료된 JWT:** 서버는 500이 아니라 401 JSON을 준다. 화면은 로그인 화면으로 보낸다. 서버 테스트는 Task 3, 화면 확인은 Task 12.

---

## 파일 구조

```
server/src/main/java/com/todaycertificate/
  common/        ApiException, ErrorCode, ErrorResponse, GlobalExceptionHandler, SecurityConfig, JwtConfig, CurrentMember
  member/        Member, Role, MemberRepository, AuthController, AuthService, MeController
  certificate/   Certificate, Subject, ExamSchedule, (각 Repository), CertificateController, AdminCertificateController, AdminCertificateService
  exam/          Exam, Question, (각 Repository), ExamController, ScoreCalculator
  exam/upload/   CsvQuestionParser, QuestionRow, CsvError, CsvValidationException, ExamUploadService, AdminExamUploadController
  solve/         SolveRecord, SolveMode, Bookmark, (각 Repository), SolveController, BookmarkController, NoteController
  attempt/       ExamAttempt, ExamAttemptRepository, AttemptController, AttemptService
  study/         StudyController, DailyProblemService, SubjectReportService   (우선순위 5)
server/src/main/resources/  application.yml, schema.sql → db/migration/V1__init.sql, data.sql → db/migration/V2__sample_data.sql
server/src/test/java/com/todaycertificate/  TestcontainersConfig + 기능별 테스트
docs/sample/sample-questions.csv   운영자 업로드 시연용 샘플 CSV

client/   (create-expo-app 최신 템플릿으로 새로 생성)
  lib/api.ts         apiFetch, ApiError
  lib/auth.tsx       AuthProvider, useAuth
  components/RichText.tsx      ``` 코드 블록 렌더링
  components/QuestionCard.tsx  문제 + 보기 4개 + 별표
  app/_layout.tsx, app/login.tsx, app/signup.tsx
  app/(tabs)/_layout.tsx, app/(tabs)/index.tsx(홈), app/(tabs)/certificates.tsx, app/(tabs)/my.tsx
  app/certificates/[id].tsx, app/exams/[examId]/study.tsx, app/exams/[examId]/exam.tsx, app/attempts/[attemptId].tsx
  app/notes/wrong.tsx, app/notes/bookmarks.tsx
  app/admin/index.tsx, app/admin/certificates/[id].tsx, app/admin/upload.tsx
```

---

## 우선순위 1: 핵심 흐름

### Task 1: 스키마 재설계, 엔티티, 테스트 환경

**Files:**
- Modify: `server/src/main/resources/schema.sql`, `server/src/main/resources/data.sql`, `server/build.gradle`
- Modify: `certificate/Certificate.java`, `exam/Question.java` (certificate 패키지에서 이동), `member/Member.java`
- Create: `certificate/Subject.java`, `certificate/ExamSchedule.java`, `exam/Exam.java`, `member/Role.java`, `solve/SolveRecord.java`, `solve/SolveMode.java`, `solve/Bookmark.java`, `attempt/ExamAttempt.java`, 각 `*Repository.java`
- Create: `server/src/test/java/com/todaycertificate/TestcontainersConfig.java`
- Test: `server/src/test/java/com/todaycertificate/ServerApplicationTests.java`

**Interfaces:**
- Produces (테이블 → 엔티티 필드):
  - `certificate(id, name UNIQUE, description, time_limit_minutes, fail_score DEFAULT 40, pass_average DEFAULT 60)`
  - `subject(id, certificate_id FK, number, name, UNIQUE(certificate_id, number))`
  - `exam_schedule(id, certificate_id FK, year, round, registration_start, registration_end, exam_date)`
  - `exam(id, certificate_id FK, year, round, UNIQUE(certificate_id, year, round))`
  - `question(id, exam_id FK, subject_id FK, number, content, choice1, choice2, choice3, choice4, answer_no CHECK 1~4, explanation, UNIQUE(exam_id, number))`
  - `member(id, login_id UNIQUE, name, password, role DEFAULT 'USER', target_schedule_id FK NULL)`
  - `solve_record(id, member_id FK, question_id FK, selected_no, correct, mode, solved_at)` + `(member_id, question_id, solved_at)` 인덱스
  - `bookmark(id, member_id FK, question_id FK, UNIQUE(member_id, question_id))`
  - `exam_attempt(id, member_id FK, exam_id FK, started_at, submitted_at NULL, average_score NULL, passed NULL)`
  - `Role { USER, ADMIN }`, `SolveMode { STUDY, EXAM }` (`@Enumerated(STRING)`)
- `data.sql`: 샘플 자격증 2개(정보처리기사, SQLD), 각 과목, 올해 일정 1건, 회차 1개, 과목당 3문제(그중 1문제는 ` ``` ` 코드 포함). 맨 위 주석으로 "AI가 만든 샘플" 표시.

- [ ] **Step 1:** `build.gradle`에 `spring-boot-testcontainers`, Testcontainers PostgreSQL 모듈(테스트 의존성)을 추가하고, `TestcontainersConfig`에 `@ServiceConnection PostgreSQLContainer("postgres:17")` 빈을 만든다. `ServerApplicationTests`가 이 설정을 `@Import`한다.
- [ ] **Step 2:** 위 스키마로 `schema.sql`과 `data.sql`을 다시 쓰고, 엔티티를 테이블에 맞춘다. 연관관계는 모두 `@ManyToOne(fetch = LAZY)`.
- [ ] **Step 3:** `./gradlew test` 실행. 기대: `contextLoads` PASS (`ddl-auto: validate` 통과).
- [ ] **Step 4:** 로컬 DB를 새 스키마로 다시 만든다: `docker compose down -v && docker compose up -d`.
- [ ] **Step 5:** Commit `refactor: redesign schema for exams, subjects and study records`

### Task 2: 공통 오류 응답과 CORS

**Files:**
- Create: `common/ErrorCode.java`, `common/ApiException.java`, `common/ErrorResponse.java`, `common/GlobalExceptionHandler.java`
- Modify: `application.yml` (`app.cors.allowed-origins: ${CORS_ALLOWED_ORIGINS:http://localhost:8081}`), `.env.example`
- Test: `common/GlobalExceptionHandlerTest.java` (`@WebMvcTest` + 테스트용 컨트롤러)

**Interfaces:**
- Produces: `ApiException(ErrorCode code)`, `ApiException(ErrorCode code, List<?> details)`
- `ErrorCode` (HTTP 상태 포함): `VALIDATION_FAILED(400)`, `UNAUTHORIZED(401)`, `FORBIDDEN(403)`, `NOT_FOUND(404)`, `DUPLICATE_LOGIN_ID(409)`, `ALREADY_SUBMITTED(409)`, `SUBJECT_IN_USE(409)`, `CSV_INVALID(400)`, `INVALID_CREDENTIALS(401)`
- `record ErrorResponse(String code, String message, List<?> details)`

- [ ] **Step 1:** 테스트 작성: `apiException_returnsCodeAndStatus` (`NOT_FOUND`를 던지면 404와 `{"code":"NOT_FOUND"}`), `beanValidation_returns400WithFieldDetails` (`@Valid` 실패 시 400, `details`에 필드 이름 포함).
- [ ] **Step 2:** 실행해서 FAIL 확인.
- [ ] **Step 3:** 구현. CORS는 Task 3의 `SecurityConfig`에서 `app.cors.allowed-origins`(쉼표 구분)를 읽어 적용한다. 이 태스크에서는 설정 키만 추가한다.
- [ ] **Step 4:** 실행해서 PASS 확인.
- [ ] **Step 5:** Commit `feat: add unified error response`

### Task 3: 회원가입, 로그인, JWT, 권한

**Files:**
- Modify: `build.gradle` (`spring-boot-starter-security`, `spring-boot-starter-oauth2-resource-server`)
- Create: `common/SecurityConfig.java`, `common/JwtConfig.java`, `common/CurrentMember.java`, `member/AuthController.java`, `member/AuthService.java`, `member/MeController.java`
- Modify: `application.yml` (`app.jwt.secret: ${JWT_SECRET}`, `app.jwt.expiry-hours: 24`), `.env.example` (`JWT_SECRET=` 32바이트 이상)
- Test: `member/AuthControllerTest.java` (`@SpringBootTest` + MockMvc + Testcontainers)

**Interfaces:**
- `POST /api/auth/signup` `{loginId, name, password}` → 201 `{id}`. 검증: loginId 4~20자 영문·숫자, name 1~20자, password 8~64자.
- `POST /api/auth/login` `{loginId, password}` → 200 `{accessToken, member: {id, loginId, name, role}}`
- `GET /api/me` → `{id, loginId, name, role}`
- JWT: HS256, `sub` = member id, `role` claim. `NimbusJwtEncoder/Decoder` 사용(라이브러리 추가 없음).
- `CurrentMember.id(Authentication) → Long`: 다른 컨트롤러가 현재 회원 ID를 얻는 방법.
- 테스트 유틸 `TestAuth.tokenFor(Member)`: 이후 테스트에서 재사용.

- [ ] **Step 1:** 테스트 작성:
  - `signup_thenLogin_returnsToken`
  - `signup_duplicateLoginId_returns409` (`code = DUPLICATE_LOGIN_ID`)
  - `login_wrongPassword_returns401`
  - `password_isStoredAsBcryptHash` (DB 값이 `$2`로 시작하고 원문과 다름)
  - `me_withoutToken_returns401Json`
  - `me_withGarbageToken_returns401Json` (Review Focus 5)
  - `adminApi_withUserToken_returns403`
- [ ] **Step 2:** 실행해서 FAIL 확인.
- [ ] **Step 3:** 구현. 중복 아이디는 먼저 조회해서 409를 내고, 동시 가입 시 `DataIntegrityViolationException`도 409로 바꾼다. 401과 403 응답도 `ErrorResponse` 형식으로 맞춘다.
- [ ] **Step 4:** 실행해서 PASS 확인.
- [ ] **Step 5:** Commit `feat: add signup, login and JWT authorization`

### Task 4: 운영자 자격증 관리 API

**Files:**
- Create: `certificate/AdminCertificateController.java`, `certificate/AdminCertificateService.java`, 요청·응답 record
- Test: `certificate/AdminCertificateControllerTest.java`

**Interfaces:**
- `POST /api/admin/certificates` `{name, description, timeLimitMinutes, failScore, passAverage, subjects: [{number, name}]}` → 201 `{id}`
- `PUT /api/admin/certificates/{id}` (같은 본문). 과목은 번호 기준으로 맞춘다: 같은 번호는 이름 수정, 새 번호는 추가, 빠진 번호는 삭제. 단, 빠진 과목에 문제가 있으면 409 `SUBJECT_IN_USE`를 내고 전체를 롤백한다.
- `POST /api/admin/certificates/{id}/schedules` `{year, round, registrationStart, registrationEnd, examDate}` → 201
- `DELETE /api/admin/schedules/{id}` → 204

- [ ] **Step 1:** 테스트 작성:
  - `create_withSubjects_persistsAll`
  - `update_renamesAndAddsSubjects`
  - `update_removingSubjectWithQuestions_returns409AndChangesNothing` (Review Focus 4)
  - `addSchedule_thenDelete`
  - `create_duplicateName_returns409`
- [ ] **Step 2:** FAIL 확인 → **Step 3:** 구현 (`@Transactional`) → **Step 4:** PASS 확인
- [ ] **Step 5:** Commit `feat: add admin certificate management API`

### Task 5: CSV 파싱과 검증

**Files:**
- Modify: `build.gradle` (`org.apache.commons:commons-csv`)
- Create: `exam/upload/CsvQuestionParser.java`, `QuestionRow.java`, `CsvError.java`, `CsvParseResult.java`
- Create: `docs/sample/sample-questions.csv`
- Test: `exam/upload/CsvQuestionParserTest.java` (순수 단위 테스트, Spring 없음)

**Interfaces:**
- `CsvParseResult CsvQuestionParser.parse(byte[] file, Set<Integer> subjectNumbers)`
- `record QuestionRow(int subjectNumber, int number, String content, List<String> choices, int answerNo, String explanation)` (`choices`는 4개)
- `record CsvError(int row, String message)`. `row`는 헤더를 뺀 1부터의 문제 순번이다. 여러 줄짜리 칸이 있어서 물리적인 줄 번호와 다르다.
- `record CsvParseResult(List<QuestionRow> rows, List<CsvError> errors)`
- 헤더는 정확히 `과목번호,문제번호,문제,보기1,보기2,보기3,보기4,정답번호,해설`.

- [ ] **Step 1:** 테스트 작성:
  - `validFile_parsesAllRows`
  - `multilineCodeCell_preservesNewlinesAndIndentation` (`"```\nif (a):\n    print(1)\n```"`가 그대로 유지)
  - `crlfInsideCell_isNormalizedToLf` (Review Focus 2)
  - `utf8Bom_isIgnored` (Review Focus 1)
  - `nonUtf8File_returnsSingleEncodingError` (CP949 바이트 → 오류 1개, 메시지에 "UTF-8")
  - `wrongHeader_returnsError`
  - `answerNoOutOfRange_reportsRow` (정답번호 5 → `row` 2)
  - `unknownSubject_reportsRow`
  - `duplicateQuestionNumber_reportsRow`
  - `emptyRequiredCell_reportsRow` (해설만 비어 있는 경우는 오류 아님)
  - `unbalancedCodeFence_reportsRow` (` ``` ` 개수가 홀수)
  - `multipleErrors_allReported`
- [ ] **Step 2:** FAIL 확인 → **Step 3:** 구현. 바이트를 엄격한 UTF-8 `CharsetDecoder`(`REPORT`)로 먼저 디코딩한다 → PASS 확인
- [ ] **Step 4:** `docs/sample/sample-questions.csv` 작성 (과목 2개, 문제 4개, 코드 문제 1개 포함). 이 파일을 파싱하는 테스트 `sampleFile_hasNoErrors`를 추가한다.
- [ ] **Step 5:** Commit `feat: add CSV question parser with validation`

### Task 6: 회차 업로드 API (문제번호 기준 덮어쓰기)

**Files:**
- Create: `exam/upload/ExamUploadService.java`, `exam/upload/AdminExamUploadController.java`, `exam/upload/CsvValidationException.java`
- Test: `exam/upload/ExamUploadServiceTest.java`

**Interfaces:**
- Consumes: `CsvQuestionParser.parse` (Task 5)
- `UploadResult ExamUploadService.upload(Long certificateId, int year, int round, byte[] csv)`, `record UploadResult(Long examId, int created, int updated, int deleted)`
- `POST /api/admin/certificates/{id}/exams` (multipart: `year`, `round`, `file`) → 200 `UploadResult`. 오류가 있으면 400 `CSV_INVALID`, `details` = `CsvError` 목록.
- 동작: 회차가 없으면 만든다. 같은 문제번호는 내용을 갱신하고, 새 번호는 추가한다. 파일에 없는 번호는 그 문제의 `solve_record`와 `bookmark`를 먼저 지운 뒤 문제를 삭제한다. 전체를 `@Transactional` 하나로 묶는다.

- [ ] **Step 1:** 테스트 작성:
  - `firstUpload_createsExamAndQuestions` (created=2)
  - `reupload_updatesByNumberAndKeepsSolveRecords` (1번의 텍스트를 수정하고 2번을 뺀 파일 → updated=1, deleted=1, 1번의 풀이 기록은 남아 있음)
  - `invalidFile_savesNothing` (문제 수가 그대로)
  - `upload_withUserToken_returns403`
- [ ] **Step 2:** FAIL 확인 → **Step 3:** 구현 → **Step 4:** PASS 확인
- [ ] **Step 5:** Commit `feat: add exam CSV upload with upsert by question number`

### Task 7: 자격증 조회와 문제 조회 API

**Files:**
- Create: `certificate/CertificateController.java`, `exam/ExamController.java`, 응답 record
- Test: `certificate/CertificateControllerTest.java`

**Interfaces:**
- `GET /api/certificates` → `[{id, name, description}]`
- `GET /api/certificates/{id}` → `{id, name, description, timeLimitMinutes, failScore, passAverage, subjects: [{id, number, name}], schedules: [올해 일정만, examDate 순], exams: [{id, year, round, questionCount}] (최신순)}`
- `GET /api/exams/{examId}/questions` → `[QuestionView]`, `record QuestionView(Long id, int number, int subjectNumber, String subjectName, String content, List<String> choices, boolean bookmarked)`. **정답과 해설은 포함하지 않는다.** Task 13과 11도 같은 `QuestionView`를 쓴다.

- [ ] **Step 1:** 테스트 작성:
  - `detail_includesOnlyCurrentYearSchedules`
  - `questions_doNotExposeAnswer` (응답 JSON에 `answerNo`와 `explanation` 키가 없음)
  - `questions_orderedByNumber`
  - `detail_unknownId_returns404`
- [ ] **Step 2:** FAIL 확인 → **Step 3:** 구현. 문제 목록은 과목을 fetch join으로 한 번에 가져와서 N+1을 피한다 → **Step 4:** PASS 확인
- [ ] **Step 5:** Commit `feat: add certificate and question read API`

### Task 8: 공부 모드 풀이 API

**Files:**
- Create: `solve/SolveController.java`, `solve/SolveService.java`
- Test: `solve/SolveControllerTest.java`

**Interfaces:**
- `POST /api/questions/{id}/solve` `{selectedNo}` (1~4) → `{correct, answerNo, explanation}`. `SolveRecord(mode=STUDY)`를 저장한다.
- `SolveService.record(Long memberId, Question q, int selectedNo, SolveMode mode) → SolveRecord`: Task 13이 재사용한다.

- [ ] **Step 1:** 테스트 작성: `solve_correct_returnsTrueAndSavesRecord`, `solve_wrong_returnsAnswerAndExplanation`, `solve_selectedNoOutOfRange_returns400`
- [ ] **Step 2:** FAIL 확인 → **Step 3:** 구현 → **Step 4:** PASS 확인
- [ ] **Step 5:** Commit `feat: add study mode solve API`

### Task 9: 클라이언트 새로 만들기, API 클라이언트, 로그인·회원가입 화면

**Files:**
- Delete: 기존 `client/` 전체 (git 기록에 남는다)
- Create: `npx create-expo-app@latest client` (TypeScript, expo-router 기본 템플릿). 예제 화면은 지운다.
- Create: `client/lib/api.ts`, `client/lib/auth.tsx`, `client/app/_layout.tsx`, `client/app/login.tsx`, `client/app/signup.tsx`, `client/.env.example` (`EXPO_PUBLIC_API_URL=http://localhost:8080`)

**Interfaces:**
- `apiFetch<T>(path: string, init?: RequestInit): Promise<T>`: 토큰을 붙이고, 2xx가 아니면 `ApiError {status, code, message, details}`를 던진다. 401이면 저장된 토큰을 지우고 `onUnauthorized` 콜백을 호출한다.
- `useAuth(): { member, token, login(loginId, password), signup(...), logout() }`. 토큰은 AsyncStorage에 저장한다.
- `_layout.tsx`: 토큰이 없으면 `/login`으로 보낸다.

- [ ] **Step 1:** 생성 후 `npx expo start --web`으로 기본 화면이 뜨는지 확인한다.
- [ ] **Step 2:** `lib/api.ts`와 `lib/auth.tsx`, 로그인·회원가입 화면을 구현한다. 서버 오류 `code`에 따라 메시지를 보여준다 (`DUPLICATE_LOGIN_ID`면 "이미 사용 중인 아이디").
- [ ] **Step 3:** 수동 확인 (서버 실행 상태): 가입 → 로그인 → 빈 탭 화면 진입 → 새로고침해도 로그인이 유지됨 → 틀린 비밀번호는 오류 메시지.
- [ ] **Step 4:** Commit `refactor: recreate client on latest Expo with auth screens`

### Task 10: 화면: 자격증 목록·상세, 회차·모드 선택

**Files:**
- Create: `client/app/(tabs)/_layout.tsx` (탭: 홈, 자격증, 내 학습), `client/app/(tabs)/certificates.tsx`, `client/app/certificates/[id].tsx`

**Interfaces:**
- Consumes: `GET /api/certificates`, `GET /api/certificates/{id}` (Task 7)
- 상세 화면의 회차를 누르면 "공부 모드 / 시험 모드" 선택 → `/exams/{examId}/study` 또는 `/exams/{examId}/exam`.

- [ ] **Step 1:** 구현
- [ ] **Step 2:** 수동 확인: 샘플 자격증 2개 → 상세에 설명·일정·회차 → 모드 선택 이동
- [ ] **Step 3:** Commit `feat: add certificate list and detail screens`

### Task 11: 화면: 공부 모드와 코드 블록

**Files:**
- Create: `client/components/RichText.tsx`, `client/components/QuestionCard.tsx`, `client/app/exams/[examId]/study.tsx`

**Interfaces:**
- `splitCodeBlocks(text: string): Array<{ type: 'text' | 'code'; value: string }>`, `RichText({ text })`
- `QuestionCard({ question: QuestionView, selectedNo, onSelect, revealedAnswerNo?, onToggleBookmark? })`: Task 14와 17에서 재사용한다.
- Consumes: `GET /api/exams/{examId}/questions`, `POST /api/questions/{id}/solve`

- [ ] **Step 1:** 구현. 보기를 고르면 정답 보기는 초록, 고른 오답은 빨강으로 표시하고 해설과 "다음" 버튼을 보여준다.
- [ ] **Step 2:** 수동 확인: 샘플의 코드 문제에서 들여쓰기와 고정폭 글꼴이 유지됨, 마지막 문제 다음은 회차 상세로 돌아감
- [ ] **Step 3:** Commit `feat: add study mode screen with code block rendering`

### Task 12: 화면: 운영자 (자격증 편집, 회차 업로드)

**Files:**
- Create: `client/app/admin/index.tsx`, `client/app/admin/certificates/[id].tsx` (`id = new`이면 생성), `client/app/admin/upload.tsx`
- Modify: `client/app/(tabs)/my.tsx` (ADMIN에게만 "운영자 화면" 버튼)

**Interfaces:**
- Consumes: Task 4, Task 6 API. 웹에서는 `<input type="file">`로 CSV를 고르고 `FormData`로 보낸다 (웹 전용 기능이므로 `Platform.OS === 'web'`일 때만 노출).
- 업로드 결과: 성공이면 "N개 추가 / N개 수정 / N개 삭제", 실패면 `details`를 "N번째 문제: 이유" 목록으로 보여준다.

- [ ] **Step 1:** 구현
- [ ] **Step 2:** 수동 확인: 로컬 DB에서 내 계정을 ADMIN으로 바꾼다 (`UPDATE member SET role='ADMIN' WHERE login_id='...'`) → 자격증 생성 → `docs/sample/sample-questions.csv` 업로드 → 사용자 화면에서 문제가 보임. 정답번호를 5로 바꾼 파일은 오류 목록이 뜸. USER 계정에는 버튼이 없음. 토큰을 지우고 새로고침하면 로그인 화면으로 감 (Review Focus 5)
- [ ] **Step 3:** Commit `feat: add admin screens for certificates and CSV upload`

## 우선순위 2: 시험 모드

### Task 13: 시험 응시·제출·채점 API

**Files:**
- Create: `exam/ScoreCalculator.java`, `attempt/AttemptController.java`, `attempt/AttemptService.java`, 응답 record
- Test: `exam/ScoreCalculatorTest.java` (단위), `attempt/AttemptControllerTest.java`

**Interfaces:**
- `ExamScore ScoreCalculator.score(List<GradedQuestion> graded, int failScore, int passAverage)`
  - `record GradedQuestion(int subjectNumber, String subjectName, boolean correct)`
  - `record ExamScore(List<SubjectScore> subjects, double averageScore, boolean passed)`
  - `record SubjectScore(int number, String name, int score, boolean failed)` (과목 번호순)
- `POST /api/exams/{examId}/attempts` → `{attemptId, startedAt, timeLimitMinutes, questions: [QuestionView]}`
- `POST /api/attempts/{id}/submit` `{answers: [{questionId, selectedNo}]}` → `{subjects: [SubjectScore], averageScore, passed, questions: [{questionId, selectedNo, answerNo, correct, explanation}]}`. 답하지 않은 문제는 오답으로 처리하고 `selectedNo = null`. 풀이 기록은 답한 문제만 `mode=EXAM`으로 저장한다.
- 오류: 두 번째 제출은 409 `ALREADY_SUBMITTED`, 남의 응시는 403, 그 회차에 없는 `questionId`는 400. 시간 초과 제출도 채점은 한다 (자동 제출이 늦게 도착할 수 있음).

- [ ] **Step 1:** `ScoreCalculatorTest` 작성:
  - `subjectScore_isFloorOfPercentage` (20문제 중 15 → 75)
  - `exactly40_isNotFailed` (8/20 → 40, `failed=false`)
  - `below40_isFailed` (7/20 → 35, `failed=true`)
  - `failedSubject_meansNotPassed_evenIfAverageHigh` (100, 100, 35 → 평균 78.3, `passed=false`)
  - `average60_noFail_passes` (60, 60 → `passed=true`)
  - `average_roundedToOneDecimal` (75, 35, 40 → 50.0)
- [ ] **Step 2:** FAIL 확인 → 구현 → PASS 확인
- [ ] **Step 3:** `AttemptControllerTest` 작성 (Review Focus 3):
  - `submit_returnsScoresAndSavesExamRecords`
  - `submit_twice_returns409AndNoDuplicateRecords`
  - `submit_othersAttempt_returns403`
  - `submit_questionFromOtherExam_returns400`
  - `unanswered_countsAsWrong`
- [ ] **Step 4:** FAIL 확인 → 구현 (`@Transactional`) → PASS 확인
- [ ] **Step 5:** Commit `feat: add exam mode attempts with subject scoring`

### Task 14: 화면: 시험 모드와 결과

**Files:**
- Create: `client/app/exams/[examId]/exam.tsx`, `client/app/attempts/[attemptId].tsx`

**Interfaces:**
- Consumes: Task 13 API, `QuestionCard` (Task 11)
- 타이머는 `startedAt + timeLimitMinutes`까지 남은 시간을 1초마다 다시 계산한다 (탭이 백그라운드에서 멈춰도 맞도록). 0이 되면 자동 제출한다. 문제 번호 목록으로 이동할 수 있다. 제출 전에 "안 푼 문제 N개" 확인을 띄운다.
- 결과 화면: 과목별 점수와 과락 표시, 평균, 합격/불합격, 문제별 정답과 해설

- [ ] **Step 1:** 구현
- [ ] **Step 2:** 수동 확인: 제한시간을 1분으로 바꾼 샘플 자격증에서 자동 제출됨. 과락 과목은 빨간 표시. 새로고침하면 응시가 새로 시작됨 (이어 풀기는 범위 밖)
- [ ] **Step 3:** Commit `feat: add exam mode screen with timer and result`

## 우선순위 3: 오답노트, 별표

### Task 15: 별표·오답노트 API

**Files:**
- Create: `solve/BookmarkController.java`, `solve/NoteController.java`, `solve/NoteService.java`
- Test: `solve/NoteControllerTest.java`

**Interfaces:**
- `PUT /api/questions/{id}/bookmark` → 204 (이미 있으면 그대로), `DELETE /api/questions/{id}/bookmark` → 204
- `GET /api/me/bookmarks`, `GET /api/me/wrong-notes` → `[{certificateName, examYear, examRound, question: QuestionView}]`
- 오답노트: 그 회원의 **가장 최근** 풀이 기록이 오답인 문제 (두 모드 모두 포함). 다시 맞히면 자연히 빠진다.

- [ ] **Step 1:** 테스트 작성: `wrongThenCorrect_removedFromNotes`, `wrongInExamMode_appearsInNotes`, `bookmark_isIdempotent`, `notes_onlyMine`
- [ ] **Step 2:** FAIL 확인 → 구현 → PASS 확인
- [ ] **Step 3:** Commit `feat: add bookmark and wrong-answer note API`

### Task 16: 화면: 별표·오답노트

**Files:**
- Create: `client/app/notes/wrong.tsx`, `client/app/notes/bookmarks.tsx`
- Modify: `client/components/QuestionCard.tsx` (별표 버튼 연결), `client/app/(tabs)/my.tsx` (두 목록으로 가는 링크)

- [ ] **Step 1:** 구현. 목록에서 바로 공부 모드처럼 다시 풀 수 있다 (`solve` API 재사용).
- [ ] **Step 2:** 수동 확인: 틀린 문제가 오답노트에 보임 → 다시 맞히면 사라짐, 별표를 달고 떼면 즉시 반영
- [ ] **Step 3:** Commit `feat: add wrong-answer note and bookmark screens`

## 우선순위 4: 배포와 문서

### Task 17: Flyway 전환과 서버 배포 (Render + Neon)

**Files:**
- Modify: `build.gradle` (`spring-boot-starter-flyway`, `flyway-database-postgresql`), `application.yml` (`spring.sql.init` 제거)
- Move: `schema.sql` → `db/migration/V1__init.sql`, `data.sql` → `db/migration/V2__sample_data.sql`
- Create: `server/Dockerfile` (멀티 스테이지: Gradle 빌드 → `eclipse-temurin:25-jre`), `server/.dockerignore`

- [ ] **Step 1:** 전환 후 `docker compose down -v && docker compose up -d && ./gradlew test bootRun`. 기대: Flyway가 V1, V2를 적용하고 테스트가 PASS.
- [ ] **Step 2:** `docker build -t todaycert-server server`가 성공한다.
- [ ] **Step 3 (사용자):** Neon 프로젝트와 Render 계정을 만든다.
- [ ] **Step 4:** Render Web Service(Docker)에 환경변수 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `CORS_ALLOWED_ORIGINS`를 설정하고 배포한다. `curl -X POST <render-url>/api/auth/signup ...`가 201을 준다.
- [ ] **Step 5:** Commit `chore: switch to Flyway and add server Dockerfile`

### Task 18: 웹 배포 (Vercel)

**Files:**
- Create: `client/vercel.json` (SPA 경로를 `index.html`로 되돌리는 rewrites)

- [ ] **Step 1:** `EXPO_PUBLIC_API_URL=<render-url> npx expo export -p web` 실행 후 `dist/` 생성 확인
- [ ] **Step 2 (사용자):** Vercel 프로젝트를 만든다 (Root `client`, Build `npx expo export -p web`, Output `dist`, 환경변수 `EXPO_PUBLIC_API_URL`).
- [ ] **Step 3:** Render의 `CORS_ALLOWED_ORIGINS`에 Vercel 주소를 추가한다. 배포된 사이트에서 가입 → 로그인 → 샘플 문제 풀기를 확인한다. `/certificates/1`로 직접 접속하고 새로고침해도 404가 아님.
- [ ] **Step 4:** 데모 계정을 가입해 둔다 (아이디·비밀번호는 README에 공개, 일반 회원).
- [ ] **Step 5:** Commit `chore: add Vercel config for web deploy`

### Task 19: README 완성과 운영자 데모 영상

**Files:**
- Modify: `README.md`, `docs/DONE.md` (체크 표시)
- Create: `docs/demo/` (운영자 화면 영상 또는 GIF, 사용자가 녹화)

- [ ] **Step 1:** README의 로드맵 항목 중 **실제로 구현된 것만** 기능 섹션으로 옮긴다. 배포 링크, 데모 계정, 로컬 실행법, 구조, 기술 선택 이유, 샘플 데이터 표시를 적는다.
- [ ] **Step 2 (사용자):** 운영자 화면 시연을 녹화한다 (자격증 생성 → CSV 업로드 → 오류 파일 업로드).
- [ ] **Step 3:** README에 있는 실행법을 처음부터 그대로 따라 해서 로컬 실행을 확인한다.
- [ ] **Step 4:** Commit `docs: complete README with demo and architecture`

## 우선순위 5: 이 앱만의 특징 (시간이 남으면, 이 순서로)

### Task 20: 오늘의 문제

**Files:**
- Create: `study/DailyProblemService.java`, `study/StudyController.java`
- Modify: `client/app/(tabs)/index.tsx` (홈: "오늘의 문제 시작")
- Test: `study/DailyProblemServiceTest.java`

**Interfaces:**
- `GET /api/me/daily-problems` → `[QuestionView]` (최대 20)
- 대상 자격증: D-day 목표의 자격증. 없으면 가장 최근에 푼 문제의 자격증. 그것도 없으면 빈 목록.
- 규칙: 오늘 풀지 않은 오답노트 문제 최대 8개 → 과락 위험 과목(Task 22 기준)의 안 푼 문제 최대 6개 → 안 푼 문제로 채우기 → 부족하면 이미 푼 문제로 채우기. 각 묶음 안에서는 `(memberId, 오늘 날짜)`를 시드로 섞어서 하루 동안 같은 목록이 나온다. Task 22 전에는 과락 위험 묶음을 건너뛴다.

- [ ] **Step 1:** 테스트 작성: `sameDay_returnsSameSet`, `wrongNotesComeFirst_max8`, `neverMoreThan20`, `noTarget_noHistory_returnsEmpty`
- [ ] **Step 2:** FAIL 확인 → 구현 → PASS 확인 → 홈 화면 연결 (공부 모드 화면을 문제 목록 입력으로 재사용)
- [ ] **Step 3:** Commit `feat: add daily problem set`

### Task 21: D-day

**Files:**
- Modify: `member/MeController.java`, `client/app/certificates/[id].tsx` (일정 옆 "목표로 설정"), `client/app/(tabs)/index.tsx`

**Interfaces:**
- `PUT /api/me/target` `{scheduleId}` (`null`이면 해제) → 204
- `GET /api/me` 응답에 `target: {certificateId, certificateName, year, round, examDate, dDay} | null` 추가. `dDay` = 오늘부터 `examDate`까지 일수 (Asia/Seoul 기준, 시험일 당일 0).

- [ ] **Step 1:** 테스트 `dDay_isZeroOnExamDay`, `target_canBeCleared` → 구현 → 홈에 "정보처리기사 필기 D-23" 표시
- [ ] **Step 2:** Commit `feat: add exam D-day target`

### Task 22: 과락 위험 리포트

**Files:**
- Create: `study/SubjectReportService.java`
- Modify: `study/StudyController.java`, `client/app/(tabs)/my.tsx`, `client/app/(tabs)/index.tsx` (요약)

**Interfaces:**
- `GET /api/me/subject-report` → `[{subjectNumber, subjectName, solvedCount, correctRate, atRisk}]` (대상 자격증은 Task 20과 같은 규칙)
- `correctRate` = 과목별로 문제마다 가장 최근 풀이 기준 정답 비율 × 100 (정수). `atRisk` = `solvedCount >= 5` AND `correctRate < failScore`. 5개 미만이면 판단 보류.

- [ ] **Step 1:** 테스트 `atRisk_whenBelowFailScore`, `fewerThan5Solved_notAtRisk`, `usesLatestAttemptPerQuestion` → 구현 → 화면
- [ ] **Step 2:** Commit `feat: add subject fail-risk report`

### Task 23: 문제별 정답률

**Files:**
- Modify: `solve/SolveService.java`, `attempt/AttemptService.java`, `client/components/QuestionCard.tsx`

**Interfaces:**
- 공부 모드 `solve` 응답과 시험 결과의 문제별 항목에 `stats: {solvedCount, correctRate, mostChosenWrongNo | null}` 추가. 집계 대상은 전체 회원의 모든 풀이 기록. `solvedCount < 5`이면 `stats = null` (표본이 적음).

- [ ] **Step 1:** 테스트 `stats_nullBelow5`, `mostChosenWrong_ignoresCorrectChoice` → 구현 → 화면에 "정답률 42%, 많이 고른 오답 3번"
- [ ] **Step 2:** Commit `feat: add per-question accuracy stats`

---

## 기존 버전에서 발견된 문제 (재작성으로 해결, 이슈로 기록만)

| 문제 | 해결하는 곳 |
|---|---|
| DB 비밀번호가 `index.js`와 `.env`로 공개 레포에 커밋됨 | 해결: Express 삭제, `.env` 무시 (DB 폐기) |
| `server/node_modules` 커밋 | 해결 |
| 로그인을 클라이언트에서 비교, `/api/userinfo`가 비밀번호 반환 | Task 3 |
| `/api/alluserinfo`가 전체 회원 비밀번호 반환 | Task 3 (해당 API 없음) |
| 비밀번호 평문 저장 | Task 3 (BCrypt) |
| 데이터 변경을 GET으로 처리 | Task 3~8 (POST/PUT) |
| 서버 IP 하드코딩, 화면마다 다름 | Task 9 (`EXPO_PUBLIC_API_URL`) |
| `App.js`, `Signup.jsx`의 `Platform` import 누락 | Task 9 (클라이언트 재작성) |
| `Answer` 테이블이 문제와 연결되지 않아 모든 해설이 모든 문제 아래 표시 | 해결: 해설을 `question`에 합침 |
| client `package.json`에 서버 패키지, npm·yarn lock 중복 | Task 9 |
| Expo SDK 46으로는 최신 Expo Go에서 열리지 않음 | Task 9 |
