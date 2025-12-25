package com.ruangong.repository;

import com.ruangong.entity.ExamInvigilatorAssignmentEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamInvigilatorAssignmentRepository extends JpaRepository<ExamInvigilatorAssignmentEntity, Long> {

    boolean existsBySchedule_IdAndTeacher_Id(Long scheduleId, Long teacherId);

    List<ExamInvigilatorAssignmentEntity> findBySchedule_Id(Long scheduleId);

    List<ExamInvigilatorAssignmentEntity> findByTeacher_Id(Long teacherId);

    List<ExamInvigilatorAssignmentEntity> findBySchedule_IdIn(List<Long> scheduleIds);
}
