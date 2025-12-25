package com.ruangong.model;

public record AdminProfileModel(String fullName, String staffId, String department)
    implements UserProfileModel {
}
