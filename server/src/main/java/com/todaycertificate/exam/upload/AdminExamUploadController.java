package com.todaycertificate.exam.upload;

import java.io.IOException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** 운영자 전용 (/api/admin/**). multipart/form-data로 year, round, file을 받는다. */
@RestController
public class AdminExamUploadController {

    private final ExamUploadService service;

    public AdminExamUploadController(ExamUploadService service) {
        this.service = service;
    }

    @PostMapping("/api/admin/certificates/{id}/exams")
    ExamUploadService.UploadResult upload(@PathVariable Long id, @RequestParam int year, @RequestParam int round,
                                          @RequestParam MultipartFile file) throws IOException {
        return service.upload(id, year, round, file.getBytes());
    }
}
