package com.ruangong.model;

public record ExamScheduleModel(
    Long id,
    ExamRoomModel examRoom,
    ExamSubjectModel examSubject,
    ExamSessionModel examSession,
    String status,
    String note
) {
}
