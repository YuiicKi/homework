package com.ruangong.service;

import com.ruangong.entity.ExamResultImportItemEntity;
import com.ruangong.entity.ExamResultImportItemStatus;
import com.ruangong.entity.ExamResultImportJobEntity;
import com.ruangong.entity.ExamResultImportJobStatus;
import com.ruangong.entity.RegistrationInfoEntity;
import com.ruangong.model.ExamResultImportItemModel;
import com.ruangong.model.ExamResultImportJobModel;
import com.ruangong.model.ExamResultImportTemplateModel;
import com.ruangong.model.input.ExamResultDetailInput;
import com.ruangong.model.input.ExamResultImportInput;
import com.ruangong.model.input.UpsertExamResultInput;
import com.ruangong.repository.ExamResultImportItemRepository;
import com.ruangong.repository.ExamResultImportJobRepository;
import com.ruangong.repository.RegistrationInfoRepository;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class ExamResultImportService {

    private static final long MAX_FILE_SIZE = 20L * 1024 * 1024;
    private static final String DEFAULT_TEMPLATE_NAME = "exam-result-import-template.xlsx";
    private static final String[] TEMPLATE_HEADERS = new String[] {
        "registrationInfoId",
        "examType",
        "examYear",
        "ticketNumber",
        "subjectId",
        "subjectName",
        "subjectScore",
        "subjectPassLine",
        "subjectIsPass",
        "subjectRemark",
        "subjectNationalRank",
        "totalScore",
        "totalPassLine",
        "qualificationStatus",
        "qualificationNote"
    };

    private final ExamResultImportJobRepository jobRepository;
    private final ExamResultImportItemRepository itemRepository;
    private final RegistrationInfoRepository registrationInfoRepository;
    private final ExamResultService examResultService;

    public ExamResultImportService(
        ExamResultImportJobRepository jobRepository,
        ExamResultImportItemRepository itemRepository,
        RegistrationInfoRepository registrationInfoRepository,
        ExamResultService examResultService
    ) {
        this.jobRepository = jobRepository;
        this.itemRepository = itemRepository;
        this.registrationInfoRepository = registrationInfoRepository;
        this.examResultService = examResultService;
    }

    public ExamResultImportTemplateModel template(String format) {
        String normalized = normalize(format);
        if (!StringUtils.hasText(normalized) || "xlsx".equalsIgnoreCase(normalized)) {
            return buildExcelTemplate();
        }
        if ("csv".equalsIgnoreCase(normalized)) {
            return buildCsvTemplate();
        }
        throw new IllegalArgumentException("暂不支持的模板格式：" + format);
    }

    public ExamResultImportJobModel importFile(ExamResultImportInput input, Long operatorId) {
        byte[] data = decodeBase64(input.getContentBase64());
        if (data.length > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("文件过大，限制 20MB");
        }
        String extension = resolveExtension(input.getFileName());

        OffsetDateTime now = OffsetDateTime.now();
        ExamResultImportJobEntity job = new ExamResultImportJobEntity();
        job.setFileName(input.getFileName());
        job.setFileSize((long) data.length);
        job.setFileType(extension);
        job.setStatus(ExamResultImportJobStatus.PROCESSING);
        job.setCreatedBy(operatorId);
        job.setCreatedAt(now);
        job = jobRepository.save(job);

        List<ExamResultImportItemEntity> jobItems = new ArrayList<>();
        try {
            List<RowPayload> rows = switch (extension) {
                case "xlsx" -> parseExcelRows(data);
                case "csv" -> parseCsvRows(data);
                default -> throw new IllegalArgumentException("暂不支持的文件类型：" + extension);
            };
            job.setTotalCount(rows.size());

            Map<String, CandidateAggregate> aggregates = new LinkedHashMap<>();
            for (RowPayload row : rows) {
                ExamResultImportItemEntity item = new ExamResultImportItemEntity();
                item.setJob(job);
                item.setRowNumber(row.rowNumber());
                jobItems.add(item);
                try {
                    RowContext context = prepareRow(row);
                    item.setRegistrationInfoId(context.registration().getId());
                    item.setTicketNumber(context.ticketNumber());
                    item.setSubjectId(context.subjectId());

                    CandidateAggregate aggregate = aggregates.computeIfAbsent(
                        aggregateKey(context.registration().getId(), context.examYear()),
                        key -> new CandidateAggregate(context.registration().getId(), context.examYear())
                    );
                    aggregate.applyRow(context);
                    aggregate.items().add(item);
                } catch (Exception ex) {
                    item.setStatus(ExamResultImportItemStatus.FAILED);
                    item.setMessage(ex.getMessage());
                }
            }

            for (CandidateAggregate aggregate : aggregates.values()) {
                if (aggregate.items().stream().allMatch(it -> it.getStatus() == ExamResultImportItemStatus.FAILED)) {
                    continue;
                }
                try {
                    UpsertExamResultInput upsertInput = aggregate.toInput();
                    examResultService.upsertResult(upsertInput);
                    aggregate.items().stream()
                        .filter(it -> it.getStatus() == ExamResultImportItemStatus.PENDING)
                        .forEach(it -> it.setStatus(ExamResultImportItemStatus.SUCCESS));
                } catch (Exception ex) {
                    aggregate.items().stream()
                        .filter(it -> it.getStatus() == ExamResultImportItemStatus.PENDING)
                        .forEach(it -> {
                            it.setStatus(ExamResultImportItemStatus.FAILED);
                            it.setMessage(ex.getMessage());
                        });
                    if (!StringUtils.hasText(job.getErrorMessage())) {
                        job.setErrorMessage(ex.getMessage());
                    }
                }
            }

            finalizeJob(job, jobItems);
        } catch (Exception ex) {
            jobItems.stream()
                .filter(it -> it.getStatus() == ExamResultImportItemStatus.PENDING)
                .forEach(it -> {
                    it.setStatus(ExamResultImportItemStatus.FAILED);
                    it.setMessage(ex.getMessage());
                });
            job.setStatus(ExamResultImportJobStatus.FAILED);
            job.setErrorMessage(ex.getMessage());
            job.setCompletedAt(OffsetDateTime.now());
            itemRepository.saveAll(jobItems);
            jobRepository.save(job);
            return mapJob(job, jobItems);
        }
        itemRepository.saveAll(jobItems);
        jobRepository.save(job);
        return mapJob(job, jobItems);
    }

    @Transactional(readOnly = true)
    public List<ExamResultImportJobModel> jobs(ExamResultImportJobStatus status, Integer limit) {
        List<ExamResultImportJobEntity> entities = status != null
            ? jobRepository.findByStatusOrderByCreatedAtDesc(status)
            : jobRepository.findAllByOrderByCreatedAtDesc();
        if (limit != null && limit > 0 && entities.size() > limit) {
            entities = entities.subList(0, limit);
        }
        return entities.stream()
            .map(entity -> mapJob(entity, false))
            .toList();
    }

    @Transactional(readOnly = true)
    public ExamResultImportJobModel job(Long id) {
        ExamResultImportJobEntity job = jobRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("导入记录不存在"));
        List<ExamResultImportItemEntity> items = itemRepository.findByJob_Id(job.getId());
        return mapJob(job, items);
    }

    private void finalizeJob(ExamResultImportJobEntity job, List<ExamResultImportItemEntity> items) {
        int successCount = (int) items.stream()
            .filter(it -> it.getStatus() == ExamResultImportItemStatus.SUCCESS)
            .count();
        int failureCount = (int) items.stream()
            .filter(it -> it.getStatus() == ExamResultImportItemStatus.FAILED)
            .count();
        job.setSuccessCount(successCount);
        job.setFailureCount(failureCount);
        job.setCompletedAt(OffsetDateTime.now());
        if (failureCount == 0) {
            job.setStatus(ExamResultImportJobStatus.SUCCESS);
        } else if (successCount == 0) {
            job.setStatus(ExamResultImportJobStatus.FAILED);
        } else {
            job.setStatus(ExamResultImportJobStatus.PARTIAL);
        }
    }

    private RowContext prepareRow(RowPayload row) {
        String registrationStr = requireField(row.registrationInfoId(), row.rowNumber(), "registrationInfoId");
        Long registrationId = parseLong(registrationStr, row.rowNumber(), "registrationInfoId");
        RegistrationInfoEntity registration = registrationInfoRepository.findById(registrationId)
            .orElseThrow(() -> new IllegalArgumentException("报名信息不存在 (row " + row.rowNumber() + ")"));

        String examType = requireField(row.examType(), row.rowNumber(), "examType");
        Integer examYear = parseInt(requireField(row.examYear(), row.rowNumber(), "examYear"), row.rowNumber(), "examYear");
        String ticketNumber = normalize(row.ticketNumber());
        Double totalScore = parseDouble(row.totalScore(), row.rowNumber(), "totalScore");
        Double totalPassLine = parseDouble(row.totalPassLine(), row.rowNumber(), "totalPassLine");
        String qualificationStatus = normalize(row.qualificationStatus());
        String qualificationNote = normalize(row.qualificationNote());
        Long subjectId = parseLong(row.subjectId(), row.rowNumber(), "subjectId", true);
        String subjectName = requireField(row.subjectName(), row.rowNumber(), "subjectName");
        Double subjectScore = parseDouble(requireField(row.subjectScore(), row.rowNumber(), "subjectScore"), row.rowNumber(), "subjectScore");
        Double subjectPassLine = parseDouble(row.subjectPassLine(), row.rowNumber(), "subjectPassLine");
        Boolean subjectIsPass = parseBoolean(row.subjectIsPass());
        Integer subjectRank = parseInt(row.subjectNationalRank(), row.rowNumber(), "subjectNationalRank", true);
        String subjectRemark = normalize(row.subjectRemark());

        return new RowContext(
            row.rowNumber(),
            registration,
            examType,
            examYear,
            ticketNumber,
            totalScore,
            totalPassLine,
            qualificationStatus,
            qualificationNote,
            subjectId,
            subjectName,
            subjectScore,
            subjectPassLine,
            subjectIsPass,
            subjectRemark,
            subjectRank
        );
    }

    private List<RowPayload> parseExcelRows(byte[] data) throws Exception {
        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(data))) {
            Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null) {
                throw new IllegalArgumentException("Excel 中未发现数据");
            }
            List<RowPayload> rows = new ArrayList<>();
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null || isBlankRow(row)) {
                    continue;
                }
                rows.add(new RowPayload(
                    rowIndex + 1,
                    cellValue(row, 0),
                    cellValue(row, 1),
                    cellValue(row, 2),
                    cellValue(row, 3),
                    cellValue(row, 4),
                    cellValue(row, 5),
                    cellValue(row, 6),
                    cellValue(row, 7),
                    cellValue(row, 8),
                    cellValue(row, 9),
                    cellValue(row, 10),
                    cellValue(row, 11),
                    cellValue(row, 12),
                    cellValue(row, 13),
                    cellValue(row, 14)
                ));
            }
            return rows;
        }
    }

    private List<RowPayload> parseCsvRows(byte[] data) throws Exception {
        String content = new String(data, StandardCharsets.UTF_8);
        // 去除 UTF-8 BOM (Excel/前端下载模板会添加BOM以正确显示中文)
        if (content.startsWith("\uFEFF")) {
            content = content.substring(1);
        }
        try (CSVParser parser = CSVParser.parse(
            content,
            CSVFormat.DEFAULT.withFirstRecordAsHeader()
        )) {
            List<RowPayload> rows = new ArrayList<>();
            AtomicInteger rowIndex = new AtomicInteger(2);
            for (CSVRecord record : parser) {
                rows.add(new RowPayload(
                    rowIndex.getAndIncrement(),
                    record.get("registrationInfoId"),
                    record.get("examType"),
                    record.get("examYear"),
                    record.get("ticketNumber"),
                    record.get("subjectId"),
                    record.get("subjectName"),
                    record.get("subjectScore"),
                    record.get("subjectPassLine"),
                    record.get("subjectIsPass"),
                    record.get("subjectRemark"),
                    record.get("subjectNationalRank"),
                    record.get("totalScore"),
                    record.get("totalPassLine"),
                    record.get("qualificationStatus"),
                    record.get("qualificationNote")
                ));
            }
            return rows;
        }
    }

    private ExamResultImportJobModel mapJob(ExamResultImportJobEntity entity, boolean includeItems) {
        List<ExamResultImportItemEntity> items = includeItems
            ? itemRepository.findByJob_Id(entity.getId())
            : List.of();
        return mapJob(entity, items);
    }

    private ExamResultImportJobModel mapJob(ExamResultImportJobEntity entity, List<ExamResultImportItemEntity> items) {
        List<ExamResultImportItemModel> itemModels = items.stream()
            .map(item -> new ExamResultImportItemModel(
                item.getId(),
                item.getJob() != null ? item.getJob().getId() : null,
                item.getRowNumber(),
                item.getRegistrationInfoId(),
                item.getTicketNumber(),
                item.getSubjectId(),
                item.getStatus() != null ? item.getStatus().name() : null,
                item.getMessage(),
                item.getCreatedAt() != null ? item.getCreatedAt().toString() : null
            ))
            .toList();
        return new ExamResultImportJobModel(
            entity.getId(),
            entity.getFileName(),
            entity.getFileSize() != null ? entity.getFileSize().intValue() : null,
            entity.getFileType(),
            entity.getStatus() != null ? entity.getStatus().name() : null,
            entity.getTotalCount(),
            entity.getSuccessCount(),
            entity.getFailureCount(),
            entity.getErrorMessage(),
            entity.getCreatedBy(),
            entity.getCreatedAt() != null ? entity.getCreatedAt().toString() : null,
            entity.getCompletedAt() != null ? entity.getCompletedAt().toString() : null,
            itemModels
        );
    }

    private ExamResultImportTemplateModel buildExcelTemplate() {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("成绩导入");
            Row header = sheet.createRow(0);
            for (int i = 0; i < TEMPLATE_HEADERS.length; i++) {
                header.createCell(i).setCellValue(TEMPLATE_HEADERS[i]);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return new ExamResultImportTemplateModel(
                DEFAULT_TEMPLATE_NAME,
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                Base64.getEncoder().encodeToString(out.toByteArray())
            );
        } catch (Exception ex) {
            throw new IllegalStateException("生成模板失败", ex);
        }
    }

    private ExamResultImportTemplateModel buildCsvTemplate() {
        String headerRow = String.join(",", TEMPLATE_HEADERS) + "\n";
        return new ExamResultImportTemplateModel(
            "exam-result-import-template.csv",
            "text/csv",
            Base64.getEncoder().encodeToString(headerRow.getBytes(StandardCharsets.UTF_8))
        );
    }

    private byte[] decodeBase64(String contentBase64) {
        try {
            return Base64.getMimeDecoder().decode(contentBase64.replaceAll("\\s", ""));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("文件内容不是有效的 Base64 编码");
        }
    }

    private String resolveExtension(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            throw new IllegalArgumentException("请提供文件名");
        }
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".xlsx")) {
            return "xlsx";
        }
        if (lower.endsWith(".csv")) {
            return "csv";
        }
        throw new IllegalArgumentException("仅支持 .xlsx 或 .csv 文件");
    }

    private boolean isBlankRow(Row row) {
        for (int i = 0; i < TEMPLATE_HEADERS.length; i++) {
            if (StringUtils.hasText(cellValue(row, i))) {
                return false;
            }
        }
        return true;
    }

    private String cellValue(Row row, int index) {
        Cell cell = row.getCell(index);
        if (cell == null) {
            return "";
        }
        cell.setCellType(CellType.STRING);
        return normalize(cell.getStringCellValue());
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String requireField(String value, int rowNumber, String field) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("row " + rowNumber + ": " + field + " 不能为空");
        }
        return value.trim();
    }

    private Long parseLong(String value, int rowNumber, String field) {
        return parseLong(value, rowNumber, field, false);
    }

    private Long parseLong(String value, int rowNumber, String field, boolean optional) {
        if (!StringUtils.hasText(value)) {
            if (optional) {
                return null;
            }
            throw new IllegalArgumentException("row " + rowNumber + ": " + field + " 不能为空");
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("row " + rowNumber + ": " + field + " 需为数字");
        }
    }

    private Integer parseInt(String value, int rowNumber, String field) {
        return parseInt(value, rowNumber, field, false);
    }

    private Integer parseInt(String value, int rowNumber, String field, boolean optional) {
        if (!StringUtils.hasText(value)) {
            if (optional) {
                return null;
            }
            throw new IllegalArgumentException("row " + rowNumber + ": " + field + " 不能为空");
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("row " + rowNumber + ": " + field + " 需为数字");
        }
    }

    private Double parseDouble(String value, int rowNumber, String field) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("row " + rowNumber + ": " + field + " 需为数字");
        }
    }

    private Boolean parseBoolean(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "true", "1", "是", "yes" -> true;
            case "false", "0", "否", "no" -> false;
            default -> null;
        };
    }

    private String aggregateKey(Long registrationInfoId, Integer examYear) {
        return registrationInfoId + "-" + examYear;
    }

    private record RowPayload(
        int rowNumber,
        String registrationInfoId,
        String examType,
        String examYear,
        String ticketNumber,
        String subjectId,
        String subjectName,
        String subjectScore,
        String subjectPassLine,
        String subjectIsPass,
        String subjectRemark,
        String subjectNationalRank,
        String totalScore,
        String totalPassLine,
        String qualificationStatus,
        String qualificationNote
    ) {
    }

    private record RowContext(
        int rowNumber,
        RegistrationInfoEntity registration,
        String examType,
        Integer examYear,
        String ticketNumber,
        Double totalScore,
        Double totalPassLine,
        String qualificationStatus,
        String qualificationNote,
        Long subjectId,
        String subjectName,
        Double subjectScore,
        Double subjectPassLine,
        Boolean subjectIsPass,
        String subjectRemark,
        Integer subjectRank
    ) {
    }

    private static class CandidateAggregate {

        private final Long registrationInfoId;
        private final Integer examYear;
        private String examType;
        private String ticketNumber;
        private Double totalScore;
        private Double totalPassLine;
        private String qualificationStatus;
        private String qualificationNote;
        private final List<ExamResultDetailInput> subjects = new ArrayList<>();
        private final List<ExamResultImportItemEntity> items = new ArrayList<>();

        CandidateAggregate(Long registrationInfoId, Integer examYear) {
            this.registrationInfoId = registrationInfoId;
            this.examYear = examYear;
        }

        void applyRow(RowContext context) {
            if (examType == null) {
                examType = context.examType();
            } else if (!Objects.equals(examType, context.examType())) {
                throw new IllegalArgumentException(
                    "报名信息 " + registrationInfoId + " 的 examType 不一致"
                );
            }
            if (ticketNumber == null && StringUtils.hasText(context.ticketNumber())) {
                ticketNumber = context.ticketNumber();
            }
            if (context.totalScore() != null) {
                totalScore = context.totalScore();
            }
            if (context.totalPassLine() != null) {
                totalPassLine = context.totalPassLine();
            }
            if (StringUtils.hasText(context.qualificationStatus())) {
                qualificationStatus = context.qualificationStatus();
            }
            if (StringUtils.hasText(context.qualificationNote())) {
                qualificationNote = context.qualificationNote();
            }
            ExamResultDetailInput detail = new ExamResultDetailInput();
            detail.setSubjectId(context.subjectId());
            detail.setSubjectName(context.subjectName());
            detail.setScore(context.subjectScore());
            detail.setPassLine(context.subjectPassLine());
            detail.setIsPass(context.subjectIsPass());
            detail.setRemark(context.subjectRemark());
            detail.setNationalRank(context.subjectRank());
            subjects.add(detail);
        }

        UpsertExamResultInput toInput() {
            UpsertExamResultInput input = new UpsertExamResultInput();
            input.setRegistrationInfoId(registrationInfoId);
            input.setExamYear(examYear);
            input.setExamType(examType);
            input.setTicketNumber(ticketNumber);
            input.setTotalScore(totalScore);
            input.setTotalPassLine(totalPassLine);
            input.setQualificationStatus(qualificationStatus);
            input.setQualificationNote(qualificationNote);
            input.setSubjects(subjects);
            return input;
        }

        List<ExamResultImportItemEntity> items() {
            return items;
        }
    }
}
