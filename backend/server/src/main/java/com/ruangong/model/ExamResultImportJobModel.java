package com.ruangong.model;

import java.util.List;

public record ExamResultImportJobModel(
    Long id,
    String fileName,
    Integer fileSize,
    String fileType,
    String status,
    Integer totalCount,
    Integer successCount,
    Integer failureCount,
    String errorMessage,
    Long createdBy,
    String createdAt,
    String completedAt,
    List<ExamResultImportItemModel> items
) {
}
