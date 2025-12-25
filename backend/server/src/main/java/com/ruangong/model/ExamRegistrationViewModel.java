package com.ruangong.model;

public record ExamRegistrationViewModel(
    Long registrationId,
    String subjectCode,
    String subjectName,
    Long sessionId,
    String sessionName,
    String examStartTime,
    String examEndTime,
    String registrationStartTime,
    String registrationEndTime,
    String status,
    String actionLabel,
    String note
) {
}
