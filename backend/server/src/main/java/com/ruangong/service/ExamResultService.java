package com.ruangong.service;

import com.ruangong.entity.ExamResultDetailEntity;
import com.ruangong.entity.ExamResultRecordEntity;
import com.ruangong.entity.ExamSubjectEntity;
import com.ruangong.entity.RegistrationInfoEntity;
import com.ruangong.model.ExamResultDetailModel;
import com.ruangong.model.ExamResultModel;
import com.ruangong.model.input.ExamResultDetailInput;
import com.ruangong.model.input.ExamResultQueryInput;
import com.ruangong.model.input.UpsertExamResultInput;
import com.ruangong.repository.ExamResultRecordRepository;
import com.ruangong.repository.ExamSubjectRepository;
import com.ruangong.repository.RegistrationInfoRepository;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class ExamResultService {

    private final ExamResultRecordRepository examResultRecordRepository;
    private final RegistrationInfoRepository registrationInfoRepository;
    private final ExamSubjectRepository examSubjectRepository;
    private final ExamResultReleaseSettingService examResultReleaseSettingService;

    public ExamResultService(
        ExamResultRecordRepository examResultRecordRepository,
        RegistrationInfoRepository registrationInfoRepository,
        ExamSubjectRepository examSubjectRepository,
        ExamResultReleaseSettingService examResultReleaseSettingService
    ) {
        this.examResultRecordRepository = examResultRecordRepository;
        this.registrationInfoRepository = registrationInfoRepository;
        this.examSubjectRepository = examSubjectRepository;
        this.examResultReleaseSettingService = examResultReleaseSettingService;
    }

    @Transactional(readOnly = true)
    public ExamResultModel queryResult(ExamResultQueryInput input) {
        if (input == null) {
            throw new IllegalArgumentException("查询条件不能为空");
        }
        String examType = normalize(input.getExamType());
        Integer examYear = input.getExamYear();
        if (!StringUtils.hasText(examType) || examYear == null) {
            throw new IllegalArgumentException("请选择考试类型与考试年份");
        }

        examResultReleaseSettingService.processSchedules();

        ExamResultRecordEntity record = locateRecord(examType, examYear, input)
            .orElseThrow(() -> new IllegalArgumentException("未找到成绩信息"));

        examResultReleaseSettingService.applyReleaseTimeIfReleased(record);
        ensureReleased(record);
        return map(record);
    }

    public ExamResultModel upsertResult(UpsertExamResultInput input) {
        RegistrationInfoEntity info = registrationInfoRepository.findById(input.getRegistrationInfoId())
            .orElseThrow(() -> new IllegalArgumentException("报名信息不存在"));

        ExamResultRecordEntity entity = examResultRecordRepository.findByRegistrationInfo_Id(info.getId())
            .orElseGet(ExamResultRecordEntity::new);

        entity.setRegistrationInfo(info);
        entity.setExamType(normalize(input.getExamType()));
        entity.setExamYear(input.getExamYear());
        entity.setTicketNumber(normalize(input.getTicketNumber()));
        entity.setReleaseTime(parseReleaseTime(input.getReleaseTime()));
        entity.setTotalScore(input.getTotalScore());
        entity.setTotalPassLine(input.getTotalPassLine());
        entity.setQualificationStatus(normalize(input.getQualificationStatus()));
        entity.setQualificationNote(normalize(input.getQualificationNote()));
        entity.setReportUrl(normalize(input.getReportUrl()));
        OffsetDateTime now = OffsetDateTime.now();
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(now);
        }
        entity.setUpdatedAt(now);
        applySubjectDetails(entity, input.getSubjects());
        ExamResultRecordEntity saved = examResultRecordRepository.save(entity);
        examResultReleaseSettingService.applyReleaseTimeIfReleased(saved);
        return map(saved);
    }

    private Optional<ExamResultRecordEntity> locateRecord(
        String examType,
        Integer examYear,
        ExamResultQueryInput input
    ) {
        if (StringUtils.hasText(input.getTicketNumber())) {
            return examResultRecordRepository.findByExamTypeIgnoreCaseAndExamYearAndTicketNumberIgnoreCase(
                examType,
                examYear,
                normalize(input.getTicketNumber())
            );
        }
        if (StringUtils.hasText(input.getFullName()) && StringUtils.hasText(input.getIdCardNumber())) {
            return examResultRecordRepository
                .findByExamTypeIgnoreCaseAndExamYearAndRegistrationInfo_FullNameIgnoreCaseAndRegistrationInfo_IdCardNumber(
                    examType,
                    examYear,
                    normalize(input.getFullName()),
                    normalize(input.getIdCardNumber())
                );
        }
        throw new IllegalArgumentException("请输入准考证号，或姓名与身份证号");
    }

    private void applySubjectDetails(ExamResultRecordEntity entity, List<ExamResultDetailInput> inputs) {
        if (entity.getDetails() == null) {
            entity.setDetails(new ArrayList<>());
        }
        entity.getDetails().clear();
        if (CollectionUtils.isEmpty(inputs)) {
            throw new IllegalArgumentException("请至少录入一门科目的成绩");
        }
        List<ExamResultDetailEntity> details = new ArrayList<>();
        for (ExamResultDetailInput detailInput : inputs) {
            ExamResultDetailEntity detail = new ExamResultDetailEntity();
            detail.setResult(entity);
            ExamSubjectEntity subject = null;
            if (detailInput.getSubjectId() != null) {
                subject = examSubjectRepository.findById(detailInput.getSubjectId())
                    .orElseThrow(() -> new IllegalArgumentException("科目不存在: " + detailInput.getSubjectId()));
            }
            detail.setSubject(subject);
            detail.setSubjectName(subject != null ? subject.getName() : detailInput.getSubjectName());
            detail.setScore(detailInput.getScore());
            detail.setPassLine(detailInput.getPassLine());
            // 自动计算是否通过：如果未指定，则根据分数和及格线判断
            Boolean isPass = detailInput.getIsPass();
            if (isPass == null && detailInput.getScore() != null && detailInput.getPassLine() != null) {
                isPass = detailInput.getScore() >= detailInput.getPassLine();
            }
            detail.setPass(isPass);
            detail.setNationalRank(detailInput.getNationalRank());
            detail.setRemark(normalize(detailInput.getRemark()));
            details.add(detail);
        }
        entity.getDetails().addAll(details);
    }

    private void ensureReleased(ExamResultRecordEntity record) {
        OffsetDateTime releaseTime = record.getReleaseTime();
        if (releaseTime == null || releaseTime.isAfter(OffsetDateTime.now())) {
            Long subjectId = record.getRegistrationInfo() != null && record.getRegistrationInfo().getSubject() != null
                ? record.getRegistrationInfo().getSubject().getId()
                : null;
            OffsetDateTime plannedTime = examResultReleaseSettingService.plannedReleaseTime(subjectId, record.getExamYear());
            if (plannedTime != null) {
                throw new IllegalArgumentException("成绩将于 " + plannedTime + " 开放查询，请耐心等待");
            }
            throw new IllegalArgumentException("成绩尚未对外公布");
        }
    }

    private ExamResultModel map(ExamResultRecordEntity entity) {
        RegistrationInfoEntity info = entity.getRegistrationInfo();
        List<ExamResultDetailModel> subjectModels = entity.getDetails() == null
            ? Collections.emptyList()
            : entity.getDetails().stream()
                .map(this::mapDetail)
                .toList();
        return new ExamResultModel(
            entity.getId(),
            info != null ? info.getId() : null,
            info != null ? info.getFullName() : null,
            info != null ? info.getIdCardNumber() : null,
            entity.getExamType(),
            entity.getExamYear(),
            entity.getTicketNumber(),
            entity.getReleaseTime() != null ? entity.getReleaseTime().toString() : null,
            entity.getTotalScore(),
            entity.getTotalPassLine(),
            entity.getQualificationStatus(),
            entity.getQualificationNote(),
            evaluateQualification(entity),
            entity.getReportUrl(),
            subjectModels
        );
    }

    private OffsetDateTime parseReleaseTime(String releaseTime) {
        if (!StringUtils.hasText(releaseTime)) {
            return null;
        }
        try {
            return OffsetDateTime.parse(releaseTime);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("成绩发布时间格式不正确，应为 ISO-8601，如 2025-03-10T10:00:00+08:00", ex);
        }
    }

    private ExamResultDetailModel mapDetail(ExamResultDetailEntity detail) {
        return new ExamResultDetailModel(
            detail.getSubject() != null ? detail.getSubject().getId() : null,
            detail.getSubjectName(),
            detail.getScore(),
            detail.getPassLine(),
            detail.getPass(),
            detail.getNationalRank(),
            detail.getRemark()
        );
    }

    private Boolean evaluateQualification(ExamResultRecordEntity entity) {
        if (StringUtils.hasText(entity.getQualificationStatus())) {
            return "PASSED".equalsIgnoreCase(entity.getQualificationStatus())
                || "QUALIFIED".equalsIgnoreCase(entity.getQualificationStatus())
                || "通过".equals(entity.getQualificationStatus());
        }
        if (entity.getTotalScore() != null && entity.getTotalPassLine() != null) {
            return entity.getTotalScore() >= entity.getTotalPassLine();
        }
        return null;
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    /**
     * 批量查询成绩（按报名ID列表）- 用于成绩管理页面显示已录入的成绩
     */
    @Transactional(readOnly = true)
    public List<ExamResultModel> queryResultsByRegistrationIds(List<Long> registrationInfoIds) {
        if (CollectionUtils.isEmpty(registrationInfoIds)) {
            return Collections.emptyList();
        }
        List<ExamResultRecordEntity> records = examResultRecordRepository.findByRegistrationInfo_IdIn(registrationInfoIds);
        return records.stream().map(this::map).toList();
    }
}
