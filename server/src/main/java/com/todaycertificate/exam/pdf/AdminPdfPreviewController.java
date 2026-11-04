package com.todaycertificate.exam.pdf;

import com.todaycertificate.certificate.CertificateRepository;
import com.todaycertificate.certificate.Subject;
import com.todaycertificate.certificate.SubjectRepository;
import com.todaycertificate.common.ApiException;
import com.todaycertificate.common.ErrorCode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** 운영자 전용 (/api/admin/**). PDF를 문제 행으로 바꿔 보여주기만 하고 아무것도 저장하지 않는다. */
@RestController
public class AdminPdfPreviewController {

    private final CertificateRepository certificates;
    private final SubjectRepository subjects;

    public AdminPdfPreviewController(CertificateRepository certificates, SubjectRepository subjects) {
        this.certificates = certificates;
        this.subjects = subjects;
    }

    public record PreviewResponse(List<PreviewRow> rows) {
    }

    @PostMapping("/api/admin/certificates/{id}/exams/pdf-preview")
    PreviewResponse preview(@PathVariable Long id, @RequestParam MultipartFile file,
                            @RequestParam(required = false) MultipartFile explanationFile) throws IOException {
        if (!certificates.existsById(id)) {
            throw new ApiException(ErrorCode.NOT_FOUND);
        }
        Set<Integer> known = subjects.findByCertificateIdOrderByNumber(id).stream()
                .map(Subject::getNumber).collect(Collectors.toSet());
        String explanations = explanationFile == null ? null : PdfTextExtractor.extract(explanationFile.getBytes());

        List<PreviewRow> rows = PdfQuestionParser.parse(PdfTextExtractor.extract(file.getBytes()), explanations)
                .stream().map(row -> withSubjectCheck(row, known)).toList();
        return new PreviewResponse(rows);
    }

    /** 과목번호 0은 파서가 이미 "과목을 찾지 못했어요"를 붙였으므로 건너뛴다. */
    private static PreviewRow withSubjectCheck(PreviewRow row, Set<Integer> known) {
        if (row.subjectNumber() == 0 || known.contains(row.subjectNumber())) {
            return row;
        }
        List<String> warnings = new ArrayList<>(row.warnings());
        warnings.add("등록되지 않은 과목번호입니다: " + row.subjectNumber());
        return new PreviewRow(row.subjectNumber(), row.number(), row.content(), row.choices(),
                row.answerNo(), row.explanation(), warnings);
    }
}
