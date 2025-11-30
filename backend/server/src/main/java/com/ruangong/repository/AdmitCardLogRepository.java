package com.ruangong.repository;

import com.ruangong.entity.AdmitCardLogEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdmitCardLogRepository extends JpaRepository<AdmitCardLogEntity, Long> {

    List<AdmitCardLogEntity> findByRegistrationInfoIdOrderByCreatedAtDesc(Long registrationInfoId);

    List<AdmitCardLogEntity> findByTicketNumberOrderByCreatedAtDesc(String ticketNumber);
}
