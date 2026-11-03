package com.todaycertificate.certificate;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CertificateRepository extends JpaRepository<Certificate, Long> {

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);
}
