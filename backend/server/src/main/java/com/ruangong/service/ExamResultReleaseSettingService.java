package com.ruangong.service;

import com.ruangong.entity.ExamResultPreNotificationEntity;
import com.ruangong.entity.ExamResultPreNotificationStatus;
import com.ruangong.entity.ExamResultRecordEntity;
import com.ruangong.entity.ExamResultReleaseSettingEntity;
import com.ruangong.entity.ExamSubjectEntity;
import com.ruangong.model.ExamResultReleaseSettingModel;
import com.ruangong.model.input.ExamResultReleaseBatchInput;
import com.ruangong.model.input.ExamResultReleaseSettingInput;
import com.ruangong.repository.ExamResultPreNotificationRepository;
import com.ruangong.repository.ExamResultRecordRepository;
import com.ruangong.repository.ExamResultReleaseSettingRepository;
import com.ruangong.repository.ExamSubjectRepository;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class ExamResultReleaseSettingService {

    private final ExamResultReleaseSettingRepository releaseSettingRepository;
    private final ExamSubjectRepository examSubjectRepository;
    private final ExamResultPreNotificationRepository preNotificationRepository;
    private final ExamResultPreNotificationService preNotificationService;
    private final ExamResultRecordRepository examResultRecordRepository;

    public ExamResultReleaseSettingService(
        ExamResultReleaseSettingRepository releaseSettingRepository,
        ExamSubjectRepository examSubjectRepository,
        ExamResultPreNotificationRepository preNotificationRepository,
        ExamResultPreNotificationService preNotificationService,
        ExamResultRecordRepository examResultRecordRepository
    ) {
        this.releaseSettingRepository = releaseSettingRepository;
        this.examSubjectRepository = examSubjectRepository;
        this.preNotificationRepository = preNotificationRepository;
        this.preNotificationService = preNotificationService;
        this.examResultRecordRepository = examResultRecordRepository;
    }

    public List<ExamResultReleaseSettingModel> list(Long subjectId, Integer examYear) {
        processSchedules();
        return filterSettings(subjectId, examYear).stream()
            .map(this::map)
            .toList();
    }

    public ExamResultReleaseSettingModel detail(Long id) {
        processSchedules();
        ExamResultReleaseSettingEntity entity = releaseSettingRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("成绩发布时间设置不存在"));
        return map(entity);
    }

    public ExamResultReleaseSettingModel upsert(Long id, ExamResultReleaseSettingInput input, Long operatorId) {
        ExamSubjectEntity subject = examSubjectRepository.findById(input.getSubjectId())
            .orElseThrow(() -> new IllegalArgumentException("科目不存在"));
        ExamResultReleaseSettingEntity entity;
        if (id != null) {
            entity = releaseSettingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("成绩发布时间设置不存在"));
        } else {
            entity = releaseSettingRepository.findBySubject_IdAndExamYear(subject.getId(), input.getExamYear())
                .orElse(new ExamResultReleaseSettingEntity());
        }
        ExamResultReleaseSettingEntity saved = applySetting(entity, subject, input, operatorId);
        return map(saved);
    }

    public List<ExamResultReleaseSettingModel> batchUpsert(ExamResultReleaseBatchInput input, Long operatorId) {
        if (CollectionUtils.isEmpty(input.getSubjectIds())) {
            throw new IllegalArgumentException("请选择至少一个科目进行批量设置");
        }
        List<ExamResultReleaseSettingEntity> saved = new ArrayList<>();
        for (Long subjectId : input.getSubjectIds()) {
            ExamSubjectEntity subject = examSubjectRepository.findById(subjectId)
                .orElseThrow(() -> new IllegalArgumentException("科目不存在: " + subjectId));
            ExamResultReleaseSettingEntity entity = releaseSettingRepository
                .findBySubject_IdAndExamYear(subjectId, input.getExamYear())
                .orElse(new ExamResultReleaseSettingEntity());
            ExamResultReleaseSettingInput mergedInput = new ExamResultReleaseSettingInput();
            mergedInput.setSubjectId(subjectId);
            mergedInput.setExamYear(input.getExamYear());
            mergedInput.setReleaseTime(input.getReleaseTime());
            mergedInput.setPreNoticeOffsetMinutes(input.getPreNoticeOffsetMinutes());
            mergedInput.setPreNotificationTitle(input.getPreNotificationTitle());
            mergedInput.setPreNotificationContent(input.getPreNotificationContent());
            saved.add(applySetting(entity, subject, mergedInput, operatorId));
        }
        return saved.stream().map(this::map).toList();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processSchedules() {
        OffsetDateTime now = OffsetDateTime.now();
        List<ExamResultReleaseSettingEntity> all = releaseSettingRepository.findAll();
        List<ExamResultReleaseSettingEntity> dirty = new ArrayList<>();
        for (ExamResultReleaseSettingEntity entity : all) {
            boolean updated = false;
            if (!entity.getReleaseTime().isAfter(now)) {
                releaseResults(entity);
                if (entity.getAutoReleaseTriggeredAt() == null) {
                    entity.setAutoReleaseTriggeredAt(now);
                    updated = true;
                }
            }
            if (shouldTriggerPreNotice(entity, now)) {
                triggerPreNotice(entity);
                entity.setPreNoticeTriggeredAt(now);
                updated = true;
            }
            if (updated) {
                entity.setUpdatedAt(now);
                dirty.add(entity);
            }
        }
        if (!dirty.isEmpty()) {
            releaseSettingRepository.saveAll(dirty);
        }
    }

    public void applyReleaseTimeIfReleased(ExamResultRecordEntity entity) {
        if (entity == null || entity.getRegistrationInfo() == null) {
            return;
        }
        ExamSubjectEntity subject = entity.getRegistrationInfo().getSubject();
        if (subject == null) {
            return;
        }
        releaseSettingRepository
            .findBySubject_IdAndExamYear(subject.getId(), entity.getExamYear())
            .filter(setting -> !setting.getReleaseTime().isAfter(OffsetDateTime.now()))
            .ifPresent(setting -> {
                entity.setReleaseTime(setting.getReleaseTime());
                examResultRecordRepository.save(entity);
            });
    }

    public OffsetDateTime plannedReleaseTime(Long subjectId, Integer examYear) {
        if (subjectId == null || examYear == null) {
            return null;
        }
        return releaseSettingRepository.findBySubject_IdAndExamYear(subjectId, examYear)
            .map(ExamResultReleaseSettingEntity::getReleaseTime)
            .orElse(null);
    }

    private List<ExamResultReleaseSettingEntity> filterSettings(Long subjectId, Integer examYear) {
        if (subjectId != null && examYear != null) {
            return releaseSettingRepository.findBySubject_IdAndExamYear(subjectId, examYear)
                .map(List::of)
                .orElse(List.of());
        }
        if (subjectId != null) {
            return releaseSettingRepository.findBySubject_Id(subjectId);
        }
        if (examYear != null) {
            return releaseSettingRepository.findByExamYear(examYear);
        }
        return releaseSettingRepository.findAll();
    }

    private ExamResultReleaseSettingEntity applySetting(
        ExamResultReleaseSettingEntity entity,
        ExamSubjectEntity subject,
        ExamResultReleaseSettingInput input,
        Long operatorId
    ) {
        OffsetDateTime releaseTime = parseReleaseTime(input.getReleaseTime());
        boolean releaseTimeChanged = entity.getId() == null
            || entity.getReleaseTime() == null
            || !Objects.equals(entity.getReleaseTime(), releaseTime)
            || !Objects.equals(entity.getExamYear(), input.getExamYear());

        if (entity.getId() == null) {
            entity.setCreatedAt(OffsetDateTime.now());
            entity.setCreatedBy(operatorId);
        }
        Integer normalizedOffset = normalizeOffset(input.getPreNoticeOffsetMinutes());
        boolean offsetChanged = !Objects.equals(entity.getPreNoticeOffsetMinutes(), normalizedOffset);

        entity.setSubject(subject);
        entity.setExamYear(input.getExamYear());
        entity.setReleaseTime(releaseTime);
        entity.setPreNoticeOffsetMinutes(normalizedOffset);
        entity.setUpdatedAt(OffsetDateTime.now());
        entity.setAutoReleaseTriggeredAt(releaseTimeChanged ? null : entity.getAutoReleaseTriggeredAt());
        entity.setPreNoticeTriggeredAt((releaseTimeChanged || offsetChanged) ? null : entity.getPreNoticeTriggeredAt());

        if (hasPreNotificationContent(input)) {
            ExamResultPreNotificationEntity preNotification = entity.getPreNotification();
            if (preNotification == null) {
                preNotification = new ExamResultPreNotificationEntity();
                preNotification.setCreatedAt(OffsetDateTime.now());
                preNotification.setCreatedBy(operatorId);
            }
            preNotification.setExamType(subject.getName());
            preNotification.setExamYear(input.getExamYear());
            preNotification.setQueryTime(releaseTime);
            preNotification.setTitle(trimToNull(input.getPreNotificationTitle()));
            preNotification.setContent(trimToNull(input.getPreNotificationContent()));
            preNotification.setStatus(ExamResultPreNotificationStatus.DRAFT);
            preNotification.setUpdatedAt(OffsetDateTime.now());
            entity.setPreNotification(preNotificationRepository.save(preNotification));
            entity.setPreNoticeTriggeredAt(null);
        }

        return releaseSettingRepository.save(entity);
    }

    private void releaseResults(ExamResultReleaseSettingEntity setting) {
        examResultRecordRepository.markReleasedBySubjectAndYear(
            setting.getSubject().getId(),
            setting.getExamYear(),
            setting.getReleaseTime()
        );
    }

    private boolean shouldTriggerPreNotice(ExamResultReleaseSettingEntity entity, OffsetDateTime now) {
        if (entity.getPreNoticeOffsetMinutes() == null || entity.getPreNoticeOffsetMinutes() <= 0) {
            return false;
        }
        if (entity.getPreNotification() == null) {
            return false;
        }
        if (entity.getPreNoticeTriggeredAt() != null) {
            return false;
        }
        OffsetDateTime preNoticeTime = entity.getReleaseTime().minusMinutes(entity.getPreNoticeOffsetMinutes());
        return !preNoticeTime.isAfter(now);
    }

    private void triggerPreNotice(ExamResultReleaseSettingEntity entity) {
        ExamResultPreNotificationEntity preNotification = entity.getPreNotification();
        if (preNotification == null) {
            return;
        }
        preNotificationService.publish(preNotification.getId(), entity.getCreatedBy());
    }

    private OffsetDateTime parseReleaseTime(String releaseTime) {
        if (!StringUtils.hasText(releaseTime)) {
            throw new IllegalArgumentException("请设置成绩发布时间");
        }
        try {
            return OffsetDateTime.parse(releaseTime.trim());
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("成绩发布时间格式不正确，应为 ISO-8601，如 2025-03-10T10:00:00+08:00", ex);
        }
    }

    private Integer normalizeOffset(Integer offset) {
        if (offset == null) {
            return null;
        }
        if (offset < 0) {
            throw new IllegalArgumentException("预告提醒时间不能为负数");
        }
        return offset;
    }

    private boolean hasPreNotificationContent(ExamResultReleaseSettingInput input) {
        return StringUtils.hasText(input.getPreNotificationTitle())
            || StringUtils.hasText(input.getPreNotificationContent());
    }

    private ExamResultReleaseSettingModel map(ExamResultReleaseSettingEntity entity) {
        ExamSubjectEntity subject = entity.getSubject();
        ExamResultPreNotificationEntity preNotification = entity.getPreNotification();
        return new ExamResultReleaseSettingModel(
            entity.getId(),
            subject != null ? subject.getId() : null,
            subject != null ? subject.getName() : null,
            entity.getExamYear(),
            entity.getReleaseTime() != null ? entity.getReleaseTime().toString() : null,
            entity.getPreNoticeOffsetMinutes(),
            preNotification != null ? preNotification.getId() : null,
            preNotification != null ? preNotification.getTitle() : null,
            preNotification != null ? preNotification.getContent() : null,
            entity.getPreNoticeTriggeredAt() != null ? entity.getPreNoticeTriggeredAt().toString() : null,
            entity.getAutoReleaseTriggeredAt() != null ? entity.getAutoReleaseTriggeredAt().toString() : null,
            entity.getCreatedBy(),
            entity.getCreatedAt() != null ? entity.getCreatedAt().toString() : null,
            entity.getUpdatedAt() != null ? entity.getUpdatedAt().toString() : null
        );
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
