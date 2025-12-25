package com.ruangong.model;

public record InvigilatorAssignmentModel(
    Long id,
    Long scheduleId,
    Long teacherUserId,
    String teacherName,
    String teacherPhone,
    String subjectName,
    String sessionName,
    String sessionStartTime,
    String sessionEndTime,
    String centerName,
    String centerAddress,
    String roomName,
    String roomNumber,
    Long assignedBy,
    String assignedAt
) {
}
