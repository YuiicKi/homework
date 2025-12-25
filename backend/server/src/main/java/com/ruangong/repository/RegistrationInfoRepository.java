package com.ruangong.repository;

import com.ruangong.entity.RegistrationInfoEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegistrationInfoRepository extends JpaRepository<RegistrationInfoEntity, Long> {

    Optional<RegistrationInfoEntity> findByUserIdAndSubjectId(Long userId, Long subjectId);

    Optional<RegistrationInfoEntity> findByUserIdAndSubjectIdAndSessionId(Long userId, Long subjectId, Long sessionId);

    List<RegistrationInfoEntity> findByUserId(Long userId);

    List<RegistrationInfoEntity> findBySubjectId(Long subjectId);

    List<RegistrationInfoEntity> findBySubjectIdAndStatus(Long subjectId, String status);

    List<RegistrationInfoEntity> findByStatusIn(List<String> statuses);
}
