package com.ruangong.service;

import com.ruangong.entity.AdminProfileEntity;
import com.ruangong.entity.RoleEntity;
import com.ruangong.entity.StudentProfileEntity;
import com.ruangong.entity.TeacherProfileEntity;
import com.ruangong.entity.UserEntity;
import com.ruangong.model.AdminProfileModel;
import com.ruangong.model.AuthPayload;
import com.ruangong.model.RoleModel;
import com.ruangong.model.StudentProfileModel;
import com.ruangong.model.TeacherProfileModel;
import com.ruangong.model.UserModel;
import com.ruangong.model.UserProfileModel;
import com.ruangong.model.input.AdminCreateUserInput;
import com.ruangong.model.input.StudentRegisterInput;
import com.ruangong.model.input.UpdateUserInput;
import com.ruangong.repository.AdminProfileRepository;
import com.ruangong.repository.RoleRepository;
import com.ruangong.repository.StudentProfileRepository;
import com.ruangong.repository.TeacherProfileRepository;
import com.ruangong.repository.UserRepository;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final TeacherProfileRepository teacherProfileRepository;
    private final AdminProfileRepository adminProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserService(
        UserRepository userRepository,
        RoleRepository roleRepository,
        StudentProfileRepository studentProfileRepository,
        TeacherProfileRepository teacherProfileRepository,
        AdminProfileRepository adminProfileRepository,
        PasswordEncoder passwordEncoder,
        JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.teacherProfileRepository = teacherProfileRepository;
        this.adminProfileRepository = adminProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthPayload registerStudent(StudentRegisterInput input) {
        if (userRepository.existsByPhone(input.getPhone())) {
            throw new IllegalArgumentException("手机号已被注册");
        }

        UserEntity user = new UserEntity();
        user.setPhone(input.getPhone());
        user.setPasswordHash(passwordEncoder.encode(input.getPassword()));
        user.setIsActive(Boolean.TRUE);
        user = userRepository.save(user);

        StudentProfileEntity profile = new StudentProfileEntity();
        profile.setUserId(user.getId());
        profile.setFullName(input.getFullName());
        profile.setIdCardNumber(input.getIdCardNumber());
        profile.setPhotoUrl(input.getPhotoUrl());
        studentProfileRepository.save(profile);

        RoleEntity studentRole = roleRepository.findByName("student")
            .orElseThrow(() -> new IllegalStateException("学生角色未初始化"));
        user.getRoles().add(studentRole);
        user = userRepository.save(user);

        UserEntity refreshedUser = refreshUser(user.getId());
        UserModel userModel = mapUser(refreshedUser);
        String token = jwtService.generateToken(
            userModel.id(),
            extractRoleNames(userModel),
            safeTokenVersion(refreshedUser)
        );
        return new AuthPayload(token, userModel);
    }

    public AuthPayload login(String phone, String password) {
        UserEntity user = userRepository.findByPhone(phone)
            .orElseThrow(() -> new IllegalArgumentException("账号或密码错误"));

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new IllegalStateException("账号已被禁用");
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("账号或密码错误");
        }

        UserEntity refreshedUser = refreshUser(user.getId());
        UserModel userModel = mapUser(refreshedUser);
        String token = jwtService.generateToken(
            userModel.id(),
            extractRoleNames(userModel),
            safeTokenVersion(refreshedUser)
        );
        return new AuthPayload(token, userModel);
    }

    public UserModel adminCreateUser(AdminCreateUserInput input) {
        if (userRepository.existsByPhone(input.getPhone())) {
            throw new IllegalArgumentException("手机号已被占用");
        }

        RoleEntity role = roleRepository.findByName(input.getRoleName())
            .orElseThrow(() -> new IllegalArgumentException("角色不存在"));

        UserEntity user = new UserEntity();
        user.setPhone(input.getPhone());
        user.setPasswordHash(passwordEncoder.encode(input.getPassword()));
        user.setIsActive(Boolean.TRUE);
        user = userRepository.save(user);

        switch (role.getName().toLowerCase(Locale.ROOT)) {
            case "teacher" -> createTeacherProfile(user.getId(), input);
            case "admin" -> createAdminProfile(user.getId(), input);
            case "student" -> createStudentProfileForAdmin(user.getId(), input);
            default -> throw new IllegalArgumentException("暂不支持该角色的资料创建");
        }

        user.getRoles().add(role);
        user = userRepository.save(user);

        return mapUser(refreshUser(user.getId()));
    }

    public UserModel updateUser(Long id, UpdateUserInput input) {
        UserEntity user = refreshUser(id);

        String nextPhone;
        if (input.getPhone() != null) {
            nextPhone = normalizeBlank(input.getPhone());
            if (!StringUtils.hasText(nextPhone)) {
                throw new IllegalArgumentException("手机号不能为空");
            }
            if (!nextPhone.equals(user.getPhone()) && userRepository.existsByPhone(nextPhone)) {
                throw new IllegalArgumentException("手机号已被占用");
            }
        } else {
            nextPhone = user.getPhone();
        }

        user.setPhone(nextPhone);
        if (input.getIsActive() != null) {
            user.setIsActive(input.getIsActive());
        }

        user = userRepository.save(user);
        return mapUser(refreshUser(user.getId()));
    }

    public boolean deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new IllegalArgumentException("用户不存在");
        }
        userRepository.deleteById(id);
        return true;
    }

    @Transactional
    public RoleModel createRole(String name, String description) {
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("角色名称不能为空");
        }
        if (roleRepository.existsByName(name)) {
            throw new IllegalArgumentException("角色名称已存在");
        }
        RoleEntity role = new RoleEntity();
        role.setName(name);
        role.setDescription(normalizeBlank(description));
        role = roleRepository.save(role);
        return mapRole(role);
    }

    @Transactional
    public RoleModel updateRole(Long id, String name, String description) {
        RoleEntity role = roleRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("角色不存在"));

        if (name != null) {
            if (!StringUtils.hasText(name)) {
                throw new IllegalArgumentException("角色名称不能为空");
            }
            if (!name.equals(role.getName()) && roleRepository.existsByName(name)) {
                throw new IllegalArgumentException("角色名称已存在");
            }
            role.setName(name);
        }
        if (description != null) {
            role.setDescription(normalizeBlank(description));
        }
        role = roleRepository.save(role);
        return mapRole(role);
    }

    @Transactional
    public boolean deleteRole(Long id) {
        if (!roleRepository.existsById(id)) {
            throw new IllegalArgumentException("角色不存在");
        }
        roleRepository.deleteById(id);
        return true;
    }

    public UserModel assignRoleToUser(Long userId, Long roleId) {
        UserEntity user = refreshUser(userId);
        RoleEntity role = roleRepository.findById(roleId)
            .orElseThrow(() -> new IllegalArgumentException("角色不存在"));
        boolean alreadyHasRole = user.getRoles().stream().anyMatch(r -> r.getId().equals(roleId));
        if (!alreadyHasRole) {
            user.getRoles().add(role);
            user = userRepository.save(user);
        }
        return mapUser(refreshUser(user.getId()));
    }

    public UserModel removeRoleFromUser(Long userId, Long roleId) {
        UserEntity user = refreshUser(userId);
        boolean removed = user.getRoles().removeIf(role -> role.getId().equals(roleId));
        if (!removed) {
            throw new IllegalArgumentException("用户未拥有该角色");
        }
        user = userRepository.save(user);
        return mapUser(refreshUser(user.getId()));
    }

    @Transactional(readOnly = true)
    public UserModel currentUser(Long userId) {
        return mapUser(refreshUser(userId));
    }

    @Transactional(readOnly = true)
    public UserModel getUser(Long id) {
        return mapUser(refreshUser(id));
    }

    @Transactional(readOnly = true)
    public List<UserModel> listUsers(String roleName) {
        List<UserEntity> users;
        if (StringUtils.hasText(roleName)) {
            users = userRepository.findByRoleName(roleName);
        } else {
            users = userRepository.findAll();
        }
        return users.stream()
            .sorted(Comparator.comparing(UserEntity::getId))
            .map(this::mapUser)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<RoleModel> listRoles() {
        return roleRepository.findAll().stream()
            .sorted(Comparator.comparing(RoleEntity::getId))
            .map(this::mapRole)
            .toList();
    }

    public void revokeTokensForUser(Long userId) {
        int updated = userRepository.incrementTokenVersionForUser(userId);
        if (updated == 0) {
            throw new IllegalArgumentException("用户不存在");
        }
    }

    public void revokeAllTokens() {
        userRepository.incrementTokenVersionForAllUsers();
    }

    private UserEntity refreshUser(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
    }

    private void createTeacherProfile(Long userId, AdminCreateUserInput input) {
        TeacherProfileEntity profile = new TeacherProfileEntity();
        profile.setUserId(userId);
        profile.setFullName(input.getFullName());
        profile.setStaffId(normalizeBlank(input.getStaffId()));
        profile.setSchoolOrDepartment(normalizeBlank(input.getSchoolOrDepartment()));
        teacherProfileRepository.save(profile);
    }

    private void createAdminProfile(Long userId, AdminCreateUserInput input) {
        AdminProfileEntity profile = new AdminProfileEntity();
        profile.setUserId(userId);
        profile.setFullName(input.getFullName());
        profile.setStaffId(normalizeBlank(input.getStaffId()));
        profile.setDepartment(normalizeBlank(input.getDepartment()));
        adminProfileRepository.save(profile);
    }

    private void createStudentProfileForAdmin(Long userId, AdminCreateUserInput input) {
        StudentProfileEntity profile = new StudentProfileEntity();
        profile.setUserId(userId);
        profile.setFullName(input.getFullName());
        profile.setIdCardNumber(null);
        profile.setPhotoUrl(null);
        studentProfileRepository.save(profile);
    }

    private RoleModel mapRole(RoleEntity role) {
        return new RoleModel(role.getId(), role.getName(), role.getDescription());
    }

    private UserModel mapUser(UserEntity user) {
        List<RoleModel> roles = user.getRoles().stream()
            .sorted(Comparator.comparing(RoleEntity::getId))
            .map(this::mapRole)
            .toList();

        UserProfileModel profile = resolveProfile(user, roles);
        OffsetDateTime createdAt = user.getCreatedAt();
        String createdAtIso = createdAt != null ? createdAt.toString() : null;

        String fullName = extractFullName(profile);

        return new UserModel(
            user.getId(),
            user.getPhone(),
            user.getIsActive(),
            createdAtIso,
            fullName,
            roles,
            profile
        );
    }

    private UserProfileModel resolveProfile(UserEntity user, List<RoleModel> roles) {
        List<String> roleNames = roles.stream().map(RoleModel::name).map(String::toLowerCase).toList();
        Long userId = user.getId();
        if (roleNames.contains("admin")) {
            return adminProfileRepository.findByUserId(userId)
                .map(profile -> new AdminProfileModel(
                    profile.getFullName(),
                    profile.getStaffId(),
                    profile.getDepartment()
                ))
                .orElse(null);
        }
        if (roleNames.contains("teacher")) {
            return teacherProfileRepository.findByUserId(userId)
                .map(profile -> new TeacherProfileModel(
                    profile.getFullName(),
                    profile.getStaffId(),
                    profile.getSchoolOrDepartment()
                ))
                .orElse(null);
        }
        if (roleNames.contains("student")) {
            return studentProfileRepository.findByUserId(userId)
                .map(profile -> new StudentProfileModel(
                    profile.getFullName(),
                    profile.getIdCardNumber(),
                    profile.getPhotoUrl()
                ))
                .orElse(null);
        }
        return null;
    }

    private List<String> extractRoleNames(UserModel userModel) {
        return userModel.roles().stream().map(RoleModel::name).collect(Collectors.toCollection(ArrayList::new));
    }

    private long safeTokenVersion(UserEntity user) {
        Integer version = user.getTokenVersion();
        return version != null ? version.longValue() : 0L;
    }

    private String extractFullName(UserProfileModel profile) {
        if (profile instanceof AdminProfileModel adminProfile) {
            return adminProfile.fullName();
        }
        if (profile instanceof TeacherProfileModel teacherProfile) {
            return teacherProfile.fullName();
        }
        if (profile instanceof StudentProfileModel studentProfile) {
            return studentProfile.fullName();
        }
        return null;
    }

    private static String normalizeBlank(String value) {
        return StringUtils.hasText(value) ? value : null;
    }

}
