package com.todaycertificate.solve;

import com.todaycertificate.common.CurrentMember;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SolveController {

    private final SolveService service;

    public SolveController(SolveService service) {
        this.service = service;
    }

    // 객관식은 selectedNo, 주관식은 answerText 하나만. 유형은 문제를 읽어야 알 수 있어서 SolveService가 검사한다
    record SolveRequest(@Min(1) @Max(4) Integer selectedNo, @Size(max = 500) String answerText) {
    }

    @PostMapping("/api/questions/{id}/solve")
    SolveService.SolveResult solve(@PathVariable Long id, @Valid @RequestBody SolveRequest request,
                                   Authentication auth) {
        return service.solveInStudy(CurrentMember.id(auth), id, request.selectedNo(), request.answerText());
    }
}
