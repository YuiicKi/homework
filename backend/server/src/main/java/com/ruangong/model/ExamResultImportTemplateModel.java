package com.ruangong.model;

public record ExamResultImportTemplateModel(
    String fileName,
    String mimeType,
    String contentBase64
) {
}
