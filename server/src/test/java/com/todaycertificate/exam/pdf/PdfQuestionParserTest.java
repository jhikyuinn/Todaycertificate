package com.todaycertificate.exam.pdf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.todaycertificate.common.ApiException;
import com.todaycertificate.common.ErrorCode;
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
    void notPdf_throwsPdfInvalid() {
        assertThatThrownBy(() -> PdfTextExtractor.extract("hello".getBytes()))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getCode())
                .isEqualTo(ErrorCode.PDF_INVALID);
    }
}
