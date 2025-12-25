<template>
  <div>
    <h1>用户管理</h1>
    
    <!-- 顶部操作栏 -->
    <div class="operation-bar">
      <el-button type="primary" @click="showCreateDialog = true">
        新增用户
      </el-button>
      <el-button @click="refreshUsers">刷新</el-button>
      
      <!-- 角色筛选 -->
      <el-select v-model="selectedRole" placeholder="按角色筛选" clearable @change="refreshUsers" style="margin-left: 10px;">
        <el-option label="全部" value="" />
        <el-option v-for="role in roles" :key="role.id" :label="getRoleDisplayName(role.name)" :value="role.name" />
      </el-select>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" v-loading="loading" element-loading-text="正在加载数据..." style="height: 200px;">
    </div>

    <!-- 错误状态 -->
    <el-alert 
      v-if="error" 
      :title="`数据加载失败: ${getErrorMessage(error)}`" 
      type="error" 
      show-icon
      :closable="false"
      style="margin-bottom: 20px;"
    />

    <!-- 用户表格 -->
    <el-table :data="users" border style="width: 100%" v-if="!loading">
      <el-table-column prop="id" label="ID" width="180" />
      <el-table-column prop="phone" label="手机号" />
      <el-table-column prop="fullName" label="姓名" />
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.isActive ? 'success' : 'danger'">
            {{ row.isActive ? '激活' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="角色">
        <template #default="{ row }">
          <el-tag 
            v-for="role in row.roles" 
            :key="role.id" 
            closable
            @close="handleRemoveSpecificRole(row, role.id)"
            :disable-transitions="false"
            style="margin-right: 5px;"
            :type="getRoleTagType(role.name)"
          >
            {{ getRoleDisplayName(role.name) }}
          </el-tag>
          <el-tag v-if="row.roles.length === 0" type="danger">
            无角色
          </el-tag>
        </template>
      </el-table-column>
      
      <el-table-column label="操作" width="280">
        <template #default="{ row }">
          <!-- 角色管理下拉框 -->
          <el-select 
            v-model="row.currentRole" 
            placeholder="分配角色" 
            size="small" 
            style="width: 140px; margin-right: 10px;"
            @change="(roleId: string) => handleAssignRole(row, roleId)"
          >
            <el-option 
              label="移除角色" 
              value="" 
              :disabled="row.roles.length <= 1"
            />
            <el-option 
              v-for="role in availableRoles(row)" 
              :key="role.id" 
              :label="`添加: ${getRoleDisplayName(role.name)}`" 
              :value="role.id" 
            />
          </el-select>
          
          <el-button size="small" @click="handleEdit(row)">编辑</el-button>
          <el-button 
            size="small" 
            :type="row.isActive ? 'warning' : 'success'" 
            @click="handleToggleStatus(row)"
          >
            {{ row.isActive ? '禁用' : '激活' }}
          </el-button>
          <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增用户对话框 -->
    <el-dialog v-model="showCreateDialog" title="新增用户" width="500px">
      <el-form :model="createForm" :rules="createRules" ref="createFormRef" label-width="120px">
        <el-form-item label="角色" prop="roleName" required>
          <el-select v-model="createForm.roleName" placeholder="请选择角色" style="width: 100%;">
            <el-option 
              v-for="role in roles" 
              :key="role.id" 
              :label="getRoleDisplayName(role.name)" 
              :value="role.name" 
            />
          </el-select>
          <div class="form-tip">* 用户必须分配至少一个角色</div>
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="createForm.phone" />
        </el-form-item>
        <el-form-item label="姓名" prop="fullName">
          <el-input v-model="createForm.fullName" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="createForm.password" type="password" />
        </el-form-item>
        <el-form-item label="工号" prop="staffId" v-if="createForm.roleName && createForm.roleName !== 'STUDENT' && createForm.roleName !== 'student'">
          <el-input v-model="createForm.staffId" />
        </el-form-item>
        <el-form-item label="部门/学校" prop="schoolOrDepartment" v-if="createForm.roleName && createForm.roleName !== 'STUDENT' && createForm.roleName !== 'student'">
          <el-input v-model="createForm.schoolOrDepartment" />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button 
          type="primary" 
          @click="handleCreate" 
          :loading="creating"
          :disabled="!createForm.roleName"
        >
          确定
        </el-button>
      </template>
    </el-dialog>

    <!-- 编辑用户对话框 -->
    <el-dialog v-model="showEditDialog" title="编辑用户" width="500px">
      <el-form :model="editForm" :rules="editRules" ref="editFormRef" label-width="120px">
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="editForm.phone" />
        </el-form-item>
        <el-form-item label="姓名" prop="fullName">
          <el-input v-model="editForm.fullName" />
        </el-form-item>
        <el-form-item label="状态" prop="isActive">
          <el-switch v-model="editForm.isActive" />
        </el-form-item>
        <el-form-item label="角色状态">
          <div class="role-status-panel">
            <div class="current-roles">
              <span class="label">当前角色：</span>
              <el-tag 
                v-for="role in getUserCurrentRoles(editForm.id)" 
                :key="role.id" 
                :type="getRoleTagType(role.name)"
                style="margin-right: 5px;"
              >
                {{ getRoleDisplayName(role.name) }}
              </el-tag>
              <el-tag v-if="getUserCurrentRoles(editForm.id).length === 0" type="danger">
                用户暂无角色！
              </el-tag>
            </div>
            <div class="role-validation" :class="{ 'error': getUserCurrentRoles(editForm.id).length === 0 }">
              <el-icon><Warning /></el-icon>
              <span>用户必须至少拥有一个角色</span>
            </div>
          </div>
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button 
          type="primary" 
          @click="handleUpdate" 
          :loading="updating"
          :disabled="getUserCurrentRoles(editForm.id).length === 0"
        >
          {{ getUserCurrentRoles(editForm.id).length === 0 ? '请先分配角色' : '确定' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { gql } from '@apollo/client/core'
import { useQuery, useMutation } from '@vue/apollo-composable'
import { ref, reactive, computed, watch } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Warning } from '@element-plus/icons-vue'

// --- 类型定义 ---
interface User {
  id: string
  phone?: string
  fullName?: string
  isActive: boolean
  createdAt: string
  roles: Role[]
  currentRole?: string
}

interface Role {
  id: string
  name: string
  description?: string
}

interface CreateFormData {
  roleName: string
  phone: string
  fullName: string
  password: string
  staffId: string
  schoolOrDepartment: string
  department: string
}

interface EditFormData {
  id: string
  phone: string
  fullName: string
  isActive: boolean
}

// --- GraphQL 查询和变更 ---
const GET_USERS_QUERY = gql`
  query GetUsers($role: String) {
    users(role: $role) {
      id
      phone
      fullName
      isActive
      createdAt
      roles {
        id
        name
        description
      }
    }
  }
`

const GET_ROLES_QUERY = gql`
  query GetRoles {
    roles {
      id
      name
      description
    }
  }
`

const CREATE_USER_MUTATION = gql`
  mutation AdminCreateUser($input: AdminCreateUserInput!) {
    adminCreateUser(input: $input) {
      id
      phone
      fullName
      isActive
      roles {
        id
        name
      }
    }
  }
`

const UPDATE_USER_MUTATION = gql`
  mutation UpdateUser($id: ID!, $input: UpdateUserInput!) {
    updateUser(id: $id, input: $input) {
      id
      phone
      fullName
      isActive
    }
  }
`

const DELETE_USER_MUTATION = gql`
  mutation DeleteUser($id: ID!) {
    deleteUser(id: $id)
  }
`

const ASSIGN_ROLE_MUTATION = gql`
  mutation AssignRoleToUser($userId: ID!, $roleId: ID!) {
    assignRoleToUser(userId: $userId, roleId: $roleId) {
      id
      phone
      fullName
      roles {
        id
        name
      }
    }
  }
`

const REMOVE_ROLE_MUTATION = gql`
  mutation RemoveRoleFromUser($userId: ID!, $roleId: ID!) {
    removeRoleFromUser(userId: $userId, roleId: $roleId) {
      id
      phone
      fullName
      roles {
        id
        name
      }
    }
  }
`

// --- 工具函数：错误处理 ---
const getErrorMessage = (error: unknown): string => {
  if (error instanceof Error) {
    return error.message
  }
  if (typeof error === 'string') {
    return error
  }
  return '未知错误'
}

// --- 角色名称映射 ---
const roleNameMap: { [key: string]: string } = {
  'admin': '管理员',
  'student': '学生',
  'teacher': '教师',
  'finance': '财务',
  'ADMIN': '管理员',
  'STUDENT': '学生',
  'TEACHER': '教师',
  'FINANCE': '财务'
}

const roleTagTypes: { [key: string]: string } = {
  'admin': 'primary',
  'student': 'success',
  'teacher': 'warning',
  'finance': 'info',
  'ADMIN': 'primary',
  'STUDENT': 'success',
  'TEACHER': 'warning',
  'FINANCE': 'info'
}

// 获取角色显示名称
const getRoleDisplayName = (roleName: string): string => {
  return roleNameMap[roleName] || roleName
}

// 获取角色标签类型
const getRoleTagType = (roleName: string): string => {
  return roleTagTypes[roleName] || 'info'
}

// --- 响应式数据 ---
const selectedRole = ref('')

// 用户查询
const { result: usersResult, loading, error, refetch: refetchUsers } = useQuery(
  GET_USERS_QUERY, 
  () => ({ role: selectedRole.value || undefined }),
  { fetchPolicy: 'network-only' }
)

// 角色查询
const { result: rolesResult } = useQuery(GET_ROLES_QUERY)

// 计算属性
const users = computed(() => {
  const userList = usersResult.value?.users || []
  return userList.map((user: User) => ({
    ...user,
    currentRole: ''
  }))
})

const roles = computed(() => rolesResult.value?.roles || [])

// 对话框状态
const showCreateDialog = ref(false)
const showEditDialog = ref(false)
const creating = ref(false)
const updating = ref(false)
const assigningRole = ref(false)

// 表单引用
const createFormRef = ref<FormInstance>()
const editFormRef = ref<FormInstance>()

// 创建表单
const createForm = reactive<CreateFormData>({
  roleName: '',
  phone: '',
  fullName: '',
  password: '',
  staffId: '',
  schoolOrDepartment: '',
  department: ''
})

// 编辑表单
const editForm = reactive<EditFormData>({
  id: '',
  phone: '',
  fullName: '',
  isActive: true
})

// 表单验证规则
const createRules: FormRules = {
  roleName: [
    { required: true, message: '请选择角色', trigger: 'change' },
    { 
      validator: (_, value, callback) => {
        if (!value) {
          callback(new Error('角色不能为空'))
        } else {
          callback()
        }
      }, 
      trigger: 'change' 
    }
  ],
  fullName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }]
}

const editRules: FormRules = {
  fullName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }]
}

// --- 变更操作 ---
const { mutate: createUser } = useMutation(CREATE_USER_MUTATION)
const { mutate: updateUser } = useMutation(UPDATE_USER_MUTATION)
const { mutate: deleteUser } = useMutation(DELETE_USER_MUTATION)
const { mutate: assignRole } = useMutation(ASSIGN_ROLE_MUTATION)
const { mutate: removeRole } = useMutation(REMOVE_ROLE_MUTATION)

// --- 方法 ---
const refreshUsers = () => {
  refetchUsers()
}

// 获取用户当前角色（用于编辑对话框）
const getUserCurrentRoles = (userId: string) => {
  if (!userId) return []
  const user = users.value.find((u: User) => u.id === userId)
  return user ? user.roles : []
}

// 获取用户可分配的角色（排除已拥有的角色）
const availableRoles = (user: User) => {
  const userRoleIds = user.roles.map((role: Role) => role.id)
  return roles.value.filter((role: Role) => !userRoleIds.includes(role.id))
}

// 移除特定角色（从角色标签的关闭按钮触发）
const handleRemoveSpecificRole = async (user: User, roleId: string) => {
  // 检查是否只剩一个角色
  if (user.roles.length <= 1) {
    ElMessage.warning('用户必须至少保留一个角色')
    return
  }

  try {
    const roleToRemove = user.roles.find((role: Role) => role.id === roleId)
    const roleDisplayName = roleToRemove ? getRoleDisplayName(roleToRemove.name) : '未知角色'
    
    await ElMessageBox.confirm(
      `确定要从用户 "${user.fullName || user.phone}" 移除 "${roleDisplayName}" 角色吗？`,
      '确认移除角色',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )

    await removeRole({
      userId: user.id,
      roleId: roleId
    })
    
    ElMessage.success(`已移除用户 ${user.fullName || user.phone} 的 ${roleDisplayName} 角色`)
    refreshUsers()
  } catch (err) {
    if (err !== 'cancel') {
      ElMessage.error(`角色移除失败: ${getErrorMessage(err)}`)
    }
  }
}

// 分配角色
const handleAssignRole = async (user: User, roleId: string) => {
  if (!roleId) {
    // 移除角色逻辑（从下拉框选择"移除角色"）
    if (user.roles.length === 0) {
      ElMessage.warning('该用户暂无角色可移除')
      user.currentRole = ''
      return
    }
    
    if (user.roles.length === 1) {
      ElMessage.warning('用户必须至少保留一个角色')
      user.currentRole = ''
      return
    } else {
      // 多个角色时让用户选择要移除哪个
      try {
        const { value: roleToRemove } = await ElMessageBox.prompt(
          '请输入要移除的角色ID（多个角色用逗号分隔）',
          '移除角色',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            inputPlaceholder: '输入角色ID',
          }
        )
        
        if (roleToRemove) {
          const roleIds = roleToRemove.split(',').map((id: string) => id.trim())
          for (const roleId of roleIds) {
            const roleExists = user.roles.some((role: Role) => role.id === roleId)
            if (roleExists) {
              await handleRemoveSpecificRole(user, roleId)
            }
          }
        }
      } catch (cancel) {
        // 用户取消操作
      }
    }
    user.currentRole = ''
    return
  }

  // 分配新角色
  assigningRole.value = true
  try {
    await assignRole({
      userId: user.id,
      roleId: roleId
    })
    
    const assignedRole = roles.value.find((role: Role) => role.id === roleId)
    const roleName = assignedRole ? getRoleDisplayName(assignedRole.name) : '未知角色'
    ElMessage.success(`成功将用户 ${user.fullName || user.phone} 分配为 ${roleName} 角色`)
    refreshUsers()
  } catch (err) {
    ElMessage.error(`角色分配失败: ${getErrorMessage(err)}`)
  } finally {
    assigningRole.value = false
    user.currentRole = ''
  }
}

// 新增用户
const handleCreate = async () => {
  if (!createFormRef.value) return

  // 验证角色是否选择
  if (!createForm.roleName) {
    ElMessage.error('请选择角色')
    return
  }

  const valid = await createFormRef.value.validate()
  if (!valid) return

  creating.value = true
  try {
    await createUser({
      input: {
        roleName: createForm.roleName,
        phone: createForm.phone,
        fullName: createForm.fullName,
        password: createForm.password,
        staffId: createForm.staffId || undefined,
        schoolOrDepartment: createForm.schoolOrDepartment || undefined,
        department: createForm.department || undefined
      }
    })
    
    ElMessage.success('用户创建成功')
    showCreateDialog.value = false
    refreshUsers()
    createFormRef.value.resetFields()
  } catch (err) {
    ElMessage.error(`创建失败: ${getErrorMessage(err)}`)
  } finally {
    creating.value = false
  }
}

// 编辑用户
const handleEdit = (row: User) => {
  Object.assign(editForm, {
    id: row.id,
    phone: row.phone || '',
    fullName: row.fullName || '',
    isActive: row.isActive
  })
  showEditDialog.value = true
}

const handleUpdate = async () => {
  if (!editFormRef.value) return

  // 检查用户是否有角色
  const userRoles = getUserCurrentRoles(editForm.id)
  if (userRoles.length === 0) {
    ElMessage.error('用户必须至少拥有一个角色才能保存')
    return
  }

  const valid = await editFormRef.value.validate()
  if (!valid) return

  updating.value = true
  try {
    await updateUser({
      id: editForm.id,
      input: {
        phone: editForm.phone || undefined,
        fullName: editForm.fullName || undefined,
        isActive: editForm.isActive
      }
    })
    
    ElMessage.success('用户更新成功')
    showEditDialog.value = false
    refreshUsers()
  } catch (err) {
    ElMessage.error(`更新失败: ${getErrorMessage(err)}`)
  } finally {
    updating.value = false
  }
}

// 切换用户状态
const handleToggleStatus = async (row: User) => {
  // 检查用户是否有角色
  if (row.roles.length === 0) {
    ElMessage.error('无法操作：用户必须至少拥有一个角色')
    return
  }

  try {
    await updateUser({
      id: row.id,
      input: {
        isActive: !row.isActive
      }
    })
    
    ElMessage.success(`用户已${!row.isActive ? '激活' : '禁用'}`)
    refreshUsers()
  } catch (err) {
    ElMessage.error(`操作失败: ${getErrorMessage(err)}`)
  }
}

// 删除用户
const handleDelete = async (row: User) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除用户 "${row.fullName || row.phone}" 吗？此操作不可恢复。`,
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )

    const result = await deleteUser({ id: row.id })
    
    if (result?.data?.deleteUser) {
      ElMessage.success('用户删除成功')
      refreshUsers()
    }
  } catch (err) {
    if (err !== 'cancel') {
      ElMessage.error(`删除失败: ${getErrorMessage(err)}`)
    }
  }
}

// 监听角色变化，重新获取用户
watch(selectedRole, () => {
  refreshUsers()
})
</script>

<style scoped>
.operation-bar {
  margin-bottom: 20px;
  display: flex;
  align-items: center;
}

.role-select {
  width: 120px;
  margin-right: 10px;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.role-status-panel {
  padding: 12px;
  background: #f5f7fa;
  border-radius: 4px;
}

.role-status-panel .label {
  color: #606266;
  margin-right: 8px;
}

.role-validation {
  display: flex;
  align-items: center;
  margin-top: 8px;
  font-size: 12px;
  color: #e6a23c;
}

.role-validation.error {
  color: #f56c6c;
}

.role-validation .el-icon {
  margin-right: 4px;
}
</style>