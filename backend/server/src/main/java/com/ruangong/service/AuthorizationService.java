package com.ruangong.service;

import com.ruangong.entity.RoleEntity;
import com.ruangong.model.JwtPayload;
import com.ruangong.repository.RoleRepository;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
public class AuthorizationService {

    private final RoleRepository roleRepository;

    public AuthorizationService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    public void ensureHasPermission(JwtPayload payload, String permissionCode) {
        if (payload == null || CollectionUtils.isEmpty(payload.roles())) {
            throw new IllegalStateException("无权访问");
        }
        if (!hasPermission(payload.roles(), permissionCode)) {
            throw new IllegalStateException("无权访问");
        }
    }

    public boolean hasPermission(Collection<String> roleNames, String permissionCode) {
        if (CollectionUtils.isEmpty(roleNames)) {
            return false;
        }

        Set<String> normalizedRoleNames = roleNames.stream()
            .filter(StringUtils::hasText)
            .map(String::toLowerCase)
            .collect(Collectors.toSet());

        if (normalizedRoleNames.isEmpty()) {
            return false;
        }

        List<RoleEntity> roles = roleRepository.findByNameInWithPermissions(normalizedRoleNames);

        Set<String> permissionCodes = new HashSet<>();
        for (RoleEntity role : roles) {
            role.getPermissions().forEach(permission -> permissionCodes.add(permission.getCode()));
        }

        return permissionCodes.stream()
            .anyMatch(code -> code.equalsIgnoreCase(permissionCode));
    }

    public boolean hasRole(JwtPayload payload, String roleName) {
        if (payload == null || CollectionUtils.isEmpty(payload.roles())) {
            return false;
        }
        return payload.roles().stream()
            .anyMatch(role -> role.equalsIgnoreCase(roleName));
    }
}
