package com.todaycertificate.certificate;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record ScheduleRequest(
        @NotNull @Min(2000) @Max(2100) Integer year,
        @NotNull @Min(1) @Max(10) Integer round,
        @NotNull LocalDate registrationStart,
        @NotNull LocalDate registrationEnd,
        @NotNull LocalDate examDate) {
}
