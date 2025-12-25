package com.ruangong.repository;

import com.ruangong.entity.ExamResultImportItemEntity;
import com.ruangong.entity.ExamResultImportItemStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamResultImportItemRepository extends JpaRepository<ExamResultImportItemEntity, Long> {

    List<ExamResultImportItemEntity> findByJob_Id(Long jobId);

    List<ExamResultImportItemEntity> findByJob_IdAndStatus(Long jobId, ExamResultImportItemStatus status);
}
