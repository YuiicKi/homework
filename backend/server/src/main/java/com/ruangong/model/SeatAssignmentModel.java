package com.ruangong.model;

public record SeatAssignmentModel(
    Long id,
    Long registrationInfoId,
    Long subjectId,
    Long sessionId,
    Long roomId,
    Integer seatNumber,
    String ticketNumber,
    String status
) {
}
