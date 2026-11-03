-- 개발용 샘플 데이터. 문제와 일정은 AI가 만든 샘플이며 실제 기출이 아니다.
-- ON CONFLICT DO NOTHING이라 여러 번 실행해도 중복되지 않는다.

INSERT INTO certificate (name, description, time_limit_minutes, fail_score, pass_average) VALUES
    ('정보처리기사', '소프트웨어 설계부터 정보시스템 구축 관리까지 개발 전반을 다루는 국가기술자격', 150, 40, 60),
    ('SQLD', '데이터 모델링과 SQL 활용 능력을 검증하는 국가공인 자격', 90, 40, 60)
ON CONFLICT (name) DO NOTHING;

INSERT INTO subject (certificate_id, number, name)
SELECT c.id, s.number, s.name
FROM certificate c
JOIN (VALUES
    ('정보처리기사', 1, '소프트웨어 설계'),
    ('정보처리기사', 2, '소프트웨어 개발'),
    ('정보처리기사', 3, '데이터베이스 구축'),
    ('정보처리기사', 4, '프로그래밍 언어 활용'),
    ('정보처리기사', 5, '정보시스템 구축 관리'),
    ('SQLD', 1, '데이터 모델링의 이해'),
    ('SQLD', 2, 'SQL 기본 및 활용')
) AS s(cert_name, number, name) ON c.name = s.cert_name
ON CONFLICT (certificate_id, number) DO NOTHING;

-- exam_schedule에는 UNIQUE 제약이 없어서, 같은 회차가 이미 있으면 넣지 않는다.
INSERT INTO exam_schedule (certificate_id, year, round, registration_start, registration_end, exam_date)
SELECT c.id, s.year, s.round, s.reg_start::date, s.reg_end::date, s.exam_date::date
FROM certificate c
JOIN (VALUES
    ('정보처리기사', 2026, 4, '2026-11-16', '2026-11-19', '2026-12-12'),
    ('SQLD', 2026, 4, '2026-11-09', '2026-11-13', '2026-12-05')
) AS s(cert_name, year, round, reg_start, reg_end, exam_date) ON c.name = s.cert_name
WHERE NOT EXISTS (
    SELECT 1 FROM exam_schedule e
    WHERE e.certificate_id = c.id AND e.year = s.year AND e.round = s.round
);

INSERT INTO exam (certificate_id, year, round)
SELECT id, 2025, 1 FROM certificate
ON CONFLICT (certificate_id, year, round) DO NOTHING;

INSERT INTO question (exam_id, subject_id, number, content, choice1, choice2, choice3, choice4, answer_no, explanation)
SELECT e.id, s.id, q.number, q.content, q.c1, q.c2, q.c3, q.c4, q.answer_no, q.explanation
FROM (VALUES
    ('정보처리기사', 1, 1, '애자일 방법론에 해당하지 않는 것은?',
        '스크럼', 'XP', '칸반', '폭포수 모델', 4,
        '폭포수 모델은 단계를 순서대로 진행하는 전통적인 방법론이다.'),
    ('정보처리기사', 1, 2, 'UML 다이어그램 중 구조 다이어그램에 해당하는 것은?',
        '클래스 다이어그램', '시퀀스 다이어그램', '유스케이스 다이어그램', '활동 다이어그램', 1,
        '클래스 다이어그램은 시스템의 정적인 구조를 표현한다. 나머지는 행위 다이어그램이다.'),
    ('정보처리기사', 2, 3, '화이트박스 테스트 기법에 해당하는 것은?',
        '동등 분할', '경계값 분석', '기초 경로 검사', '원인-결과 그래프', 3,
        '기초 경로 검사는 코드의 제어 흐름을 보고 테스트 케이스를 만든다. 나머지는 블랙박스 기법이다.'),
    ('정보처리기사', 2, 4, '스택(Stack)의 특징으로 옳은 것은?',
        '먼저 들어온 데이터가 먼저 나간다', '나중에 들어온 데이터가 먼저 나간다', '우선순위가 높은 데이터가 먼저 나간다', '임의의 위치에서 꺼낼 수 있다', 2,
        '스택은 LIFO(Last In First Out) 구조다.'),
    ('정보처리기사', 3, 5, '릴레이션에서 튜플을 유일하게 식별하는 최소한의 속성 집합은?',
        '외래키', '후보키', '슈퍼키', '대체키', 2,
        '후보키는 유일성과 최소성을 모두 만족한다. 슈퍼키는 유일성만 만족한다.'),
    ('정보처리기사', 3, 6, '트랜잭션의 특성(ACID)에 해당하지 않는 것은?',
        '원자성', '일관성', '독립성', '확장성', 4,
        'ACID는 원자성(Atomicity), 일관성(Consistency), 독립성(Isolation), 지속성(Durability)이다.'),
    ('정보처리기사', 4, 7, E'다음 Java 코드의 출력 결과는?\n```\nint sum = 0;\nfor (int i = 1; i <= 5; i++) {\n    if (i % 2 == 0) continue;\n    sum += i;\n}\nSystem.out.println(sum);\n```',
        '6', '9', '15', '10', 2,
        '짝수일 때는 continue로 건너뛰므로 1 + 3 + 5 = 9가 출력된다.'),
    ('정보처리기사', 4, 8, 'OSI 7계층 중 IP가 속한 계층은?',
        '데이터 링크 계층', '네트워크 계층', '전송 계층', '세션 계층', 2,
        'IP는 3계층(네트워크 계층)에서 경로 지정을 담당한다.'),
    ('정보처리기사', 5, 9, '소프트웨어 개발 비용 산정 기법 중 LOC 기법의 기준은?',
        '기능 점수', '소스 코드 라인 수', '개발자 수', '개발 기간', 2,
        'LOC(Lines Of Code)는 소스 코드 라인 수를 기준으로 비용을 추정한다.'),
    ('정보처리기사', 5, 10, '서비스 거부 공격(DoS)에 해당하는 것은?',
        'SQL 인젝션', 'SYN 플러딩', 'XSS', '피싱', 2,
        'SYN 플러딩은 연결 요청만 대량으로 보내 서버 자원을 고갈시키는 DoS 공격이다.'),
    ('SQLD', 1, 1, '엔터티의 특징으로 옳지 않은 것은?',
        '두 개 이상의 인스턴스를 가진다', '속성을 반드시 가진다', '업무에서 관리가 필요한 정보다', '다른 엔터티와 관계가 없어도 된다', 4,
        '엔터티는 다른 엔터티와 최소 한 개 이상의 관계를 가져야 한다.'),
    ('SQLD', 1, 2, '정규화의 목적으로 가장 알맞은 것은?',
        '조회 성능 향상', '데이터 중복 제거와 이상 현상 방지', '테이블 수 감소', '인덱스 생성', 2,
        '정규화는 중복을 줄여 삽입, 수정, 삭제 이상을 방지한다.'),
    ('SQLD', 2, 3, '중복 행을 제거하는 SELECT 키워드는?',
        'UNIQUE', 'DISTINCT', 'GROUP', 'ONLY', 2,
        'SELECT DISTINCT는 결과에서 중복 행을 제거한다.'),
    ('SQLD', 2, 4, E'다음 SQL의 결과 행 수는? (EMP 테이블의 DEPT_NO 값: 10, 10, 20, NULL)\n```\nSELECT DEPT_NO, COUNT(*)\nFROM EMP\nGROUP BY DEPT_NO;\n```',
        '2', '3', '4', '1', 2,
        'GROUP BY는 NULL도 하나의 그룹으로 묶으므로 10, 20, NULL의 3개 그룹이 나온다.')
) AS q(cert_name, subject_no, number, content, c1, c2, c3, c4, answer_no, explanation)
JOIN certificate c ON c.name = q.cert_name
JOIN exam e ON e.certificate_id = c.id AND e.year = 2025 AND e.round = 1
JOIN subject s ON s.certificate_id = c.id AND s.number = q.subject_no
ON CONFLICT (exam_id, number) DO NOTHING;
