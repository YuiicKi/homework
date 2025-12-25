# GraphQL 接口说明（/graphql）

> 统一入口：`POST /graphql`（GraphQL 查询/变更）；GraphiQL: `http://localhost:8080/graphiql`  
> 鉴权：除显式标记“公开”外，其余均要求已登录；标注的权限字符串由后端校验。

## 用户与角色
- `registerStudent` (Mutation, 公开)：学生注册，入参 `StudentRegisterInput`。
- `login` (Mutation, 公开)：手机号+密码登录，返回 JWT。
- `me` (Query)：当前用户信息。
- `users` (Query, `user.read.all`)：按角色可选过滤用户列表。
- `user` (Query, `user.read`)：指定 ID 的用户详情。
- `roles` (Query, `role.read`)：角色列表。
- `adminCreateUser` (Mutation, `user.create`)：管理员创建用户。
- `updateUser` (Mutation, `user.update`)：更新用户。
- `deleteUser` (Mutation, `user.delete`)：删除用户。
- `createRole` (Mutation, `role.create`)：创建角色。
- `updateRole` (Mutation, `role.update`)：更新角色。
- `deleteRole` (Mutation, `role.delete`)：删除角色。
- `assignRoleToUser` / `removeRoleFromUser` (Mutation, `role.assign`)：为用户分配/移除角色。

示例：
```graphql
# 注册与登录
mutation {
  registerStudent(input: { phone: "13800000000", password: "Pass@123", name: "张三" }) {
    token
    user { id phone name }
  }
}

mutation {
  login(phone: "13800000000", password: "Pass@123") {
    token
    user { id roles { name } }
  }
}

# 当前用户
query { me { id phone roles { name } } }

# 管理员创建/更新/删除用户
mutation {
  adminCreateUser(input: { phone: "13900000000", password: "Pass@123", name: "李四", roleIds: [1,2] }) {
    id phone roles { name }
  }
}

mutation { updateUser(id: 10, input: { name: "新名字" }) { id name } }
mutation { deleteUser(id: 10) }

# 角色管理
mutation { createRole(name: "teacher", description: "老师") { id name } }
mutation { updateRole(id: 3, name: "teacher", description: "任课老师") { id name description } }
mutation { deleteRole(id: 3) }
mutation { assignRoleToUser(userId: 5, roleId: 2) { id phone roles { name } } }
mutation { removeRoleFromUser(userId: 5, roleId: 2) { id phone roles { name } } }

# 查询
query { users(role: "student") { id phone roles { name } } }
query { user(id: 5) { id phone name } }
query { roles { id name description } }
```

## 考点与考场
- `examCenters` / `examCenter` / `exportExamCenters` (Query, `center.read`)：考点列表/详情/导出。
- `createExamCenter` (Mutation, `center.create`)：新建考点。
- `updateExamCenter` (Mutation, `center.update`)：更新考点。
- `deleteExamCenter` (Mutation, `center.update`)：删除考点。
- `importExamCenters` (Mutation, `center.create`)：批量导入考点。
- `examRooms` / `examRoom` / `exportExamRooms` (Query, `room.read`)：考场列表/详情/导出，可按考点过滤。
- `createExamRoom` / `importExamRooms` (Mutation, `room.create`)：创建/批量导入考场。
- `updateExamRoom` (Mutation, `room.update`)：更新考场。
- `changeExamRoomStatus` (Mutation, `room.status.update`)：变更考场状态与原因。

示例：
```graphql
query { examCenters { id name address } }
query { examCenter(id: 1) { id name } }
query { exportExamCenters { id name } }

mutation {
  createExamCenter(input: { name: "第一考点", address: "XX路1号" }) {
    id name
  }
}

mutation {
  updateExamCenter(id: 1, input: { name: "第一考点(更新)", address: "XX路1号" }) {
    id name
  }
}

mutation { deleteExamCenter(id: 2) }
mutation {
  importExamCenters(inputs: [{ name: "考点A", address: "A路" }, { name: "考点B", address: "B路" }]) {
    id name
  }
}

query { examRooms(centerId: 1) { id name capacity } }
query { examRoom(id: 10) { id name centerId } }
query { exportExamRooms(centerId: 1) { id name } }

mutation {
  createExamRoom(input: { centerId: 1, name: "101", capacity: 30 }) {
    id name capacity
  }
}

mutation {
  importExamRooms(inputs: [{ centerId: 1, name: "201", capacity: 40 }]) {
    id name
  }
}

mutation {
  updateExamRoom(id: 10, input: { name: "101(更新)", capacity: 32 }) {
    id name capacity
  }
}

mutation {
  changeExamRoomStatus(input: { roomId: 10, status: "DISABLED", reason: "维修" }) {
    id status
  }
}
```

## 科目（考试项目）
- `examSubjects` / `examSubject` / `examSubjectLogs` (Query, `subject.read`)：科目列表/详情/日志，支持关键词、状态过滤。
- `exportExamSubjects` (Query, `subject.export`)：导出科目。
- `createExamSubject` (Mutation, `subject.create`)：新建科目。
- `updateExamSubject` (Mutation, `subject.update`)：更新科目。
- `updateExamSubjectStatus` / `batchUpdateExamSubjectStatus` (Mutation, `subject.status.update`)：单个/批量更新科目状态。
- `batchDeleteExamSubjects` (Mutation, `subject.delete`)：批量删除科目。
- `importExamSubjects` (Mutation, `subject.import`)：批量导入科目。

示例：
```graphql
query { examSubjects(keyword: "英语", status: "ACTIVE") { id code name status } }
query { examSubject(id: 5) { id code name } }
query { examSubjectLogs(subjectId: 5) { id action remark } }
query { exportExamSubjects(status: "ACTIVE") { id code name } }

mutation {
  createExamSubject(input: { code: "ENG-01", name: "英语一", status: "ACTIVE" }) {
    id code name
  }
}

mutation {
  updateExamSubject(id: 5, input: { code: "ENG-01", name: "英语一(更新)", status: "ACTIVE" }) {
    id name
  }
}

mutation {
  updateExamSubjectStatus(input: { subjectId: 5, status: "PAUSED", reason: "调整" }) {
    id status
  }
}

mutation {
  batchUpdateExamSubjectStatus(input: { subjectIds: [5,6], status: "ACTIVE", reason: "恢复" }) {
    id status
  }
}

mutation { batchDeleteExamSubjects(input: { subjectIds: [7,8] }) }

mutation {
  importExamSubjects(inputs: [{ code: "MATH-01", name: "数学" }, { code: "PHY-01", name: "物理" }]) {
    id code name
  }
}
```

## 报名窗口
- `examRegistrationWindows` / `examRegistrationWindow` (Query, `registration.read`)：报名窗口列表/详情。
- `exportExamRegistrationWindows` (Query, `registration.export`)：导出报名窗口。
- `examRegistrationWindowLogs` (Query, `registration.read`)：报名窗口日志。
- `upsertExamRegistrationWindow` (Mutation, `registration.create`)：创建或更新报名窗口。
- `updateExamRegistrationWindowStatus` / `batchUpdateExamRegistrationWindowStatus` (Mutation, `registration.status.update`)：单个/批量更新报名窗口状态。
- `batchDeleteExamRegistrationWindows` (Mutation, `registration.delete`)：批量删除报名窗口。
- `availableExams` / `availableExam` (Query，需登录)：学员可报考的考试列表/详情。

示例：
```graphql
query { examRegistrationWindows(subjectId: 5, status: "OPEN") { id subjectId status startAt endAt } }
query { examRegistrationWindow(id: 3) { id subjectId status } }
query { exportExamRegistrationWindows(subjectId: 5) { id status } }
query { examRegistrationWindowLogs(registrationId: 3) { id action remark } }

mutation {
  upsertExamRegistrationWindow(
    id: 3
    input: { subjectId: 5, startAt: "2025-12-10T00:00:00Z", endAt: "2025-12-20T00:00:00Z", status: "OPEN" }
  ) {
    id status
  }
}

mutation {
  updateExamRegistrationWindowStatus(input: { registrationId: 3, status: "CLOSED", reason: "截止" }) {
    id status
  }
}

mutation {
  batchUpdateExamRegistrationWindowStatus(input: { registrationIds: [3,4], status: "OPEN", reason: "重新开放" }) {
    id status
  }
}

mutation { batchDeleteExamRegistrationWindows(input: { registrationIds: [5,6] }) }

query { availableExams(keyword: "英语", status: "OPEN") { id name status } }
query { availableExam(id: 3) { id name status } }
```

## 通知
- `notifications` / `notification` (Query, `notification.read`)：通知列表/详情。
- `notificationTemplates` / `notificationTemplate` (Query, `notification.template`)：通知模板列表/详情。
- `notificationLogs` (Query, `notification.log`)：通知日志。
- `createNotification` (Mutation, `notification.create`)：创建通知。
- `updateNotification` (Mutation, `notification.update`)：更新通知。
- `publishNotification` (Mutation, `notification.publish`)：发布通知。
- `withdrawNotification` (Mutation, `notification.withdraw`)：撤回通知。
- `createNotificationTemplate` / `updateNotificationTemplate` (Mutation, `notification.template`)：创建/更新通知模板。

示例：
```graphql
query { notifications(keyword: "报名", status: "PUBLISHED") { id title status } }
query { notification(id: 2) { id title content status } }
query { notificationTemplates { id name channel } }
query { notificationTemplate(id: 1) { id name channel content } }
query { notificationLogs(id: 2) { id status message } }

mutation {
  createNotification(input: { title: "报名截止提醒", content: "请尽快提交资料", channel: "EMAIL" }) {
    id title status
  }
}

mutation {
  updateNotification(id: 2, input: { title: "报名截止提醒(更新)", content: "截止时间延长" }) {
    id title
  }
}

mutation { publishNotification(input: { notificationId: 2 }) { id status } }
mutation { withdrawNotification(input: { notificationId: 2 }) { id status } }

mutation {
  createNotificationTemplate(input: { name: "邮件模板", channel: "EMAIL", content: "Hello {{name}}" }) {
    id name
  }
}

mutation {
  updateNotificationTemplate(id: 1, input: { name: "邮件模板(更新)", content: "Hi {{name}}" }) {
    id name
  }
}
```

## 报名信息与材料
- `registrationInfo` (Query，需登录)：当前用户在某科目的报名信息。
- `registrationMaterialTemplates` (Query, `registration.material.template`)：报名材料模板列表。
- `pendingRegistrations` (Query, 管理员或 `registration.audit`)：待审核报名列表。
- `registrationAuditDetail` / `registrationAuditLogs` (Query, 管理员或 `registration.audit`)：报名审核详情/日志。
- `upsertRegistrationInfo` (Mutation，需登录)：新增/更新报名信息。
- `uploadRegistrationMaterial` (Mutation，需登录)：上传或更新报名材料。
- `deleteRegistrationMaterial` (Mutation，需登录)：删除报名材料。
- `upsertRegistrationMaterialTemplate` (Mutation, `registration.material.template`)：新增/更新材料模板。
- `deleteRegistrationMaterialTemplate` (Mutation, `registration.material.template`)：删除材料模板。
- `approveRegistration` / `rejectRegistration` (Mutation, 管理员或 `registration.audit`)：审核通过/拒绝报名。

示例：
```graphql
query { registrationInfo(subjectId: 5) { id subjectId status materials { id type url } } }
query { registrationMaterialTemplates { id type required allowedFormats } }
query { pendingRegistrations { id subjectId userId status } }
query { registrationAuditDetail(id: 10) { id status reviewerId } }
query { registrationAuditLogs(registrationInfoId: 10) { id action remark } }

mutation {
  upsertRegistrationInfo(input: { subjectId: 5, extraInfo: "备注" }) {
    id status
  }
}

mutation {
  uploadRegistrationMaterial(input: { registrationInfoId: 10, type: "ID_CARD", url: "https://example.com/id.png" }) {
    id type url
  }
}

mutation { deleteRegistrationMaterial(id: 20) }

mutation {
  upsertRegistrationMaterialTemplate(input: { id: null, type: "ID_CARD", allowedFormats: ["png","jpg"], required: true }) {
    id type required
  }
}

mutation { deleteRegistrationMaterialTemplate(id: 3) }

mutation { approveRegistration(registrationInfoId: 10) { id status } }
mutation { rejectRegistration(input: { registrationInfoId: 11, reason: "资料不全" }) { id status } }
```

## 排考与座位
- `examSessions` / `examSession` (Query, `session.read`)：场次列表/详情。
- `createExamSession` / `updateExamSession` (Mutation, `session.create` / `session.update`)：创建/更新场次。
- `examSchedules` / `examSchedule` (Query, `schedule.read`)：排考计划列表/详情，可按考场/科目/场次过滤。
- `createExamSchedule` / `updateExamSchedule` (Mutation, `schedule.create` / `schedule.update`)：创建/更新排考计划。
- `updateExamScheduleStatus` (Mutation, `schedule.status.update`)：更新排考计划状态。
- `deleteExamSchedule` (Mutation, `schedule.delete`)：删除排考计划。
- `seatAssignments` (Query, `schedule.read`)：座位编排列表。
- `seatAssignmentStats` / `seatAssignmentTasks` (Query, `schedule.read`)：座位编排统计/任务。
- `assignSeats` (Mutation, `schedule.update`)：执行座位编排。
- `resetSeats` (Mutation, `schedule.update`)：重置座位编排。

示例：
```graphql
query { examSessions { id name startAt endAt } }
query { examSession(id: 2) { id name } }

mutation {
  createExamSession(input: { name: "上午场", startAt: "2025-12-15T01:00:00Z", endAt: "2025-12-15T03:00:00Z" }) {
    id name
  }
}

mutation { updateExamSession(id: 2, input: { name: "上午场(更新)" }) { id name } }

query { examSchedules(roomId: 10, subjectId: 5, sessionId: 2) { id roomId subjectId sessionId } }
query { examSchedule(id: 3) { id status } }

mutation {
  createExamSchedule(input: { roomId: 10, subjectId: 5, sessionId: 2, plannedAt: "2025-12-15T01:00:00Z" }) {
    id status
  }
}

mutation {
  updateExamSchedule(id: 3, input: { roomId: 10, subjectId: 5, sessionId: 2, plannedAt: "2025-12-15T02:00:00Z" }) {
    id status
  }
}

mutation {
  updateExamScheduleStatus(input: { scheduleId: 3, status: "PUBLISHED", reason: "发布" }) {
    id status
  }
}

mutation { deleteExamSchedule(id: 4) }

query { seatAssignments(subjectId: 5, sessionId: 2, roomId: 10) { id seatNumber userId } }
query { seatAssignmentStats(subjectId: 5, sessionId: 2) { totalAssigned totalPending } }
query { seatAssignmentTasks(subjectId: 5, sessionId: 2) { id status } }

mutation {
  assignSeats(input: { subjectId: 5, sessionId: 2, roomId: 10 }) {
    id seatNumber userId
  }
}

mutation { resetSeats(subjectId: 5, sessionId: 2) }
```

## 准考证与个人排考
- `admitCard` (Query，需登录)：查询自己的准考证（支持指定模板）。
- `admitCardTemplates` (Query, `registration.material.template`)：准考证模板列表。
- `admitCardLogs` (Query，管理员/教师或本人拥有的报名)：准考证生成日志。
- `refreshAdmitCard` (Mutation，需登录)：刷新准考证。
- `upsertAdmitCardTemplate` (Mutation, `registration.material.template`)：新增/更新准考证模板。
- `deleteAdmitCardTemplate` (Mutation, `registration.material.template`)：删除准考证模板。
- `myExamSchedules` (Query，需登录)：当前用户的个人排考安排。

示例：
```graphql
query { admitCard(registrationInfoId: 10, templateId: 1) { registrationInfoId ticketNumber fileUrl } }
query { admitCardTemplates { id name logoUrl } }
query { admitCardLogs(registrationInfoId: 10) { id status message } }

mutation { refreshAdmitCard(registrationInfoId: 10, templateId: 1) { ticketNumber fileUrl } }

mutation {
  upsertAdmitCardTemplate(input: { id: null, name: "默认模板", logoUrl: "https://logo.png", examNotice: "注意事项", qrStyle: "square" }) {
    id name
  }
}

mutation { deleteAdmitCardTemplate(id: 1) }

query { myExamSchedules(subjectId: 5) { subjectId sessionId roomId seatNumber } }
```
