package com.ruangong.model;

public record NotificationTemplateModel(
    Long id,
    String name,
    String type,
    String content,
    String variables
) {
}
