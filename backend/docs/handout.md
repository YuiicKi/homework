

## 接口清单（GraphQL）

所有请求统一走 `POST /graphql`，以下列出主要 Query/Mutation：

| 名称 | 类型 | 主要参数 | 权限 | 说明 |
| --- | --- | --- | --- | --- |
| `registerStudent` | Mutation | `input: StudentRegisterInput!`<br>（`phone`, `password`, `fullName`, `idCardNumber`, `photoUrl`） | 公共 | 考生自助注册，创建用户并返回 JWT。 |
| `login` | Mutation | `phone: String!`<br>`password: String!` | 公共 | 通过手机号登录，返回 JWT 与用户信息。 |
| `me` | Query | 无 | 登录用户 | 读取当前登录用户信息与角色、Profile。 |
| `users` | Query | `role: String`(可选) | `admin` | 查询所有用户，可按角色过滤。 |
| `user` | Query | `id: ID!` | `admin` | 查看单个用户详细信息。 |
| `roles` | Query | 无 | `admin` | 查看全部角色列表。 |
| `adminCreateUser` | Mutation | `input: AdminCreateUserInput!`<br>（`phone`、`roleName`、`fullName`、`staffId`、部门等） | `admin` | 创建账号并补齐 Profile。 |
| `updateUser` | Mutation | `id: ID!`<br>`input: UpdateUserInput!` | `admin` | 更新手机号、启用状态。 |
| `deleteUser` | Mutation | `id: ID!` | `admin` | 删除用户，级联清理关联资料。 |
| `createRole`/`updateRole`/`deleteRole` | Mutation | 见 Schema | `admin` | 角色 CRUD。 |
| `assignRoleToUser` | Mutation | `userId: ID!`<br>`roleId: ID!` | `admin` | 为用户授予角色。 |
| `removeRoleFromUser` | Mutation | `userId: ID!`<br>`roleId: ID!` | `admin` | 移除用户角色。 |

> GraphQL Schema 全量定义参见：`server/src/main/resources/graphql/schema.graphqls`。

## 数据库表关系（PostgreSQL）

```
users
 ├─ id (PK)
 ├─ phone (唯一，必填)
 ├─ password_hash
 ├─ is_active
 └─ created_at

roles
 ├─ id (PK)
 ├─ name (唯一)
 └─ description

user_roles_link
 ├─ user_id (FK -> users.id)
 ├─ role_id (FK -> roles.id)
 └─ PRIMARY KEY (user_id, role_id)

student_profiles / teacher_profiles / admin_profiles
 ├─ user_id (PK，FK -> users.id)
 └─ 各自角色对应的扩展字段
permissions
 ├─ id (PK)
 ├─ code (唯一)
 └─ description
role_permissions
 ├─ role_id (FK -> roles.id)
 ├─ permission_id (FK -> permissions.id)
 └─ PRIMARY KEY (role_id, permission_id)
```

特性：
- 手机号为唯一登录凭证，必须填写且符合大陆 11 位号段校验。
- `user_roles_link` 采用多对多关系 + `ON DELETE CASCADE`，用户删除后自动清理。
- Profile 表与用户一一对应，确保认证信息与业务信息分离。
- `permissions` + `role_permissions` 用于维护“角色 → 权限 code”的映射，便于动态扩展。

## 权限 / 鉴权流程

1. **注册/登录**：`registerStudent` 与 `login` 成功后返回 `AuthPayload { token, user }`。密码采用 `BCryptPasswordEncoder`。
2. **JWT 签发**：`JwtService` 将 `userId`、`roles` 写入 JWT，默认 7 天过期。
3. **请求拦截**：`JwtGraphQlInterceptor` 读取 `Authorization: Bearer <token>`，校验有效后把 `JwtPayload { userId, roles }` 放入 GraphQL context。
4. **权限检查**：各 Resolver 使用 `AuthorizationService.ensureHasPermission(jwtPayload, permissionCode)` 校验权限 code（如 `user.create`、`user.delete`），再结合角色级别额外限制（例如教师只能创建/删除学生或教师账号）。
5. **权限表驱动**：管理员账号可调用 `assignRoleToUser`、`removeRoleFromUser` 动态调整角色；具体接口权限由数据库的 `permissions`、`role_permissions` 决定，并可通过 SQL/后台界面维护。

> 验收演示时，可通过管理员账号 `13876543210 / Admin@2024` 登录获取 token 进行接口调试。

## 运行要点（备查）

- 启动：`cd server && mvn spring-boot:run`
- 本地调试端点：
  - GraphQL：`http://<IP>:8080/graphql`
  - GraphiQL：`http://<IP>:8080/graphiql`
- 需要测试账号时，可使用管理员或通过 `registerStudent` 自行注册。
