<template>
  <div class="login-container">
    <el-card class="login-card">
      <h2>在线考试系统</h2>
      <el-tabs v-model="activeTab" stretch>
        
        <!-- 登录标签页 -->
        <el-tab-pane label="登录" name="login">
          <el-form :model="loginForm" @submit.prevent="handleLogin">
            <el-form-item>
              <el-input v-model="loginForm.phone" placeholder="手机号" size="large" />
            </el-form-item>
            <el-form-item>
              <el-input v-model="loginForm.password" type="password" placeholder="密码" size="large" show-password />
            </el-form-item>
            <el-form-item>
              <el-button 
                type="primary" 
                @click="handleLogin" 
                :loading="loginLoading"
                style="width: 100%;"
                size="large"
              >
                登录
              </el-button>
            </el-form-item>
            
            <!-- 登录错误提示 -->
            <el-alert
              v-if="loginError"
              :title="getErrorMessage(loginError)"
              type="error"
              show-icon
              :closable="false"
            />
          </el-form>
        </el-tab-pane>

        <!-- 注册标签页 -->
        <el-tab-pane label="注册" name="register">
          <el-form :model="regForm" @submit.prevent="handleRegister">
            
            <el-form-item>
              <el-input v-model="regForm.fullName" placeholder="姓名" size="large" />
            </el-form-item>
            
            <el-form-item>
              <el-input v-model="regForm.phone" placeholder="手机号" size="large" />
            </el-form-item>
            
            <el-form-item>
              <el-input v-model="regForm.password" type="password" placeholder="密码" size="large" show-password />
            </el-form-item>
            
            <el-form-item>
              <el-input v-model="regForm.confirmPassword" type="password" placeholder="确认密码" size="large" show-password />
            </el-form-item>

            <el-form-item>
              <el-button 
                type="primary" 
                @click="handleRegister" 
                :loading="registerLoading"
                style="width: 100%;"
                size="large"
              >
                注册
              </el-button>
            </el-form-item>
            
            <!-- 注册错误提示 -->
            <el-alert
              v-if="registerError"
              :title="getErrorMessage(registerError)"
              type="error"
              show-icon
              :closable="false"
            />
            
            <!-- 注册成功提示 -->
            <el-alert
              v-if="registerSuccess"
              title="注册成功！请切换到登录标签页进行登录。"
              type="success"
              show-icon
              :closable="false"
            />
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { useMutation } from '@vue/apollo-composable'
import { gql } from '@apollo/client/core'
import type { ApolloError } from '@apollo/client/core'

const activeTab = ref('login')
const router = useRouter()
const userStore = useUserStore()

// --- 类型定义 ---
interface Role {
  name: string
}

interface User {
  id: string
  fullName: string
  phone: string
  roles: Role[]
}

// 登录 GQL 的类型
interface LoginData {
  login: {
    token: string
    user: User
  }
}
interface LoginVariables {
  phone: string
  password: string
}

// 注册 GQL 的类型
interface RegisterData {
  registerStudent: {
    token: string
    user: User
  }
}
interface RegisterVariables {
  input: {
    fullName: string
    phone: string
    password: string
  }
}

// --- 登录逻辑 ---
const loginForm = reactive({
  phone: '',
  password: '',
})

const LOGIN_MUTATION = gql`
  mutation Login($phone: String!, $password: String!) {
    login(phone: $phone, password: $password) {
      token
      user {
        id
        fullName
        phone
        roles {
          name
        }
      }
    }
  }
`

const { 
  mutate: loginMutation, 
  loading: loginLoading, 
  error: loginError,
} = useMutation<LoginData, LoginVariables>(LOGIN_MUTATION)

const handleLogin = async () => {
  if (!loginForm.phone) {
    alert('请输入手机号')
    return
  }
  if (!loginForm.password) {
    alert('请输入密码')
    return
  }
  
  // 手机号格式验证
  const phoneRegex = /^1[3-9]\d{9}$/
  if (!phoneRegex.test(loginForm.phone)) {
    alert('请输入正确的手机号格式')
    return
  }

  try {
    loginError.value = null
    const result = await loginMutation({
      phone: loginForm.phone,
      password: loginForm.password,
    })

    if (result?.data?.login?.token) {
      const token = result.data.login.token
      const user = result.data.login.user
      const userRole = user.roles[0]?.name || 'user'
      
      userStore.setUserInfo(token, userRole, user.fullName)
      router.push('/')
    } else {
      alert('登录失败：未获取到 token')
    }
  } catch (error) {
    console.error('Login error:', error)
  }
}

// --- 注册逻辑 ---
const regForm = reactive({
  fullName: '',
  phone: '',
  password: '',
  confirmPassword: '',
})
const registerSuccess = ref(false)

const REGISTER_MUTATION = gql`
  mutation RegisterStudent($input: StudentRegisterInput!) {
    registerStudent(input: $input) {
      token
      user {
        id
        fullName
        phone
        roles {
          name
        }
      }
    }
  }
`

const { 
  mutate: registerMutation, 
  loading: registerLoading, 
  error: registerError,
} = useMutation<RegisterData, RegisterVariables>(REGISTER_MUTATION)

const handleRegister = async () => {
  // 表单验证
  if (!regForm.fullName.trim()) {
    alert('请输入姓名')
    return
  }
  if (!regForm.phone.trim()) {
    alert('请输入手机号')
    return
  }
  if (!regForm.password) {
    alert('请输入密码')
    return
  }
  if (regForm.password !== regForm.confirmPassword) {
    alert('两次输入的密码不一致')
    return
  }

  // 手机号格式验证
  const phoneRegex = /^1[3-9]\d{9}$/
  if (!phoneRegex.test(regForm.phone)) {
    alert('请输入正确的手机号格式')
    return
  }

  // 密码强度验证
  if (regForm.password.length < 6) {
    alert('密码长度至少6位')
    return
  }

  try {
    registerError.value = null
    const result = await registerMutation({
      input: {
        fullName: regForm.fullName.trim(),
        phone: regForm.phone,
        password: regForm.password,
      }
    })

    if (result?.data?.registerStudent?.token) {
      // 自动登录
      const token = result.data.registerStudent.token
      const user = result.data.registerStudent.user
      const userRole = user.roles[0]?.name || 'user'
      
      userStore.setUserInfo(token, userRole, user.fullName)
      router.push('/')
    } else {
      // 注册成功但不自动登录
      registerSuccess.value = true
      // 清空表单
      regForm.fullName = ''
      regForm.phone = ''
      regForm.password = ''
      regForm.confirmPassword = ''
    }
  } catch (error) {
    console.error('Register error:', error)
  }
}

// --- 错误处理 ---
const getErrorMessage = (err: ApolloError | null | undefined): string => {
  if (!err) {
    return '发生未知错误'
  }

  // 网络错误
  if (err.networkError) {
    return `网络错误: ${err.networkError.message}`
  }

  // GraphQL 错误
  if (err.graphQLErrors && err.graphQLErrors.length > 0) {
    const firstError = err.graphQLErrors[0]
    if (firstError?.message) {
      // 处理常见的错误信息
      if (firstError.message.includes('phone') && firstError.message.includes('exist')) {
        return '手机号不存在'
      }
      if (firstError.message.includes('password')) {
        return '密码错误'
      }
      if (firstError.message.includes('registered')) {
        return '该手机号已注册'
      }
      return firstError.message
    }
    return '请求失败'
  }

  return '发生未知错误'
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background-color: #f5f7fa;
}
.login-card {
  width: 450px;
  padding: 20px;
}
.login-card h2 {
  text-align: center;
  margin-bottom: 24px;
}
.el-alert {
  margin-top: 20px;
}
</style>