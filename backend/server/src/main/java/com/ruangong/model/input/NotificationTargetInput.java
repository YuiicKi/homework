package com.ruangong.model.input;

import jakarta.validation.constraints.NotBlank;

public class NotificationTargetInput {

    @NotBlank
    private String targetType;

    private String targetValue;

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public String getTargetValue() {
        return targetValue;
    }

    public void setTargetValue(String targetValue) {
        this.targetValue = targetValue;
    }
}
