-- 개발용 샘플 데이터. 문제와 일정은 AI가 만든 샘플이며 실제 기출이 아니다.
-- ON CONFLICT DO NOTHING이라 여러 번 실행해도 중복되지 않는다.

INSERT INTO certificate (name, description, time_limit_minutes, fail_score, pass_average) VALUES
    ('정보처리기사', '소프트웨어 설계부터 정보시스템 구축 관리까지 개발 전반을 다루는 국가기술자격', 150, 40, 60),
    ('SQLD', '데이터 모델링과 SQL 활용 능력을 검증하는 국가공인 자격', 90, 40, 60),
    ('정보처리기사 실기', '정보처리기사 실기 시험. 단답형으로 답을 직접 써서 푼다', 150, 0, 60)
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
    ('SQLD', 2, 'SQL 기본 및 활용'),
    ('정보처리기사 실기', 1, '실기')
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

-- 실기 단답형 예시 (직접 만든 문제). 허용 답은 |로 구분, exact_match면 대소문자까지 그대로 비교한다.
INSERT INTO question (exam_id, subject_id, number, type, content, answer_text, exact_match, explanation)
SELECT e.id, s.id, q.number, 'SHORT_ANSWER', q.content, q.answer_text, q.exact_match, q.explanation
FROM (VALUES
    (1, '데이터베이스의 구조(테이블, 인덱스 등)를 정의하는 SQL 명령어 분류를 영문 약자로 쓰시오.',
        'DDL', false, 'DDL(Data Definition Language)은 CREATE, ALTER, DROP처럼 구조를 정의한다.'),
    (2, 'HTTP에 TLS 암호화를 더한 프로토콜의 이름을 쓰시오.',
        'HTTPS', false, 'HTTPS는 HTTP 통신을 TLS로 암호화한다. 기본 포트는 443이다.'),
    (3, '테이블의 구조는 남기고 모든 행을 한 번에 삭제하는 SQL 명령어를 쓰시오.',
        'TRUNCATE', false, 'TRUNCATE는 DDL로 분류되며 DELETE와 달리 행 단위로 기록하지 않아 빠르다.'),
    (4, E'다음 Java 코드의 출력 결과를 쓰시오.
```
String s = "ab";
System.out.println(s.toUpperCase().charAt(0) + s.substring(1) + "C");
```',
        'AbC', true, 'toUpperCase().charAt(0)은 ''A'', substring(1)은 "b"이므로 "AbC"가 출력된다. 대소문자까지 정확히 써야 한다.'),
    (5, '관계형 데이터베이스에서 릴레이션의 한 행을 가리키는 용어를 쓰시오.',
        '튜플|tuple', false, '행은 튜플(tuple), 열은 속성(attribute)이라 부른다.')
) AS q(number, content, answer_text, exact_match, explanation)
JOIN certificate c ON c.name = '정보처리기사 실기'
JOIN exam e ON e.certificate_id = c.id AND e.year = 2025 AND e.round = 1
JOIN subject s ON s.certificate_id = c.id AND s.number = 1
ON CONFLICT (exam_id, number) DO NOTHING;

-- ===== 데모용 추가 데이터 (2026-11-06) =====
-- 점수 이력 그래프, 과목별 리포트, 오늘의 문제가 의미 있게 보이도록 회차와 문제를 늘린다.
-- 기존 블록을 바꾸지 않고 추가만 한다 (운영 DB에 이미 들어간 데이터는 그대로 둔다).

INSERT INTO exam_schedule (certificate_id, year, round, registration_start, registration_end, exam_date)
SELECT c.id, s.year, s.round, s.reg_start::date, s.reg_end::date, s.exam_date::date
FROM certificate c
JOIN (VALUES
    ('정보처리기사 실기', 2026, 4, '2026-11-23', '2026-11-26', '2026-12-19'),
    ('정보처리기사', 2027, 1, '2027-01-26', '2027-01-29', '2027-02-27'),
    ('SQLD', 2027, 1, '2027-02-02', '2027-02-06', '2027-03-08'),
    ('정보처리기사 실기', 2027, 1, '2027-03-23', '2027-03-26', '2027-04-24')
) AS s(cert_name, year, round, reg_start, reg_end, exam_date) ON c.name = s.cert_name
WHERE NOT EXISTS (
    SELECT 1 FROM exam_schedule e
    WHERE e.certificate_id = c.id AND e.year = s.year AND e.round = s.round
);

INSERT INTO exam (certificate_id, year, round)
SELECT c.id, x.year, x.round
FROM certificate c
JOIN (VALUES
    ('정보처리기사', 2025, 2),
    ('정보처리기사', 2025, 3),
    ('SQLD', 2025, 2),
    ('정보처리기사 실기', 2025, 2)
) AS x(cert_name, year, round) ON c.name = x.cert_name
ON CONFLICT (certificate_id, year, round) DO NOTHING;

INSERT INTO question (exam_id, subject_id, number, content, choice1, choice2, choice3, choice4, answer_no, explanation)
SELECT e.id, s.id, q.number, q.content, q.c1, q.c2, q.c3, q.c4, q.answer_no, q.explanation
FROM (VALUES
    -- 정보처리기사 2025년 2회
    ('정보처리기사', 2, 1, 1, '구조적 분석에서 사용하는 요구사항 분석 도구가 아닌 것은?',
        '자료 흐름도(DFD)', '자료 사전(DD)', '소단위 명세서', '컴파일러', 4,
        '구조적 분석 도구는 DFD, 자료 사전, 소단위 명세서, ERD 등이다. 컴파일러는 소스 코드를 번역하는 도구다.'),
    ('정보처리기사', 2, 1, 2, 'GoF 디자인 패턴 중 생성 패턴에 해당하는 것은?',
        '싱글톤(Singleton)', '어댑터(Adapter)', '옵저버(Observer)', '프록시(Proxy)', 1,
        '싱글톤은 생성 패턴이다. 어댑터와 프록시는 구조 패턴, 옵저버는 행위 패턴이다.'),
    ('정보처리기사', 2, 2, 3, '큐(Queue)에 대한 설명으로 옳은 것은?',
        'LIFO 구조다', 'FIFO 구조다', '한쪽 끝에서만 삽입과 삭제가 일어난다', '우선순위가 높은 데이터가 항상 먼저 나간다', 2,
        '큐는 먼저 들어온 데이터가 먼저 나가는 FIFO(First In First Out) 구조다.'),
    ('정보처리기사', 2, 2, 4, '형상(버전) 관리 도구가 아닌 것은?',
        'Git', 'SVN', 'CVS', 'JUnit', 4,
        'JUnit은 Java 단위 테스트 프레임워크다.'),
    ('정보처리기사', 2, 3, 5, 'SQL 명령어 중 DCL(데이터 제어어)에 해당하는 것은?',
        'SELECT', 'GRANT', 'CREATE', 'UPDATE', 2,
        'DCL은 GRANT, REVOKE처럼 권한을 다룬다. CREATE는 DDL, SELECT와 UPDATE는 DML이다.'),
    ('정보처리기사', 2, 3, 6, '제2정규형(2NF)을 만족하기 위한 조건은?',
        '모든 도메인이 원자값이다', '부분 함수 종속을 제거한다', '이행 함수 종속을 제거한다', '다치 종속을 제거한다', 2,
        '1NF에서 부분 함수 종속을 제거하면 2NF, 이행 함수 종속까지 제거하면 3NF다.'),
    ('정보처리기사', 2, 4, 7, E'다음 C 코드의 출력 결과는?\n```\nint a = 7, b = 3;\nprintf("%d", a / b + a % b);\n```',
        '3', '2', '4', '1', 1,
        '정수 나눗셈 7 / 3 = 2, 나머지 7 % 3 = 1이므로 2 + 1 = 3이다.'),
    ('정보처리기사', 2, 4, 8, 'TCP의 특징이 아닌 것은?',
        '연결 지향', '신뢰성 보장', '흐름 제어', '비연결형 전송', 4,
        '비연결형 전송은 UDP의 특징이다. TCP는 연결을 맺고 순서와 도착을 보장한다.'),
    ('정보처리기사', 2, 5, 9, '나선형(Spiral) 모델의 특징으로 옳은 것은?',
        '위험 분석을 반복하며 점진적으로 개발한다', '모든 단계를 한 번씩만 순서대로 진행한다', '요구사항 정의 없이 바로 구현한다', '유지보수 단계가 없다', 1,
        '나선형 모델은 계획, 위험 분석, 개발, 평가를 반복하며 위험을 줄인다.'),
    ('정보처리기사', 2, 5, 10, '대칭키 암호화 알고리즘에 해당하는 것은?',
        'RSA', 'AES', 'ECC', 'DSA', 2,
        'AES는 암호화와 복호화에 같은 키를 쓰는 대칭키 방식이다. RSA, ECC, DSA는 공개키 방식이다.'),

    -- 정보처리기사 2025년 3회
    ('정보처리기사', 3, 1, 1, '모듈 결합도 중 가장 약해서 바람직한 것은?',
        '내용 결합도', '공통 결합도', '제어 결합도', '자료 결합도', 4,
        '결합도는 자료 < 스탬프 < 제어 < 외부 < 공통 < 내용 순으로 강해진다. 약할수록 좋다.'),
    ('정보처리기사', 3, 1, 2, '모듈 응집도 중 가장 강해서 바람직한 것은?',
        '우연적 응집도', '논리적 응집도', '기능적 응집도', '시간적 응집도', 3,
        '기능적 응집도는 모듈 안의 모든 요소가 하나의 기능을 위해 존재한다. 응집도는 강할수록 좋다.'),
    ('정보처리기사', 3, 2, 3, '이진 탐색(Binary Search)을 쓰기 위한 조건은?',
        '데이터가 정렬되어 있어야 한다', '데이터가 해시 테이블에 있어야 한다', '데이터 개수가 홀수여야 한다', '연결 리스트로 구현해야 한다', 1,
        '이진 탐색은 정렬된 데이터의 가운데 값과 비교해 탐색 범위를 절반씩 줄인다.'),
    ('정보처리기사', 3, 2, 4, '하향식 통합 테스트에서 아직 만들지 않은 하위 모듈 대신 쓰는 것은?',
        '스텁(Stub)', '드라이버(Driver)', '목업 화면', '디버거', 1,
        '하향식은 하위 모듈을 스텁으로, 상향식은 상위 모듈을 드라이버로 대신한다.'),
    ('정보처리기사', 3, 3, 5, '인덱스에 대한 설명으로 옳지 않은 것은?',
        '조회 속도를 높일 수 있다', '추가 저장 공간이 필요하다', '인덱스가 많을수록 INSERT가 빨라진다', 'B-Tree 구조를 많이 사용한다', 3,
        '데이터를 넣을 때 인덱스도 함께 갱신해야 하므로 인덱스가 많을수록 INSERT는 느려진다.'),
    ('정보처리기사', 3, 3, 6, '일반 뷰(View)에 대한 설명으로 옳지 않은 것은?',
        '논리적 데이터 독립성을 제공한다', '보안(접근 제한)에 활용할 수 있다', '복잡한 질의를 단순하게 만든다', '데이터를 별도의 물리적 공간에 따로 저장한다', 4,
        '일반 뷰는 저장된 질의일 뿐, 데이터를 따로 저장하지 않는다.'),
    ('정보처리기사', 3, 4, 7, E'다음 Python 코드의 출력 결과는?\n```\nnums = [1, 2, 3, 4]\nprint(sum(n * n for n in nums if n % 2 == 0))\n```',
        '20', '30', '6', '10', 1,
        '짝수 2와 4만 제곱해서 더하므로 4 + 16 = 20이다.'),
    ('정보처리기사', 3, 4, 8, 'IPv4 주소의 길이는?',
        '16비트', '32비트', '64비트', '128비트', 2,
        'IPv4는 32비트, IPv6는 128비트다.'),
    ('정보처리기사', 3, 5, 9, 'COCOMO 모델의 개발 유형이 아닌 것은?',
        '조직형(Organic)', '반분리형(Semi-detached)', '내장형(Embedded)', '분산형(Distributed)', 4,
        'COCOMO는 프로젝트를 조직형, 반분리형, 내장형으로 나눠 비용을 산정한다.'),
    ('정보처리기사', 3, 5, 10, '파일을 암호화해 사용할 수 없게 만든 뒤 돈을 요구하는 악성코드는?',
        '랜섬웨어', '웜', '스파이웨어', '애드웨어', 1,
        '랜섬웨어는 파일을 인질로 잡고 복호화 대가를 요구한다.'),

    -- SQLD 2025년 2회
    ('SQLD', 2, 1, 1, '업무에 원래 존재하지 않지만 편의를 위해 인위적으로 만든 식별자는?',
        '본질식별자', '인조식별자', '보조식별자', '외부식별자', 2,
        '인조식별자는 일련번호처럼 시스템이 만들어 붙인 식별자다.'),
    ('SQLD', 2, 1, 2, '반정규화에 대한 설명으로 옳은 것은?',
        '성능 향상을 위해 의도적으로 중복을 허용한다', '데이터 중복을 완전히 없앤다', '항상 데이터 무결성을 높인다', '정규화보다 먼저 수행한다', 1,
        '반정규화는 조회 성능을 위해 정규화된 모델에 중복을 다시 허용한다. 무결성이 깨질 위험이 있다.'),
    ('SQLD', 2, 1, 3, '한 엔터티의 인스턴스가 다른 엔터티의 인스턴스 몇 개와 관계를 맺는지 나타내는 것은?',
        '관계 차수(Cardinality)', '관계 선택사양(Optionality)', '속성(Attribute)', '도메인(Domain)', 1,
        '관계 차수는 1:1, 1:N, M:N처럼 관계에 참여하는 인스턴스 수를 나타낸다.'),
    ('SQLD', 2, 2, 4, E'T 테이블의 COL1 값이 1, NULL, 3, NULL일 때 결과는?\n```\nSELECT COUNT(COL1) FROM T;\n```',
        '4', '2', '0', '3', 2,
        'COUNT(컬럼)은 NULL을 세지 않는다. COUNT(*)라면 4가 나온다.'),
    ('SQLD', 2, 2, 5, 'GROUP BY로 묶은 그룹에 조건을 걸 때 쓰는 절은?',
        'WHERE', 'HAVING', 'ORDER BY', 'DISTINCT', 2,
        'WHERE는 그룹으로 묶기 전 행에, HAVING은 묶은 뒤 그룹에 조건을 건다.'),
    ('SQLD', 2, 2, 6, E'A 테이블의 ID가 1, 2, 3이고 B 테이블의 ID가 1, 1, 4일 때 결과 행 수는?\n```\nSELECT *\nFROM A LEFT OUTER JOIN B ON A.ID = B.ID;\n```',
        '3', '4', '5', '2', 2,
        'A의 1은 B의 두 행과 연결되고, 2와 3은 B가 NULL인 행으로 남는다. 2 + 1 + 1 = 4행이다.')
) AS q(cert_name, exam_round, subject_no, number, content, c1, c2, c3, c4, answer_no, explanation)
JOIN certificate c ON c.name = q.cert_name
JOIN exam e ON e.certificate_id = c.id AND e.year = 2025 AND e.round = q.exam_round
JOIN subject s ON s.certificate_id = c.id AND s.number = q.subject_no
ON CONFLICT (exam_id, number) DO NOTHING;

-- 실기 2025년 2회: C, Python, Java 코드 문제를 넣어 코드 표시(줄바꿈·들여쓰기)와 정확 일치 채점을 보여준다.
INSERT INTO question (exam_id, subject_id, number, type, content, answer_text, exact_match, explanation)
SELECT e.id, s.id, q.number, 'SHORT_ANSWER', q.content, q.answer_text, q.exact_match, q.explanation
FROM (VALUES
    (1, E'다음 C 코드의 출력 결과를 쓰시오.\n```\n#include <stdio.h>\nint main() {\n    int arr[] = {3, 1, 4, 1, 5};\n    int max = arr[0];\n    for (int i = 1; i < 5; i++) {\n        if (arr[i] > max) max = arr[i];\n    }\n    printf("%d", max);\n    return 0;\n}\n```',
        '5', true, '배열을 한 번 돌며 더 큰 값을 max에 저장하므로 가장 큰 값 5가 출력된다.'),
    (2, E'다음 Python 코드의 출력 결과를 쓰시오.\n```\ns = "hello"\nprint(s[::-1])\n```',
        'olleh', true, '슬라이싱 [::-1]은 문자열을 뒤집는다.'),
    (3, E'점수가 90 이상인 학생 수를 구하려고 한다. 빈칸에 들어갈 함수 이름을 쓰시오.\n```\nSELECT ____(*) FROM STUDENT WHERE SCORE >= 90;\n```',
        'COUNT', false, 'COUNT(*)는 조건에 맞는 행의 개수를 센다.'),
    (4, '한 객체의 상태가 바뀌면 그 객체에 의존하는 다른 객체들에게 자동으로 알리는 GoF 행위 패턴의 이름을 쓰시오.',
        '옵저버|옵저버 패턴|Observer|Observer Pattern', false, '옵저버 패턴은 구독자 목록을 두고 상태가 바뀔 때 알림을 보낸다. 이벤트 리스너가 대표적인 예다.'),
    (5, E'다음 Java 코드의 출력 결과를 쓰시오.\n```\nint x = 10;\nx += x++ + ++x;\nSystem.out.println(x);\n```',
        '32', true, 'x += E는 원래 x 값 10을 먼저 읽어 둔다. x++는 10을 돌려주고 x를 11로, ++x는 x를 12로 만들고 12를 돌려준다. 10 + (10 + 12) = 32다.')
) AS q(number, content, answer_text, exact_match, explanation)
JOIN certificate c ON c.name = '정보처리기사 실기'
JOIN exam e ON e.certificate_id = c.id AND e.year = 2025 AND e.round = 2
JOIN subject s ON s.certificate_id = c.id AND s.number = 1
ON CONFLICT (exam_id, number) DO NOTHING;
