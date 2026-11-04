package com.todaycertificate.exam.pdf;

import com.todaycertificate.exam.QuestionType;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 실기(주관식) PDF 글자를 문제 행으로 나눈다. 순수 코드다.
 * 형식: "문제 N" → 태그 줄(예: 26-1) → 본문 → "정답 및 해설" → 정답 → "[해설]" → 해설.
 * 브라우저에서 인쇄한 PDF라 머리글(날짜 + 문서 제목)과 바닥글(주소, 쪽수)을 지운다.
 */
final class PdfShortAnswerParser {

    private static final Pattern QUESTION = Pattern.compile("^문제\\s*(\\d{1,3})$");
    private static final Pattern TAG = Pattern.compile("^\\S+-\\d+$");
    private static final Pattern PRINT_HEADER = Pattern.compile("^\\d{2}\\. \\d{1,2}\\. \\d{1,2}\\. (오전|오후) \\d{1,2}:\\d{2}(.*)$");
    private static final Pattern PRINT_FOOTER = Pattern.compile("^((https?|file)://\\S+)?\\s*(\\d+/\\d+)?$");
    private static final String ANSWER_MARK = "정답 및 해설";
    private static final String EXPLANATION_MARK = "[해설]";
    private static final int SUBJECT = 1; // 실기 자격증은 과목이 하나다 (PDF에 과목 줄이 없다)

    private enum Part { CONTENT, ANSWER, EXPLANATION }

    private record Block(int number, List<String> content, List<String> answer, List<String> explanation) {
    }

    private PdfShortAnswerParser() {
    }

    static boolean matches(String text) {
        return text.lines().anyMatch(line -> QUESTION.matcher(line.strip()).matches());
    }

    static List<PreviewRow> parse(String text) {
        List<String> lines = withoutPrintNoise(text);
        List<Block> blocks = new ArrayList<>();
        Block current = null;
        Part part = Part.CONTENT;
        boolean tagAllowed = false;
        for (String line : lines) {
            String stripped = line.strip();
            Matcher q = QUESTION.matcher(stripped);
            if (q.matches()) {
                current = new Block(Integer.parseInt(q.group(1)), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
                blocks.add(current);
                part = Part.CONTENT;
                tagAllowed = true;
                continue;
            }
            if (current == null || stripped.isEmpty()) {
                continue;
            }
            if (tagAllowed && TAG.matcher(stripped).matches()) { // 문제 바로 아래의 회차 태그
                tagAllowed = false;
                continue;
            }
            tagAllowed = false;
            // 표시 줄 뒤에 같은 높이의 다른 글자(워터마크 등)가 붙어 나올 수 있어서 startsWith로 본다
            if (stripped.startsWith(ANSWER_MARK)) {
                part = Part.ANSWER;
            } else if (stripped.startsWith(EXPLANATION_MARK)) {
                part = Part.EXPLANATION;
            } else {
                (part == Part.CONTENT ? current.content() : part == Part.ANSWER ? current.answer() : current.explanation())
                        .add(line);
            }
        }
        return blocks.stream().map(PdfShortAnswerParser::toRow).toList();
    }

    private static PreviewRow toRow(Block block) {
        String answer = PdfQuestionParser.clean(String.join(" ", block.answer()));
        List<String> warnings = new ArrayList<>();
        if (answer.isEmpty()) {
            warnings.add("정답을 찾지 못했어요");
        }
        String explanation = render(block.explanation());
        return new PreviewRow(SUBJECT, block.number(), QuestionType.SHORT_ANSWER, render(block.content()), List.of(),
                null, answer.isEmpty() ? null : answer, explanation.isEmpty() ? null : explanation, warnings);
    }

    /** 머리글(날짜 + 문서 제목), 문서 제목만 있는 줄, 바닥글(주소·쪽수)을 지운다. */
    private static List<String> withoutPrintNoise(String text) {
        List<String> lines = text.lines().toList();
        Set<String> titles = new HashSet<>();
        for (String line : lines) {
            Matcher h = PRINT_HEADER.matcher(line.strip());
            if (h.matches() && !h.group(2).isBlank()) {
                titles.add(h.group(2).strip());
            }
        }
        return lines.stream().filter(line -> {
            String s = line.strip();
            return s.isEmpty() || !(PRINT_HEADER.matcher(s).matches() || titles.contains(s) || PRINT_FOOTER.matcher(s).matches());
        }).toList();
    }

    /**
     * 글 줄은 공백으로 잇고(PDF가 폭에 맞춰 끊은 줄), "•" 줄은 새 줄로 둔다.
     * 코드처럼 보이는 줄이 2줄 이상 이어지면 줄바꿈·들여쓰기를 살려 ``` 로 감싼다 (화면의 RichText가 고정폭으로 그린다).
     */
    static String render(List<String> lines) {
        List<String> out = new ArrayList<>();
        StringBuilder prose = new StringBuilder();
        int i = 0;
        while (i < lines.size()) {
            int end = i;
            while (end < lines.size() && looksLikeCode(lines.get(end))) {
                end++;
            }
            if (end - i >= 2) {
                flush(prose, out);
                out.add("```\n" + String.join("\n", dedent(lines.subList(i, end))) + "\n```");
                i = end;
                continue;
            }
            String s = lines.get(i).strip();
            if (s.startsWith("•")) {
                flush(prose, out);
            }
            prose.append(' ').append(s);
            i++;
        }
        flush(prose, out);
        return String.join("\n", out);
    }

    /**
     * 들여쓴 줄이거나, 공백을 뺀 글자의 80% 이상이 ASCII면 코드로 본다. "•" 목록은 글로 둔다.
     * shortcut: 한글 주석이 많은 코드 줄은 글로 본다. 그런 문제가 많으면 PDF 글꼴(고정폭)로 판단하도록 바꾼다.
     */
    private static boolean looksLikeCode(String line) {
        String s = line.strip();
        if (s.isEmpty() || s.startsWith("•")) {
            return false;
        }
        if (Character.isWhitespace(line.charAt(0))) {
            return true;
        }
        String compact = s.replaceAll("\\s", "");
        long ascii = compact.chars().filter(c -> c < 128).count();
        return ascii >= compact.length() * 0.8;
    }

    private static List<String> dedent(List<String> lines) {
        int min = lines.stream().mapToInt(l -> l.length() - l.stripLeading().length()).min().orElse(0);
        return lines.stream().map(l -> l.substring(min).stripTrailing()).toList();
    }

    private static void flush(StringBuilder prose, List<String> out) {
        String text = PdfQuestionParser.clean(prose);
        if (!text.isEmpty()) {
            out.add(text);
        }
        prose.setLength(0);
    }
}
