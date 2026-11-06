# API 정리

서버(Spring Boot)에 **실제로 구현되어 있고 테스트가 있는** API만 적는다. (2026-11-03 기준, 테스트 96개 통과)

## 주소

- 로컬: `http://localhost:8080`
- 배포: `https://todaycertificate.onrender.com` (웹 화면은 같은 API를 `https://todaycertificate-client.vercel.app/api/proxy`로 거쳐 호출)

## 공통 규칙

- **인증:** `/api/auth/**`만 로그인 없이 호출할 수 있다. 나머지는 `Authorization: Bearer <accessToken>` 헤더가 필요하다.
  `/api/admin/**`는 ADMIN 권한이 필요하다.
  - 토큰이 없거나 잘못되면 401 `UNAUTHORIZED`.
  - 권한이 없으면 403 `FORBIDDEN`.
- **오류 응답:** 모든 오류는 같은 모양이다.

  ```json
  { "code": "VALIDATION_FAILED", "message": "입력값이 올바르지 않습니다.", "details": [{ "field": "selectedNo", "message": "..." }] }
  ```

  | code | HTTP | 언제 |
  |---|---|---|
  | `VALIDATION_FAILED` | 400 | 입력값 검증 실패. JSON 문법 오류, 파라미터나 파일 누락, 타입 불일치 포함 |
  | `CSV_INVALID` | 400 | 업로드한 CSV에 오류가 있음. `details`는 `[{row, message}]` |
  | `UNAUTHORIZED` | 401 | 토큰 없음 또는 잘못됨 |
  | `INVALID_CREDENTIALS` | 401 | 아이디나 비밀번호가 틀림 (어느 쪽인지 알려주지 않음) |
  | `FORBIDDEN` | 403 | 권한 없음, 또는 남의 응시 기록에 접근 |
  | `NOT_FOUND` | 404 | 대상 없음 |
  | `DUPLICATE_LOGIN_ID` / `DUPLICATE_NAME` | 409 | 아이디 또는 자격증 이름 중복 |
  | `SUBJECT_IN_USE` | 409 | 문제가 있는 과목을 삭제하려 함 |
  | `ALREADY_SUBMITTED` | 409 | 이미 제출한 시험을 다시 제출 |
  | `INTERNAL_ERROR` | 500 | 예상하지 못한 오류 (내부 메시지는 로그에만 남김) |

- **공통 모양 `QuestionView`:** 풀기 전에 보여주는 문제다. 정답과 해설은 넣지 않는다.
  `type`은 `MULTIPLE_CHOICE` 또는 `SHORT_ANSWER`(주관식)이고, 주관식은 `choices`가 빈 목록이다.

  ```json
  { "id": 1, "number": 1, "type": "MULTIPLE_CHOICE", "subjectNumber": 1, "subjectName": "소프트웨어 설계",
    "content": "문제 본문 (코드는 ``` 로 감쌈)", "choices": ["보기1", "보기2", "보기3", "보기4"], "bookmarked": false }
  ```

---

## 1. 회원 · 인증 (#3, #21)

| 메서드 · 경로 | 기능 | 요청 | 응답 | 주요 오류 |
|---|---|---|---|---|
| `POST /api/auth/signup` | 회원가입 | `{loginId, name, password}` (loginId는 영문·숫자 4~20자, 비밀번호는 8~64자) | 201 `{id}` | 400, 409 `DUPLICATE_LOGIN_ID` |
| `POST /api/auth/login` | 로그인 | `{loginId, password}` | `{accessToken, member: {id, loginId, name, role}}` | 401 `INVALID_CREDENTIALS` |
| `GET /api/me` | 내 정보와 D-day | 없음 | `{id, loginId, name, role, target}`. `target`은 `{certificateId, certificateName, year, round, examDate, dDay}`이고, 목표가 없으면 `null` | 401 |
| `PUT /api/me/target` | D-day 목표 시험 설정 | `{scheduleId}` (`null`이면 해제) | 204 | 404 (없는 일정) |

- **관련:**
  - 테이블: `member` (`target_schedule_id` → `exam_schedule`).
  - 화면: #9 로그인·회원가입, 홈(#21, "목표로 설정" 버튼과 D-day 표시).
- **설계 포인트:**
  - 비밀번호는 BCrypt로 저장한다.
  - JWT는 HS256이고 만료는 24시간이다. 토큰의 `sub`는 회원 ID, `role` 클레임은 권한이다.
  - `dDay`는 한국 날짜 기준이고, 시험 당일이 0이며 지나면 음수다.
  - 로그인 응답 모양을 바꾸지 않으려고 `/api/me` 전용 응답을 따로 둔다.
- **개선·확장 후보:**
  - 리프레시 토큰이나 로그아웃(토큰 무효화)이 없다. 지금은 만료될 때까지 유효하다.
  - 로그인 시도 횟수 제한(무차별 대입 방어)이 없다.
  - 회원 탈퇴, 비밀번호 변경 API가 없다.

## 2. 운영자: 자격증 · 일정 · 기출 업로드 (#4, #6)

모두 ADMIN 전용이다. 일반 회원이 호출하면 403.

| 메서드 · 경로 | 기능 | 요청 | 응답 | 주요 오류 |
|---|---|---|---|---|
| `POST /api/admin/certificates` | 자격증 생성 | `{name, description, timeLimitMinutes, failScore, passAverage, subjects: [{number, name}]}` | 201 `{id}` | 400, 409 `DUPLICATE_NAME` |
| `PUT /api/admin/certificates/{id}` | 자격증 수정 (과목은 번호로 맞춰서 추가·이름 변경·삭제) | 생성과 같음 | 204 | 404, 409 `DUPLICATE_NAME` · `SUBJECT_IN_USE` |
| `POST /api/admin/certificates/{id}/schedules` | 시험 일정 추가 | `{year, round, registrationStart, registrationEnd, examDate}` (접수 시작 ≤ 접수 마감 ≤ 시험일) | 201 `{id}` | 400, 404 |
| `DELETE /api/admin/schedules/{id}` | 시험 일정 삭제 (이 일정을 목표로 둔 회원의 목표는 해제) | 없음 | 204 | 404 |
| `POST /api/admin/certificates/{id}/exams` | 회차 기출 CSV 업로드 | multipart: `year`, `round`, `file` (CSV 헤더: `과목번호,문제번호,문제,보기1,보기2,보기3,보기4,정답번호,해설`) | `{examId, created, updated, deleted}` | 400 `CSV_INVALID` (`details: [{row, message}]`), 404 |
| `POST /api/admin/certificates/{id}/exams/pdf-preview` | 기출 PDF 미리보기 (#41). **저장하지 않는다** | multipart: `file` (문제 PDF, 필수), `explanationFile` (해설 PDF, 선택) | `{rows: [{subjectNumber, number, type, content, choices, answerNo, answerText, explanation, warnings}]}` | 400 `PDF_INVALID` (`details: [이유]`: 열 수 없음 / 암호 / 글자 없는 스캔본), 404 |

- **관련:**
  - 테이블: `certificate`, `subject`, `exam_schedule`, `exam`, `question`. 업로드는 `solve_record`, `bookmark`도 건드린다.
  - 코드: `CsvQuestionParser`(#5), `PdfTextExtractor`·`PdfQuestionParser`(#40).
  - 화면: #12 운영자 화면(데모 영상으로 공개).
- **설계 포인트:**
  - 업로드 전체가 트랜잭션 하나다. CSV에 오류가 한 줄이라도 있으면 아무것도 저장하지 않는다.
  - 다시 올리면 **문제번호 기준으로 덮어써서** 문제 ID가 유지된다. 그래서 사용자의 풀이 기록과 별표가 남는다.
  - 새 파일에서 빠진 번호는 그 문제의 풀이 기록, 별표, 문제 순서로 지운다(외래키 때문).
  - 업로드 한도는 파일 하나 20MB, 요청 전체 30MB(`spring.servlet.multipart`). Spring 기본값(1MB)은 그림이 있는 PDF에 작다. 넘으면 400 `VALIDATION_FAILED` "파일이 너무 큽니다 (최대 20MB)."
  - PDF 형식 두 가지: 필기(`1.` 문제 + ①~④ 보기, 2단 편집은 단별로 읽음)와 실기(`문제 N` → `정답 및 해설` → `[해설]`, `type: SHORT_ANSWER`, 정답은 `answerText`). 실기는 코드처럼 보이는 줄이 2줄 이상 이어지면 줄바꿈·들여쓰기를 살려 ``` 로 감싸고, 브라우저 인쇄 머리글·바닥글(날짜, 문서 제목, 주소, 쪽수)을 뺀다. 과목 줄이 없어서 과목번호는 1이다.
  - PDF 미리보기는 DB에 쓰지 않는다. 형식이 틀린 문제도 버리지 않고 `warnings`에 이유를 담아 행으로 돌려준다. 그 자격증에 없는 과목번호는 `"등록되지 않은 과목번호입니다: N"` 경고가 붙는다.
- **개선·확장 후보:**
  - 자격증 삭제, 회차 삭제, 운영자용 회차 목록 API가 없다.
  - 업로드 이력(누가, 언제, 몇 문제)을 남기지 않는다.

## 3. 조회: 자격증 · 문제 (#7)

| 메서드 · 경로 | 기능 | 요청 | 응답 | 주요 오류 |
|---|---|---|---|---|
| `GET /api/certificates` | 자격증 목록 (이름순) | 없음 | `[{id, name, description}]` | 401 |
| `GET /api/certificates/{id}` | 자격증 상세 | 없음 | `{id, name, description, timeLimitMinutes, failScore, passAverage, subjects: [{number, name}], schedules: [{id, year, round, registrationStart, registrationEnd, examDate}], exams: [{id, year, round, questionCount}]}` | 404 |
| `GET /api/exams/{examId}/questions` | 회차 문제 목록 (번호순) | 없음 | `[QuestionView]` | 404 |

- **관련:**
  - 화면: #10 자격증 목록·상세, 회차·모드 선택. #11 공부 모드.
- **설계 포인트:**
  - `schedules`는 **올해(한국 기준)** 일정만 준다.
  - `exams`는 최신 회차부터이고, 회차별 문제 수를 쿼리 한 번(GROUP BY)으로 가져온다.
  - 별표 여부는 내 별표 ID를 한 번에 조회해서 붙인다. 문제마다 따로 조회하는 N+1이 없다.
- **개선·확장 후보:**
  - 목록 페이지네이션이 없다. 자격증 수가 적어서 지금은 필요 없다.
  - 자격증 검색이나 분류(분야별)가 없다.
  - 자주 바뀌지 않는 데이터라 캐시를 둘 수 있다.

## 4. 풀이: 공부 모드 · 시험 모드 (#8, #13, #23)

| 메서드 · 경로 | 기능 | 요청 | 응답 | 주요 오류 |
|---|---|---|---|---|
| `POST /api/questions/{id}/solve` | 공부 모드: 한 문제 풀고 바로 채점 | 객관식 `{selectedNo}` (1~4), 주관식 `{answerText}` (최대 500자) | `{correct, answerNo, correctAnswerText, explanation, stats}` | 400, 404 |
| `POST /api/exams/{examId}/attempts` | 시험 시작 | 없음 | 201 `{attemptId, startedAt, timeLimitMinutes, questions: [QuestionView]}` | 404 |
| `POST /api/attempts/{id}/submit` | 시험 제출과 채점 | `{answers: [{questionId, selectedNo 또는 answerText}]}` (안 푼 문제는 빼고 보냄) | 아래 `AttemptResult` | 400, 403, 404, 409 `ALREADY_SUBMITTED` |
| `GET /api/attempts/{id}` | 제출한 시험 결과 다시 보기 | 없음 | `AttemptResult` (제출 응답과 같은 모양) | 403, 404 (없거나 아직 제출 안 함) |
| `GET /api/me/attempts?certificateId={id}` | 내 시험 점수 이력 (#32) | `certificateId` 필수 | `[{attemptId, examYear, examRound, submittedAt, averageScore, passed}]` (제출 오래된 순) | 404 (자격증 없음) |

```json
// AttemptResult
{ "attemptId": 3, "averageScore": 78.3, "passed": false, "failScore": 40,
  "subjects": [{ "number": 1, "name": "소프트웨어 설계", "score": 100, "failed": false }],
  "questions": [{ "questionId": 10, "number": 1, "type": "MULTIPLE_CHOICE", "selectedNo": null, "answerText": null,
                  "answerNo": 2, "correctAnswerText": null, "correct": false, "explanation": "...", "stats": null },
                { "questionId": 11, "number": 2, "type": "SHORT_ANSWER", "selectedNo": null, "answerText": "ddl",
                  "answerNo": null, "correctAnswerText": "DDL|데이터 정의어", "correct": true, "explanation": "...",
                  "stats": null }] }

// stats (#23): 전체 회원 기준. 5번 미만으로 풀린 문제는 null. 주관식은 mostChosenWrongNo가 항상 null
{ "solvedCount": 42, "correctRate": 57, "mostChosenWrongNo": 3 }
```

- **주관식 답 (#27):**
  - 내 답은 `answerText`, 정답은 `correctAnswerText`(`|`로 구분한 허용 답 원문)에 담는다. 객관식은 `selectedNo` / `answerNo`.
  - 문제 유형에 맞는 쪽 하나만 보내야 한다. 객관식에 `answerText`, 주관식에 `selectedNo`, 또는 둘 다 없으면 400 `VALIDATION_FAILED`(`field`는 `selectedNo` 또는 `answerText`)이고 기록을 남기지 않는다. 시험 제출이면 응시도 열린 채로 둔다.
  - `correctRate`는 저장된 정답 여부가 아니라, 같은 답끼리 묶은 뒤 현재 정답으로 다시 채점해서 낸다.

- **점수 이력 (#32):**
  - 내가 제출한 시험만 나온다. 시작만 하고 제출하지 않은 응시와 다른 회원의 응시는 빠진다.
  - 점수는 제출할 때 `exam_attempt`에 저장한 값이다. 다시 채점하지 않는다.
  - 회차(`examYear`, `examRound`)는 exam을 fetch join으로 함께 읽어서 쿼리 한 번으로 가져온다.

- **관련:**
  - 테이블: `solve_record`(모드 STUDY / EXAM), `exam_attempt`.
  - 화면: #11 공부 모드, #14 시험 모드(타이머, 자동 제출) + 결과.
- **채점 규칙 (`ScoreCalculator`):**
  - 과목 점수 = 맞힌 수 × 100 / 문항 수 (소수점 버림).
  - 과목 점수가 `failScore`(기본 40) 미만이면 과락.
  - 평균(소수 첫째 자리 반올림)이 `passAverage`(기본 60) 이상이고 과락이 없으면 합격.
  - 안 푼 문제는 오답으로 친다.
- **설계 포인트:**
  - 중복 제출은 `UPDATE … WHERE submitted_at IS NULL`의 결과 행 수로 막는다. 0행이면 409를 준다.
  - 두 스레드로 동시에 제출하는 테스트로 확인했다.
  - 잘못된 요청이 응시를 "제출됨"으로 만들지 않도록 검증을 잠금보다 먼저 한다.
  - 결과 다시 보기는 저장된 풀이 기록으로 다시 채점한다.
- **개선·확장 후보:**
  - 제한시간을 서버가 강제하지 않는다. 클라이언트 타이머의 자동 제출에 맡긴다. 필요하면 제출할 때 `startedAt + 제한시간`과 비교한다.
  - 제출하지 않고 떠난 응시가 계속 남는다. 정리 작업이나 "이어서 풀기"가 없다.
  - 운영자가 정답을 고쳐서 다시 올리면, 결과 다시 보기의 점수가 제출 당시 점수와 달라질 수 있다. `exam_attempt`에 저장된 점수와 다시 계산한 점수가 다를 수 있다는 뜻이다. 점수 이력은 저장된 점수를 쓰므로, 이때 이력과 결과 화면의 점수가 다르게 보일 수 있다.
  - `stats`는 요청마다 집계한다. 기록이 아주 많아지면 집계 테이블이나 캐시가 필요하다.

## 5. 별표 · 오답노트 (#15)

| 메서드 · 경로 | 기능 | 요청 | 응답 | 주요 오류 |
|---|---|---|---|---|
| `PUT /api/questions/{id}/bookmark` | 별표 켜기 (이미 있으면 그대로) | 없음 | 204 | 404 (없는 문제) |
| `DELETE /api/questions/{id}/bookmark` | 별표 끄기 (없어도 204) | 없음 | 204 | |
| `GET /api/me/bookmarks` | 내 별표 목록 (최근에 별표한 것부터) | 없음 | `[{certificateName, examYear, examRound, question: QuestionView}]` | |
| `GET /api/me/wrong-notes` | 내 오답노트 | 없음 | 위와 같은 모양 | |

- **관련:**
  - 테이블: `bookmark`(회원+문제 UNIQUE), `solve_record`.
  - 화면: #16 별표·오답노트.
- **설계 포인트:**
  - PUT과 DELETE는 **멱등**이다. 몇 번을 보내도 결과가 같다(토글이 아니다).
  - 별표 켜기는 `INSERT … ON CONFLICT DO NOTHING`이라 동시에 눌러도 오류가 나지 않는다.
  - 오답노트는 "문제마다 **가장 최근** 풀이가 오답인 것"이다(PostgreSQL `DISTINCT ON`). 공부 모드와 시험 모드를 모두 포함하고, 다시 맞히면 자동으로 빠진다.
- **개선·확장 후보:**
  - 페이지네이션이나 자격증별 필터가 없다. 지금은 내 전체 풀이 기록을 대상으로 계산한다.
  - 오답노트 문제만 모아서 다시 풀기 기능이 없다. 화면에서 공부 모드를 재사용하면 된다.

## 6. 학습 도우미 (#20, #22)

| 메서드 · 경로 | 기능 | 요청 | 응답 | 주요 오류 |
|---|---|---|---|---|
| `GET /api/me/daily-problems` | 오늘의 문제 (최대 20) | 없음 | `[QuestionView]` (대상 자격증이 없으면 빈 목록) | |
| `GET /api/me/subject-report` | 과목별 과락 위험 | 없음 | `[{subjectNumber, subjectName, solvedCount, correctRate, atRisk}]` | |

- **대상 자격증 (`StudyTarget`):** D-day 목표의 자격증을 쓴다. 없으면 가장 최근에 푼 문제의 자격증을 쓴다. 그것도 없으면 대상이 없다.
- **오늘의 문제:** 아래 순서로 채운다.
  1. 오늘 아직 안 푼 오답 최대 8개
  2. 과락 위험 과목의 안 푼 문제 최대 6개
  3. 안 푼 문제
  4. 이미 푼 문제

  묶음 안에서는 `(회원 ID, 오늘 날짜)`를 시드로 섞는다. 기록이 그대로면 하루 동안 같은 목록이 나온다.
- **과락 리포트:** 문제마다 가장 최근 풀이만 센다. `atRisk` = 5문제 이상 풀었고, 정답률이 과락 기준 미만인 경우다.
- **관련:**
  - 화면: 홈(`index.tsx`)의 "오늘의 문제 시작"과 리포트 요약, 마이(`my.tsx`). 화면은 아직 연결하지 않았다.
- **개선·확장 후보:**
  - 오늘의 문제는 문제를 풀면 목록이 바뀔 수 있다. "오늘 푼 오답 제외" 규칙 때문이다. 하루 목록을 고정하려면 "오늘 0시 이전 기록"만 기준으로 삼으면 된다.
  - 자격증의 전체 문제를 메모리에 올려서 고른다. 문제가 수만 개가 되면 SQL에서 후보를 줄여야 한다.
  - 대상 자격증을 사용자가 직접 고르는 파라미터가 없다.

---

## 다음에 하면 좋은 공통 확장

- **API 문서 자동화:** springdoc-openapi로 Swagger UI를 띄우면 이 문서를 코드에서 생성할 수 있다.
- **테스트 범위:** 지금 테스트는 컨트롤러부터 실제 PostgreSQL(Testcontainers)까지 통합 테스트가 중심이다. 응답시간 같은 성능 수치는 아직 측정하지 않았다.
