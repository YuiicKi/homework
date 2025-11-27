package com.ruangong.model;

public record SeatAssignmentStatsModel(
    Long subjectId,
    Long sessionId,
    Integer registrationCount,
    Integer assignmentCount,
    Integer availableRooms
) {
}
