package com.ruangong.service;

import com.ruangong.entity.ExamSubjectEntity;
import com.ruangong.entity.ExamSubjectLogEntity;
import com.ruangong.entity.ExamSubjectStatus;
import com.ruangong.model.ExamSubjectModel;
import com.ruangong.model.ExamSubjectLogModel;
import com.ruangong.model.input.ExamSubjectInput;
import com.ruangong.model.input.BatchDeleteExamSubjectInput;
import com.ruangong.model.input.BatchExamSubjectStatusInput;
import com.ruangong.model.input.UpdateExamSubjectStatusInput;
import com.ruangong.repository.ExamSubjectLogRepository;
import com.ruangong.repository.ExamSubjectRepository;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class ExamSubjectService {

    private final ExamSubjectRepository examSubjectRepository;
    private final ExamSubjectLogRepository examSubjectLogRepository;

    public ExamSubjectService(
        ExamSubjectRepository examSubjectRepository,
        ExamSubjectLogRepository examSubjectLogRepository
    ) {
        this.examSubjectRepository = examSubjectRepository;
        this.examSubjectLogRepository = examSubjectLogRepository;
    }

    public ExamSubjectModel createSubject(ExamSubjectInput input) {
        if (examSubjectRepository.existsByCode(input.getCode())) {
            throw new IllegalArgumentException("科目编码已存在");
        }
        validateDurationAndCount(input.getDurationMinutes(), input.getQuestionCount());
        ExamSubjectEntity entity = new ExamSubjectEntity();
        entity.setCode(input.getCode());
        entity.setName(input.getName());
        entity.setDurationMinutes(input.getDurationMinutes());
        entity.setQuestionCount(input.getQuestionCount());
        entity.setDescription(normalize(input.getDescription()));
        entity = examSubjectRepository.save(entity);
        return mapSubject(entity, false);
    }

    public ExamSubjectModel updateSubject(Long id, ExamSubjectInput input) {
        ExamSubjectEntity entity = examSubjectRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("科目不存在"));
        if (!entity.getCode().equals(input.getCode()) && examSubjectRepository.existsByCode(input.getCode())) {
            throw new IllegalArgumentException("科目编码已存在");
        }
        validateDurationAndCount(input.getDurationMinutes(), input.getQuestionCount());
        entity.setCode(input.getCode());
        entity.setName(input.getName());
        entity.setDurationMinutes(input.getDurationMinutes());
        entity.setQuestionCount(input.getQuestionCount());
        entity.setDescription(normalize(input.getDescription()));
        entity = examSubjectRepository.save(entity);
        return mapSubject(entity, false);
    }

    @Transactional(readOnly = true)
    public List<ExamSubjectModel> listSubjects(String keyword, String status) {
        ExamSubjectStatus statusFilter = parseStatusOrNull(status);
        return examSubjectRepository.findAll().stream()
            .filter(entity -> matchesKeyword(entity, keyword))
            .filter(entity -> statusFilter == null || entity.getStatus() == statusFilter)
            .map(this::mapSubject)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ExamSubjectModel getSubject(Long id) {
        ExamSubjectEntity entity = examSubjectRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("科目不存在"));
        return mapSubject(entity, true);
    }

    @Transactional(readOnly = true)
    public ExamSubjectEntity getSubjectEntity(Long id) {
        return examSubjectRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("科目不存在"));
    }

    public ExamSubjectModel changeStatus(UpdateExamSubjectStatusInput input, Long operatorId) {
        ExamSubjectEntity entity = examSubjectRepository.findById(input.getSubjectId())
            .orElseThrow(() -> new IllegalArgumentException("科目不存在"));
        ExamSubjectStatus target = parseStatus(input.getStatus());
        if (entity.getStatus() == target) {
            return mapSubject(entity, true);
        }
        ExamSubjectStatus from = entity.getStatus();
        entity.setStatus(target);
        entity = examSubjectRepository.save(entity);

        ExamSubjectLogEntity log = buildLog(entity, from, target, input.getReason(), operatorId);
        examSubjectLogRepository.save(log);

        return mapSubject(entity, true);
    }

    public List<ExamSubjectModel> batchChangeStatus(BatchExamSubjectStatusInput input, Long operatorId) {
        ExamSubjectStatus target = parseStatus(input.getStatus());
        List<ExamSubjectEntity> subjects = examSubjectRepository.findByIdIn(input.getSubjectIds());
        if (subjects.size() != input.getSubjectIds().size()) {
            throw new IllegalArgumentException("存在无效的科目ID");
        }
        List<ExamSubjectLogEntity> logs = subjects.stream()
            .filter(entity -> entity.getStatus() != target)
            .map(entity -> {
                ExamSubjectStatus from = entity.getStatus();
                entity.setStatus(target);
                return buildLog(entity, from, target, input.getReason(), operatorId);
            })
            .toList();
        examSubjectRepository.saveAll(subjects);
        if (!logs.isEmpty()) {
            examSubjectLogRepository.saveAll(logs);
        }
        return subjects.stream().map(subject -> mapSubject(subject, false)).toList();
    }

    public boolean batchDelete(BatchDeleteExamSubjectInput input) {
        List<ExamSubjectEntity> subjects = examSubjectRepository.findByIdIn(input.getSubjectIds());
        if (subjects.size() != input.getSubjectIds().size()) {
            throw new IllegalArgumentException("存在无效的科目ID");
        }
        examSubjectRepository.deleteAll(subjects);
        return true;
    }

    public List<ExamSubjectModel> importSubjects(List<ExamSubjectInput> inputs) {
        if (inputs == null || inputs.isEmpty()) {
            throw new IllegalArgumentException("导入数据不能为空");
        }
        Set<String> codesInPayload = new HashSet<>();
        for (ExamSubjectInput input : inputs) {
            validateDurationAndCount(input.getDurationMinutes(), input.getQuestionCount());
            if (!codesInPayload.add(input.getCode())) {
                throw new IllegalArgumentException("导入数据中存在重复编码: " + input.getCode());
            }
        }
        List<String> duplicateCodes = examSubjectRepository.findByCodeIn(codesInPayload).stream()
            .map(ExamSubjectEntity::getCode)
            .toList();
        if (!duplicateCodes.isEmpty()) {
            throw new IllegalArgumentException("数据库中已存在编码: " + String.join(",", duplicateCodes));
        }
        List<ExamSubjectEntity> entities = inputs.stream()
            .map(input -> {
                ExamSubjectEntity entity = new ExamSubjectEntity();
                entity.setCode(input.getCode());
                entity.setName(input.getName());
                entity.setDurationMinutes(input.getDurationMinutes());
                entity.setQuestionCount(input.getQuestionCount());
                entity.setStatus(ExamSubjectStatus.ENABLED);
                entity.setDescription(normalize(input.getDescription()));
                return entity;
            })
            .toList();
        List<ExamSubjectEntity> saved = examSubjectRepository.saveAll(entities);
        return saved.stream().map(this::mapSubject).toList();
    }

    @Transactional(readOnly = true)
    public List<ExamSubjectModel> exportSubjects(String keyword, String status) {
        return listSubjects(keyword, status);
    }

    @Transactional(readOnly = true)
    public List<ExamSubjectLogModel> getSubjectLogs(Long subjectId) {
        return examSubjectLogRepository.findByExamSubjectIdOrderByCreatedAtDesc(subjectId).stream()
            .map(this::mapLog)
            .toList();
    }

    public void ensureSubjectEnabledOrThrow(ExamSubjectEntity subject) {
        if (subject.getStatus() != ExamSubjectStatus.ENABLED) {
            throw new IllegalStateException("科目已停用，无法使用");
        }
    }

    private boolean matchesKeyword(ExamSubjectEntity entity, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        String normalized = keyword.toLowerCase(Locale.ROOT);
        return (entity.getCode() != null && entity.getCode().toLowerCase(Locale.ROOT).contains(normalized))
            || (entity.getName() != null && entity.getName().toLowerCase(Locale.ROOT).contains(normalized));
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private ExamSubjectModel mapSubject(ExamSubjectEntity entity) {
        return mapSubject(entity, false);
    }

    private ExamSubjectModel mapSubject(ExamSubjectEntity entity, boolean includeLogs) {
        List<ExamSubjectLogModel> logs = includeLogs
            ? getSubjectLogs(entity.getId())
            : List.of();
        return new ExamSubjectModel(
            entity.getId(),
            entity.getCode(),
            entity.getName(),
            entity.getStatus() != null ? entity.getStatus().name() : null,
            entity.getDurationMinutes(),
            entity.getQuestionCount(),
            entity.getDescription(),
            logs
        );
    }

    private ExamSubjectStatus parseStatus(String status) {
        if (!StringUtils.hasText(status)) {
            throw new IllegalArgumentException("状态不能为空");
        }
        try {
            return ExamSubjectStatus.valueOf(status.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("非法的科目状态");
        }
    }

    private ExamSubjectStatus parseStatusOrNull(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        return parseStatus(status);
    }

    private void validateDurationAndCount(Integer durationMinutes, Integer questionCount) {
        if (durationMinutes == null || durationMinutes <= 0) {
            throw new IllegalArgumentException("考试时长必须大于0");
        }
        if (questionCount == null || questionCount <= 0) {
            throw new IllegalArgumentException("题量必须大于0");
        }
    }

    private ExamSubjectLogEntity buildLog(
        ExamSubjectEntity entity,
        ExamSubjectStatus from,
        ExamSubjectStatus to,
        String reason,
        Long operatorId
    ) {
        ExamSubjectLogEntity log = new ExamSubjectLogEntity();
        log.setExamSubject(entity);
        log.setFromStatus(from);
        log.setToStatus(to);
        log.setReason(normalize(reason));
        log.setOperatorId(operatorId);
        log.setCreatedAt(OffsetDateTime.now());
        return log;
    }

    private ExamSubjectLogModel mapLog(ExamSubjectLogEntity entity) {
        return new ExamSubjectLogModel(
            entity.getId(),
            entity.getFromStatus() != null ? entity.getFromStatus().name() : null,
            entity.getToStatus() != null ? entity.getToStatus().name() : null,
            entity.getReason(),
            entity.getOperatorId(),
            entity.getCreatedAt() != null ? entity.getCreatedAt().toString() : null
        );
    }
}
