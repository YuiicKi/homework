package com.ruangong.model;

public record ExamCertificateFileModel(
    String fileName,
    String mimeType,
    String contentBase64
) {
}
