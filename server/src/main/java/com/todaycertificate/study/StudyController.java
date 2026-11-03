package com.todaycertificate.study;

import com.todaycertificate.common.CurrentMember;
import com.todaycertificate.exam.QuestionView;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StudyController {

    private final SubjectReportService reports;
    private final DailyProblemService daily;

    public StudyController(SubjectReportService reports, DailyProblemService daily) {
        this.reports = reports;
        this.daily = daily;
    }

    @GetMapping("/api/me/daily-problems")
    List<QuestionView> dailyProblems(Authentication auth) {
        return daily.dailyProblems(CurrentMember.id(auth));
    }

    @GetMapping("/api/me/subject-report")
    List<SubjectReportService.SubjectReport> subjectReport(Authentication auth) {
        return reports.report(CurrentMember.id(auth));
    }
}
