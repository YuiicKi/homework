package com.ruangong.model;

public record TeacherProfileModel(String fullName, String staffId, String schoolOrDepartment)
    implements UserProfileModel {
}
