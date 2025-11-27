package com.ruangong.model;

public record ExamRegistrationWindowLogModel(
    Long id,
    String fromStatus,
    String toStatus,
    String reason,
    Long operatorId,
    String createdAt
) {
}
