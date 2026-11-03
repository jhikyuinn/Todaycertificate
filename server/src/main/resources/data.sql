-- 개발용 샘플 데이터. ON CONFLICT DO NOTHING이라 여러 번 실행해도 중복되지 않는다.
INSERT INTO certificate (name, category, information) VALUES
    ('정보처리기사', 'IT', '소프트웨어 개발 전반을 다루는 국가기술자격'),
    ('SQLD', 'IT', 'SQL 활용 능력을 검증하는 데이터 자격')
ON CONFLICT (name) DO NOTHING;

INSERT INTO question (certificate_id, question_code, question, answer, explanation)
SELECT c.id, q.code, q.question, q.answer, q.explanation
FROM certificate c
JOIN (VALUES
    ('정보처리기사', 1, '애자일 방법론에 해당하지 않는 것은?', '폭포수 모델', '폭포수 모델은 단계를 순서대로 진행하는 전통적 방법론이다.'),
    ('정보처리기사', 2, 'OSI 7계층 중 IP가 속한 계층은?', '네트워크 계층', 'IP는 3계층(네트워크)에서 경로 지정을 담당한다.'),
    ('SQLD', 1, '중복 행을 제거하는 SELECT 키워드는?', 'DISTINCT', 'SELECT DISTINCT는 결과에서 중복 행을 제거한다.'),
    ('SQLD', 2, '그룹 조건을 거는 절은?', 'HAVING', 'WHERE는 행, HAVING은 GROUP BY 이후 그룹에 조건을 건다.')
) AS q(cert_name, code, question, answer, explanation) ON c.name = q.cert_name
ON CONFLICT (certificate_id, question_code) DO NOTHING;
