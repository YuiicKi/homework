# GraphiQL 演示脚本速查

在 `http://<IP>:8080/graphiql` 中使用下列脚本，并在 Headers 面板加上  
`{"Authorization":"Bearer <token>"}`（需权限的操作）。

## 1. 管理员登录获取 Token
```graphql
mutation AdminLogin($phone:String!, $pwd:String!){
  login(phone:$phone, password:$pwd){
    token
    user {
      id
      phone
      fullName
      roles { name }
    }
  }
}
```
Variables
```json
{
  "phone": "13876543210",
  "pwd": "Admin@2024"
}
```

## 2. 查询当前登录用户
```graphql
query CurrentUser {
  me {
    id
    phone
    fullName
    roles { name }
    profile {
      ... on AdminProfile { fullName department }
      ... on TeacherProfile { fullName schoolOrDepartment }
      ... on StudentProfile { fullName idCardNumber }
    }
  }
}
```

## 3. 管理员创建教师账号
```graphql
mutation CreateTeacher($input: AdminCreateUserInput!){
  adminCreateUser(input: $input){
    id
    phone
    fullName
    roles { name }
    profile {
      ... on TeacherProfile {
        fullName
        staffId
        schoolOrDepartment
      }
    }
  }
}
```
Variables
```json
{
  "input": {
    "phone": "13987654321",
    "password": "Teacher@123",
    "roleName": "teacher",
    "fullName": "魏老师",
    "staffId": "T1001",
    "schoolOrDepartment": "数学系"
  }
}
```
> 说明：仅管理员拥有 `user.create` 权限，其它角色调用该接口会提示“无权访问”。

## 4. 查询教师列表
```graphql
query Teachers {
  users(role: "teacher") {
    id
    phone
    fullName
    roles { name }
    profile {
      ... on TeacherProfile { fullName staffId schoolOrDepartment }
    }
  }
}
```

## 5. 为用户赋予管理员角色
```graphql
mutation GrantAdmin($userId:ID!, $roleId:ID!){
  assignRoleToUser(userId:$userId, roleId:$roleId){
    id
    roles { name }
  }
}
```
Variables
```json
{
  "userId": "<目标用户ID>",
  "roleId": "1"
}
```

## 6. 删除用户
```graphql
mutation DeleteUser($id:ID!){
  deleteUser(id:$id)
}
```
Variables
```json
{
  "id": "<目标用户ID>"
}
```
