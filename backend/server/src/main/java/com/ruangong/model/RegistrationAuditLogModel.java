package com.ruangong.model;

public record RegistrationAuditLogModel(
    Long id,
    Long registrationInfoId,
    String result,
    String reason,
    Long operatorId,
    String createdAt
) {}
