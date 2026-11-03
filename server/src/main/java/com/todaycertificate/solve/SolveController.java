package com.todaycertificate.solve;

import com.todaycertificate.common.CurrentMember;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
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

    record SolveRequest(@NotNull @Min(1) @Max(4) Integer selectedNo) {
    }

    @PostMapping("/api/questions/{id}/solve")
    SolveService.SolveResult solve(@PathVariable Long id, @Valid @RequestBody SolveRequest request,
                                   Authentication auth) {
        return service.solveInStudy(CurrentMember.id(auth), id, request.selectedNo());
    }
}
