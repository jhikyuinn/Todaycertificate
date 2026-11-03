-- 앱 시작 때마다 실행된다. IF NOT EXISTS라서 기존 데이터는 유지된다.
CREATE TABLE IF NOT EXISTS certificate (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    category    VARCHAR(50)  NOT NULL,
    information TEXT
);

CREATE TABLE IF NOT EXISTS question (
    id             BIGSERIAL PRIMARY KEY,
    certificate_id BIGINT    NOT NULL REFERENCES certificate (id),
    question_code  INT       NOT NULL,
    question       TEXT      NOT NULL,
    answer         TEXT      NOT NULL,
    explanation    TEXT,
    UNIQUE (certificate_id, question_code)
);

CREATE TABLE IF NOT EXISTS member (
    id                      BIGSERIAL    PRIMARY KEY,
    login_id                VARCHAR(50)  NOT NULL UNIQUE,
    name                    VARCHAR(50)  NOT NULL,
    password                VARCHAR(100) NOT NULL, -- BCrypt 해시
    acquired_certificate    VARCHAR(100),
    acquired_certificate_review TEXT
);
