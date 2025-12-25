package com.ruangong.repository;

import com.ruangong.entity.ExamResultImportJobEntity;
import com.ruangong.entity.ExamResultImportJobStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamResultImportJobRepository extends JpaRepository<ExamResultImportJobEntity, Long> {

    List<ExamResultImportJobEntity> findByStatusOrderByCreatedAtDesc(ExamResultImportJobStatus status);

    List<ExamResultImportJobEntity> findAllByOrderByCreatedAtDesc();
}
