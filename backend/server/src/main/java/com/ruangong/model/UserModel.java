package com.ruangong.model;

import java.util.List;

public record UserModel(
    Long id,
    String phone,
    Boolean isActive,
    String createdAt,
    String fullName,
    List<RoleModel> roles,
    UserProfileModel profile
) {
}
