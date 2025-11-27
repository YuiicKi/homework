package com.ruangong.model;

public record ExamSubjectLogModel(
    Long id,
    String fromStatus,
    String toStatus,
    String reason,
    Long operatorId,
    String createdAt
) {
}
