package com.ruangong.model.input;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class AdminCreateUserInput {

    @NotBlank
    @Pattern(
        regexp = "^(?:13[0-9]|14[5-9]|15[0-35-9]|16[2567]|17[0-8]|18[0-9]|19[0-35-9])\\d{8}$",
        message = "手机号格式不正确"
    )
    private String phone;

    @NotBlank
    @Size(min = 6, max = 100)
    private String password;

    @NotBlank
    private String roleName;

    @NotBlank
    private String fullName;

    private String staffId;

    private String schoolOrDepartment;

    private String department;

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getStaffId() {
        return staffId;
    }

    public void setStaffId(String staffId) {
        this.staffId = staffId;
    }

    public String getSchoolOrDepartment() {
        return schoolOrDepartment;
    }

    public void setSchoolOrDepartment(String schoolOrDepartment) {
        this.schoolOrDepartment = schoolOrDepartment;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }
}
