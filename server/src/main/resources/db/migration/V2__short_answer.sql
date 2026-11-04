-- 주관식(단답형) 문제와 그 풀이 기록. 기존 객관식 행은 기본값으로 MULTIPLE_CHOICE가 된다.

ALTER TABLE question
    ADD COLUMN type        VARCHAR(20) NOT NULL DEFAULT 'MULTIPLE_CHOICE',
    ADD COLUMN answer_text TEXT,
    ADD COLUMN exact_match BOOLEAN     NOT NULL DEFAULT false,
    ALTER COLUMN choice1   DROP NOT NULL,
    ALTER COLUMN choice2   DROP NOT NULL,
    ALTER COLUMN choice3   DROP NOT NULL,
    ALTER COLUMN choice4   DROP NOT NULL,
    ALTER COLUMN answer_no DROP NOT NULL,
    ADD CONSTRAINT question_type_fields_check CHECK (
        (type = 'MULTIPLE_CHOICE'
            AND choice1 IS NOT NULL AND choice2 IS NOT NULL AND choice3 IS NOT NULL AND choice4 IS NOT NULL
            AND answer_no BETWEEN 1 AND 4)
        OR (type = 'SHORT_ANSWER' AND answer_text IS NOT NULL)
    );

-- 기존 selected_no의 1~4 CHECK는 그대로 둔다 (NULL은 CHECK를 통과한다).
ALTER TABLE solve_record
    ADD COLUMN answer_text TEXT,
    ALTER COLUMN selected_no DROP NOT NULL,
    ADD CONSTRAINT solve_record_one_answer_check CHECK (num_nonnulls(selected_no, answer_text) = 1);
