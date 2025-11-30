package com.ruangong.service;

import com.ruangong.entity.ExamCenterEntity;
import com.ruangong.entity.ExamRoomEntity;
import com.ruangong.entity.ExamRoomStatus;
import com.ruangong.entity.ExamScheduleEntity;
import com.ruangong.entity.ExamScheduleStatus;
import com.ruangong.entity.ExamSessionEntity;
import com.ruangong.entity.ExamSubjectEntity;
import com.ruangong.model.ExamCenterModel;
import com.ruangong.model.ExamRoomModel;
import com.ruangong.model.ExamScheduleModel;
import com.ruangong.model.ExamSessionModel;
import com.ruangong.model.ExamSubjectModel;
import com.ruangong.model.input.ExamScheduleInput;
import com.ruangong.model.input.UpdateExamScheduleStatusInput;
import com.ruangong.repository.ExamScheduleRepository;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class ExamScheduleService {

    private final ExamScheduleRepository examScheduleRepository;
    private final ExamRoomService examRoomService;
    private final ExamSubjectService examSubjectService;
    private final ExamSessionService examSessionService;

    public ExamScheduleService(
        ExamScheduleRepository examScheduleRepository,
        ExamRoomService examRoomService,
        ExamSubjectService examSubjectService,
        ExamSessionService examSessionService
    ) {
        this.examScheduleRepository = examScheduleRepository;
        this.examRoomService = examRoomService;
        this.examSubjectService = examSubjectService;
        this.examSessionService = examSessionService;
    }

    public ExamScheduleModel createSchedule(ExamScheduleInput input) {
        ExamRoomEntity room = examRoomService.getRoomEntity(input.getRoomId());
        ensureRoomAvailable(room);
        ExamSubjectEntity subject = examSubjectService.getSubjectEntity(input.getSubjectId());
        ExamSessionEntity session = examSessionService.getSessionEntity(input.getSessionId());
        examSubjectService.ensureSubjectEnabledOrThrow(subject);
        ensureUnique(room.getId(), session.getId(), null);

        ExamScheduleEntity entity = new ExamScheduleEntity();
        entity.setExamRoom(room);
        entity.setExamSubject(subject);
        entity.setExamSession(session);
        entity.setNote(normalize(input.getNote()));
        entity = examScheduleRepository.save(entity);
        return mapSchedule(entity);
    }

    public ExamScheduleModel updateSchedule(Long id, ExamScheduleInput input) {
        ExamScheduleEntity entity = examScheduleRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("排考记录不存在"));

        ExamRoomEntity room = examRoomService.getRoomEntity(input.getRoomId());
        ExamSubjectEntity subject = examSubjectService.getSubjectEntity(input.getSubjectId());
        ExamSessionEntity session = examSessionService.getSessionEntity(input.getSessionId());

        ensureRoomAvailable(room);
        examSubjectService.ensureSubjectEnabledOrThrow(subject);
        if (!Objects.equals(room.getId(), entity.getExamRoom().getId())
            || !Objects.equals(session.getId(), entity.getExamSession().getId())) {
            ensureUnique(room.getId(), session.getId(), entity.getId());
        }

        entity.setExamRoom(room);
        entity.setExamSubject(subject);
        entity.setExamSession(session);
        entity.setNote(normalize(input.getNote()));
        entity = examScheduleRepository.save(entity);
        return mapSchedule(entity);
    }

    public ExamScheduleModel updateScheduleStatus(UpdateExamScheduleStatusInput input) {
        ExamScheduleEntity entity = examScheduleRepository.findById(input.getScheduleId())
            .orElseThrow(() -> new IllegalArgumentException("排考记录不存在"));
        ExamScheduleStatus status = parseScheduleStatus(input.getStatus());
        if (entity.getStatus() == status) {
            return mapSchedule(entity);
        }
        entity.setStatus(status);
        entity = examScheduleRepository.save(entity);
        return mapSchedule(entity);
    }

    public boolean deleteSchedule(Long id) {
        if (!examScheduleRepository.existsById(id)) {
            throw new IllegalArgumentException("排考记录不存在");
        }
        examScheduleRepository.deleteById(id);
        return true;
    }

    @Transactional(readOnly = true)
    public List<ExamScheduleModel> listSchedules(Long roomId, Long subjectId, Long sessionId) {
        List<ExamScheduleEntity> entities = examScheduleRepository.findAll();
        return entities.stream()
            .filter(entity -> roomId == null || entity.getExamRoom().getId().equals(roomId))
            .filter(entity -> subjectId == null || entity.getExamSubject().getId().equals(subjectId))
            .filter(entity -> sessionId == null || entity.getExamSession().getId().equals(sessionId))
            .map(this::mapSchedule)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ExamScheduleModel getSchedule(Long id) {
        ExamScheduleEntity entity = examScheduleRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("排考记录不存在"));
        return mapSchedule(entity);
    }

    private void ensureRoomAvailable(ExamRoomEntity room) {
        if (room.getStatus() != ExamRoomStatus.AVAILABLE) {
            throw new IllegalStateException("考场不可用，无法排考");
        }
    }

    private void ensureUnique(Long roomId, Long sessionId, Long currentId) {
        boolean exists = examScheduleRepository.existsByExamRoomIdAndExamSessionId(roomId, sessionId);
        if (exists) {
            if (currentId == null) {
                throw new IllegalArgumentException("该考场在该场次已有排考");
            }
            ExamScheduleEntity existing = examScheduleRepository.findByExamRoomId(roomId).stream()
                .filter(schedule -> schedule.getExamSession().getId().equals(sessionId))
                .findFirst()
                .orElse(null);
            if (existing != null && !existing.getId().equals(currentId)) {
                throw new IllegalArgumentException("该考场在该场次已有排考");
            }
        }
    }

    private ExamScheduleStatus parseScheduleStatus(String status) {
        try {
            return ExamScheduleStatus.valueOf(status.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("非法的排考状态");
        }
    }

    private ExamScheduleModel mapSchedule(ExamScheduleEntity entity) {
        ExamRoomEntity room = entity.getExamRoom();
        ExamCenterEntity center = room.getCenter();
        ExamSubjectEntity subject = entity.getExamSubject();
        ExamSessionEntity session = entity.getExamSession();

        ExamCenterModel centerModel = new ExamCenterModel(
            center.getId(),
            center.getName(),
            center.getAddress(),
            center.getDescription()
        );
        ExamRoomModel roomModel = new ExamRoomModel(
            room.getId(),
            room.getRoomNumber(),
            room.getName(),
            room.getStatus().name(),
            room.getCapacity(),
            room.getLocation(),
            room.getManagerName(),
            room.getManagerPhone(),
            centerModel,
            Collections.emptyList()
        );
        ExamSubjectModel subjectModel = new ExamSubjectModel(
            subject.getId(),
            subject.getCode(),
            subject.getName(),
            subject.getStatus() != null ? subject.getStatus().name() : null,
            subject.getDurationMinutes(),
            subject.getQuestionCount(),
            subject.getDescription(),
            Collections.emptyList()
        );
        ExamSessionModel sessionModel = new ExamSessionModel(
            session.getId(),
            session.getName(),
            session.getStartTime() != null ? session.getStartTime().toString() : null,
            session.getEndTime() != null ? session.getEndTime().toString() : null,
            session.getNote()
        );

        return new ExamScheduleModel(
            entity.getId(),
            roomModel,
            subjectModel,
            sessionModel,
            entity.getStatus().name(),
            entity.getNote()
        );
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
