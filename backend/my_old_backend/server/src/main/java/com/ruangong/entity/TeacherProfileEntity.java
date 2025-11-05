package com.ruangong.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "teacher_profiles")
public class TeacherProfileEntity {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "staff_id")
    private String staffId;

    @Column(name = "school_or_department")
    private String schoolOrDepartment;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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
}
