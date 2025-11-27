package com.ruangong.repository;

import com.ruangong.entity.RegistrationAuditLogEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegistrationAuditLogRepository extends JpaRepository<RegistrationAuditLogEntity, Long> {

    List<RegistrationAuditLogEntity> findByRegistrationInfoIdOrderByCreatedAtDesc(Long registrationInfoId);
}
