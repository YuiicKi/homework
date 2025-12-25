package com.ruangong.model;

public record ExamResultReleaseSettingModel(
    Long id,
    Long subjectId,
    String subjectName,
    Integer examYear,
    String releaseTime,
    Integer preNoticeOffsetMinutes,
    Long preNotificationId,
    String preNotificationTitle,
    String preNotificationContent,
    String preNoticeTriggeredAt,
    String autoReleaseTriggeredAt,
    Long createdBy,
    String createdAt,
    String updatedAt
) {
}
