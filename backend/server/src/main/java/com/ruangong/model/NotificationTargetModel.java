package com.ruangong.model;

public record NotificationTargetModel(
    Long id,
    String targetType,
    String targetValue
) {
}
