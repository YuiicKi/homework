package com.ruangong.model;

public record StudentProfileModel(String fullName, String idCardNumber, String photoUrl)
    implements UserProfileModel {
}
