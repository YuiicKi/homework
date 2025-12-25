package com.ruangong.model;

import java.util.List;

public record ExamResultModel(
    Long resultId,
    Long registrationInfoId,
    String fullName,
    String idCardNumber,
    String examType,
    Integer examYear,
    String ticketNumber,
    String releaseTime,
    Double totalScore,
    Double totalPassLine,
    String qualificationStatus,
    String qualificationNote,
    Boolean isQualified,
    String reportUrl,
    List<ExamResultDetailModel> subjects
) {
}
