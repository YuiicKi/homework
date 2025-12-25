package com.ruangong.model.input;

import jakarta.validation.constraints.NotNull;

public class PublishNotificationInput {

    @NotNull
    private Long notificationId;

    public Long getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(Long notificationId) {
        this.notificationId = notificationId;
    }
}
