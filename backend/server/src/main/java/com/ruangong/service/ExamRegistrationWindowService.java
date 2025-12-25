package com.ruangong.service;

import com.ruangong.entity.ExamRegistrationStatus;
import com.ruangong.entity.ExamRegistrationWindowEntity;
import com.ruangong.entity.ExamRegistrationWindowLogEntity;
import com.ruangong.entity.ExamSessionEntity;
import com.ruangong.entity.ExamSubjectEntity;
import com.ruangong.entity.ExamSubjectStatus;
import com.ruangong.model.ExamRegistrationWindowLogModel;
import com.ruangong.model.ExamRegistrationWindowModel;
import com.ruangong.model.ExamSessionModel;
import com.ruangong.model.ExamSubjectModel;
import com.ruangong.model.ExamRegistrationViewModel;
import com.ruangong.model.input.BatchDeleteExamRegistrationInput;
import com.ruangong.model.input.BatchUpdateExamRegistrationStatusInput;
import com.ruangong.model.input.ExamRegistrationWindowInput;
import com.ruangong.model.input.UpdateExamRegistrationStatusInput;
import com.ruangong.repository.ExamRegistrationWindowLogRepository;
import com.ruangong.repository.ExamRegistrationWindowRepository;
import com.ruangong.repository.RegistrationInfoRepository;
import com.ruangong.entity.RegistrationInfoEntity;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class ExamRegistrationWindowService {

    private final ExamRegistrationWindowRepository examRegistrationWindowRepository;
    private final ExamRegistrationWindowLogRepository examRegistrationWindowLogRepository;
    private final ExamSubjectService examSubjectService;
    private final ExamSessionService examSessionService;
    private final RegistrationInfoRepository registrationInfoRepository;

    public ExamRegistrationWindowService(
        ExamRegistrationWindowRepository examRegistrationWindowRepository,
        ExamRegistrationWindowLogRepository examRegistrationWindowLogRepository,
        ExamSubjectService examSubjectService,
        ExamSessionService examSessionService,
        RegistrationInfoRepository registrationInfoRepository
    ) {
        this.examRegistrationWindowRepository = examRegistrationWindowRepository;
        this.examRegistrationWindowLogRepository = examRegistrationWindowLogRepository;
        this.examSubjectService = examSubjectService;
        this.examSessionService = examSessionService;
        this.registrationInfoRepository = registrationInfoRepository;
    }

    public ExamRegistrationWindowModel createOrUpdate(Long id, ExamRegistrationWindowInput input) {
        ExamSubjectEntity subject = examSubjectService.getSubjectEntity(input.getSubjectId());
        examSubjectService.ensureSubjectEnabledOrThrow(subject);
        ExamSessionEntity session = examSessionService.getSessionEntity(input.getSessionId());
        OffsetDateTime start = parseTime(input.getStartTime());
        OffsetDateTime end = parseTime(input.getEndTime());
        validateTime(start, end);

        ExamRegistrationWindowEntity entity = id == null
            ? new ExamRegistrationWindowEntity()
            : examRegistrationWindowRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("报名配置不存在"));

        entity.setSubject(subject);
        entity.setSession(session);
        entity.setStartTime(start);
        entity.setEndTime(end);
        entity.setNote(normalize(input.getNote()));
        entity = examRegistrationWindowRepository.save(entity);
        return map(entity, false);
    }

    public ExamRegistrationWindowModel updateStatus(UpdateExamRegistrationStatusInput input, Long operatorId) {
        ExamRegistrationWindowEntity entity = examRegistrationWindowRepository.findById(input.getRegistrationId())
            .orElseThrow(() -> new IllegalArgumentException("报名配置不存在"));
        ExamRegistrationStatus target = parseStatus(input.getStatus());
        if (entity.getStatus() == target) {
            return map(entity, true);
        }
        ExamRegistrationStatus from = entity.getStatus();
        entity.setStatus(target);
        entity = examRegistrationWindowRepository.save(entity);

        ExamRegistrationWindowLogEntity log = buildLog(entity, from, target, input.getReason(), operatorId);
        examRegistrationWindowLogRepository.save(log);
        return map(entity, true);
    }

    public List<ExamRegistrationWindowModel> batchUpdateStatus(
        BatchUpdateExamRegistrationStatusInput input,
        Long operatorId
    ) {
        ExamRegistrationStatus target = parseStatus(input.getStatus());
        List<ExamRegistrationWindowEntity> list = examRegistrationWindowRepository.findAllById(input.getRegistrationIds());
        if (list.size() != input.getRegistrationIds().size()) {
            throw new IllegalArgumentException("存在无效的报名配置ID");
        }
        List<ExamRegistrationWindowLogEntity> logs = list.stream()
            .filter(entity -> entity.getStatus() != target)
            .map(entity -> {
                ExamRegistrationStatus from = entity.getStatus();
                entity.setStatus(target);
                return buildLog(entity, from, target, input.getReason(), operatorId);
            })
            .toList();
        examRegistrationWindowRepository.saveAll(list);
        if (!logs.isEmpty()) {
            examRegistrationWindowLogRepository.saveAll(logs);
        }
        return list.stream().map(entity -> map(entity, false)).toList();
    }

    public boolean batchDelete(BatchDeleteExamRegistrationInput input) {
        List<ExamRegistrationWindowEntity> list = examRegistrationWindowRepository.findAllById(input.getRegistrationIds());
        if (list.size() != input.getRegistrationIds().size()) {
            throw new IllegalArgumentException("存在无效的报名配置ID");
        }
        examRegistrationWindowRepository.deleteAll(list);
        return true;
    }

    @Transactional(readOnly = true)
    public List<ExamRegistrationWindowModel> list(Long subjectId, String status) {
        ExamRegistrationStatus statusFilter = parseStatusOrNull(status);
        return examRegistrationWindowRepository.findAll().stream()
            .filter(entity -> subjectId == null || entity.getSubject().getId().equals(subjectId))
            .filter(entity -> statusFilter == null || entity.getStatus() == statusFilter)
            .map(this::map)
            .toList();
    }

    @Transactional(readOnly = true)
    public ExamRegistrationWindowModel get(Long id) {
        ExamRegistrationWindowEntity entity = examRegistrationWindowRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("报名配置不存在"));
        return map(entity, true);
    }

    @Transactional(readOnly = true)
    public List<ExamRegistrationWindowLogModel> logs(Long registrationId) {
        return examRegistrationWindowLogRepository.findByRegistrationWindowIdOrderByCreatedAtDesc(registrationId).stream()
            .map(this::mapLog)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<ExamRegistrationWindowModel> exportList(Long subjectId, String status) {
        return list(subjectId, status);
    }

    @Transactional(readOnly = true)
    public List<ExamRegistrationViewModel> availableExams(String keyword, String status, Long userId) {
        List<ExamRegistrationWindowEntity> windows = examRegistrationWindowRepository.findAll();
        List<ExamRegistrationViewStatus> statusFilter = status != null
            ? List.of(parseViewStatus(status))
            : List.of();

        // 查询用户已报名的科目+场次及其状态 (key = "subjectId-sessionId")
        Map<String, String> userRegistrations = new HashMap<>();
        if (userId != null) {
            List<RegistrationInfoEntity> registrations = registrationInfoRepository.findByUserId(userId);
            for (RegistrationInfoEntity reg : registrations) {
                if (reg.getSubject() != null && reg.getSession() != null) {
                    String key = reg.getSubject().getId() + "-" + reg.getSession().getId();
                    userRegistrations.put(key, reg.getStatus());
                }
            }
        }

        return windows.stream()
            .filter(window -> window.getStatus() == ExamRegistrationStatus.ENABLED)
            .filter(window -> window.getSubject() != null && window.getSubject().getStatus() == ExamSubjectStatus.ENABLED)
            .map(window -> toViewModelWithUserStatus(window, userRegistrations))
            .filter(view -> matchesKeyword(view, keyword))
            .filter(view -> statusFilter.isEmpty() || statusFilter.contains(ExamRegistrationViewStatus.valueOf(view.status())))
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ExamRegistrationViewModel availableExam(Long registrationId) {
        ExamRegistrationWindowEntity entity = examRegistrationWindowRepository.findById(registrationId)
            .orElseThrow(() -> new IllegalArgumentException("报名配置不存在"));
        if (entity.getStatus() != ExamRegistrationStatus.ENABLED
            || entity.getSubject() == null
            || entity.getSubject().getStatus() != ExamSubjectStatus.ENABLED) {
            throw new IllegalStateException("该考试不可报名");
        }
        return toViewModel(entity);
    }

    private boolean matchesKeyword(ExamRegistrationViewModel view, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        String normalized = keyword.toLowerCase(Locale.ROOT);
        return (view.subjectCode() != null && view.subjectCode().toLowerCase(Locale.ROOT).contains(normalized))
            || (view.subjectName() != null && view.subjectName().toLowerCase(Locale.ROOT).contains(normalized));
    }

    private ExamRegistrationStatus parseStatus(String status) {
        if (!StringUtils.hasText(status)) {
            throw new IllegalArgumentException("状态不能为空");
        }
        try {
            return ExamRegistrationStatus.valueOf(status.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("非法的报名配置状态");
        }
    }

    private ExamRegistrationStatus parseStatusOrNull(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        return parseStatus(status);
    }

    private OffsetDateTime parseTime(String value) {
        try {
            return OffsetDateTime.parse(value);
        } catch (Exception ex) {
            throw new IllegalArgumentException("时间格式需为 ISO-8601，示例：2025-01-01T09:00:00+08:00");
        }
    }

    private void validateTime(OffsetDateTime start, OffsetDateTime end) {
        if (start.isAfter(end) || start.isEqual(end)) {
            throw new IllegalArgumentException("报名开始时间必须早于截止时间");
        }
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private ExamRegistrationWindowLogEntity buildLog(
        ExamRegistrationWindowEntity entity,
        ExamRegistrationStatus from,
        ExamRegistrationStatus to,
        String reason,
        Long operatorId
    ) {
        ExamRegistrationWindowLogEntity log = new ExamRegistrationWindowLogEntity();
        log.setRegistrationWindow(entity);
        log.setFromStatus(from);
        log.setToStatus(to);
        log.setReason(normalize(reason));
        log.setOperatorId(operatorId);
        log.setCreatedAt(OffsetDateTime.now());
        return log;
    }

    private ExamRegistrationWindowModel map(ExamRegistrationWindowEntity entity) {
        return map(entity, false);
    }

    private ExamRegistrationWindowModel map(ExamRegistrationWindowEntity entity, boolean includeLogs) {
        ExamSubjectEntity subject = entity.getSubject();
        ExamSessionEntity session = entity.getSession();
        ExamSubjectModel subjectModel = new ExamSubjectModel(
            subject.getId(),
            subject.getCode(),
            subject.getName(),
            subject.getStatus() != null ? subject.getStatus().name() : null,
            subject.getDurationMinutes(),
            subject.getQuestionCount(),
            subject.getDescription(),
            List.of()
        );
        ExamSessionModel sessionModel = new ExamSessionModel(
            session.getId(),
            session.getName(),
            session.getStartTime() != null ? session.getStartTime().toString() : null,
            session.getEndTime() != null ? session.getEndTime().toString() : null,
            session.getNote()
        );

        List<ExamRegistrationWindowLogModel> logs = includeLogs ? logs(entity.getId()) : List.of();

        return new ExamRegistrationWindowModel(
            entity.getId(),
            entity.getStartTime() != null ? entity.getStartTime().toString() : null,
            entity.getEndTime() != null ? entity.getEndTime().toString() : null,
            entity.getStatus() != null ? entity.getStatus().name() : null,
            entity.getNote(),
            subjectModel,
            sessionModel,
            logs
        );
    }

    private ExamRegistrationWindowLogModel mapLog(ExamRegistrationWindowLogEntity entity) {
        return new ExamRegistrationWindowLogModel(
            entity.getId(),
            entity.getFromStatus() != null ? entity.getFromStatus().name() : null,
            entity.getToStatus() != null ? entity.getToStatus().name() : null,
            entity.getReason(),
            entity.getOperatorId(),
            entity.getCreatedAt() != null ? entity.getCreatedAt().toString() : null
        );
    }

    private ExamRegistrationViewModel toViewModel(ExamRegistrationWindowEntity entity) {
        ExamSubjectEntity subject = entity.getSubject();
        ExamSessionEntity session = entity.getSession();
        ExamRegistrationViewStatus status = computeViewStatus(entity.getStartTime(), entity.getEndTime());
        String actionLabel = switch (status) {
            case NOT_STARTED -> "未开始";
            case OPEN -> "立即报名";
            case CLOSED -> "已截止";
        };
        return new ExamRegistrationViewModel(
            entity.getId(),
            subject != null ? subject.getCode() : null,
            subject != null ? subject.getName() : null,
            session != null ? session.getId() : null,
            session != null ? session.getName() : null,
            session != null && session.getStartTime() != null ? session.getStartTime().toString() : null,
            session != null && session.getEndTime() != null ? session.getEndTime().toString() : null,
            entity.getStartTime() != null ? entity.getStartTime().toString() : null,
            entity.getEndTime() != null ? entity.getEndTime().toString() : null,
            status.name(),
            actionLabel,
            entity.getNote()
        );
    }

    private ExamRegistrationViewModel toViewModelWithUserStatus(ExamRegistrationWindowEntity entity, Map<String, String> userRegistrations) {
        ExamSubjectEntity subject = entity.getSubject();
        ExamSessionEntity session = entity.getSession();
        ExamRegistrationViewStatus windowStatus = computeViewStatus(entity.getStartTime(), entity.getEndTime());

        // 检查用户是否已报名该科目+场次
        String key = (subject != null && session != null) ? subject.getId() + "-" + session.getId() : null;
        String userRegStatus = key != null ? userRegistrations.get(key) : null;

        // status 保持为窗口状态（枚举值），actionLabel 显示用户报名状态
        String actionLabel;
        if (userRegStatus != null) {
            // 用户已报名，显示报名状态
            actionLabel = switch (userRegStatus) {
                case "PENDING_REVIEW" -> "待审核";
                case "APPROVED" -> "已通过";
                case "REJECTED" -> "已驳回";
                case "PENDING" -> "待完善";
                case "COMPLETED" -> "待审核";
                default -> userRegStatus;
            };
        } else {
            // 用户未报名，显示报名窗口状态
            actionLabel = switch (windowStatus) {
                case NOT_STARTED -> "未开始";
                case OPEN -> "立即报名";
                case CLOSED -> "已截止";
            };
        }

        return new ExamRegistrationViewModel(
            entity.getId(),
            subject != null ? subject.getCode() : null,
            subject != null ? subject.getName() : null,
            session != null ? session.getId() : null,
            session != null ? session.getName() : null,
            session != null && session.getStartTime() != null ? session.getStartTime().toString() : null,
            session != null && session.getEndTime() != null ? session.getEndTime().toString() : null,
            entity.getStartTime() != null ? entity.getStartTime().toString() : null,
            entity.getEndTime() != null ? entity.getEndTime().toString() : null,
            windowStatus.name(),
            actionLabel,
            entity.getNote()
        );
    }

    private ExamRegistrationViewStatus computeViewStatus(OffsetDateTime start, OffsetDateTime end) {
        OffsetDateTime now = OffsetDateTime.now();
        if (now.isBefore(start)) {
            return ExamRegistrationViewStatus.NOT_STARTED;
        }
        if (now.isAfter(end)) {
            return ExamRegistrationViewStatus.CLOSED;
        }
        return ExamRegistrationViewStatus.OPEN;
    }

    private ExamRegistrationViewStatus parseViewStatus(String status) {
        if (!StringUtils.hasText(status)) {
            throw new IllegalArgumentException("状态不能为空");
        }
        try {
            return ExamRegistrationViewStatus.valueOf(status.toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            throw new IllegalArgumentException("非法的报名状态筛选");
        }
    }

    private enum ExamRegistrationViewStatus {
        NOT_STARTED,
        OPEN,
        CLOSED
    }
}
