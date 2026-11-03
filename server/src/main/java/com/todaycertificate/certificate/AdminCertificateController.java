package com.todaycertificate.certificate;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 운영자 전용. /api/admin/** 은 SecurityConfig에서 ADMIN만 통과시킨다. */
@RestController
@RequestMapping("/api/admin")
public class AdminCertificateController {

    private final AdminCertificateService service;

    public AdminCertificateController(AdminCertificateService service) {
        this.service = service;
    }

    record IdResponse(Long id) {
    }

    @PostMapping("/certificates")
    @ResponseStatus(HttpStatus.CREATED)
    IdResponse create(@Valid @RequestBody CertificateRequest request) {
        return new IdResponse(service.create(request));
    }

    @PutMapping("/certificates/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void update(@PathVariable Long id, @Valid @RequestBody CertificateRequest request) {
        service.update(id, request);
    }

    @PostMapping("/certificates/{id}/schedules")
    @ResponseStatus(HttpStatus.CREATED)
    IdResponse addSchedule(@PathVariable Long id, @Valid @RequestBody ScheduleRequest request) {
        return new IdResponse(service.addSchedule(id, request));
    }

    @DeleteMapping("/schedules/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteSchedule(@PathVariable Long id) {
        service.deleteSchedule(id);
    }
}
