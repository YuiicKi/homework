package com.ruangong.repository;

import com.ruangong.entity.ExamRoomStatusLogEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamRoomStatusLogRepository extends JpaRepository<ExamRoomStatusLogEntity, Long> {

    List<ExamRoomStatusLogEntity> findByExamRoomIdOrderByCreatedAtDesc(Long examRoomId);
}
