package com.todaycertificate.attempt;

import com.todaycertificate.common.CurrentMember;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AttemptController {

    private final AttemptService service;

    public AttemptController(AttemptService service) {
        this.service = service;
    }

    record AnswerRequest(@NotNull Long questionId, @NotNull @Min(1) @Max(4) Integer selectedNo) {
    }

    // 안 푼 문제는 answers에서 빠진다. 전부 안 풀고 제출하는 것도 허용 (빈 목록)
    record SubmitRequest(@NotNull List<@Valid AnswerRequest> answers) {
    }

    @PostMapping("/api/exams/{examId}/attempts")
    @ResponseStatus(HttpStatus.CREATED)
    AttemptService.StartResult start(@PathVariable Long examId, Authentication auth) {
        return service.start(CurrentMember.id(auth), examId);
    }

    @PostMapping("/api/attempts/{id}/submit")
    AttemptService.AttemptResult submit(@PathVariable Long id, @Valid @RequestBody SubmitRequest request,
                                        Authentication auth) {
        return service.submit(CurrentMember.id(auth), id, request.answers().stream()
                .map(a -> new AttemptService.Answer(a.questionId(), a.selectedNo())).toList());
    }

    @GetMapping("/api/attempts/{id}")
    AttemptService.AttemptResult result(@PathVariable Long id, Authentication auth) {
        return service.result(CurrentMember.id(auth), id);
    }
}
