package com.todaycertificate.exam;

import com.todaycertificate.common.ApiException;
import com.todaycertificate.common.CurrentMember;
import com.todaycertificate.common.ErrorCode;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ExamController {

    private final ExamRepository exams;
    private final QuestionRepository questions;
    private final QuestionViewAssembler assembler;

    public ExamController(ExamRepository exams, QuestionRepository questions, QuestionViewAssembler assembler) {
        this.exams = exams;
        this.questions = questions;
        this.assembler = assembler;
    }

    /** 공부 모드 문제 목록. 정답은 문제를 푼 뒤 solve 응답으로만 알려준다. */
    @GetMapping("/api/exams/{examId}/questions")
    List<QuestionView> questions(@PathVariable Long examId, Authentication auth) {
        if (!exams.existsById(examId)) {
            throw new ApiException(ErrorCode.NOT_FOUND);
        }
        return assembler.toViews(CurrentMember.id(auth), questions.findWithSubjectByExamId(examId));
    }
}
