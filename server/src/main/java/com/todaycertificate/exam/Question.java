package com.todaycertificate.exam;

import com.todaycertificate.certificate.Subject;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import java.util.List;

@Entity
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Exam exam;

    @ManyToOne(fetch = FetchType.LAZY)
    private Subject subject;

    @Enumerated(EnumType.STRING)
    private QuestionType type;

    private int number;
    private String content; // 코드는 ``` 로 감싸며, 줄바꿈과 공백을 그대로 저장한다
    private String choice1;
    private String choice2;
    private String choice3;
    private String choice4;
    private Integer answerNo;   // 객관식만 1~4
    private String answerText;  // 주관식만. '|'로 구분한 허용 답
    private boolean exactMatch; // 주관식만. true면 대소문자·안쪽 공백까지 그대로 비교
    private String explanation;

    protected Question() {
    }

    public Question(Exam exam, Subject subject, int number, String content, List<String> choices,
                    int answerNo, String explanation) {
        this.exam = exam;
        this.number = number;
        update(subject, content, choices, answerNo, explanation);
    }

    public Question(Exam exam, Subject subject, int number, String content, String answerText,
                    boolean exactMatch, String explanation) {
        this.exam = exam;
        this.number = number;
        updateShortAnswer(subject, content, answerText, exactMatch, explanation);
    }

    /** 같은 회차를 다시 업로드할 때 문제번호는 그대로 두고 내용만 바꾼다 (풀이 기록이 이 문제를 계속 가리키도록). */
    public void update(Subject subject, String content, List<String> choices, int answerNo, String explanation) {
        this.type = QuestionType.MULTIPLE_CHOICE;
        this.subject = subject;
        this.content = content;
        this.answerText = null;
        this.exactMatch = false;
        this.choice1 = choices.get(0);
        this.choice2 = choices.get(1);
        this.choice3 = choices.get(2);
        this.choice4 = choices.get(3);
        this.answerNo = answerNo;
        this.explanation = explanation;
    }

    /** 다시 업로드할 때 객관식이던 문제가 주관식으로 바뀔 수도 있어서 보기와 정답번호를 비운다. */
    public void updateShortAnswer(Subject subject, String content, String answerText, boolean exactMatch,
                                  String explanation) {
        this.type = QuestionType.SHORT_ANSWER;
        this.subject = subject;
        this.content = content;
        this.choice1 = null;
        this.choice2 = null;
        this.choice3 = null;
        this.choice4 = null;
        this.answerNo = null;
        this.answerText = answerText;
        this.exactMatch = exactMatch;
        this.explanation = explanation;
    }

    public Long getId() { return id; }
    public Exam getExam() { return exam; }
    public Subject getSubject() { return subject; }
    public QuestionType getType() { return type; }
    public int getNumber() { return number; }
    public String getContent() { return content; }
    public List<String> getChoices() {
        return type == QuestionType.SHORT_ANSWER ? List.of() : List.of(choice1, choice2, choice3, choice4);
    }
    public Integer getAnswerNo() { return answerNo; }
    public String getAnswerText() { return answerText; }
    public boolean isExactMatch() { return exactMatch; }
    public String getExplanation() { return explanation; }

    public boolean isCorrect(int selectedNo) {
        return answerNo != null && answerNo == selectedNo;
    }

    public boolean isCorrect(String input) {
        return type == QuestionType.SHORT_ANSWER && ShortAnswerGrader.isCorrect(answerText, exactMatch, input);
    }
}
