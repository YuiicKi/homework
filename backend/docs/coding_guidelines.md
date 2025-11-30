# 应用开发编码指南

面向冲刺阶段的后端开发者，说明如何在现有 Spring Boot + GraphQL + PostgreSQL 框架上扩展新功能。

---

## 一、整体原则

1. **分层清晰**：遵循 `GraphQL Controller → Service → Repository → Entity` 的结构，保持业务逻辑集中在 Service 层。
2. **统一 Schema**：所有接口通过 GraphQL 暴露；REST 仅用于内部调试时的健康检查，不再新增。
3. **显式鉴权**：新增 Mutation/Query 若涉及权限，务必使用 `AuthorizationService.ensureHasPermission` 或同类工具进行校验；若存在角色等级限制（如教师不可操作管理员），需额外结合 `AuthorizationService.hasRole` 或业务逻辑判断目标对象的角色。
4. **自动化优先**：每个业务功能需同步补充 JUnit/集成测试，保证 `mvn test` 通过。
5. **文档同步**：更新 `docs/handout.md` / `docs/graphiql_cheatsheet.md` 中的接口说明与流程示例。

---

## 二、代码组织与命名约定

### 目录结构（保持 existing pattern）

```
server/src/main/java/com/ruangong/
├─ graphql/        # GraphQL Controller（Query/Mutation）
├─ service/        # 业务服务，包含事务逻辑、权限校验
├─ repository/     # JPA Repository 接口
├─ entity/         # JPA Entity
├─ model/          # GraphQL 返回/输入模型
├─ model/input/    # GraphQL Input 类型映射
├─ config/         # 配置、拦截器、Wiring
├─ auth/           # JWT 等辅助类
└─ ...
```

### 命名和风格

- GraphQL Controller 命名为 `XxxGraphqlController`，方法名直接对应 Schema 中的字段。
- Service 层使用动词开头，例如 `createTeacherProfile`、`assignRoleToUser`；接口只做参数校验和事务，复杂逻辑拆分成私有方法。
- Repository 使用 Spring Data JPA，语义化命名，如 `findByRoleName`、`existsByPhone`.
- GraphQL Schema 类型名使用驼峰（如 `TeacherProfile`）；字段名遵循 GraphQL 命名习惯（小写+驼峰）。

---

## 三、GraphQL 开发流程

1. **Schema 修改**
   - 在 `server/src/main/resources/graphql/schema.graphqls` 中新增 Query/Mutation/Input/Type。
   - 对 union/interface（如 `UserProfile`）额外添加 type resolver（见 `GraphQlWiringConfig`）。

2. **模型映射**
   - 为新增的 Input/Output 类型创建对应的 Java Record/POJO（放在 `model`、`model/input`）。
   - 保持字段名称与 Schema 一致；必要时通过 Builder 或构造函数封装。

3. **Resolver 实现**
   - 在 `graphql/UserGraphqlController` 中添加方法，使用 `@QueryMapping` / `@MutationMapping`。
   - 注入 Service 调用；需要鉴权时调用 `requireCurrentUser`、`authorizationService.ensureHasPermission` 等。

4. **Service 与数据层**
   - 在 Service 中编写业务逻辑，使用 `@Transactional` 控制事务。
   - 通过 Repository/JPA 与数据库交互，避免在 Resolver 中直接操作 Repository。
   - 变更数据库结构需同步更新 `db/schema.sql`、`src/test/resources/test-schema.sql`。

5. **异常处理**
   - 统一使用 `IllegalArgumentException`、`IllegalStateException` 或自定义异常，并提供易懂信息。
   - GraphQL 会将未捕获异常包装为 `INTERNAL_ERROR`；对预期错误可在 Service 抛出 `IllegalArgumentException`，由客户端处理。

---

## 四、数据库变更规范

1. **设计评审**：新增表或字段前先更新 `docs/rbac_design_v1.md`，与团队确认。
2. **Schema 文件**：修改 `db/schema.sql`（生产/演示环境） 和 `server/src/test/resources/test-schema.sql`（测试环境）。
3. **迁移策略**：若未来引入迁移工具（Flyway/Liquibase），需同步编写迁移脚本；当前阶段以 SQL 手动执行。
4. **测试数据**：必要时在测试脚本中插入种子数据，确保用例可运行。

---

## 五、单元 / 集成测试

1. **基础标准**
   - 所有新增业务功能必须至少有一个测试覆盖（`server/src/test/java`）。
   - 首选 GraphQL 集成测试：使用 `@SpringBootTest` + `WebTestClient`（或 `GraphQlTester`）针对 `/graphql` 做端到端验证。
   - 嵌入式数据库：复用 `EmbeddedPostgres`（见 `UserGraphqlIntegrationTest`），确保测试隔离。

2. **测试内容建议**
   - 正常路径：验证新增 Mutation/Query 的成功响应、字段值。
   - 异常场景：无权限、参数非法、重复数据等。
   - 辅助校验：断言数据库侧效果，如新增用户数、角色关联等（可通过 Repository 检查）。

3. **执行命令**
   ```bash
   cd server
  mvn test
   ```
   - 首次运行会下载 PostgreSQL 二进制（已配置 `io.zonky.test` 依赖）。
   - 将测试结果保存在 `server/target/surefire-reports/`，供回归分析。

---

## 六、提交与协作流程

1. **分支策略**：建议使用 `feature/<module>` 格式创建分支，完成后发起 PR。
2. **代码审查**：PR 需包含修改摘要、测试结果截图或日志；确保 reviewer 明确变更点。
3. **格式检查**：遵守现有 Checkstyle/格式约定（目前未强制，可后续引入）。
4. **文档更新**：改动接口或流程时同步更新 `docs/handout.md`、`docs/graphiql_cheatsheet.md`。
5. **联调准备**：确保管理员账号可用、数据库已初始化，更新 README/运行说明。

---

## 七、运行与演示流程

1. **启动后端**
   ```bash
   cd server
  mvn spring-boot:run
   ```
   - 需要 Postgres 连接（默认 `jdbc:postgresql://localhost:5432/ruangong`）。
   - GraphQL 端点：`http://<IP>:8080/graphql`
   - GraphiQL：`http://<IP>:8080/graphiql`

2. **演示账号**
   - 管理员：手机号 `13876543210` / 密码 `Admin@2024`
   - 其他账号：可通过 `registerStudent` 或 GraphQL Mutation 创建。

3. **调试脚本**
   - `docs/graphiql_cheatsheet.md` 提供完整操作流程（登录、创建用户、赋权、删除）。
   - Postman/GraphiQL 均可直接使用 cheatsheet 中的 body。

---

## 八、常见问题 & 指南

- **`UserProfile` Union 解析报错**：确保 `GraphQlWiringConfig` 中更新了新的 Profile 类型映射。
- **手机号校验不通过**：系统限制大陆号段，正则位于 `StudentRegisterInput` 等输入模型。
- **重复用户异常**：手机、用户名具备唯一约束，重试前先删除旧数据或更换标识。
- **自动化测试失败**：执行 `mvn -e test` 查看详细信息；常见原因是 Schema 未同步或断言不匹配。

---

## 九、冲刺期开发清单（执行顺序建议）

1. 需求评审 → 更新 Schema、设计文档。
2. 编写 Service/Repository → GraphQL Resolver → 更新 Schema → 单元/集成测试。
3. `mvn test` 自行验证 → 更新 cheatsheet/handout → 提交 PR。
4. 合并后由负责人执行数据库脚本（如有）。
5. 与前端联调前，确保启动文档和管理员账号信息同步。

---

如需更多规范或自动化工具（Lint、CI 等），在冲刺前评估引入。请确保团队成员阅读本指南，并在开发 PR 中引用相关章节，方便 reviewer 审查。***
