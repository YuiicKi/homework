package com.ruangong.repository;

import com.ruangong.entity.ExamRegistrationWindowEntity;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamRegistrationWindowRepository extends JpaRepository<ExamRegistrationWindowEntity, Long> {

    List<ExamRegistrationWindowEntity> findBySubjectId(Long subjectId);

    List<ExamRegistrationWindowEntity> findBySubjectIdIn(Collection<Long> subjectIds);
}
