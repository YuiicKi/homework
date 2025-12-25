package com.ruangong.model;

public record AdmitCardLogModel(
    Long id,
    Long registrationInfoId,
    String ticketNumber,
    String filePath,
    String status,
    String message,
    String createdAt
) {}
