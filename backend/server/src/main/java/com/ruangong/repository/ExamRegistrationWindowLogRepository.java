package com.ruangong.repository;

import com.ruangong.entity.ExamRegistrationWindowLogEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamRegistrationWindowLogRepository extends JpaRepository<ExamRegistrationWindowLogEntity, Long> {

    List<ExamRegistrationWindowLogEntity> findByRegistrationWindowIdOrderByCreatedAtDesc(Long registrationWindowId);
}
