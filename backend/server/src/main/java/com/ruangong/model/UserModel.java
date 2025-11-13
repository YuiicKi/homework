package com.ruangong.model;

import java.util.List;

public record UserModel(
    Long id,
    String phone,
    String username,
    Boolean isActive,
    String createdAt,
    List<RoleModel> roles,
    UserProfileModel profile
) {
}
