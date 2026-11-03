package com.todaycertificate.certificate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** 운영자의 자격증 생성·수정 요청. 숫자는 Integer로 받아야 빠진 값을 @NotNull로 잡을 수 있다 (int는 0이 된다). */
public record CertificateRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 2000) String description,
        @NotNull @Min(1) @Max(600) Integer timeLimitMinutes,
        @NotNull @Min(0) @Max(100) Integer failScore,
        @NotNull @Min(0) @Max(100) Integer passAverage,
        @NotEmpty @Valid List<SubjectRequest> subjects) {

    public record SubjectRequest(@NotNull @Min(1) Integer number, @NotBlank @Size(max = 100) String name) {
    }
}
