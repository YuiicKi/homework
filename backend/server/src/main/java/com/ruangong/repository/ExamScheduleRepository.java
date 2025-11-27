package com.ruangong.repository;

import com.ruangong.entity.ExamScheduleEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamScheduleRepository extends JpaRepository<ExamScheduleEntity, Long> {

    boolean existsByExamRoomIdAndExamSessionId(Long examRoomId, Long examSessionId);

    List<ExamScheduleEntity> findByExamRoomId(Long examRoomId);

    List<ExamScheduleEntity> findByExamSessionId(Long examSessionId);

    List<ExamScheduleEntity> findByExamSubjectId(Long examSubjectId);
}
