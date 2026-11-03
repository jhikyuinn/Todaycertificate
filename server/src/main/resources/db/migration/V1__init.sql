-- Flyway가 한 번만 적용한다. 첫 배포 전까지는 이 파일을 고치고 로컬 DB를 다시 만든다:
--   docker compose down -v && docker compose up -d
-- 첫 배포 이후에는 이 파일을 고치지 않고 V2__...를 추가한다 (체크섬이 바뀌면 Flyway가 시작을 막는다).

CREATE TABLE certificate (
    id                 BIGSERIAL    PRIMARY KEY,
    name               VARCHAR(100) NOT NULL UNIQUE,
    description        TEXT,
    time_limit_minutes INT          NOT NULL,
    fail_score         INT          NOT NULL DEFAULT 40,
    pass_average       INT          NOT NULL DEFAULT 60
);

CREATE TABLE subject (
    id             BIGSERIAL    PRIMARY KEY,
    certificate_id BIGINT       NOT NULL REFERENCES certificate (id),
    number         INT          NOT NULL,
    name           VARCHAR(100) NOT NULL,
    UNIQUE (certificate_id, number)
);

CREATE TABLE exam_schedule (
    id                 BIGSERIAL PRIMARY KEY,
    certificate_id     BIGINT    NOT NULL REFERENCES certificate (id),
    year               INT       NOT NULL,
    round              INT       NOT NULL,
    registration_start DATE      NOT NULL,
    registration_end   DATE      NOT NULL,
    exam_date          DATE      NOT NULL
);

-- 기출 회차 (예: 2025년 1회)
CREATE TABLE exam (
    id             BIGSERIAL PRIMARY KEY,
    certificate_id BIGINT    NOT NULL REFERENCES certificate (id),
    year           INT       NOT NULL,
    round          INT       NOT NULL,
    UNIQUE (certificate_id, year, round)
);

CREATE TABLE question (
    id          BIGSERIAL PRIMARY KEY,
    exam_id     BIGINT    NOT NULL REFERENCES exam (id),
    subject_id  BIGINT    NOT NULL REFERENCES subject (id),
    number      INT       NOT NULL,
    content     TEXT      NOT NULL,
    choice1     TEXT      NOT NULL,
    choice2     TEXT      NOT NULL,
    choice3     TEXT      NOT NULL,
    choice4     TEXT      NOT NULL,
    answer_no   INT       NOT NULL CHECK (answer_no BETWEEN 1 AND 4),
    explanation TEXT,
    UNIQUE (exam_id, number)
);

CREATE TABLE member (
    id                 BIGSERIAL    PRIMARY KEY,
    login_id           VARCHAR(50)  NOT NULL UNIQUE,
    name               VARCHAR(50)  NOT NULL,
    password           VARCHAR(100) NOT NULL, -- BCrypt 해시
    role               VARCHAR(10)  NOT NULL DEFAULT 'USER',
    target_schedule_id BIGINT       REFERENCES exam_schedule (id)
);

-- 시험 모드 응시. 제출 전에는 submitted_at, average_score, passed가 비어 있다.
CREATE TABLE exam_attempt (
    id            BIGSERIAL        PRIMARY KEY,
    member_id     BIGINT           NOT NULL REFERENCES member (id),
    exam_id       BIGINT           NOT NULL REFERENCES exam (id),
    started_at    TIMESTAMPTZ      NOT NULL,
    submitted_at  TIMESTAMPTZ,
    average_score DOUBLE PRECISION,
    passed        BOOLEAN
);

-- 공부 모드와 시험 모드의 모든 풀이. 오답노트, 정답률, 과락 리포트가 이 테이블을 쓴다.
-- attempt_id는 시험 모드 기록만 채운다. 제출한 시험의 결과를 다시 보여줄 때 쓴다.
CREATE TABLE solve_record (
    id          BIGSERIAL   PRIMARY KEY,
    member_id   BIGINT      NOT NULL REFERENCES member (id),
    question_id BIGINT      NOT NULL REFERENCES question (id),
    attempt_id  BIGINT      REFERENCES exam_attempt (id),
    selected_no INT         NOT NULL CHECK (selected_no BETWEEN 1 AND 4),
    correct     BOOLEAN     NOT NULL,
    mode        VARCHAR(10) NOT NULL,
    solved_at   TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_solve_record_member_question
    ON solve_record (member_id, question_id, solved_at);

CREATE TABLE bookmark (
    id          BIGSERIAL PRIMARY KEY,
    member_id   BIGINT    NOT NULL REFERENCES member (id),
    question_id BIGINT    NOT NULL REFERENCES question (id),
    UNIQUE (member_id, question_id)
);
