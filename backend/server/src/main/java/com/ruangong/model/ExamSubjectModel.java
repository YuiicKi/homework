package com.ruangong.model;

public record ExamSubjectModel(
    Long id,
    String code,
    String name,
    String status,
    Integer durationMinutes,
    Integer questionCount,
    String description,
    java.util.List<ExamSubjectLogModel> logs
) {
}
