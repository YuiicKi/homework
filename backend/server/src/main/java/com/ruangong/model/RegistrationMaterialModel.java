package com.ruangong.model;

public record RegistrationMaterialModel(
    Long id,
    String type,
    String fileUrl,
    String fileFormat,
    Long fileSize,
    String status,
    String note
) {
}
