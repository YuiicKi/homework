package com.ruangong.model;

public record AdmitCardTemplateModel(
    Long id,
    String name,
    String logoUrl,
    String examNotice,
    String qrStyle
) {
}
