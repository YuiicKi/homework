package com.ruangong.model;

import java.util.List;

public record NotificationModel(
    Long id,
    String title,
    String type,
    String content,
    String channel,
    String status,
    String scheduledAt,
    Long createdBy,
    String createdAt,
    String updatedAt,
    List<NotificationTargetModel> targets,
    List<NotificationLogModel> logs
) {
}
