package com.ruangong.service;

import com.ruangong.entity.ExamCenterEntity;
import com.ruangong.entity.ExamInvigilatorAssignmentEntity;
import com.ruangong.entity.ExamRoomEntity;
import com.ruangong.entity.ExamScheduleEntity;
import com.ruangong.entity.ExamSessionEntity;
import com.ruangong.entity.ExamSubjectEntity;
import com.ruangong.entity.UserEntity;
import com.ruangong.entity.TeacherProfileEntity;
import com.ruangong.model.InvigilatorAssignmentModel;
import com.ruangong.model.InvigilatorAssignmentStatsModel;
import com.ruangong.model.input.AssignInvigilatorsInput;
import com.ruangong.model.input.NotificationInput;
import com.ruangong.model.input.NotificationTargetInput;
import com.ruangong.model.input.PublishNotificationInput;
import com.ruangong.repository.ExamInvigilatorAssignmentRepository;
import com.ruangong.repository.ExamScheduleRepository;
import com.ruangong.repository.UserRepository;
import com.ruangong.repository.TeacherProfileRepository;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class InvigilatorAssignmentService {

    private final ExamInvigilatorAssignmentRepository assignmentRepository;
    private final ExamScheduleRepository examScheduleRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final TeacherProfileRepository teacherProfileRepository;

    public InvigilatorAssignmentService(
        ExamInvigilatorAssignmentRepository assignmentRepository,
        ExamScheduleRepository examScheduleRepository,
        UserRepository userRepository,
        NotificationService notificationService,
        TeacherProfileRepository teacherProfileRepository
    ) {
        this.assignmentRepository = assignmentRepository;
        this.examScheduleRepository = examScheduleRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.teacherProfileRepository = teacherProfileRepository;
    }

    public List<InvigilatorAssignmentModel> assign(AssignInvigilatorsInput input, Long operatorId) {
        if (input.getTeacherUserIds() == null || input.getTeacherUserIds().isEmpty()) {
            throw new IllegalArgumentException("请选择至少一位监考老师");
        }
        if (input.getTeacherUserIds().size() > 2) {
            throw new IllegalArgumentException("每个考场最多分配 2 名监考老师");
        }
        List<Long> teacherIds = input.getTeacherUserIds().stream()
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        if (teacherIds.isEmpty()) {
            throw new IllegalArgumentException("请选择有效的监考老师");
        }
        ExamScheduleEntity schedule = examScheduleRepository.findById(input.getScheduleId())
            .orElseThrow(() -> new IllegalArgumentException("排考记录不存在"));
        List<ExamInvigilatorAssignmentEntity> created = new ArrayList<>();
        if (Boolean.TRUE.equals(input.getReplaceExisting())) {
            List<ExamInvigilatorAssignmentEntity> existing = assignmentRepository.findBySchedule_Id(schedule.getId());
            for (ExamInvigilatorAssignmentEntity entity : existing) {
                if (!teacherIds.contains(entity.getTeacher().getId())) {
                    assignmentRepository.delete(entity);
                }
            }
        }
        for (Long teacherId : teacherIds) {
            if (teacherId == null) {
                continue;
            }
            UserEntity teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在: " + teacherId));
            ensureIsTeacher(teacher);
            if (assignmentRepository.existsBySchedule_IdAndTeacher_Id(schedule.getId(), teacher.getId())) {
                continue;
            }
            if (!StringUtils.hasText(teacher.getPhone())) {
                throw new IllegalArgumentException("监考老师缺少手机号，无法通知: " + resolveTeacherName(teacher.getId()));
            }
            ExamInvigilatorAssignmentEntity entity = new ExamInvigilatorAssignmentEntity();
            entity.setSchedule(schedule);
            entity.setTeacher(teacher);
            entity.setAssignedBy(operatorId);
            entity.setAssignedAt(OffsetDateTime.now());
            assignmentRepository.save(entity);
            created.add(entity);
            dispatchNotification(teacher, schedule);
        }
        return created.stream().map(this::map).toList();
    }

    @Transactional(readOnly = true)
    public List<InvigilatorAssignmentModel> list(Long scheduleId, Long teacherUserId) {
        List<ExamInvigilatorAssignmentEntity> entities;
        if (scheduleId != null) {
            entities = assignmentRepository.findBySchedule_Id(scheduleId);
        } else if (teacherUserId != null) {
            entities = assignmentRepository.findByTeacher_Id(teacherUserId);
        } else {
            entities = assignmentRepository.findAll();
        }
        return entities.stream().map(this::map).toList();
    }

    @Transactional(readOnly = true)
    public InvigilatorAssignmentStatsModel stats(Long subjectId, Long sessionId) {
        List<ExamScheduleEntity> schedules;
        if (subjectId != null) {
            schedules = examScheduleRepository.findByExamSubjectId(subjectId);
        } else if (sessionId != null) {
            schedules = examScheduleRepository.findByExamSessionId(sessionId);
        } else {
            schedules = examScheduleRepository.findAll();
        }
        int total = schedules.size();
        int assigned = 0;
        if (!schedules.isEmpty()) {
            List<Long> scheduleIds = schedules.stream()
                .map(ExamScheduleEntity::getId)
                .toList();
            List<ExamInvigilatorAssignmentEntity> assignments = assignmentRepository.findBySchedule_IdIn(scheduleIds);
            assigned = (int) assignments.stream()
                .map(entity -> entity.getSchedule().getId())
                .distinct()
                .count();
        }
        int teacherCount = (int) userRepository.findByRoleName("teacher").stream()
            .filter(user -> Boolean.TRUE.equals(user.getIsActive()))
            .count();
        return new InvigilatorAssignmentStatsModel(total, assigned, teacherCount);
    }

    public boolean remove(Long id) {
        if (!assignmentRepository.existsById(id)) {
            throw new IllegalArgumentException("监考分配不存在");
        }
        assignmentRepository.deleteById(id);
        return true;
    }

    private void ensureIsTeacher(UserEntity teacher) {
        boolean hasRole = teacher.getRoles().stream()
            .anyMatch(role -> "teacher".equalsIgnoreCase(role.getName()));
        if (!hasRole) {
            throw new IllegalArgumentException("仅可选择教师角色的用户");
        }
    }

    private void dispatchNotification(UserEntity teacher, ExamScheduleEntity schedule) {
        if (!StringUtils.hasText(teacher.getPhone())) {
            return;
        }
        NotificationInput input = new NotificationInput();
        input.setTitle("监考任务通知");
        input.setType("INVIGILATOR_ASSIGNMENT");
        input.setChannel("web");
        input.setContent(buildContent(schedule));
        NotificationTargetInput target = new NotificationTargetInput();
        target.setTargetType("user");
        target.setTargetValue(teacher.getPhone());
        input.setTargets(Collections.singletonList(target));
        var notification = notificationService.create(input, teacher.getId());
        PublishNotificationInput publishInput = new PublishNotificationInput();
        publishInput.setNotificationId(notification.id());
        notificationService.publish(publishInput, teacher.getId());
    }

    private String buildContent(ExamScheduleEntity schedule) {
        ExamRoomEntity room = schedule.getExamRoom();
        ExamCenterEntity center = room != null ? room.getCenter() : null;
        ExamSessionEntity session = schedule.getExamSession();
        ExamSubjectEntity subject = schedule.getExamSubject();
        StringBuilder builder = new StringBuilder();
        builder.append("您被安排监考如下考试：\n");
        builder.append("- 科目：").append(subject != null ? subject.getName() : "未知").append("\n");
        if (session != null) {
            builder.append("- 场次：").append(session.getName());
            if (session.getStartTime() != null) {
                builder.append("（").append(session.getStartTime()).append(" - ");
                builder.append(session.getEndTime()).append("）");
            }
            builder.append("\n");
        }
        builder.append("- 考点：").append(center != null ? center.getName() : "未知");
        if (center != null && StringUtils.hasText(center.getAddress())) {
            builder.append("（").append(center.getAddress()).append("）");
        }
        builder.append("\n");
        builder.append("- 考场：").append(room != null ? room.getRoomNumber() : "未知");
        if (room != null && StringUtils.hasText(room.getName())) {
            builder.append("（").append(room.getName()).append("）");
        }
        builder.append("\n");
        builder.append("请提前 30 分钟到场签到，若有冲突请尽快联系考务组。");
        return builder.toString();
    }

    private InvigilatorAssignmentModel map(ExamInvigilatorAssignmentEntity entity) {
        ExamScheduleEntity schedule = entity.getSchedule();
        ExamRoomEntity room = schedule.getExamRoom();
        ExamCenterEntity center = room != null ? room.getCenter() : null;
        ExamSessionEntity session = schedule.getExamSession();
        ExamSubjectEntity subject = schedule.getExamSubject();
        UserEntity teacher = entity.getTeacher();
        String teacherName = resolveTeacherName(teacher != null ? teacher.getId() : null);
        return new InvigilatorAssignmentModel(
            entity.getId(),
            schedule != null ? schedule.getId() : null,
            teacher != null ? teacher.getId() : null,
            teacherName,
            teacher != null ? teacher.getPhone() : null,
            subject != null ? subject.getName() : null,
            session != null ? session.getName() : null,
            session != null && session.getStartTime() != null ? session.getStartTime().toString() : null,
            session != null && session.getEndTime() != null ? session.getEndTime().toString() : null,
            center != null ? center.getName() : null,
            center != null ? center.getAddress() : null,
            room != null ? room.getName() : null,
            room != null ? room.getRoomNumber() : null,
            entity.getAssignedBy(),
            entity.getAssignedAt() != null ? entity.getAssignedAt().toString() : null
        );
    }

    private String resolveTeacherName(Long userId) {
        if (userId == null) {
            return null;
        }
        return teacherProfileRepository.findByUserId(userId)
            .map(TeacherProfileEntity::getFullName)
            .orElse(null);
    }
}
