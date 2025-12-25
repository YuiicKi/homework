package com.ruangong.repository;

import com.ruangong.entity.ExamResultReleaseSettingEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamResultReleaseSettingRepository extends JpaRepository<ExamResultReleaseSettingEntity, Long> {

    Optional<ExamResultReleaseSettingEntity> findBySubject_IdAndExamYear(Long subjectId, Integer examYear);

    List<ExamResultReleaseSettingEntity> findBySubject_Id(Long subjectId);

    List<ExamResultReleaseSettingEntity> findByExamYear(Integer examYear);
}
