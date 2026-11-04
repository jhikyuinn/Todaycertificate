package com.todaycertificate.exam.upload;

import static org.assertj.core.api.Assertions.assertThat;

import com.todaycertificate.exam.QuestionType;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CsvQuestionParserTest {

    static final String HEADER = "과목번호,문제번호,문제,보기1,보기2,보기3,보기4,정답번호,해설\n";
    static final Set<Integer> SUBJECTS = Set.of(1, 2);

    static CsvParseResult parse(String csv) {
        return CsvQuestionParser.parse(csv.getBytes(StandardCharsets.UTF_8), SUBJECTS);
    }

    @Test
    void validFile_parsesAllRows() {
        CsvParseResult result = parse(HEADER + """
                1,1,스택의 특징은?,FIFO,LIFO,랜덤,정렬,2,스택은 LIFO다.
                2,2,중복 제거 키워드는?,UNIQUE,DISTINCT,GROUP,ONLY,2,
                """);

        assertThat(result.errors()).isEmpty();
        assertThat(result.rows()).hasSize(2);
        QuestionRow first = result.rows().get(0);
        assertThat(first.subjectNumber()).isEqualTo(1);
        assertThat(first.number()).isEqualTo(1);
        assertThat(first.choices()).containsExactly("FIFO", "LIFO", "랜덤", "정렬");
        assertThat(first.answerNo()).isEqualTo(2);
        assertThat(first.explanation()).isEqualTo("스택은 LIFO다.");
        assertThat(result.rows().get(1).explanation()).isNull(); // 해설은 선택
    }

    @Test
    void multilineCodeCell_preservesNewlinesAndIndentation() {
        String content = "출력 결과는?\n```\nif (a):\n    print(1)\n```";
        CsvParseResult result = parse(HEADER + "1,1,\"" + content + "\",1,2,3,4,1,\n");

        assertThat(result.errors()).isEmpty();
        assertThat(result.rows().get(0).content()).isEqualTo(content);
    }

    @Test
    void crlfInsideCell_isNormalizedToLf() {
        String csv = (HEADER + "1,1,\"첫 줄\r\n    둘째 줄\",1,2,3,4,1,\r\n").replace("해설\n", "해설\r\n");
        CsvParseResult result = parse(csv);

        assertThat(result.errors()).isEmpty();
        assertThat(result.rows().get(0).content()).isEqualTo("첫 줄\n    둘째 줄");
    }

    @Test
    void utf8Bom_isIgnored() {
        byte[] body = (HEADER + "1,1,문제,1,2,3,4,1,\n").getBytes(StandardCharsets.UTF_8);
        byte[] withBom = new byte[body.length + 3];
        withBom[0] = (byte) 0xEF;
        withBom[1] = (byte) 0xBB;
        withBom[2] = (byte) 0xBF;
        System.arraycopy(body, 0, withBom, 3, body.length);

        CsvParseResult result = CsvQuestionParser.parse(withBom, SUBJECTS);

        assertThat(result.errors()).isEmpty();
        assertThat(result.rows()).hasSize(1);
    }

    @Test
    void nonUtf8File_returnsSingleEncodingError() {
        // 엑셀에서 "CSV(쉼표로 분리)"로 저장하면 한국어 Windows에서는 CP949가 된다
        byte[] cp949 = (HEADER + "1,1,문제,1,2,3,4,1,\n").getBytes(Charset.forName("MS949"));

        CsvParseResult result = CsvQuestionParser.parse(cp949, SUBJECTS);

        assertThat(result.rows()).isEmpty();
        assertThat(result.errors()).singleElement()
                .satisfies(e -> assertThat(e.message()).contains("UTF-8"));
    }

    @Test
    void wrongHeader_returnsError() {
        CsvParseResult result = parse("과목,번호,문제,보기1,보기2,보기3,보기4,정답,해설\n1,1,문제,1,2,3,4,1,\n");

        assertThat(result.rows()).isEmpty();
        assertThat(result.errors()).singleElement()
                .satisfies(e -> assertThat(e.message()).contains("헤더"));
    }

    @Test
    void answerNoOutOfRange_reportsRow() {
        CsvParseResult result = parse(HEADER + """
                1,1,문제,1,2,3,4,1,
                1,2,문제,1,2,3,4,5,
                """);

        assertThat(result.errors()).singleElement()
                .satisfies(e -> {
                    assertThat(e.row()).isEqualTo(2);
                    assertThat(e.message()).contains("정답번호");
                });
    }

    @Test
    void unknownSubject_reportsRow() {
        CsvParseResult result = parse(HEADER + "9,1,문제,1,2,3,4,1,\n");

        assertThat(result.errors()).singleElement()
                .satisfies(e -> assertThat(e.message()).contains("과목번호"));
    }

    @Test
    void duplicateQuestionNumber_reportsRow() {
        CsvParseResult result = parse(HEADER + """
                1,1,문제,1,2,3,4,1,
                1,1,문제,1,2,3,4,1,
                """);

        assertThat(result.errors()).singleElement()
                .satisfies(e -> {
                    assertThat(e.row()).isEqualTo(2);
                    assertThat(e.message()).contains("중복");
                });
    }

    @Test
    void emptyRequiredCell_reportsRow() {
        CsvParseResult result = parse(HEADER + "1,1,문제,1,,3,4,1,\n");

        assertThat(result.errors()).singleElement()
                .satisfies(e -> assertThat(e.message()).contains("보기2"));
    }

    @Test
    void unbalancedCodeFence_reportsRow() {
        CsvParseResult result = parse(HEADER + "1,1,\"코드\n```\nint a;\",1,2,3,4,1,\n");

        assertThat(result.errors()).singleElement()
                .satisfies(e -> assertThat(e.message()).contains("```"));
    }

    @Test
    void wrongColumnCount_reportsRow() {
        CsvParseResult result = parse(HEADER + "1,1,문제,1,2,3,4\n");

        assertThat(result.errors()).singleElement()
                .satisfies(e -> assertThat(e.message()).contains("9개"));
    }

    @Test
    void multipleErrors_allReported() {
        CsvParseResult result = parse(HEADER + """
                1,1,문제,1,2,3,4,7,
                9,2,문제,1,2,3,4,1,
                1,3,,1,2,3,4,1,
                """);

        assertThat(result.errors()).extracting(CsvError::row).containsExactly(1, 2, 3);
    }

    static final String TYPED_HEADER = "과목번호,문제번호,문제,보기1,보기2,보기3,보기4,정답번호,해설,유형\n";

    @Test
    void oldNineColumnCsv_stillParses() {
        CsvParseResult result = parse(HEADER + "1,1,문제,1,2,3,4,1,\n");

        assertThat(result.errors()).isEmpty();
        assertThat(result.rows().get(0).type()).isEqualTo(QuestionType.MULTIPLE_CHOICE);
        assertThat(result.rows().get(0).answerText()).isNull();
    }

    @Test
    void shortAnswerRow_parsesAnswerText() {
        CsvParseResult result = parse(TYPED_HEADER + """
                1,1,문제,1,2,3,4,1,,객관식
                1,2,정의어는?,,,,, DDL|데이터 정의어 ,,주관식
                """);

        assertThat(result.errors()).isEmpty();
        assertThat(result.rows().get(0).type()).isEqualTo(QuestionType.MULTIPLE_CHOICE);
        QuestionRow row = result.rows().get(1);
        assertThat(row.type()).isEqualTo(QuestionType.SHORT_ANSWER);
        assertThat(row.answerText()).isEqualTo("DDL|데이터 정의어");
        assertThat(row.exactMatch()).isFalse();
        assertThat(row.answerNo()).isNull();
        assertThat(row.choices()).isEmpty();
    }

    @Test
    void typedHeader_acceptsNineColumnMultipleChoiceRow() {
        // 유형 헤더가 있어도 객관식 행은 예전처럼 9칸으로 써도 된다 (유형 칸 생략 = 객관식)
        CsvParseResult result = parse(TYPED_HEADER + """
                1,1,문제,1,2,3,4,1,
                1,2,정의어는?,,,,,DDL,,주관식
                """);

        assertThat(result.errors()).isEmpty();
        assertThat(result.rows().get(0).type()).isEqualTo(QuestionType.MULTIPLE_CHOICE);
        assertThat(result.rows().get(1).type()).isEqualTo(QuestionType.SHORT_ANSWER);
    }

    @Test
    void shortAnswerExact_setsExactMatch() {
        CsvParseResult result = parse(TYPED_HEADER + "1,1,출력은?,,,,,Hello,,주관식-정확히\n");

        assertThat(result.errors()).isEmpty();
        assertThat(result.rows().get(0).type()).isEqualTo(QuestionType.SHORT_ANSWER);
        assertThat(result.rows().get(0).exactMatch()).isTrue();
    }

    @Test
    void shortAnswerWithChoices_isError() {
        CsvParseResult result = parse(TYPED_HEADER + "1,1,문제,A,,,,DDL,,주관식\n");

        assertThat(result.errors()).singleElement()
                .satisfies(e -> assertThat(e.message()).isEqualTo("주관식 문제는 보기 칸을 비워 주세요."));
    }

    @Test
    void emptyAlternative_isError() {
        CsvParseResult result = parse(TYPED_HEADER + """
                1,1,문제,,,,,A||B,,주관식
                1,2,문제,,,,,A|,,주관식
                1,3,문제,,,,,,,주관식
                """);

        assertThat(result.errors()).extracting(CsvError::message)
                .containsExactly("주관식 정답이 비어 있습니다.", "주관식 정답이 비어 있습니다.", "주관식 정답이 비어 있습니다.");
    }

    @Test
    void unknownType_isError() {
        CsvParseResult result = parse(TYPED_HEADER + "1,1,문제,1,2,3,4,1,,서술형\n");

        assertThat(result.errors()).singleElement()
                .satisfies(e -> assertThat(e.message())
                        .isEqualTo("'유형'은 객관식, 주관식, 주관식-정확히 중 하나여야 합니다."));
    }

    @Test
    void sampleFile_hasNoErrors() throws Exception {
        byte[] file = Files.readAllBytes(Path.of("../docs/sample/sample-questions.csv"));

        CsvParseResult result = CsvQuestionParser.parse(file, SUBJECTS);

        assertThat(result.errors()).isEmpty();
        assertThat(result.rows()).hasSize(4);
    }
}
