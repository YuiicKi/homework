package com.ruangong.service;

import com.ruangong.entity.ExamSessionEntity;
import com.ruangong.model.ExamSessionModel;
import com.ruangong.model.input.ExamSessionInput;
import com.ruangong.repository.ExamSessionRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class ExamSessionService {

    private final ExamSessionRepository examSessionRepository;

    public ExamSessionService(ExamSessionRepository examSessionRepository) {
        this.examSessionRepository = examSessionRepository;
    }

    public ExamSessionModel createSession(ExamSessionInput input) {
        OffsetDateTime start = parseDateTime(input.getStartTime());
        OffsetDateTime end = parseDateTime(input.getEndTime());
        validateTimeRange(start, end);

        ExamSessionEntity entity = new ExamSessionEntity();
        entity.setName(input.getName());
        entity.setStartTime(start);
        entity.setEndTime(end);
        entity.setNote(normalize(input.getNote()));
        entity = examSessionRepository.save(entity);
        return mapSession(entity);
    }

    public ExamSessionModel updateSession(Long id, ExamSessionInput input) {
        ExamSessionEntity entity = examSessionRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("场次不存在"));

        OffsetDateTime start = parseDateTime(input.getStartTime());
        OffsetDateTime end = parseDateTime(input.getEndTime());
        validateTimeRange(start, end);

        entity.setName(input.getName());
        entity.setStartTime(start);
        entity.setEndTime(end);
        entity.setNote(normalize(input.getNote()));
        entity = examSessionRepository.save(entity);
        return mapSession(entity);
    }

    @Transactional(readOnly = true)
    public List<ExamSessionModel> listSessions() {
        return examSessionRepository.findAll().stream()
            .map(this::mapSession)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ExamSessionModel getSession(Long id) {
        ExamSessionEntity entity = examSessionRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("场次不存在"));
        return mapSession(entity);
    }

    @Transactional(readOnly = true)
    public ExamSessionEntity getSessionEntity(Long id) {
        return examSessionRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("场次不存在"));
    }

    private OffsetDateTime parseDateTime(String value) {
        try {
            return OffsetDateTime.parse(value);
        } catch (Exception ex) {
            throw new IllegalArgumentException("时间格式需为 ISO-8601，示例：2024-01-01T08:00:00+08:00");
        }
    }

    private void validateTimeRange(OffsetDateTime start, OffsetDateTime end) {
        if (start.isAfter(end) || start.isEqual(end)) {
            throw new IllegalArgumentException("开始时间必须早于结束时间");
        }
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private ExamSessionModel mapSession(ExamSessionEntity entity) {
        return new ExamSessionModel(
            entity.getId(),
            entity.getName(),
            entity.getStartTime() != null ? entity.getStartTime().toString() : null,
            entity.getEndTime() != null ? entity.getEndTime().toString() : null,
            entity.getNote()
        );
    }
}
