package com.ruangong.model;

public record ExamResultImportItemModel(
    Long id,
    Long jobId,
    Integer rowNumber,
    Long registrationInfoId,
    String ticketNumber,
    Long subjectId,
    String status,
    String message,
    String createdAt
) {
}
