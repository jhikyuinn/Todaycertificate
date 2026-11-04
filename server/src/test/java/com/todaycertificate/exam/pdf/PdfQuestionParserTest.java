package com.todaycertificate.exam.pdf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.todaycertificate.common.ApiException;
import com.todaycertificate.common.ErrorCode;
import com.todaycertificate.exam.QuestionType;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class PdfQuestionParserTest {

    static byte[] resource(String name) throws IOException {
        try (InputStream in = PdfQuestionParserTest.class.getResourceAsStream("/pdf/" + name)) {
            return in.readAllBytes();
        }
    }

    static String text(String name) throws IOException {
        return new String(resource(name), StandardCharsets.UTF_8);
    }

    @Test
    void samplePdf_parsesTenQuestionsWithAnswers() throws IOException {
        List<PreviewRow> rows = PdfQuestionParser.parse(PdfTextExtractor.extract(resource("sample-exam.pdf")), null);

        assertThat(rows).hasSize(10);
        assertThat(rows).extracting(PreviewRow::number).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        assertThat(rows).extracting(PreviewRow::subjectNumber).containsExactly(1, 1, 1, 1, 1, 2, 2, 2, 2, 2);
        assertThat(rows).extracting(PreviewRow::answerNo).containsExactly(2, 1, 3, 2, 4, 2, 1, 3, 4, 2);
        assertThat(rows).allSatisfy(row -> assertThat(row.warnings()).isEmpty());

        PreviewRow first = rows.get(0);
        assertThat(first.content()).isEqualTo("가장 나중에 넣은 데이터를 가장 먼저 꺼내는 자료구조는?");
        assertThat(first.choices()).containsExactly("큐", "스택", "트리", "그래프");
    }

    @Test
    void twoColumnPdf_readsLeftColumnThenRight() throws IOException {
        // 같은 문제를 2단으로 배치한 PDF. 위→아래로만 읽으면 두 단의 줄이 섞여 문제가 합쳐진다.
        List<PreviewRow> rows = PdfQuestionParser.parse(PdfTextExtractor.extract(resource("sample-exam-2col.pdf")), null);

        assertThat(rows).extracting(PreviewRow::number).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        assertThat(rows).extracting(PreviewRow::answerNo).containsExactly(2, 1, 3, 2, 4, 2, 1, 3, 4, 2);
        assertThat(rows).allSatisfy(row -> assertThat(row.warnings()).isEmpty());
        assertThat(rows.get(0).choices()).containsExactly("큐", "스택", "트리", "그래프");
    }

    @Test
    void answerTable_fillsAnswers() throws IOException {
        List<PreviewRow> rows = PdfQuestionParser.parse(text("answer-table.txt"), null);

        assertThat(rows).hasSize(3);
        assertThat(rows).extracting(PreviewRow::answerNo).containsExactly(2, 2, 2);
        assertThat(rows).extracting(PreviewRow::subjectNumber).containsExactly(1, 1, 2);
        // 줄바꿈으로 끊긴 문제는 공백으로 이어 붙이고, 정답표 줄은 마지막 보기에 섞이지 않는다
        assertThat(rows.get(0).content()).isEqualTo("가장 나중에 넣은 데이터를 가장 먼저 꺼내는 자료구조는?");
        assertThat(rows.get(2).choices()).containsExactly("UNIQUE", "DISTINCT", "GROUP", "ONLY");
        assertThat(rows).allSatisfy(row -> assertThat(row.warnings()).isEmpty());
    }

    @Test
    void missingChoice_keepsRowWithWarning() {
        List<PreviewRow> rows = PdfQuestionParser.parse("""
                1과목
                1. 보기가 하나 빠진 문제
                ❶ 가 ② 나 ③ 다
                2. 정상 문제
                ① 가 ② 나 ❸ 다 ④ 라
                """, null);

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).choices()).containsExactly("가", "나", "다");
        assertThat(rows.get(0).warnings()).containsExactly("보기를 4개 찾지 못했어요 (3개)");
        assertThat(rows.get(1).warnings()).isEmpty();
    }

    @Test
    void noAnswerOrSubject_keepsRowWithWarnings() {
        List<PreviewRow> rows = PdfQuestionParser.parse("""
                1. 정답 표시가 없는 문제
                ① 가 ② 나 ③ 다 ④ 라
                """, null);

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).answerNo()).isNull();
        assertThat(rows.get(0).warnings()).containsExactly("정답을 찾지 못했어요", "과목을 찾지 못했어요");
    }

    @Test
    void numberInsideChoice_notSplit() {
        List<PreviewRow> rows = PdfQuestionParser.parse("""
                1과목
                1. 첫 문제
                ① 가 ② 나 ③ 다 ❹ 라
                2. 다음 중 맞는 것은?
                ❶ 아래 목록의 첫 줄
                1. 이 줄은 보기의 일부다
                ② 나 ③ 다 ④ 라
                """, null);

        assertThat(rows).hasSize(2);
        assertThat(rows.get(1).choices()).containsExactly("아래 목록의 첫 줄 1. 이 줄은 보기의 일부다", "나", "다", "라");
    }

    @Test
    void explanations_attachedByNumber_authorRemoved() throws IOException {
        List<PreviewRow> rows = PdfQuestionParser.parse(text("answer-table.txt"), text("explanations.txt"));

        assertThat(rows.get(0).explanation()).isEqualTo("스택은 LIFO 구조라서 마지막에 넣은 것이 먼저 나온다.\n\n큐는 FIFO다.");
        assertThat(rows.get(1).explanation()).isEqualTo("결합도는 낮을수록 좋다. 1. 이 줄은 해설 안의 번호 목록이다.");
        assertThat(rows.get(2).explanation()).isNull();
    }

    @Test
    void practicalPdf_parsesShortAnswerRowsWithCode() throws IOException {
        // 브라우저에서 인쇄한 실기 형식. 머리글(날짜, 문서 제목)·바닥글(주소, 쪽수)이 들어 있고 3번은 두 쪽에 걸친다.
        List<PreviewRow> rows = PdfQuestionParser.parse(PdfTextExtractor.extract(resource("sample-practical.pdf")), null);

        assertThat(rows).extracting(PreviewRow::number).containsExactly(1, 2, 3);
        assertThat(rows).extracting(PreviewRow::type).containsOnly(QuestionType.SHORT_ANSWER);
        assertThat(rows).extracting(PreviewRow::answerText).containsExactly("6", "DDL", "싱글톤");
        assertThat(rows).allSatisfy(row -> {
            assertThat(row.warnings()).isEmpty();
            assertThat(row.choices()).isEmpty();
            assertThat(row.answerNo()).isNull();
            assertThat(row.content() + row.answerText() + row.explanation())
                    .doesNotContain("http", "오후", "예시-1", "예시 실기 시험지");
        });
        assertThat(rows.get(0).content()).isEqualTo("""
                다음 Python 코드를 실행하면 출력되는 값을 적으시오.
                ```
                def total(items):
                    result = 0
                    for x in items:
                        if x % 2 == 0:
                            result += x
                    return result
                print(total([1, 2, 3, 4]))
                ```""");
        assertThat(rows.get(1).explanation()).isEqualTo("CREATE, ALTER, DROP처럼 구조를 정의하는 명령은 데이터 정의어(DDL)이다.");
    }

    @Test
    void shortAnswer_singleEnglishLineIsNotCode_missingAnswerWarns() {
        String text = """
                문제 1
                빈칸에 들어갈 용어를 적으시오.
                Race Condition
                공격과 관련 있다.
                정답 및 해설
                [해설]
                설명
                """;
        PreviewRow row = PdfQuestionParser.parse(text, null).getFirst();

        assertThat(row.content()).isEqualTo("빈칸에 들어갈 용어를 적으시오. Race Condition 공격과 관련 있다.");
        assertThat(row.answerText()).isNull();
        assertThat(row.warnings()).containsExactly("정답을 찾지 못했어요");
    }

    @Test
    void notPdf_throwsPdfInvalid() {
        assertThatThrownBy(() -> PdfTextExtractor.extract("hello".getBytes()))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getCode())
                .isEqualTo(ErrorCode.PDF_INVALID);
    }
}
