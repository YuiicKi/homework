package com.ruangong.service;

import com.ruangong.entity.ExamSubjectEntity;
import com.ruangong.entity.RegistrationAuditLogEntity;
import com.ruangong.entity.RegistrationInfoEntity;
import com.ruangong.entity.RegistrationMaterialEntity;
import com.ruangong.entity.RegistrationMaterialTemplateEntity;
import com.ruangong.entity.UserEntity;
import com.ruangong.model.RegistrationAuditLogModel;
import com.ruangong.model.RegistrationInfoModel;
import com.ruangong.model.RegistrationMaterialModel;
import com.ruangong.model.RegistrationMaterialTemplateModel;
import com.ruangong.model.input.RegistrationInfoInput;
import com.ruangong.model.input.RegistrationMaterialInput;
import com.ruangong.repository.ExamSubjectRepository;
import com.ruangong.repository.RegistrationAuditLogRepository;
import com.ruangong.repository.RegistrationInfoRepository;
import com.ruangong.repository.RegistrationMaterialRepository;
import com.ruangong.repository.RegistrationMaterialTemplateRepository;
import com.ruangong.repository.UserRepository;
import java.time.LocalDate;
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
public class RegistrationService {

    private final RegistrationInfoRepository registrationInfoRepository;
    private final RegistrationMaterialRepository registrationMaterialRepository;
    private final RegistrationMaterialTemplateRepository registrationMaterialTemplateRepository;
    private final ExamSubjectRepository examSubjectRepository;
    private final UserRepository userRepository;
    private final RegistrationAuditLogRepository registrationAuditLogRepository;

    public RegistrationService(
        RegistrationInfoRepository registrationInfoRepository,
        RegistrationMaterialRepository registrationMaterialRepository,
        RegistrationMaterialTemplateRepository registrationMaterialTemplateRepository,
        ExamSubjectRepository examSubjectRepository,
        UserRepository userRepository,
        RegistrationAuditLogRepository registrationAuditLogRepository
    ) {
        this.registrationInfoRepository = registrationInfoRepository;
        this.registrationMaterialRepository = registrationMaterialRepository;
        this.registrationMaterialTemplateRepository = registrationMaterialTemplateRepository;
        this.examSubjectRepository = examSubjectRepository;
        this.userRepository = userRepository;
        this.registrationAuditLogRepository = registrationAuditLogRepository;
    }

    public RegistrationInfoModel upsertInfo(Long userId, RegistrationInfoInput input) {
        UserEntity user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        ExamSubjectEntity subject = examSubjectRepository.findById(input.getSubjectId())
            .orElseThrow(() -> new IllegalArgumentException("科目不存在"));

        RegistrationInfoEntity entity = registrationInfoRepository.findByUserIdAndSubjectId(userId, input.getSubjectId())
            .orElseGet(RegistrationInfoEntity::new);

        entity.setUser(user);
        entity.setSubject(subject);
        entity.setFullName(input.getFullName());
        entity.setIdCardNumber(input.getIdCardNumber());
        entity.setGender(normalize(input.getGender()));
        entity.setBirthDate(parseDate(input.getBirthDate()));
        entity.setPhone(normalize(input.getPhone()));
        entity.setEmail(normalize(input.getEmail()));
        entity.setUpdatedAt(OffsetDateTime.now());
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(OffsetDateTime.now());
        }
        entity.setStatus("PENDING_REVIEW");
        entity = registrationInfoRepository.save(entity);
        return buildModel(entity, true, true);
    }

    public RegistrationMaterialModel addOrUpdateMaterial(Long userId, RegistrationMaterialInput input) {
        RegistrationInfoEntity info = registrationInfoRepository.findById(input.getRegistrationInfoId())
            .orElseThrow(() -> new IllegalArgumentException("报名信息不存在"));
        ensureOwner(info, userId);
        RegistrationMaterialTemplateEntity template = registrationMaterialTemplateRepository.findByType(input.getType())
            .orElse(null);
        validateFile(input, template);

        RegistrationMaterialEntity entity = new RegistrationMaterialEntity();
        entity.setRegistrationInfo(info);
        entity.setType(input.getType());
        entity.setFileUrl(input.getFileUrl());
        entity.setFileFormat(input.getFileFormat());
        entity.setFileSize(input.getFileSize());
        entity.setStatus("UPLOADED");
        entity.setNote(normalize(input.getNote()));
        entity.setCreatedAt(OffsetDateTime.now());
        entity = registrationMaterialRepository.save(entity);
        return mapMaterial(entity);
    }

    public boolean deleteMaterial(Long userId, Long materialId) {
        RegistrationMaterialEntity entity = registrationMaterialRepository.findById(materialId)
            .orElseThrow(() -> new IllegalArgumentException("材料不存在"));
        ensureOwner(entity.getRegistrationInfo(), userId);
        registrationMaterialRepository.delete(entity);
        return true;
    }

    @Transactional(readOnly = true)
    public RegistrationInfoModel current(Long userId, Long subjectId) {
        RegistrationInfoEntity entity = registrationInfoRepository.findByUserIdAndSubjectId(userId, subjectId)
            .orElse(null);
        if (entity == null) {
            return null;
        }
        return buildModel(entity, true, true);
    }

    @Transactional(readOnly = true)
    public List<RegistrationMaterialTemplateModel> listTemplates() {
        return registrationMaterialTemplateRepository.findAll().stream()
            .map(this::mapTemplate)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<RegistrationInfoModel> pendingList() {
        List<RegistrationInfoEntity> all = registrationInfoRepository.findAll();
        return all.stream()
            .filter(entity -> {
                String st = entity.getStatus();
                return st == null
                    || "PENDING".equalsIgnoreCase(st)
                    || "PENDING_REVIEW".equalsIgnoreCase(st)
                    || "COMPLETED".equalsIgnoreCase(st);
            })
            .map(entity -> buildModel(entity, true, true))
            .toList();
    }

    @Transactional(readOnly = true)
    public RegistrationInfoModel auditDetail(Long registrationInfoId) {
        RegistrationInfoEntity entity = registrationInfoRepository.findById(registrationInfoId)
            .orElseThrow(() -> new IllegalArgumentException("报名信息不存在"));
        return buildModel(entity, true, true);
    }

    public RegistrationInfoModel approve(Long registrationInfoId, Long operatorId) {
        RegistrationInfoEntity info = registrationInfoRepository.findById(registrationInfoId)
            .orElseThrow(() -> new IllegalArgumentException("报名信息不存在"));
        info.setStatus("APPROVED");
        info.setUpdatedAt(OffsetDateTime.now());
        registrationInfoRepository.save(info);
        registrationMaterialRepository.findByRegistrationInfoId(info.getId())
            .forEach(mat -> {
                mat.setStatus("APPROVED");
                mat.setUpdatedAt(OffsetDateTime.now());
                registrationMaterialRepository.save(mat);
            });
        recordAudit(info, "APPROVED", null, operatorId);
        return buildModel(info, true, true);
    }

    public RegistrationInfoModel reject(Long registrationInfoId, String reason, Long operatorId) {
        if (!StringUtils.hasText(reason)) {
            throw new IllegalArgumentException("驳回需填写理由");
        }
        RegistrationInfoEntity info = registrationInfoRepository.findById(registrationInfoId)
            .orElseThrow(() -> new IllegalArgumentException("报名信息不存在"));
        info.setStatus("REJECTED");
        info.setUpdatedAt(OffsetDateTime.now());
        registrationInfoRepository.save(info);
        registrationMaterialRepository.findByRegistrationInfoId(info.getId())
            .forEach(mat -> {
                mat.setStatus("REJECTED");
                mat.setNote(reason);
                mat.setUpdatedAt(OffsetDateTime.now());
                registrationMaterialRepository.save(mat);
            });
        recordAudit(info, "REJECTED", reason, operatorId);
        return buildModel(info, true, true);
    }

    @Transactional(readOnly = true)
    public List<RegistrationAuditLogModel> auditLogs(Long registrationInfoId) {
        return registrationAuditLogRepository.findByRegistrationInfoIdOrderByCreatedAtDesc(registrationInfoId).stream()
            .map(log -> new RegistrationAuditLogModel(
                log.getId(),
                log.getRegistrationInfo() != null ? log.getRegistrationInfo().getId() : null,
                log.getResult(),
                log.getReason(),
                log.getOperatorId(),
                log.getCreatedAt() != null ? log.getCreatedAt().toString() : null
            ))
            .toList();
    }

    public RegistrationMaterialTemplateModel upsertTemplate(RegistrationMaterialTemplateModel templateModel) {
        RegistrationMaterialTemplateEntity entity = templateModel.id() != null
            ? registrationMaterialTemplateRepository.findById(templateModel.id())
            .orElse(new RegistrationMaterialTemplateEntity())
            : new RegistrationMaterialTemplateEntity();
        entity.setType(templateModel.type());
        entity.setAllowedFormats(templateModel.allowedFormats());
        entity.setMaxSize(templateModel.maxSize());
        entity.setRequired(Boolean.TRUE.equals(templateModel.required()));
        entity.setDescription(templateModel.description());
        entity = registrationMaterialTemplateRepository.save(entity);
        return mapTemplate(entity);
    }

    public boolean deleteTemplate(Long id) {
        if (!registrationMaterialTemplateRepository.existsById(id)) {
            throw new IllegalArgumentException("模板不存在");
        }
        registrationMaterialTemplateRepository.deleteById(id);
        return true;
    }

    private void ensureOwner(RegistrationInfoEntity info, Long userId) {
        if (info.getUser() == null || info.getUser().getId() == null || !info.getUser().getId().equals(userId)) {
            throw new IllegalStateException("无权操作该报名信息");
        }
    }

    private void validateFile(RegistrationMaterialInput input, RegistrationMaterialTemplateEntity template) {
        if (template == null) {
            return;
        }
        if (template.getMaxSize() != null && input.getFileSize() != null
            && input.getFileSize() > template.getMaxSize()) {
            throw new IllegalArgumentException("文件大小超过限制");
        }
        if (StringUtils.hasText(template.getAllowedFormats())) {
            Set<String> allowed = new HashSet<>();
            for (String fmt : template.getAllowedFormats().split(",")) {
                if (StringUtils.hasText(fmt)) {
                    allowed.add(fmt.trim().toLowerCase(Locale.ROOT));
                }
            }
            if (!allowed.isEmpty() && StringUtils.hasText(input.getFileFormat())) {
                if (!allowed.contains(input.getFileFormat().toLowerCase(Locale.ROOT))) {
                    throw new IllegalArgumentException("文件格式不被允许");
                }
            }
        }
    }

    private RegistrationInfoModel buildModel(RegistrationInfoEntity entity, boolean includeMaterials, boolean includeTemplates) {
        List<RegistrationMaterialModel> materials = includeMaterials
            ? registrationMaterialRepository.findByRegistrationInfoId(entity.getId()).stream()
            .map(this::mapMaterial)
            .collect(Collectors.toList())
            : List.of();
        List<RegistrationMaterialTemplateModel> templates = includeTemplates
            ? registrationMaterialTemplateRepository.findAll().stream().map(this::mapTemplate).toList()
            : List.of();
        String status = StringUtils.hasText(entity.getStatus())
            ? entity.getStatus()
            : computeStatus(materials, templates);
        return new RegistrationInfoModel(
            entity.getId(),
            entity.getUser() != null ? entity.getUser().getId() : null,
            entity.getSubject() != null ? entity.getSubject().getId() : null,
            entity.getFullName(),
            entity.getIdCardNumber(),
            entity.getGender(),
            entity.getBirthDate() != null ? entity.getBirthDate().toString() : null,
            entity.getPhone(),
            entity.getEmail(),
            status,
            materials,
            templates
        );
    }

    private String computeStatus(List<RegistrationMaterialModel> materials,
                                 List<RegistrationMaterialTemplateModel> templates) {
        if (templates.isEmpty()) {
            return "PENDING";
        }
        Set<String> uploadedTypes = materials.stream()
            .map(RegistrationMaterialModel::type)
            .collect(Collectors.toSet());
        boolean allRequired = templates.stream()
            .filter(t -> Boolean.TRUE.equals(t.required()))
            .allMatch(t -> uploadedTypes.contains(t.type()));
        return allRequired ? "COMPLETED" : "PENDING";
    }

    private RegistrationMaterialModel mapMaterial(RegistrationMaterialEntity entity) {
        return new RegistrationMaterialModel(
            entity.getId(),
            entity.getType(),
            entity.getFileUrl(),
            entity.getFileFormat(),
            entity.getFileSize(),
            entity.getStatus(),
            entity.getNote()
        );
    }

    private RegistrationMaterialTemplateModel mapTemplate(RegistrationMaterialTemplateEntity entity) {
        return new RegistrationMaterialTemplateModel(
            entity.getId(),
            entity.getType(),
            entity.getAllowedFormats(),
            entity.getMaxSize(),
            entity.getRequired(),
            entity.getDescription()
        );
    }

    private LocalDate parseDate(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (Exception ex) {
            throw new IllegalArgumentException("出生日期格式需为 yyyy-MM-dd");
        }
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private void recordAudit(RegistrationInfoEntity info, String result, String reason, Long operatorId) {
        RegistrationAuditLogEntity log = new RegistrationAuditLogEntity();
        log.setRegistrationInfo(info);
        log.setResult(result);
        log.setReason(normalize(reason));
        log.setOperatorId(operatorId);
        log.setCreatedAt(OffsetDateTime.now());
        registrationAuditLogRepository.save(log);
    }
}
