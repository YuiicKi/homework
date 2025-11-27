package com.ruangong.model;

public record ExamRoomStatusLogModel(
    Long id,
    String fromStatus,
    String toStatus,
    String reason,
    String createdAt
) {
}
