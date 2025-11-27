package com.ruangong.model.input;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class RegistrationRejectInput {

    @NotNull
    private Long registrationInfoId;

    @NotBlank
    private String reason;

    public Long getRegistrationInfoId() {
        return registrationInfoId;
    }

    public void setRegistrationInfoId(Long registrationInfoId) {
        this.registrationInfoId = registrationInfoId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
