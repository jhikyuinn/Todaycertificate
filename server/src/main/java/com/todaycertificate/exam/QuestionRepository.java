package com.todaycertificate.exam;

import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    boolean existsBySubjectIdIn(Collection<Long> subjectIds);
}
