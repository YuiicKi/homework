package com.ruangong.model.input;

import jakarta.validation.constraints.Pattern;

public class UpdateUserInput {

    @Pattern(
        regexp = "^(?:13[0-9]|14[5-9]|15[0-35-9]|16[2567]|17[0-8]|18[0-9]|19[0-35-9])\\d{8}$",
        message = "手机号格式不正确"
    )
    private String phone;
    private Boolean isActive;

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}
