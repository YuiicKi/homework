package com.ruangong.model;

import java.util.List;

public record JwtPayload(Long userId, List<String> roles, long tokenVersion) {

    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }
}
