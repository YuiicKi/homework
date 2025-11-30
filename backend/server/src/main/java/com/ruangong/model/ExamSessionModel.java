package com.ruangong.model;

public record ExamSessionModel(
    Long id,
    String name,
    String startTime,
    String endTime,
    String note
) {
}
