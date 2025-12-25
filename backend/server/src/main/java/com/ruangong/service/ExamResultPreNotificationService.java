package com.ruangong.service;

import com.ruangong.entity.ExamResultPreNotificationEntity;
import com.ruangong.entity.ExamResultPreNotificationStatus;
import com.ruangong.entity.ExamSubjectEntity;
import com.ruangong.entity.RegistrationInfoEntity;
import com.ruangong.model.ExamResultPreNotificationModel;
import com.ruangong.model.NotificationModel;
import com.ruangong.model.input.ExamResultPreNotificationInput;
import com.ruangong.model.input.NotificationInput;
import com.ruangong.model.input.NotificationTargetInput;
import com.ruangong.model.input.PublishNotificationInput;
import com.ruangong.repository.ExamResultPreNotificationRepository;
import com.ruangong.repository.ExamSubjectRepository;
import com.ruangong.repository.RegistrationInfoRepository;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class ExamResultPreNotificationService {

    private static final DateTimeFormatter QUERY_TIME_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm xxx", Locale.CHINA);

    private static final Sort DEFAULT_SORT = Sort.by(
        Sort.Order.desc("updatedAt"),
        Sort.Order.desc("createdAt"),
        Sort.Order.desc("id")
    );

    private final ExamResultPreNotificationRepository repository;
    private final NotificationService notificationService;
    private final ExamSubjectRepository examSubjectRepository;
    private final RegistrationInfoRepository registrationInfoRepository;

    public ExamResultPreNotificationService(
        ExamResultPreNotificationRepository repository,
        NotificationService notificationService,
        ExamSubjectRepository examSubjectRepository,
        RegistrationInfoRepository registrationInfoRepository
    ) {
        this.repository = repository;
        this.notificationService = notificationService;
        this.examSubjectRepository = examSubjectRepository;
        this.registrationInfoRepository = registrationInfoRepository;
    }

    @Transactional(readOnly = true)
    public List<ExamResultPreNotificationModel> list() {
        return repository.findAll(DEFAULT_SORT).stream()
            .map(this::map)
            .toList();
    }

    @Transactional(readOnly = true)
    public ExamResultPreNotificationModel detail(Long id) {
        ExamResultPreNotificationEntity entity = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("预告不存在"));
        return map(entity);
    }

    public ExamResultPreNotificationModel create(ExamResultPreNotificationInput input, Long operatorId) {
        ExamResultPreNotificationEntity entity = new ExamResultPreNotificationEntity();
        applyInput(entity, input);
        entity.setStatus(ExamResultPreNotificationStatus.DRAFT);
        entity.setCreatedBy(operatorId);
        OffsetDateTime now = OffsetDateTime.now();
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        ExamResultPreNotificationEntity saved = repository.save(entity);
        return map(saved);
    }

    public ExamResultPreNotificationModel update(Long id, ExamResultPreNotificationInput input, Long operatorId) {
        ExamResultPreNotificationEntity entity = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("预告不存在"));
        applyInput(entity, input);
        entity.setStatus(ExamResultPreNotificationStatus.DRAFT);
        entity.setUpdatedAt(OffsetDateTime.now());
        ExamResultPreNotificationEntity saved = repository.save(entity);
        return map(saved);
    }

    public ExamResultPreNotificationModel publish(Long id, Long operatorId) {
        ExamResultPreNotificationEntity entity = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("预告不存在"));
        if (entity.getQueryTime() == null) {
            throw new IllegalStateException("请先完善成绩查询时间");
        }
        NotificationModel published = dispatchNotification(entity, operatorId);
        OffsetDateTime now = OffsetDateTime.now();
        entity.setStatus(ExamResultPreNotificationStatus.PUBLISHED);
        entity.setLastNotificationId(published.id());
        entity.setLastPublishedAt(now);
        entity.setUpdatedAt(now);
        ExamResultPreNotificationEntity saved = repository.save(entity);
        return map(saved);
    }

    private NotificationModel dispatchNotification(ExamResultPreNotificationEntity entity, Long operatorId) {
        NotificationInput notificationInput = new NotificationInput();
        notificationInput.setTitle(entity.getTitle());
        notificationInput.setType("RESULT_PREVIEW");
        notificationInput.setContent(buildNotificationContent(entity));
        notificationInput.setChannel("web");

        // 根据考试类型（科目名称）找到科目，然后只发给审核通过的考生
        List<NotificationTargetInput> targets = new ArrayList<>();
        ExamSubjectEntity subject = examSubjectRepository.findByName(entity.getExamType()).orElse(null);
        if (subject != null) {
            List<RegistrationInfoEntity> approvedRegistrations =
                registrationInfoRepository.findBySubjectIdAndStatus(subject.getId(), "APPROVED");
            for (RegistrationInfoEntity reg : approvedRegistrations) {
                if (reg.getUser() != null) {
                    NotificationTargetInput target = new NotificationTargetInput();
                    target.setTargetType("user");
                    target.setTargetValue(String.valueOf(reg.getUser().getId()));
                    targets.add(target);
                }
            }
        }

        // 如果没有找到通过审核的考生，不发送通知
        if (targets.isEmpty()) {
            throw new IllegalStateException("未找到审核通过的考生，无法发送通知");
        }

        notificationInput.setTargets(targets);
        NotificationModel created = notificationService.create(notificationInput, operatorId);
        PublishNotificationInput publishInput = new PublishNotificationInput();
        publishInput.setNotificationId(created.id());
        return notificationService.publish(publishInput, operatorId);
    }

    private void applyInput(ExamResultPreNotificationEntity entity, ExamResultPreNotificationInput input) {
        entity.setExamType(normalize(input.getExamType()));
        entity.setExamYear(input.getExamYear());
        entity.setQueryTime(parseQueryTime(input.getQueryTime()));
        entity.setTitle(normalize(input.getTitle()));
        entity.setContent(normalize(input.getContent()));
    }

    private OffsetDateTime parseQueryTime(String value) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("请填写成绩查询开放时间");
        }
        try {
            return OffsetDateTime.parse(value.trim());
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("成绩查询时间格式不正确，应为 ISO-8601，如 2025-03-10T10:00:00+08:00", ex);
        }
    }

    private ExamResultPreNotificationModel map(ExamResultPreNotificationEntity entity) {
        return new ExamResultPreNotificationModel(
            entity.getId(),
            entity.getExamType(),
            entity.getExamYear(),
            entity.getQueryTime() != null ? entity.getQueryTime().toString() : null,
            entity.getTitle(),
            entity.getContent(),
            entity.getStatus() != null ? entity.getStatus().name() : null,
            entity.getLastNotificationId(),
            entity.getLastPublishedAt() != null ? entity.getLastPublishedAt().toString() : null,
            entity.getCreatedBy(),
            entity.getCreatedAt() != null ? entity.getCreatedAt().toString() : null,
            entity.getUpdatedAt() != null ? entity.getUpdatedAt().toString() : null
        );
    }

    private String buildNotificationContent(ExamResultPreNotificationEntity entity) {
        StringBuilder builder = new StringBuilder();
        builder.append("考试类型：").append(entity.getExamType()).append('\n');
        builder.append("考试年度：").append(entity.getExamYear()).append('\n');
        builder.append("成绩查询开放时间：")
            .append(entity.getQueryTime() != null ? entity.getQueryTime().format(QUERY_TIME_FORMATTER) : "")
            .append('\n')
            .append('\n');
        builder.append(entity.getContent());
        return builder.toString();
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
