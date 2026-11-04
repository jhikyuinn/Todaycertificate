package com.todaycertificate.exam.pdf;

import com.todaycertificate.exam.QuestionType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PDF에서 뽑은 글자를 문제 행으로 나눈다. DB나 Spring에 의존하지 않는 순수 코드다.
 * 줄마다 과목 줄 / 문제 시작 줄 / 정답표 줄인지 보고, 나머지 줄은 지금 문제에 이어 붙인다.
 * "문제 N" 줄이 있는 실기(주관식) 형식은 {@link PdfShortAnswerParser}가 맡는다.
 */
public final class PdfQuestionParser {

    private static final Pattern SUBJECT = Pattern.compile("^제?(\\d)과목");
    private static final Pattern QUESTION = Pattern.compile("^(\\d{1,3})\\.\\s");
    private static final Pattern ANSWER_ENTRY = Pattern.compile("(\\d{1,3})\\.\\s*([①②③④])");
    private static final Pattern EXPLANATION_NUMBER = Pattern.compile("^(\\d{1,3})\\.");
    private static final Pattern AUTHOR = Pattern.compile("\\[해설작성자\\s*:[^\\]]*\\]");
    private static final String EXPLANATION_MARK = "<문제 해설>";
    private static final String CHOICE = "①②③④";
    private static final String ANSWER_CHOICE = "❶❷❸❹"; // 이 기호로 표시된 보기가 정답

    private record Block(int subjectNumber, int number, StringBuilder text) {
    }

    private PdfQuestionParser() {
    }

    public static List<PreviewRow> parse(String questionText, String explanationText) {
        if (PdfShortAnswerParser.matches(questionText)) {
            return PdfShortAnswerParser.parse(questionText); // 실기 형식은 정답·해설이 문제 바로 아래에 있다
        }
        List<Block> blocks = new ArrayList<>();
        Map<Integer, Integer> answerTable = new HashMap<>();
        int subject = 0;
        Block current = null;
        for (String raw : questionText.split("\\R")) {
            String line = raw.strip();
            if (line.isEmpty() || readAnswerTable(line, answerTable)) {
                continue;
            }
            Matcher s = SUBJECT.matcher(line);
            if (s.find()) {
                subject = Integer.parseInt(s.group(1));
                current = null;
                continue;
            }
            Matcher q = QUESTION.matcher(line);
            // 직전 번호 + 1일 때만 새 문제로 본다. 보기 안의 "1. ..." 같은 줄이 문제로 잘리지 않게.
            if (q.find() && (blocks.isEmpty() || Integer.parseInt(q.group(1)) == blocks.getLast().number() + 1)) {
                current = new Block(subject, Integer.parseInt(q.group(1)), new StringBuilder(line.substring(q.end())));
                blocks.add(current);
            } else if (current != null) {
                current.text().append(' ').append(line); // 줄바꿈으로 끊긴 문장은 공백으로 잇는다
            }
        }

        Map<Integer, String> explanations = explanations(explanationText);
        return blocks.stream().map(b -> toRow(b, answerTable, explanations)).toList();
    }

    private static PreviewRow toRow(Block block, Map<Integer, Integer> answerTable, Map<Integer, String> explanations) {
        StringBuilder content = new StringBuilder();
        List<StringBuilder> choices = new ArrayList<>();
        Integer answerNo = null;
        for (char c : block.text().toString().toCharArray()) {
            int plain = CHOICE.indexOf(c);
            int filled = ANSWER_CHOICE.indexOf(c);
            if (plain >= 0 || filled >= 0) {
                choices.add(new StringBuilder());
                if (filled >= 0) {
                    answerNo = filled + 1;
                }
            } else {
                (choices.isEmpty() ? content : choices.getLast()).append(c);
            }
        }
        if (answerNo == null) {
            answerNo = answerTable.get(block.number());
        }

        List<String> warnings = new ArrayList<>();
        if (choices.size() != 4) {
            warnings.add("보기를 4개 찾지 못했어요 (" + choices.size() + "개)");
        }
        if (answerNo == null) {
            warnings.add("정답을 찾지 못했어요");
        }
        if (block.subjectNumber() == 0) {
            warnings.add("과목을 찾지 못했어요");
        }
        return new PreviewRow(block.subjectNumber(), block.number(), QuestionType.MULTIPLE_CHOICE, clean(content),
                choices.stream().map(PdfQuestionParser::clean).toList(),
                answerNo, null, explanations.get(block.number()), warnings);
    }

    /** "1. ② 2. ③"처럼 정답 항목만 있는 줄이면 정답표에 담고 true. */
    private static boolean readAnswerTable(String line, Map<Integer, Integer> answerTable) {
        Matcher m = ANSWER_ENTRY.matcher(line);
        Map<Integer, Integer> found = new HashMap<>();
        while (m.find()) {
            found.put(Integer.parseInt(m.group(1)), CHOICE.indexOf(m.group(2).charAt(0)) + 1);
        }
        if (found.isEmpty() || !m.replaceAll("").isBlank()) {
            return false;
        }
        answerTable.putAll(found);
        return true;
    }

    /** 문제 번호 → 해설. 번호 줄 뒤의 <문제 해설>부터 다음 문제 전까지, 여러 개면 빈 줄로 잇는다. */
    private static Map<Integer, String> explanations(String text) {
        if (text == null) {
            return Map.of();
        }
        Map<Integer, List<StringBuilder>> sections = new HashMap<>();
        int number = 0;
        StringBuilder section = null;
        for (String raw : text.split("\\R")) {
            String line = raw.strip();
            Matcher n = EXPLANATION_NUMBER.matcher(line);
            int mark = line.indexOf(EXPLANATION_MARK);
            // 번호가 커질 때만 다음 문제로 본다. 해설 안의 "1. ..." 번호 목록은 해설로 남긴다.
            if (n.find() && Integer.parseInt(n.group(1)) > number) {
                number = Integer.parseInt(n.group(1));
                section = null;
            } else if (mark >= 0) {
                section = new StringBuilder(line.substring(mark + EXPLANATION_MARK.length()));
                sections.computeIfAbsent(number, k -> new ArrayList<>()).add(section);
            } else if (section != null) {
                section.append(' ').append(line);
            }
        }

        Map<Integer, String> result = new HashMap<>();
        sections.forEach((no, list) -> {
            List<String> cleaned = list.stream()
                    .map(sb -> clean(AUTHOR.matcher(sb).replaceAll("")))
                    .filter(s -> !s.isEmpty())
                    .toList();
            if (!cleaned.isEmpty()) {
                result.put(no, String.join("\n\n", cleaned));
            }
        });
        return result;
    }

    static String clean(CharSequence text) {
        return text.toString().replaceAll("\\s+", " ").strip();
    }
}
