package com.ruangong.graphql;

import com.ruangong.model.AuthPayload;
import com.ruangong.model.JwtPayload;
import com.ruangong.model.RoleModel;
import com.ruangong.model.UserModel;
import com.ruangong.model.input.AdminCreateUserInput;
import com.ruangong.model.input.StudentRegisterInput;
import com.ruangong.model.input.UpdateUserInput;
import com.ruangong.service.AuthorizationService;
import com.ruangong.service.UserService;
import graphql.schema.DataFetchingEnvironment;
import jakarta.validation.Valid;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;

@Controller
@Validated
public class UserGraphqlController {

    private static final Logger log = LoggerFactory.getLogger(UserGraphqlController.class);

    private final UserService userService;
    private final AuthorizationService authorizationService;

    public UserGraphqlController(UserService userService, AuthorizationService authorizationService) {
        this.userService = userService;
        this.authorizationService = authorizationService;
    }

    @MutationMapping
    public AuthPayload registerStudent(@Argument("input") @Valid StudentRegisterInput input) {
        return userService.registerStudent(input);
    }

    @MutationMapping
    public AuthPayload login(@Argument("phone") String phone,
                             @Argument("password") String password) {
        return userService.login(phone, password);
    }

    @MutationMapping
    public UserModel adminCreateUser(
        @Argument("input") @Valid AdminCreateUserInput input,
        DataFetchingEnvironment env
    ) {
        JwtPayload user = requireCurrentUser(env);
        authorizationService.ensureHasPermission(user, "user.create");
        logAdminAction(user, "user.create", "phone=" + input.getPhone());
        return userService.adminCreateUser(input);
    }

    @MutationMapping
    public UserModel updateUser(
        @Argument("id") Long id,
        @Argument("input") @Valid UpdateUserInput input,
        DataFetchingEnvironment env
    ) {
        JwtPayload user = requireCurrentUser(env);
        authorizationService.ensureHasPermission(user, "user.update");
        logAdminAction(user, "user.update", "targetId=" + id);
        return userService.updateUser(id, input);
    }

    @MutationMapping
    public Boolean deleteUser(@Argument("id") Long id, DataFetchingEnvironment env) {
        JwtPayload user = requireCurrentUser(env);
        authorizationService.ensureHasPermission(user, "user.delete");
        logAdminAction(user, "user.delete", "targetId=" + id);
        return userService.deleteUser(id);
    }

    @MutationMapping
    public RoleModel createRole(
        @Argument("name") String name,
        @Argument("description") String description,
        DataFetchingEnvironment env
    ) {
        JwtPayload user = requireCurrentUser(env);
        authorizationService.ensureHasPermission(user, "role.create");
        logAdminAction(user, "role.create", "name=" + name);
        return userService.createRole(name, description);
    }

    @MutationMapping
    public RoleModel updateRole(
        @Argument("id") Long id,
        @Argument("name") String name,
        @Argument("description") String description,
        DataFetchingEnvironment env
    ) {
        JwtPayload user = requireCurrentUser(env);
        authorizationService.ensureHasPermission(user, "role.update");
        logAdminAction(user, "role.update", "targetId=" + id);
        return userService.updateRole(id, name, description);
    }

    @MutationMapping
    public Boolean deleteRole(@Argument("id") Long id, DataFetchingEnvironment env) {
        JwtPayload user = requireCurrentUser(env);
        authorizationService.ensureHasPermission(user, "role.delete");
        logAdminAction(user, "role.delete", "targetId=" + id);
        return userService.deleteRole(id);
    }

    @MutationMapping
    public UserModel assignRoleToUser(
        @Argument("userId") Long userId,
        @Argument("roleId") Long roleId,
        DataFetchingEnvironment env
    ) {
        JwtPayload user = requireCurrentUser(env);
        authorizationService.ensureHasPermission(user, "role.assign");
        logAdminAction(user, "role.assign", "userId=" + userId + ", roleId=" + roleId);
        return userService.assignRoleToUser(userId, roleId);
    }

    @MutationMapping
    public UserModel removeRoleFromUser(
        @Argument("userId") Long userId,
        @Argument("roleId") Long roleId,
        DataFetchingEnvironment env
    ) {
        JwtPayload user = requireCurrentUser(env);
        authorizationService.ensureHasPermission(user, "role.assign");
        logAdminAction(user, "role.remove", "userId=" + userId + ", roleId=" + roleId);
        return userService.removeRoleFromUser(userId, roleId);
    }

    @QueryMapping
    public UserModel me(DataFetchingEnvironment env) {
        JwtPayload user = requireCurrentUser(env);
        return userService.currentUser(user.userId());
    }

    @QueryMapping
    public List<UserModel> users(@Argument("role") String role, DataFetchingEnvironment env) {
        JwtPayload user = requireCurrentUser(env);
        authorizationService.ensureHasPermission(user, "user.read.all");
        logAdminAction(user, "user.read.all", role != null ? "role=" + role : "role=ALL");
        return userService.listUsers(role);
    }

    @QueryMapping
    public UserModel user(@Argument("id") Long id, DataFetchingEnvironment env) {
        JwtPayload user = requireCurrentUser(env);
        authorizationService.ensureHasPermission(user, "user.read");
        return userService.getUser(id);
    }

    @QueryMapping
    public List<RoleModel> roles(DataFetchingEnvironment env) {
        JwtPayload user = requireCurrentUser(env);
        authorizationService.ensureHasPermission(user, "role.read");
        return userService.listRoles();
    }

    private JwtPayload requireCurrentUser(DataFetchingEnvironment env) {
        JwtPayload payload = env.getGraphQlContext().get("currentUser");
        if (payload == null) {
            throw new IllegalStateException("未登录");
        }
        return payload;
    }

    private void logAdminAction(JwtPayload actor, String action, String detail) {
        if (actor == null) {
            return;
        }
        log.info(
            "Audit action={} actorId={} roles={} tokenVersion={} detail={}",
            action,
            actor.userId(),
            actor.roles(),
            actor.tokenVersion(),
            detail
        );
    }
}
