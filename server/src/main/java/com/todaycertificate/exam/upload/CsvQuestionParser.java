package com.todaycertificate.exam.upload;

import com.todaycertificate.exam.QuestionType;
import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

/**
 * 기출 CSV를 읽고 검증한다. DB나 Spring에 의존하지 않는 순수 코드라 단위 테스트가 쉽다.
 * 형식: 과목번호,문제번호,문제,보기1,보기2,보기3,보기4,정답번호,해설[,유형]
 * 유형 칸은 선택이다(헤더에 없거나, 행에서 생략하거나, 비면 객관식). 주관식은 보기를 비우고 정답번호 칸에 허용 답을 '|'로 구분해 쓴다.
 */
public final class CsvQuestionParser {

    static final List<String> HEADER =
            List.of("과목번호", "문제번호", "문제", "보기1", "보기2", "보기3", "보기4", "정답번호", "해설");
    static final List<String> TYPED_HEADER =
            Stream.concat(HEADER.stream(), Stream.of("유형")).toList();
    private static final String FENCE = "```";

    private CsvQuestionParser() {
    }

    public static CsvParseResult parse(byte[] file, Set<Integer> subjectNumbers) {
        String text;
        try {
            text = decodeUtf8(file);
        } catch (CharacterCodingException e) {
            return CsvParseResult.fileError("파일을 UTF-8로 저장해 주세요. (엑셀: 다른 이름으로 저장 → CSV UTF-8)");
        }
        // 엑셀(Windows)의 \r\n 줄바꿈을 \n으로 통일한다. 칸 안의 코드 줄바꿈도 함께 바뀐다
        text = text.replace("\r\n", "\n");

        List<CSVRecord> records;
        try (CSVParser parser = CSVFormat.DEFAULT.parse(new StringReader(text))) {
            records = parser.getRecords();
        } catch (IOException | UncheckedIOException | IllegalStateException e) {
            return CsvParseResult.fileError("CSV 형식을 읽을 수 없습니다. 큰따옴표가 닫히지 않은 칸이 있는지 확인해 주세요.");
        }

        List<String> header = records.isEmpty() ? List.of() : records.getFirst().stream().map(String::strip).toList();
        if (!header.equals(HEADER) && !header.equals(TYPED_HEADER)) {
            return CsvParseResult.fileError("첫 줄(헤더)은 " + String.join(",", HEADER)
                    + " 이어야 합니다. (주관식이 있으면 끝에 ,유형 을 붙이세요)");
        }

        List<QuestionRow> rows = new ArrayList<>();
        List<CsvError> errors = new ArrayList<>();
        Set<Integer> seenNumbers = new HashSet<>();
        for (int i = 1; i < records.size(); i++) {
            int rowNumber = i; // 헤더를 뺀 1부터의 순번
            List<String> rowErrors = new ArrayList<>();
            QuestionRow row = toRow(records.get(i), header.size(), subjectNumbers, seenNumbers, rowErrors);
            rowErrors.forEach(message -> errors.add(new CsvError(rowNumber, message)));
            if (rowErrors.isEmpty()) {
                rows.add(row);
            }
        }
        return new CsvParseResult(errors.isEmpty() ? rows : List.of(), errors);
    }

    // 잘못된 바이트를 대체 문자(�)로 바꾸지 않고 오류로 처리한다. 깨진 글자가 DB에 저장되는 것을 막는다
    private static String decodeUtf8(byte[] file) throws CharacterCodingException {
        int start = file.length >= 3 && (file[0] & 0xFF) == 0xEF && (file[1] & 0xFF) == 0xBB && (file[2] & 0xFF) == 0xBF
                ? 3 : 0; // UTF-8 BOM (엑셀의 "CSV UTF-8" 저장 시 붙는다)
        return StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(file, start, file.length - start))
                .toString();
    }

    private static QuestionRow toRow(CSVRecord record, int columns, Set<Integer> subjectNumbers,
                                     Set<Integer> seenNumbers, List<String> errors) {
        // 유형 헤더가 있어도 객관식 행은 예전처럼 9칸(유형 생략)으로 쓸 수 있다
        boolean typeOmitted = columns == TYPED_HEADER.size() && record.size() == HEADER.size();
        if (record.size() != columns && !typeOmitted) {
            errors.add("열이 " + columns + "개여야 합니다. (현재 " + record.size() + "개)");
            return null;
        }
        String typeCell = record.size() > HEADER.size() ? record.get(9).strip() : "";
        if (!List.of("", "객관식", "주관식", "주관식-정확히").contains(typeCell)) {
            errors.add("'유형'은 객관식, 주관식, 주관식-정확히 중 하나여야 합니다.");
            return null;
        }
        boolean shortAnswer = typeCell.startsWith("주관식");
        // 텍스트 칸은 strip하지 않는다: 코드 들여쓰기를 지키기 위해서다
        Integer subjectNumber = parseInt(record.get(0), "과목번호", errors);
        if (subjectNumber != null && !subjectNumbers.contains(subjectNumber)) {
            errors.add("등록되지 않은 과목번호입니다: " + subjectNumber);
        }
        Integer number = parseInt(record.get(1), "문제번호", errors);
        if (number != null && !seenNumbers.add(number)) {
            errors.add("문제번호 " + number + "이(가) 중복됩니다.");
        }
        if (record.get(2).isBlank()) {
            errors.add("'문제' 칸이 비어 있습니다.");
        }
        if (shortAnswer) {
            validateShortAnswer(record, errors);
        } else {
            validateChoices(record, errors);
        }
        for (int col : new int[] {2, 3, 4, 5, 6, 8}) {
            if (countFences(record.get(col)) % 2 != 0) {
                errors.add("'" + HEADER.get(col) + "' 칸의 ``` 가 닫히지 않았습니다.");
            }
        }
        if (!errors.isEmpty()) {
            return null;
        }
        String explanation = record.get(8).isBlank() ? null : record.get(8);
        if (shortAnswer) {
            return new QuestionRow(subjectNumber, number, record.get(2), List.of(), null, explanation,
                    QuestionType.SHORT_ANSWER, record.get(7).strip(), typeCell.equals("주관식-정확히"));
        }
        return new QuestionRow(subjectNumber, number, record.get(2),
                List.of(record.get(3), record.get(4), record.get(5), record.get(6)), Integer.valueOf(record.get(7).strip()),
                explanation, QuestionType.MULTIPLE_CHOICE, null, false);
    }

    private static void validateChoices(CSVRecord record, List<String> errors) {
        for (int col = 3; col <= 6; col++) {
            if (record.get(col).isBlank()) {
                errors.add("'" + HEADER.get(col) + "' 칸이 비어 있습니다.");
            }
        }
        Integer answerNo = parseInt(record.get(7), "정답번호", errors);
        if (answerNo != null && (answerNo < 1 || answerNo > 4)) {
            errors.add("정답번호는 1~4여야 합니다. (현재 " + answerNo + ")");
        }
    }

    private static void validateShortAnswer(CSVRecord record, List<String> errors) {
        for (int col = 3; col <= 6; col++) {
            if (!record.get(col).isBlank()) {
                errors.add("주관식 문제는 보기 칸을 비워 주세요.");
                break;
            }
        }
        // '|'는 허용 답 구분자다. limit -1로 끝의 빈 칸("A|")까지 잡는다
        String[] alternatives = record.get(7).strip().split("\\|", -1);
        if (Arrays.stream(alternatives).anyMatch(String::isBlank)) {
            errors.add("주관식 정답이 비어 있습니다.");
        }
    }

    private static Integer parseInt(String value, String column, List<String> errors) {
        try {
            return Integer.valueOf(value.strip());
        } catch (NumberFormatException e) {
            errors.add("'" + column + "'는 숫자여야 합니다. (현재 \"" + value + "\")");
            return null;
        }
    }

    private static int countFences(String value) {
        int count = 0;
        for (int i = value.indexOf(FENCE); i >= 0; i = value.indexOf(FENCE, i + FENCE.length())) {
            count++;
        }
        return count;
    }
}
