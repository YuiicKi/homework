package com.ruangong.repository;

import com.ruangong.entity.ExamSubjectLogEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamSubjectLogRepository extends JpaRepository<ExamSubjectLogEntity, Long> {

    List<ExamSubjectLogEntity> findByExamSubjectIdOrderByCreatedAtDesc(Long subjectId);
}
