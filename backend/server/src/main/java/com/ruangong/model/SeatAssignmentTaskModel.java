package com.ruangong.model;

public record SeatAssignmentTaskModel(
    Long id,
    Long subjectId,
    Long sessionId,
    String algorithm,
    Integer registrationsCount,
    Integer assignedCount,
    Long createdBy,
    String createdAt
) {
}
