# 📅 오늘의 자격증 (Today Certificate)

> **시험일까지, 오늘 풀 문제를 골라주는 국가자격 필기 기출 학습 앱**

자격증 공부를 하면 "오늘은 뭘 풀어야 하지?"부터 고민하게 됩니다.
문제 은행은 많지만 문제만 쌓여 있을 뿐, 무엇을 풀지는 스스로 정해야 하죠.

**오늘의 자격증**은 시험일(D-day), 과목별 과락 위험, 내가 틀린 문제를 바탕으로
**오늘 풀 문제**를 골라 줍니다. 앱을 열면 바로 오늘의 공부를 시작할 수 있습니다.

> 🚧 **현재 개발 중입니다.** 2022년에 만든 Expo + Express + MySQL 앱을
> **Spring Boot + PostgreSQL** 기반으로 다시 만들고 있습니다.
> 기능은 로컬에서 동작하며, 배포는 아직 하지 않았습니다. (✅ 완료 · 🚧 진행 예정)

---

## 이렇게 공부합니다

```
자격증 선택  →  회차 선택 (예: 2024년 1회)  →  공부 모드 / 시험 모드
                                                  ↓
                                 틀린 문제는 오답노트로, 중요한 문제는 별표로
                                                  ↓
                              다음 날, "오늘의 문제"가 약한 부분을 다시 골라줌
```

## 기능

### 👤 사용자
| 기능 | 설명 | 상태 |
|---|---|---|
| 회원가입·로그인 | BCrypt 비밀번호, JWT 토큰, USER/ADMIN 권한 | ✅ |
| 자격증 둘러보기 | 자격증 설명, 올해 시험 일정, 최근 기출 회차 목록 | ✅ |
| **공부 모드** | 한 문제씩 풀고 바로 정답과 해설 확인 | ✅ |
| **시험 모드** | 실제 시험과 같은 제한시간, 과목별 점수, **과락·합격 여부** 채점 | ✅ |
| 주관식(단답형) | 답을 직접 입력. 앞뒤·연속 공백과 대소문자 차이는 같은 답으로 보고, 복수 정답 허용 (코드 문제처럼 정확히 맞춰야 하는 문제는 예외) | ✅ |
| 시험 점수 이력 | 지난 시험 점수를 그래프와 목록으로 보기 (과락선 표시) | ✅ |
| 오답노트 | 틀린 문제가 자동으로 모이고, 다시 맞히면 빠짐 | ✅ |
| 별표 문제 | 다시 보고 싶은 문제 모아보기 | ✅ |
| 코드 문제 표시 | 프로그래밍 문제의 코드를 줄바꿈·들여쓰기 그대로 표시 | ✅ |
| ⭐ **오늘의 문제** | 오답 + 과락 위험 과목 + 안 푼 문제를 섞어 매일 약 20문제 추천 | ✅ |
| ⭐ **D-day** | 응시할 시험을 정하면 홈에 "정보처리기사 필기 D-23" | ✅ |
| ⭐ **과락 위험 리포트** | 과목별 정답률로 과락 위험 과목 알림 | ✅ |
| ⭐ 문제별 정답률 | "이 문제 정답률 42%, 많이 고른 오답은 3번" | ✅ |

### 🛠 운영자
| 기능 | 설명 | 상태 |
|---|---|---|
| 자격증 관리 | 설명, 과목, 시험 일정, 제한시간·과락·합격 기준 설정 | ✅ |
| 기출 CSV 업로드 | 회차별 문제를 CSV로 한 번에 등록. 오류가 있으면 줄별로 알려주고 전체를 저장하지 않음 | ✅ |
| 기출 PDF 가져오기 | 필기(객관식)·실기(주관식) PDF를 읽어 미리보기 → 수정 → 저장. 2단 편집 PDF, 코드 들여쓰기 유지 | ✅ |
| 배포 + 데모 계정 | 서버(Render), DB(Neon), 웹(Vercel) | 🚧 |

## 기술 스택

| 영역 | 기술 | 선택 이유 |
|---|---|---|
| 서버 | Java 25, Spring Boot 4.1, Spring Data JPA | 국내 백엔드에서 가장 많이 쓰는 조합으로 다시 작성 |
| DB | PostgreSQL 17 (로컬은 Docker Compose) | 배포할 때 무료로 쓸 수 있는 호스팅이 많음 |
| 화면 | Expo (React Native), TypeScript | 하나의 코드로 웹과 앱을 모두 빌드할 수 있음. 웹으로 먼저 배포 |
| 인증 | Spring Security, JWT | 서버가 세션을 들고 있지 않아 웹·앱 어디서든 같은 방식으로 인증 |
| DB 마이그레이션 | Flyway | 스키마 변경을 SQL 파일로 기록해 로컬·테스트·배포 DB를 똑같이 맞춤 |
| 테스트 | JUnit, Testcontainers | H2 대신 실제 PostgreSQL로 테스트해 운영 DB와 차이로 생기는 버그를 막음 |
| CI | GitHub Actions | PR마다 서버 테스트와 화면 타입 검사를 자동 실행 |

## 지금까지 된 것

- ✅ Express + MySQL 서버 제거 (DB 비밀번호와 `node_modules`가 커밋되어 있던 문제 정리)
- ✅ Spring Boot + JPA + PostgreSQL 서버, Flyway로 스키마 관리
- ✅ 기존 설계의 버그 수정: 해설 테이블이 문제와 연결되지 않아 모든 해설이 모든 문제 아래 표시되던 문제
- ✅ Expo 화면 새로 작성 (위 기능 목록의 ✅ 전부)
- ✅ 채점·과락·CSV/PDF 검증 테스트, GitHub Actions CI
- 🚧 배포, 운영자 화면 데모 영상

## 로컬 실행

**필요한 것:** Java 25, Docker, Node.js 24

### 1. 서버

```bash
cd server
cp .env.example .env        # DB_PASSWORD, JWT_SECRET 값을 채운다
docker compose up -d        # PostgreSQL 실행
./gradlew bootRun           # http://localhost:8080 (테이블과 샘플 데이터가 자동으로 생성됨)
```

### 2. 화면

```bash
cd client
cp .env.example .env        # 서버 주소 (기본값 http://localhost:8080)
npm install
npm run web                 # http://localhost:8081
```

### 3. 운영자 계정 (선택)

회원가입하면 USER 권한입니다. 운영자 화면을 보려면 DB에서 권한을 바꾼 뒤 다시 로그인합니다.

```sql
UPDATE member SET role = 'ADMIN' WHERE login_id = '내 아이디';
```

CSV 업로드는 [`docs/sample/sample-questions.csv`](docs/sample/sample-questions.csv)로 해 볼 수 있습니다.

### 테스트

```bash
cd server && ./gradlew test         # Docker가 켜져 있어야 함 (Testcontainers)
cd client && npx tsc --noEmit       # 타입 검사
```

> 샘플 데이터(자격증·문제·일정)는 개발용으로 만든 것이며 실제 기출문제가 아닙니다.

## 프로젝트 구조

```
server/   Spring Boot API 서버
client/   Expo 앱 (웹 우선)
docs/     완료 기준, API 문서, 설계 문서, 작업 계획
```

## 문서

- [완료 기준](docs/DONE.md)
- [API 문서](docs/API.md)
- [설계 문서](docs/superpowers/specs/2026-11-03-todaycertificate-design.md)
- [작업 계획](docs/superpowers/plans/2026-11-03-todaycertificate.md)
- [주관식 · 점수 이력 · PDF 가져오기 설계](docs/superpowers/specs/2026-11-03-short-answer-history-pdf-design.md)
