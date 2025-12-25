package com.ruangong.model;

public sealed interface UserProfileModel permits StudentProfileModel, TeacherProfileModel, AdminProfileModel {
}
