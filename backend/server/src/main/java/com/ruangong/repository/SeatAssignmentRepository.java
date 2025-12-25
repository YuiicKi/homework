package com.ruangong.repository;

import com.ruangong.entity.SeatAssignmentEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeatAssignmentRepository extends JpaRepository<SeatAssignmentEntity, Long> {

    List<SeatAssignmentEntity> findBySubjectIdAndSessionId(Long subjectId, Long sessionId);

    List<SeatAssignmentEntity> findByRoomId(Long roomId);

    List<SeatAssignmentEntity> findByRegistrationInfoId(Long registrationInfoId);

    void deleteBySubjectIdAndSessionId(Long subjectId, Long sessionId);
}
