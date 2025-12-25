package com.ruangong.model;

public record RegistrationMaterialTemplateModel(
    Long id,
    String type,
    String allowedFormats,
    Long maxSize,
    Boolean required,
    String description
) {
}
