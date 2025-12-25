package com.ruangong.model;

public record AdmitCardModel(
    Long registrationInfoId,
    String ticketNumber,
    String subjectName,
    String sessionName,
    String sessionStartTime,
    String sessionEndTime,
    String roomName,
    String roomNumber,
    Integer seatNumber,
    String fullName,
    String idCardNumber,
    String examNotice,
    String logoUrl,
    String qrContent,
    String filePath
) {
}
