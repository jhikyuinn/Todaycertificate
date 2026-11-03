package com.todaycertificate.study;

import com.todaycertificate.common.CurrentMember;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StudyController {

    private final SubjectReportService reports;

    public StudyController(SubjectReportService reports) {
        this.reports = reports;
    }

    @GetMapping("/api/me/subject-report")
    List<SubjectReportService.SubjectReport> subjectReport(Authentication auth) {
        return reports.report(CurrentMember.id(auth));
    }
}
