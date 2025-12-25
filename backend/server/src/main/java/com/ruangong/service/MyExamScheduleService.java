package com.ruangong.service;

import com.ruangong.entity.ExamSessionEntity;
import com.ruangong.entity.SeatAssignmentEntity;
import com.ruangong.model.MyExamScheduleModel;
import com.ruangong.repository.SeatAssignmentRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MyExamScheduleService {

    private final SeatAssignmentRepository seatAssignmentRepository;

    public MyExamScheduleService(SeatAssignmentRepository seatAssignmentRepository) {
        this.seatAssignmentRepository = seatAssignmentRepository;
    }

    public List<MyExamScheduleModel> list(Long userId, Long subjectId) {
        List<SeatAssignmentEntity> assignments = seatAssignmentRepository.findAll().stream()
            .filter(sa -> sa.getRegistrationInfo() != null
                && sa.getRegistrationInfo().getUser() != null
                && sa.getRegistrationInfo().getUser().getId().equals(userId))
            .filter(sa -> subjectId == null || (sa.getSubject() != null && sa.getSubject().getId().equals(subjectId)))
            .toList();
        return assignments.stream()
            .map(this::map)
            .toList();
    }

    private MyExamScheduleModel map(SeatAssignmentEntity entity) {
        ExamSessionEntity session = entity.getSession();
        return new MyExamScheduleModel(
            entity.getRegistrationInfo().getId(),
            entity.getSubject() != null ? entity.getSubject().getId() : null,
            entity.getSubject() != null ? entity.getSubject().getName() : null,
            session != null ? session.getId() : null,
            session != null ? session.getName() : null,
            session != null && session.getStartTime() != null ? session.getStartTime().toString() : null,
            session != null && session.getEndTime() != null ? session.getEndTime().toString() : null,
            entity.getRoom() != null && entity.getRoom().getCenter() != null ? entity.getRoom().getCenter().getName() : null,
            entity.getRoom() != null ? entity.getRoom().getName() : null,
            entity.getRoom() != null ? entity.getRoom().getRoomNumber() : null,
            entity.getSeatNumber(),
            entity.getTicketNumber()
        );
    }
}
