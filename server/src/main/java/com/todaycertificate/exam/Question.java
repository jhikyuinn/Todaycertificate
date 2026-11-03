package com.todaycertificate.exam;

import com.todaycertificate.certificate.Subject;
import jakarta.persistence.Entity;
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

    private int number;
    private String content; // 코드는 ``` 로 감싸며, 줄바꿈과 공백을 그대로 저장한다
    private String choice1;
    private String choice2;
    private String choice3;
    private String choice4;
    private int answerNo;   // 1~4
    private String explanation;

    protected Question() {
    }

    public Long getId() { return id; }
    public Exam getExam() { return exam; }
    public Subject getSubject() { return subject; }
    public int getNumber() { return number; }
    public String getContent() { return content; }
    public List<String> getChoices() { return List.of(choice1, choice2, choice3, choice4); }
    public int getAnswerNo() { return answerNo; }
    public String getExplanation() { return explanation; }

    public boolean isCorrect(int selectedNo) {
        return answerNo == selectedNo;
    }
}
