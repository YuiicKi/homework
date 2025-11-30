# RBAC V1 预研方案

## 1. 核心设计思想
- 采用经典 RBAC（Role-Based Access Control）模型，核心实体包括 `User`、`Role`、`Permission`。
- V1 暂时将 `Permission` 逻辑内嵌在后端服务中，通过角色判定进行权限控制。
- 预留 `User`、`Role`、`UserRoleLink` 等表结构，方便后续细粒度权限扩展。

## 2. 角色与权限映射
- `admin`：考务人员，具备用户管理、角色管理等高级权限。
- `teacher`：教师 / 监考人员，专注于考试、成绩等业务，不具备账户创建/删除权限。
- `student`：考生，具备报名、参加考试和查看成绩的基础权限。
- 权限以 `permissions` 表中的 `code` 表示（如 `user.create`、`role.assign`），`role_permissions` 记录角色与权限关系；后端通过 JWT 中的角色 + 数据库映射进行动态校验。

## 3. 数据库表结构（PostgreSQL）
详见 `db/schema.sql`，主要表包含：
- `users`：存储登录凭据与基础状态。
- `roles`：系统角色定义。
- `permissions`：业务动作定义，以 `code` 唯一标识。
- `role_permissions`：角色与权限的多对多关联。
- `user_roles_link`：用户与角色的多对多关联。
- `student_profiles`、`teacher_profiles`、`admin_profiles`：角色专属扩展信息。

### 关键约束
- `users.phone` 唯一约束，作为唯一登录凭证。
- `user_roles_link` 联合主键，避免重复授权；`ON DELETE CASCADE` 确保数据一致性。
- 各 profile 表与 `users.id` 一一对应，实现统一登录信息与业务数据分离。

## 4. GraphQL API 设计
GraphQL Schema 位于 `server/src/main/resources/graphql/schema.graphqls`，包含以下要点：
- `User` 类型聚合基础信息、角色列表和 `UserProfile` 联合类型。
- 登录流程返回 `AuthPayload`（包含 JWT）。
- `Query`：`me`、`users`（支持按角色过滤）、`user`、`roles`。
- `Mutation`：考生注册、登录、管理员新增/更新/删除用户、角色 CRUD、用户角色分配。

## 5. 核心流程
### 5.1 注册（考生）
1. 调用 `registerStudent` Mutation。
2. 后端事务流程：校验手机号 → 使用 `BCryptPasswordEncoder` 加密 → 写入 `users` → 写入 `student_profiles` → 插入 `user_roles_link` → 提交事务。
3. 生成包含 `userId`、`roles` 的 JWT，返回 `AuthPayload`。

### 5.2 登录
1. 调用 `login` Mutation，使用手机号。
2. 后端查询 `users`，使用 `PasswordEncoder#matches` 验证密码。
3. 查询用户角色，生成 JWT，写入 `roles` 集合。
4. 返回 `AuthPayload`。

### 5.3 权限校验
1. 前端将 JWT 放入 `Authorization: Bearer <token>`。
2. Spring GraphQL 拦截器解析 JWT，将 `{ userId, roles }` 注入 GraphQL Context。
3. Resolver 通过 `AuthorizationService` 查询 `role_permissions`，判断是否具备对应 `permission code`，不满足条件时抛出未授权异常；目前仅管理员拥有用户与角色管理权限，其余角色权限空白。

## 6. 后续扩展方向
- 审计日志与操作留痕。
- 多租户或地域化数据隔离策略。
- UI 管理后台中的角色/权限可视化配置界面。
