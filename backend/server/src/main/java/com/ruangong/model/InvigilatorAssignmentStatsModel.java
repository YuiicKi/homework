package com.ruangong.model;

public record InvigilatorAssignmentStatsModel(
    Integer totalSchedules,
    Integer assignedSchedules,
    Integer availableTeachers
) {
}
