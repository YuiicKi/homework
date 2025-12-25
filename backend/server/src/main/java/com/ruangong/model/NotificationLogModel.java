package com.ruangong.model;

public record NotificationLogModel(
    Long id,
    String channel,
    String target,
    String status,
    String error,
    String createdAt
) {
}
