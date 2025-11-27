package com.ruangong.model;

public record MyExamScheduleModel(
    Long registrationInfoId,
    Long subjectId,
    String subjectName,
    Long sessionId,
    String sessionName,
    String sessionStartTime,
    String sessionEndTime,
    String centerName,
    String roomName,
    String roomNumber,
    Integer seatNumber,
    String ticketNumber
) {
}
