package com.todaycertificate.certificate;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubjectRepository extends JpaRepository<Subject, Long> {

    List<Subject> findByCertificateIdOrderByNumber(Long certificateId);
}
