package com.ruangong.model;

public record ExamResultPreNotificationModel(
    Long id,
    String examType,
    Integer examYear,
    String queryTime,
    String title,
    String content,
    String status,
    Long lastNotificationId,
    String lastPublishedAt,
    Long createdBy,
    String createdAt,
    String updatedAt
) {
}
