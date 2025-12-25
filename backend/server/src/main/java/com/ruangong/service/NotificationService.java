package com.ruangong.service;

import com.ruangong.entity.NotificationEntity;
import com.ruangong.entity.NotificationLogEntity;
import com.ruangong.entity.NotificationStatus;
import com.ruangong.entity.NotificationTargetEntity;
import com.ruangong.entity.NotificationTemplateEntity;
import com.ruangong.entity.RegistrationInfoEntity;
import com.ruangong.entity.UserEntity;
import com.ruangong.model.NotificationLogModel;
import com.ruangong.model.NotificationModel;
import com.ruangong.model.NotificationTargetModel;
import com.ruangong.model.NotificationTemplateModel;
import com.ruangong.model.input.NotificationInput;
import com.ruangong.model.input.NotificationTargetInput;
import com.ruangong.model.input.NotificationTemplateInput;
import com.ruangong.model.input.PublishNotificationInput;
import com.ruangong.repository.NotificationLogRepository;
import com.ruangong.repository.NotificationRepository;
import com.ruangong.repository.NotificationTargetRepository;
import com.ruangong.repository.NotificationTemplateRepository;
import com.ruangong.repository.RegistrationInfoRepository;
import com.ruangong.repository.UserRepository;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationTemplateRepository notificationTemplateRepository;
    private final NotificationTargetRepository notificationTargetRepository;
    private final NotificationLogRepository notificationLogRepository;
    private final UserRepository userRepository;
    private final RegistrationInfoRepository registrationInfoRepository;

    public NotificationService(
        NotificationRepository notificationRepository,
        NotificationTemplateRepository notificationTemplateRepository,
        NotificationTargetRepository notificationTargetRepository,
        NotificationLogRepository notificationLogRepository,
        UserRepository userRepository,
        RegistrationInfoRepository registrationInfoRepository
    ) {
        this.notificationRepository = notificationRepository;
        this.notificationTemplateRepository = notificationTemplateRepository;
        this.notificationTargetRepository = notificationTargetRepository;
        this.notificationLogRepository = notificationLogRepository;
        this.userRepository = userRepository;
        this.registrationInfoRepository = registrationInfoRepository;
    }

    public NotificationModel create(NotificationInput input, Long operatorId) {
        NotificationEntity entity = new NotificationEntity();
        applyNotification(entity, input, operatorId);
        entity = notificationRepository.save(entity);
        return map(entity, true);
    }

    public NotificationModel update(Long id, NotificationInput input, Long operatorId) {
        NotificationEntity entity = notificationRepository.findWithTargetsById(id);
        if (entity == null) {
            throw new IllegalArgumentException("通知不存在");
        }
        if (entity.getStatus() == NotificationStatus.PUBLISHED) {
            throw new IllegalStateException("已发布通知不可修改");
        }
        applyNotification(entity, input, operatorId);
        entity = notificationRepository.save(entity);
        return map(entity, true);
    }

    public NotificationModel publish(PublishNotificationInput input, Long operatorId) {
        NotificationEntity entity = notificationRepository.findWithTargetsById(input.getNotificationId());
        if (entity == null) {
            throw new IllegalArgumentException("通知不存在");
        }
        if (!"web".equalsIgnoreCase(entity.getChannel())) {
            throw new IllegalArgumentException("当前仅支持网页弹窗渠道");
        }
        if (entity.getStatus() == NotificationStatus.PUBLISHED) {
            return map(entity, true);
        }
        if (entity.getTargets() == null || entity.getTargets().isEmpty()) {
            throw new IllegalArgumentException("目标范围不能为空");
        }
        OffsetDateTime now = OffsetDateTime.now();
        entity.setUpdatedAt(now);
        if (entity.getScheduledAt() != null && entity.getScheduledAt().isAfter(now)) {
            entity.setStatus(NotificationStatus.SCHEDULED);
            notificationRepository.save(entity);
            recordLog(entity, "web", "scheduled", "SCHEDULED", null);
            return map(entity, true);
        }
        entity.setStatus(NotificationStatus.PUBLISHED);
        notificationRepository.save(entity);
        dispatchWeb(entity);
        return map(entity, true);
    }

    public NotificationModel withdraw(PublishNotificationInput input, Long operatorId) {
        NotificationEntity entity = notificationRepository.findWithTargetsById(input.getNotificationId());
        if (entity == null) {
            throw new IllegalArgumentException("通知不存在");
        }
        entity.setStatus(NotificationStatus.CANCELLED);
        entity.setUpdatedAt(OffsetDateTime.now());
        notificationRepository.save(entity);
        recordLog(entity, "system", "withdraw", "CANCELLED", null);
        return map(entity, true);
    }

    @Transactional(readOnly = true)
    public List<NotificationModel> list(String keyword, String status, Long userId, List<String> roles) {
        NotificationStatus statusFilter = parseStatusOrNull(status);

        // 管理员可以看到所有通知
        boolean isAdmin = roles != null && roles.contains("admin");

        // 获取用户信息用于过滤
        String userPhone = null;
        Set<Long> userSubjectIds = new java.util.HashSet<>();
        if (!isAdmin && userId != null) {
            UserEntity user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                userPhone = user.getPhone();
            }
            // 获取用户报名的科目ID列表
            registrationInfoRepository.findByUserId(userId).forEach(reg -> {
                if (reg.getSubject() != null) {
                    userSubjectIds.add(reg.getSubject().getId());
                }
            });
        }

        final String finalUserPhone = userPhone;
        final Set<Long> finalUserSubjectIds = userSubjectIds;

        return notificationRepository.findAllWithTargets().stream()
            .filter(entity -> statusFilter == null || entity.getStatus() == statusFilter)
            .filter(entity -> matchesKeyword(entity, keyword))
            .filter(entity -> isAdmin || matchesUserTarget(entity, userId, finalUserPhone, roles, finalUserSubjectIds))
            .map(entity -> map(entity, false))
            .toList();
    }

    /**
     * 检查通知的目标是否匹配当前用户
     */
    private boolean matchesUserTarget(NotificationEntity entity, Long userId, String userPhone, List<String> roles, Set<Long> userSubjectIds) {
        if (entity.getTargets() == null || entity.getTargets().isEmpty()) {
            return false;
        }
        for (NotificationTargetEntity target : entity.getTargets()) {
            String type = target.getTargetType();
            String value = target.getTargetValue();
            if (type == null) continue;

            switch (type.toLowerCase(Locale.ROOT)) {
                case "all":
                    return true;
                case "role":
                    if (roles != null && value != null && roles.stream().anyMatch(r -> r.equalsIgnoreCase(value))) {
                        return true;
                    }
                    break;
                case "user":
                    if (value != null) {
                        // target_value 可能是用户ID或手机号
                        try {
                            Long targetUserId = Long.parseLong(value);
                            if (userId != null && userId.equals(targetUserId)) {
                                return true;
                            }
                        } catch (NumberFormatException e) {
                            // 如果不是数字，则按手机号匹配
                            if (userPhone != null && userPhone.equals(value)) {
                                return true;
                            }
                        }
                    }
                    break;
                case "subject":
                    if (value != null) {
                        try {
                            Long subjectId = Long.parseLong(value);
                            if (userSubjectIds.contains(subjectId)) {
                                return true;
                            }
                        } catch (NumberFormatException ignored) {
                        }
                    }
                    break;
            }
        }
        return false;
    }

    @Transactional(readOnly = true)
    public NotificationModel detail(Long id) {
        NotificationEntity entity = notificationRepository.findWithTargetsById(id);
        if (entity == null) {
            throw new IllegalArgumentException("通知不存在");
        }
        return map(entity, true);
    }

    @Transactional(readOnly = true)
    public List<NotificationLogModel> logs(Long id) {
        return notificationLogRepository.findByNotificationIdOrderByCreatedAtDesc(id).stream()
            .map(this::mapLog)
            .toList();
    }

    public NotificationTemplateModel createTemplate(NotificationTemplateInput input) {
        NotificationTemplateEntity entity = new NotificationTemplateEntity();
        entity.setName(input.getName());
        entity.setType(input.getType());
        entity.setContent(input.getContent());
        entity.setVariables(normalize(input.getVariables()));
        entity = notificationTemplateRepository.save(entity);
        return mapTemplate(entity);
    }

    public NotificationTemplateModel updateTemplate(Long id, NotificationTemplateInput input) {
        NotificationTemplateEntity entity = notificationTemplateRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("模板不存在"));
        entity.setName(input.getName());
        entity.setType(input.getType());
        entity.setContent(input.getContent());
        entity.setVariables(normalize(input.getVariables()));
        entity = notificationTemplateRepository.save(entity);
        return mapTemplate(entity);
    }

    @Transactional(readOnly = true)
    public List<NotificationTemplateModel> listTemplates() {
        return notificationTemplateRepository.findAll().stream()
            .map(this::mapTemplate)
            .toList();
    }

    @Transactional(readOnly = true)
    public NotificationTemplateModel templateDetail(Long id) {
        NotificationTemplateEntity entity = notificationTemplateRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("模板不存在"));
        return mapTemplate(entity);
    }

    private void applyNotification(NotificationEntity entity, NotificationInput input, Long operatorId) {
        entity.setTitle(input.getTitle());
        entity.setType(input.getType());
        entity.setContent(input.getContent());
        entity.setChannel(input.getChannel());
        entity.setScheduledAt(parseTimeOrNull(input.getScheduledAt()));
        entity.setUpdatedAt(OffsetDateTime.now());
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(OffsetDateTime.now());
        }
        if (operatorId != null) {
            entity.setCreatedBy(operatorId);
        }
        if (entity.getTargets() == null) {
            entity.setTargets(new ArrayList<>());
        } else {
            entity.getTargets().clear();
        }
        for (NotificationTargetInput targetInput : input.getTargets()) {
            NotificationTargetEntity target = new NotificationTargetEntity();
            target.setNotification(entity);
            target.setTargetType(targetInput.getTargetType());
            target.setTargetValue(normalize(targetInput.getTargetValue()));
            entity.getTargets().add(target);
        }
        entity.setStatus(NotificationStatus.DRAFT);
    }

    private void recordLog(NotificationEntity entity, String channel, String target, String status, String error) {
        NotificationLogEntity log = new NotificationLogEntity();
        log.setNotification(entity);
        log.setChannel(channel);
        log.setTarget(target);
        log.setStatus(status);
        log.setError(error);
        log.setCreatedAt(OffsetDateTime.now());
        notificationLogRepository.save(log);
    }

    /**
     * 站内弹窗提醒：仅记录日志，便于前端轮询/推送展示。
     */
    private void dispatchWeb(NotificationEntity entity) {
        Set<Recipient> recipients = new LinkedHashSet<>();
        if (entity.getTargets() != null) {
            for (NotificationTargetEntity target : entity.getTargets()) {
                recipients.addAll(resolveRecipients(target));
            }
        }
        if (recipients.isEmpty()) {
            recordLog(entity, "web", "n/a", "FAILED", "未匹配到可通知的用户");
            return;
        }
        for (Recipient recipient : recipients) {
            String target = recipient.name() != null ? recipient.name() : safe(recipient.phone());
            // 站内弹窗暂不落库到单独表，先记录日志以便审计
            recordLog(entity, "web", target, "SENT", null);
        }
    }

    private List<Recipient> resolveRecipients(NotificationTargetEntity target) {
        if (target == null || !StringUtils.hasText(target.getTargetType())) {
            return List.of();
        }
        String type = target.getTargetType().toLowerCase(Locale.ROOT);
        String value = normalize(target.getTargetValue());
        List<Recipient> results = new ArrayList<>();
        switch (type) {
            case "all":
                userRepository.findAll().stream()
                    .filter(u -> Boolean.TRUE.equals(u.getIsActive()))
                    .forEach(u -> results.add(new Recipient(u.getPhone(), null, null)));
                break;
            case "role":
                if (value != null) {
                    userRepository.findByRoleName(value).stream()
                        .filter(u -> Boolean.TRUE.equals(u.getIsActive()))
                        .forEach(u -> results.add(new Recipient(u.getPhone(), null, null)));
                }
                break;
            case "subject":
                if (value != null) {
                    try {
                        Long subjectId = Long.parseLong(value);
                        registrationInfoRepository.findBySubjectId(subjectId).forEach(reg -> {
                            UserEntity user = reg.getUser();
                            if (user != null && Boolean.TRUE.equals(user.getIsActive())) {
                                results.add(new Recipient(user.getPhone(), reg.getFullName(), reg.getSubject() != null ? reg.getSubject().getName() : null));
                            }
                        });
                    } catch (NumberFormatException ignored) {
                    }
                }
                break;
            case "user":
                if (value != null) {
                    userRepository.findByPhone(value).ifPresent(u -> {
                        if (Boolean.TRUE.equals(u.getIsActive())) {
                            results.add(new Recipient(u.getPhone(), null, null));
                        }
                    });
                }
                break;
            default:
                // 未知类型不发送
                break;
        }
        return results;
    }

    private boolean matchesKeyword(NotificationEntity entity, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        String normalized = keyword.toLowerCase(Locale.ROOT);
        return (entity.getTitle() != null && entity.getTitle().toLowerCase(Locale.ROOT).contains(normalized))
            || (entity.getType() != null && entity.getType().toLowerCase(Locale.ROOT).contains(normalized));
    }

    private OffsetDateTime parseTimeOrNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value);
        } catch (Exception ex) {
            throw new IllegalArgumentException("时间格式需为 ISO-8601，示例：2025-01-01T09:00:00+08:00");
        }
    }

    private NotificationStatus parseStatusOrNull(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        try {
            return NotificationStatus.valueOf(status.toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            throw new IllegalArgumentException("非法的通知状态筛选");
        }
    }

    private NotificationModel map(NotificationEntity entity, boolean includeLogs) {
        List<NotificationTargetModel> targets = entity.getTargets() != null
            ? entity.getTargets().stream().map(this::mapTarget).toList()
            : List.of();
        List<NotificationLogModel> logs = includeLogs
            ? logs(entity.getId())
            : List.of();
        return new NotificationModel(
            entity.getId(),
            entity.getTitle(),
            entity.getType(),
            entity.getContent(),
            entity.getChannel(),
            entity.getStatus() != null ? entity.getStatus().name() : null,
            entity.getScheduledAt() != null ? entity.getScheduledAt().toString() : null,
            entity.getCreatedBy(),
            entity.getCreatedAt() != null ? entity.getCreatedAt().toString() : null,
            entity.getUpdatedAt() != null ? entity.getUpdatedAt().toString() : null,
            targets,
            logs
        );
    }

    private NotificationTargetModel mapTarget(NotificationTargetEntity entity) {
        return new NotificationTargetModel(
            entity.getId(),
            entity.getTargetType(),
            entity.getTargetValue()
        );
    }

    private NotificationLogModel mapLog(NotificationLogEntity entity) {
        return new NotificationLogModel(
            entity.getId(),
            entity.getChannel(),
            entity.getTarget(),
            entity.getStatus(),
            entity.getError(),
            entity.getCreatedAt() != null ? entity.getCreatedAt().toString() : null
        );
    }

    private NotificationTemplateModel mapTemplate(NotificationTemplateEntity entity) {
        return new NotificationTemplateModel(
            entity.getId(),
            entity.getName(),
            entity.getType(),
            entity.getContent(),
            entity.getVariables()
        );
    }

    private record Recipient(String phone, String name, String subjectName) {}

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
