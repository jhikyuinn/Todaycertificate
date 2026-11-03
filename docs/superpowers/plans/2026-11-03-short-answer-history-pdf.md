# 주관식 · 점수 이력 · PDF 가져오기 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 단답형 문제(실기), 자격증별 시험 점수 이력, 운영자 PDF → 미리보기 → 저장 기능을 추가한다.

**Architecture:** 주관식은 `question.type`으로 구분하고 채점은 순수 함수 `ShortAnswerGrader`에 둔다. 풀이 기록은 `selected_no` 또는 `answer_text` 중 하나를 채운다. 점수 이력은 이미 있는 `exam_attempt` 조회만 추가한다. PDF는 `PdfTextExtractor`(PDFBox, 얇은 층) → `PdfQuestionParser`(순수, 글자 → 행)로 나누고, 저장은 화면이 행을 CSV로 바꿔 기존 업로드 API로 보낸다.

**Tech Stack:** 기존(Spring Boot 4.1, JPA, Flyway, Commons CSV, Testcontainers / Expo 58) + Apache PDFBox 3.x (서버에만 추가)

**Spec:** `docs/superpowers/specs/2026-11-03-short-answer-history-pdf-design.md` (9절 판단 3가지 사용자 승인)

이 문서는 `2026-11-03-todaycertificate.md`의 Task 23 다음을 잇는다 (Task 34~42, GitHub 이슈 번호와 같다. #24~#33은 이미 다른 이슈·PR이 쓰고 있다). 경로는 `server/src/main/java/com/todaycertificate/` 아래 기준.

## Global Constraints

- 기존 계획의 Global Constraints를 모두 따른다 (비밀값, 커밋 접두어, 오류 응답 형식, 권한, 채점 공식).
- **실제 기출(시나공, comcbt) 텍스트를 레포·테스트 리소스·샘플 데이터에 넣지 않는다.** 테스트와 데모는 직접 만든 예시 문제만.
- `V1__init.sql`은 고치지 않는다. 스키마 변경은 `V2__short_answer.sql`.
- 기존 CSV(9칸)는 그대로 동작해야 한다.
- 주관식 기본 채점: 앞뒤 공백 제거, 연속 공백 하나로, 대소문자 무시, `|`로 구분한 허용 답 중 하나와 같으면 정답. `exact_match`: 앞뒤 공백만 제거하고 그대로 비교. 빈 답은 오답. 부분 점수 없음.
- 실기는 별도 자격증 항목("정보처리기사 실기": 과목 1개, `failScore` 0, `passAverage` 60). `ScoreCalculator`는 바꾸지 않는다.
- PDF 미리보기 API는 **아무것도 저장하지 않는다.**
- 클라이언트에 새 의존성을 넣지 않는다 (그래프는 View로 그린다).

## Review Focus

1. **주관식 답에 `|`가 들어간 경우:** 허용 답 구분자이므로 답 자체에 `|`를 쓸 수 없다. CSV 검증에서 빈 허용 답(`A||B`, 끝의 `|`)은 오류. 테스트는 Task 36.
2. **문제 유형과 안 맞는 답:** 객관식에 `answerText`, 주관식에 `selectedNo`, 둘 다/둘 다 없음 → 400, 기록 저장 안 됨. 테스트는 Task 35.
3. **객관식·주관식이 섞인 회차의 정답률:** `mostChosenWrongNo`는 객관식만. 주관식 기록(`selected_no` NULL)이 집계 쿼리를 깨지 않는다. 테스트는 Task 35.
4. **PDF가 아닌 파일·암호 걸린 PDF·글자 없는 스캔 PDF:** 500이 아니라 400 `PDF_INVALID` + 이유. 테스트는 Task 41.
5. **PDF 변환 실패 문제:** 보기 4개가 안 되거나 정답을 못 찾은 문제도 빠지지 않고 `warnings`와 함께 행으로 나온다. 테스트는 Task 40.

---

## A. 주관식 (단답형)

### Task 34: 스키마와 채점 규칙

**Files:**
- Create: `server/src/main/resources/db/migration/V2__short_answer.sql`, `exam/QuestionType.java`, `exam/ShortAnswerGrader.java`
- Modify: `exam/Question.java`, `solve/SolveRecord.java`
- Test: `exam/ShortAnswerGraderTest.java`

**Interfaces:**
- `enum QuestionType { MULTIPLE_CHOICE, SHORT_ANSWER }`
- `ShortAnswerGrader.isCorrect(String accepted, boolean exactMatch, String input) → boolean` (static, 순수)
- `Question`: 필드 `type`, `answerText`, `exactMatch` 추가. `choice1~4`, `answerNo`는 `Integer`/nullable. 주관식 생성자 `Question(Exam, Subject, int number, String content, String answerText, boolean exactMatch, String explanation)`, `updateShortAnswer(...)`. `isCorrect(int selectedNo)`는 유지, `isCorrect(String answerText)` 추가. `getChoices()`는 주관식이면 `List.of()`.
- `SolveRecord`: `selectedNo` → `Integer`, `answerText` 추가. 주관식 생성자 추가. `correct`는 생성 시 `question.isCorrect(...)`로 계산(지금과 같음).
- V2: `question.type VARCHAR(20) NOT NULL DEFAULT 'MULTIPLE_CHOICE'`, `answer_text TEXT`, `exact_match BOOLEAN NOT NULL DEFAULT false`, `choice1~4`·`answer_no` NOT NULL 해제, CHECK:
  `(type='MULTIPLE_CHOICE' AND choice1..4 IS NOT NULL AND answer_no BETWEEN 1 AND 4) OR (type='SHORT_ANSWER' AND answer_text IS NOT NULL)`.
  `solve_record.selected_no` NOT NULL 해제(기존 1~4 CHECK 유지), `answer_text TEXT`, CHECK `num_nonnulls(selected_no, answer_text) = 1`.

- [ ] **Step 1:** 테스트 작성 (값은 그대로):
  - `ignoresCaseAndOuterSpaces`: `isCorrect("DDL", false, "  ddl ")` → true
  - `collapsesInnerSpaces`: `isCorrect("GROUP BY", false, "group   by")` → true
  - `acceptsAnyAlternative`: `isCorrect("DDL|데이터 정의어", false, "데이터 정의어")` → true
  - `exactMatchKeepsCase`: `isCorrect("Hello", true, "hello")` → false, `isCorrect("Hello", true, " Hello ")` → true
  - `exactMatchKeepsInnerSpaces`: `isCorrect("a  b", true, "a b")` → false
  - `blankIsWrong`: `isCorrect("DDL", false, "   ")` → false, `isCorrect("DDL", false, null)` → false
- [ ] **Step 2:** FAIL 확인: `cd server && ./gradlew test --tests '*ShortAnswerGraderTest'`
- [ ] **Step 3:** 구현 (그레이더, V2, 엔티티). 허용 답도 같은 규칙으로 정규화해서 비교한다.
- [ ] **Step 4:** `./gradlew test` 전체 PASS (V2가 Testcontainers에서 적용되고 기존 테스트가 깨지지 않음)
- [ ] **Step 5:** Commit `feat: add short-answer question type and grading rules (#27)`

### Task 35: 풀이·시험 API의 주관식

**Files:**
- Modify: `solve/SolveController.java`, `solve/SolveService.java`, `attempt/AttemptController.java`, `attempt/AttemptService.java`, `exam/QuestionView.java`, `solve/QuestionStatsService.java`, `solve/SolveRecordRepository.java`(필요 시)
- Test: `solve/SolveControllerTest.java`, `attempt/AttemptControllerTest.java`

**Interfaces:**
- `SolveRequest(Integer selectedNo, String answerText)`: 문제 유형에 맞는 쪽 하나만 있어야 한다. 아니면 400 `VALIDATION_FAILED` (`field: "selectedNo"` 또는 `"answerText"`).
- `SolveService.solveInStudy(Long memberId, Long questionId, Integer selectedNo, String answerText)`
- `SolveResult(boolean correct, Integer answerNo, String correctAnswerText, String explanation, Stats stats)`: 주관식은 `answerNo=null`, `correctAnswerText`=허용 답 원문. (이름을 `QuestionResult`와 맞춤: `answerText`=내 답, `correctAnswerText`=정답)
- `AnswerRequest(Long questionId, Integer selectedNo, String answerText)`, `AttemptService.Answer(Long questionId, Integer selectedNo, String answerText)`
- `QuestionResult`에 `String type, String answerText(내 답), String correctAnswerText` 추가, `answerNo`는 `Integer`.
- `QuestionView`에 `String type` 추가 (정답은 여전히 넣지 않음).
- `Stats.mostChosenWrongNo`: 주관식이면 항상 null. `correctRate`는 유형과 관계없이 계산.

- [ ] **Step 1:** 테스트 추가 (기존 테스트 클래스의 데이터 준비 방식을 따른다):
  - `solve_shortAnswer_correctIgnoringCase`: 정답 "DDL" 문제에 `{answerText:"ddl"}` → 200, `correct=true`, `correctAnswerText="DDL"`
  - `solve_shortAnswerWithSelectedNo_returns400AndSavesNothing`
  - `solve_multipleChoiceWithAnswerText_returns400`
  - `submit_mixedExam_gradesBoth`: 객관식 1 + 주관식 1 회차, 둘 다 정답 → 결과 `questions[*].correct` 모두 true, 다시 `GET /api/attempts/{id}`해도 같은 결과(주관식 내 답 `answerText` 포함)
  - `stats_shortAnswer_noMostChosenWrong`: 주관식 기록 5개 이상 → `stats.mostChosenWrongNo == null`, `correctRate` 있음
- [ ] **Step 2:** FAIL 확인 → **Step 3:** 구현 → **Step 4:** `./gradlew test` 전체 PASS
- [ ] **Step 5:** `docs/API.md`의 풀이·시험 절에 `answerText`, `type` 반영 (코드에 있는 것만)
- [ ] **Step 6:** Commit `feat: accept short answers in study and exam APIs (#27)`

### Task 36: CSV `유형` 칸

**Files:**
- Modify: `exam/upload/CsvQuestionParser.java`, `exam/upload/QuestionRow.java`, `exam/upload/ExamUploadService.java`
- Test: `exam/upload/CsvQuestionParserTest.java`

**Interfaces:**
- 헤더는 기존 9칸 또는 9칸 + `유형`(10칸). 다른 헤더는 지금처럼 파일 오류.
- `유형` 값: 빈칸 또는 `객관식` → 객관식(지금 검증 그대로), `주관식` → 주관식, `주관식-정확히` → 주관식 + `exactMatch`. 그 밖의 값은 행 오류 `"'유형'은 객관식, 주관식, 주관식-정확히 중 하나여야 합니다."`
- 주관식 행: 보기1~4는 비어 있어야 하고(아니면 오류 `"주관식 문제는 보기 칸을 비워 주세요."`), `정답번호` 칸 = 허용 답 원문(strip). 비었거나 빈 허용 답이 있으면 오류 `"주관식 정답이 비어 있습니다."`
- `QuestionRow`에 `QuestionType type, String answerText, boolean exactMatch` 추가. 객관식은 `answerText=null`.
- 업로드 덮어쓰기: 같은 문제번호의 유형이 바뀌면 그 문제를 새 유형으로 업데이트한다(엔티티의 update 메서드 사용).

- [ ] **Step 1:** 테스트: `oldNineColumnCsv_stillParses`, `shortAnswerRow_parsesAnswerText`(값 `"DDL|데이터 정의어"`), `shortAnswerExact_setsExactMatch`, `shortAnswerWithChoices_isError`, `emptyAlternative_isError`(`"A||B"`), `unknownType_isError`
- [ ] **Step 2:** FAIL → **Step 3:** 구현 → **Step 4:** `./gradlew test` PASS
- [ ] **Step 5:** 운영자 업로드 화면의 "CSV 형식" 안내 카드에 `유형` 칸 한 줄 추가 (`client/src/app/(app)/admin/upload.tsx`)
- [ ] **Step 6:** Commit `feat: support short-answer rows in exam CSV (#27)`

### Task 37: 화면: 주관식 풀기 + 실기 예시 데이터

**Files:**
- Modify: `client/src/lib/types.ts`, `client/src/components/QuestionCard.tsx`, `client/src/components/StudySession.tsx`, `client/src/app/(app)/exams/[examId]/exam.tsx`, `client/src/app/(app)/attempts/[attemptId].tsx`, `server/src/main/resources/db/migration/R__sample_data.sql`

**Interfaces:**
- `QuestionView.type: 'MULTIPLE_CHOICE' | 'SHORT_ANSWER'`, `SolveResult.answerNo: number | null`, `correctAnswerText: string | null`, `QuestionResult`에 `type`, `answerText`, `correctAnswerText`.
- `QuestionCard`: `type === 'SHORT_ANSWER'`이면 보기 대신 입력칸. 새 props `answerText?: string`, `onChangeText?(v: string)`, `onSubmitAnswer?()` (Enter). 공개 후: "내 답"(맞으면 초록, 틀리면 빨강) + "정답: " 허용 답을 `|` 대신 ` / `로.
- 키보드 단축키(1~4, ←/→)는 입력칸에 포커스가 있으면 작동하지 않는다(`useKeyboard`가 이미 INPUT 무시).
- 시험 모드: 주관식 답은 입력 즉시 상태에 저장, 번호표의 "답함"은 공백 아닌 글자가 있을 때. 제출 시 `{questionId, answerText}`.
- 예시 데이터: 자격증 "정보처리기사 실기"(과목 1개 "실기", failScore 0, passAverage 60), 2025년 1회 직접 만든 단답형 5문제 (대소문자 무시 3, `exact_match` 코드 출력 1, 복수 정답 1).

- [ ] **Step 1:** 구현 후 `cd client && npx tsc --noEmit` 통과
- [ ] **Step 2:** Playwright(390px, 1280px)로 확인: 실기 회차 공부 모드에서 소문자로 답해도 정답, 시험 모드에서 5문제 제출 → 결과 화면에 내 답/정답, 오답노트에 주관식이 보임
- [ ] **Step 3:** Commit `feat: add short-answer input to study and exam screens (#27)`

## B. 시험 점수 이력

### Task 38: 점수 이력 API

**Files:**
- Modify: `attempt/AttemptController.java`, `attempt/AttemptService.java`, `attempt/ExamAttemptRepository.java`
- Test: `attempt/AttemptHistoryTest.java`

**Interfaces:**
- `GET /api/me/attempts?certificateId={id}` (필수) → `List<HistoryItem>`
- `record HistoryItem(Long attemptId, int examYear, int examRound, Instant submittedAt, double averageScore, boolean passed)`
- 제출된 내 시험만, `submittedAt` 오래된 순. 자격증 없으면 404.
- 리포지토리 쿼리는 exam을 fetch join (N+1 없이 한 번).

- [ ] **Step 1:** 테스트: `history_onlySubmittedAndMine_oldestFirst`(내 제출 2개 + 미제출 1개 + 남의 제출 1개 → 2개, 순서), `history_otherCertificateExcluded`, `history_unknownCertificate_returns404`
- [ ] **Step 2:** FAIL → **Step 3:** 구현 → **Step 4:** `./gradlew test` PASS
- [ ] **Step 5:** `docs/API.md`에 추가 → Commit `feat: add exam score history API (#32)`

### Task 39: 화면: 점수 이력

**Files:**
- Create: `client/src/app/(app)/history.tsx`, `client/src/components/ScoreChart.tsx`
- Modify: `client/src/app/(app)/my.tsx`, `client/src/components/AppShell.tsx`(`/history`를 "내 학습" 탭에 매칭), `client/src/lib/types.ts`

**Interfaces:**
- `ScoreChart({items: HistoryItem[], passLine: number})`: View로 그린 막대 그래프. 막대 높이 = 평균 점수, 합격 막대는 mint/불합격은 wrong 색, 60점 위치에 점선 가로줄, 막대 아래 "2025-1" 라벨. 넓은 화면은 높이 220, 좁은 화면 160, 많으면 가로 스크롤.
- `history.tsx`: 자격증 pill(내 목표 시험 자격증이 기본 선택, 없으면 첫 번째) → 결과 2개 이상이면 그래프 + 목록, 1개면 목록만, 0개면 Empty "아직 본 시험이 없어요". 목록 줄을 누르면 `/attempts/{id}`.
- `my.tsx`: "점수 이력" 타일 추가 (아이콘 `trending-up`).

- [ ] **Step 1:** 구현 후 `npx tsc --noEmit`
- [ ] **Step 2:** Playwright로 `demo01`이 시험 2회 본 상태에서 390px/1280px 스크린샷 확인
- [ ] **Step 3:** Commit `feat: add score history screen (#32)`

## C. PDF 가져오기

### Task 40: PDF 글자 → 문제 행 파서

**Files:**
- Modify: `server/build.gradle` (`org.apache.pdfbox:pdfbox` 3.x 최신)
- Create: `exam/pdf/PdfTextExtractor.java`, `exam/pdf/PdfQuestionParser.java`, `exam/pdf/PreviewRow.java`
- Create: `server/src/test/resources/pdf/sample-exam.html`(직접 만든 예시 10문제, 2과목, 보기 옆 ❶~❹ 정답 형식), 그것을 Edge로 인쇄한 `sample-exam.pdf`, 정답표 형식 글자 픽스처 `answer-table.txt`, 해설 형식 `explanations.txt`
- Test: `exam/pdf/PdfQuestionParserTest.java`

**Interfaces:**
- `PdfTextExtractor.extract(byte[] pdf) → String` (PDFBox `Loader.loadPDF`, `PDFTextStripper` + `setSortByPosition(true)`). 실패는 `ApiException(PDF_INVALID, 이유)`: 열 수 없음/암호/글자 없음(공백 제외 50자 미만 → "글자를 읽을 수 없는 PDF입니다 (스캔본은 지원하지 않아요)").
- `PdfQuestionParser.parse(String questionText, String explanationText /* nullable */) → List<PreviewRow>` (순수)
- `record PreviewRow(int subjectNumber, int number, String content, List<String> choices, Integer answerNo, String explanation, List<String> warnings)`
- 규칙: 과목 줄 `^제?(\d)과목` → subjectNumber. 문제 시작 `^(\d{1,3})\.\s` (직전 번호 + 1일 때만 새 문제로 본다: 보기 안의 "1." 오인 방지). 보기 기호 `①②③④`/`❶❷❸❹`로 자르고 `❶~❹`가 있으면 그 번호가 정답. 정답이 없으면 글자 끝의 정답표 `(\d{1,3})\.\s*([①②③④])` 를 모아 채운다. 해설: `^(\d{1,3})\.` 뒤 `<문제 해설>`부터 다음 문제 전까지, `[해설작성자 : ...]` 제거, 해설이 여러 개면 빈 줄로 이어 붙임.
- 경고 문구: `"보기를 4개 찾지 못했어요 (N개)"`, `"정답을 찾지 못했어요"`, `"과목을 찾지 못했어요"`. 줄바꿈으로 끊긴 문장은 공백으로 이어 붙인다.

- [ ] **Step 1:** `sample-exam.html` 작성 → `"/c/Program Files (x86)/Microsoft/Edge/Application/msedge.exe" --headless --print-to-pdf=... sample-exam.html`로 PDF 생성 (커밋한다. 직접 만든 문제라 문제없음)
- [ ] **Step 2:** 테스트:
  - `samplePdf_parsesTenQuestionsWithAnswers`: `extract(sample-exam.pdf)` → `parse` → 10행, 2과목, 모든 `answerNo` 채워짐, 경고 0
  - `answerTable_fillsAnswers`: `answer-table.txt` → 정답표로 `answerNo` 채움
  - `missingChoice_keepsRowWithWarning`: 보기 3개짜리 문제 → 행은 있고 경고 `"보기를 4개 찾지 못했어요 (3개)"`
  - `numberInsideChoice_notSplit`: 보기 안의 `"1. "`이 새 문제로 잘리지 않음
  - `explanations_attachedByNumber_authorRemoved`
  - `notPdf_throwsPdfInvalid`: `extract("hello".getBytes())`
- [ ] **Step 3:** FAIL → **Step 4:** 구현 → **Step 5:** PASS
- [ ] **Step 6:** Commit `feat: add PDF question parser for exam import`

### Task 41: PDF 미리보기 API

**Files:**
- Create: `exam/pdf/AdminPdfPreviewController.java`
- Modify: `common/ErrorCode.java` (`PDF_INVALID(BAD_REQUEST, "PDF 파일을 읽을 수 없습니다.")`)
- Test: `exam/pdf/AdminPdfPreviewControllerTest.java`

**Interfaces:**
- `POST /api/admin/certificates/{id}/exams/pdf-preview` multipart `file`(필수), `explanationFile`(선택) → `{rows: PreviewRow[]}`. 저장 없음. 자격증 없으면 404. 과목번호가 그 자격증에 없으면 해당 행에 경고 `"등록되지 않은 과목번호입니다: N"`.
- 파일 크기 한도는 기존 multipart 설정을 따른다.

- [ ] **Step 1:** 테스트: `preview_samplePdf_returnsRowsAndSavesNothing`(호출 전후 question 수 같음), `preview_notPdf_returns400PdfInvalid`, `preview_asUser_returns403`, `preview_unknownSubject_addsWarning`
- [ ] **Step 2:** FAIL → **Step 3:** 구현 → **Step 4:** `./gradlew test` PASS
- [ ] **Step 5:** `docs/API.md`에 추가 → Commit `feat: add PDF preview API for exam import`

### Task 42: 화면: PDF 미리보기 표 → 저장

**Files:**
- Create: `client/src/lib/csv.ts`, `client/src/components/PreviewTable.tsx`
- Modify: `client/src/app/(app)/admin/upload.tsx`, `client/src/lib/types.ts`

**Interfaces:**
- 업로드 화면 3단계에 `Segment`(이미 있는 컴포넌트)로 "CSV | PDF" 선택. PDF면 문제 PDF + 선택 해설 PDF를 고르고 "미리보기" → `PreviewTable`.
- `PreviewTable({rows, onChange})`: 행마다 번호·과목번호·문제(여러 줄)·보기 4개·정답번호·해설 편집, 경고 있는 행은 위로 모으고 "확인 필요" 배지. 행 삭제 가능.
- `toCsv(rows: PreviewRow[]) → string` (`csv.ts`): 9칸 헤더 + 모든 칸을 큰따옴표로 감싸고 `"`는 `""`로. `answerNo`가 null이면 빈칸(→ 서버 CSV 검증이 행 오류로 잡음).
- "저장"은 `toCsv` 결과를 `new File([csv], 'pdf-import.csv', {type: 'text/csv'})`로 기존 업로드와 같은 FormData로 보낸다. 결과/오류 표시는 기존 CSV 업로드 화면 것을 그대로 쓴다.
- `toCsv`의 따옴표·줄바꿈 처리는 Step 2의 e2e(코드 문제 수정 → 저장 → 화면 확인)로 검증한다. 클라이언트에는 테스트 도구가 없어서 새로 넣지 않는다.

- [ ] **Step 1:** 구현 후 `npx tsc --noEmit`
- [ ] **Step 2:** Playwright(1280px): `sample-exam.pdf` 미리보기 → 한 문제 내용을 줄바꿈과 큰따옴표가 든 글로 수정 → 저장 → "10개 추가" → 사용자 화면에서 수정한 내용이 보임. 보기를 하나 지운 행을 저장하면 "N번째 문제: …" 오류가 보이고 아무것도 저장되지 않음.
- [ ] **Step 3:** Commit `feat: add PDF import preview to admin upload screen`

---

## 이슈와 일정

| Task | 이슈 제목 | 라벨 | 예상 |
|---|---|---|---|
| 34 | 주관식: 스키마와 채점 규칙 | feature, P5, day-6 | 1.5h |
| 35 | 주관식: 풀이·시험 API | feature, P5, day-6 | 2h |
| 36 | 주관식: CSV 유형 칸 | feature, P5, day-6 | 1h |
| 37 | 화면: 주관식 풀기 + 실기 예시 데이터 | feature, P5, day-6 | 2h |
| 38 | 점수 이력 API | feature, P5, day-6 | 1h |
| 39 | 화면: 점수 이력 | feature, P5, day-7 | 1.5h |
| 40 | PDF 글자 → 문제 행 파서 | feature, P5, day-7 | 2h |
| 41 | PDF 미리보기 API | feature, P5, day-7 | 1h |
| 42 | 화면: PDF 미리보기 표 → 저장 | feature, P5, day-7 | 2h |

이후 기존 #17 #18 #19 (배포, README). 배포 전에 `R__sample_data.sql`에 직접 만든 문제만 있는지 확인한다.
