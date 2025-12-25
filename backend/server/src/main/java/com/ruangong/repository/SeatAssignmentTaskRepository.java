package com.ruangong.repository;

import com.ruangong.entity.SeatAssignmentTaskEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeatAssignmentTaskRepository extends JpaRepository<SeatAssignmentTaskEntity, Long> {

    List<SeatAssignmentTaskEntity> findBySubjectIdAndSessionIdOrderByCreatedAtDesc(Long subjectId, Long sessionId);
}
