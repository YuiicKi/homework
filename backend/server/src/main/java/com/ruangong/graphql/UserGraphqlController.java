package com.ruangong.graphql;

import com.ruangong.model.AuthPayload;
import com.ruangong.model.JwtPayload;
import com.ruangong.model.RoleModel;
import com.ruangong.model.UserModel;
import com.ruangong.model.input.AdminCreateUserInput;
import com.ruangong.model.input.StudentRegisterInput;
import com.ruangong.model.input.UpdateUserInput;
import com.ruangong.service.UserService;
import graphql.schema.DataFetchingEnvironment;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.util.CollectionUtils;

@Controller
@Validated
public class UserGraphqlController {

    private final UserService userService;

    public UserGraphqlController(UserService userService) {
        this.userService = userService;
    }

    @MutationMapping
    public AuthPayload registerStudent(@Argument("input") @Valid StudentRegisterInput input) {
        return userService.registerStudent(input);
    }

    @MutationMapping
    public AuthPayload login(@Argument("loginIdentifier") String loginIdentifier,
                             @Argument("password") String password) {
        return userService.login(loginIdentifier, password);
    }

    @MutationMapping
    public UserModel adminCreateUser(
        @Argument("input") @Valid AdminCreateUserInput input,
        DataFetchingEnvironment env
    ) {
        JwtPayload user = requireCurrentUser(env);
        ensureHasRole(user, "admin");
        return userService.adminCreateUser(input);
    }

    @MutationMapping
    public UserModel updateUser(
        @Argument("id") Long id,
        @Argument("input") @Valid UpdateUserInput input,
        DataFetchingEnvironment env
    ) {
        JwtPayload user = requireCurrentUser(env);
        ensureHasRole(user, "admin");
        return userService.updateUser(id, input);
    }

    @MutationMapping
    public Boolean deleteUser(@Argument("id") Long id, DataFetchingEnvironment env) {
        JwtPayload user = requireCurrentUser(env);
        ensureHasRole(user, "admin");
        return userService.deleteUser(id);
    }

    @MutationMapping
    public RoleModel createRole(
        @Argument("name") String name,
        @Argument("description") String description,
        DataFetchingEnvironment env
    ) {
        JwtPayload user = requireCurrentUser(env);
        ensureHasRole(user, "admin");
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
        ensureHasRole(user, "admin");
        return userService.updateRole(id, name, description);
    }

    @MutationMapping
    public Boolean deleteRole(@Argument("id") Long id, DataFetchingEnvironment env) {
        JwtPayload user = requireCurrentUser(env);
        ensureHasRole(user, "admin");
        return userService.deleteRole(id);
    }

    @MutationMapping
    public UserModel assignRoleToUser(
        @Argument("userId") Long userId,
        @Argument("roleId") Long roleId,
        DataFetchingEnvironment env
    ) {
        JwtPayload user = requireCurrentUser(env);
        ensureHasRole(user, "admin");
        return userService.assignRoleToUser(userId, roleId);
    }

    @MutationMapping
    public UserModel removeRoleFromUser(
        @Argument("userId") Long userId,
        @Argument("roleId") Long roleId,
        DataFetchingEnvironment env
    ) {
        JwtPayload user = requireCurrentUser(env);
        ensureHasRole(user, "admin");
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
        ensureHasRole(user, "admin");
        return userService.listUsers(role);
    }

    @QueryMapping
    public UserModel user(@Argument("id") Long id, DataFetchingEnvironment env) {
        JwtPayload user = requireCurrentUser(env);
        ensureHasRole(user, "admin");
        return userService.getUser(id);
    }

    @QueryMapping
    public List<RoleModel> roles(DataFetchingEnvironment env) {
        JwtPayload user = requireCurrentUser(env);
        ensureHasRole(user, "admin");
        return userService.listRoles();
    }

    private JwtPayload requireCurrentUser(DataFetchingEnvironment env) {
        JwtPayload payload = env.getGraphQlContext().get("currentUser");
        if (payload == null) {
            throw new IllegalStateException("未登录");
        }
        return payload;
    }

    private void ensureHasRole(JwtPayload payload, String role) {
        if (CollectionUtils.isEmpty(payload.roles()) || payload.roles().stream().noneMatch(r -> r.equalsIgnoreCase(role))) {
            throw new IllegalStateException("无权访问");
        }
    }
}
