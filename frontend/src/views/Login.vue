<template>
  <div class="login-container">
    <el-card class="login-card">
      <h2>在线考试系统</h2>
      <el-tabs v-model="activeTab" stretch>
        
        <!-- 登录标签页 -->
        <el-tab-pane label="登录" name="login">
          <el-form :model="loginForm" @submit.prevent="handleLogin">
            <el-form-item>
              <!-- 已修复：使用 loginForm.phone -->
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
              <!-- 【已修复】删除了括号里的示例文字 -->
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
              title="注册成功！请切换到“登录”标签页进行登录。"
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
import { useUserStore } from '../stores/user' // 修复了路径
import { useMutation } from '@vue/apollo-composable'
import { gql } from 'graphql-tag'
import type { ApolloError } from '@apollo/client/core'

const activeTab = ref('login')
const router = useRouter()
const userStore = useUserStore()

// --- 【新增】为 GQL 定义严格的 TS 类型 ---
interface Role {
  name: string
}

interface User {
  id: string
  username: string
  // 【!!】请和后端确认，这个字段是 `roles` 还是别的?
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
  loginIdentifier: string
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
// --- 类型定义结束 ---


// --- 登录逻辑 ---
const loginForm = reactive({
  phone: '', // 已修复：使用 phone
  password: '',
})

// 【!!】请和后端确认 GQL 语法 (roles { name }) 是否正确
// 已修复：使用 loginIdentifier
const LOGIN_MUTATION = gql`
  mutation login($loginIdentifier: String!, $password: String!) {
    login(loginIdentifier: $loginIdentifier, password: $password) {
      token
      user {
        id
        username
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

const handleLogin = () => {
  if (!loginForm.phone || !loginForm.password) {
    alert('请输入手机号和密码')
    return
  }
  
  loginError.value = null // 清除之前的错误

  loginMutation({
    loginIdentifier: loginForm.phone,
    password: loginForm.password,
  })
  // 【已修复】回调参数是 result，不是 { data }
  .then((result) => {
    // 登录成功后
    // 【已修复】从 result.data 中获取数据
    if (result?.data?.login?.token) {
      const token = result.data.login.token
      
      // 【!!】请和后端确认，'roles' 和 'name' 字段是否正确
      const userRole = result.data.login.user?.roles[0]?.name || 'user'
      
      userStore.setUserInfo(token, userRole)
      router.push('/') // 跳转到首页
    } else {
      // result?.data 存在，但 login.token 不存在
      // 这通常不应该发生，除非 GQL 允许 login 为 null
      alert('登录失败：后端未返回 Token。')
    }
  })
  .catch(e => {
    // GQL 错误或网络错误
    // 错误已由 loginError 自动捕获和显示
    console.error('Login error:', e)
  });
}


// --- 注册逻辑 ---
const regForm = reactive({
  fullName: '', 
  phone: '',    
  password: '',
  confirmPassword: '',
})
const registerSuccess = ref(false)

// 【!!】请和后端确认 GQL 语法 (roles { name }) 是否正确
// 已修复：使用 StudentRegisterInput, phone, fullName
const REGISTER_MUTATION = gql`
  mutation registerStudent($input: StudentRegisterInput!) {
    registerStudent(input: $input) { 
      token
      user {
        id
        username
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

const handleRegister = () => {
  if (!regForm.fullName) {
    alert('请输入姓名')
    return
  }
  if (!regForm.phone) {
    alert('请输入手机号')
    return
  }
  if (!regForm.password || !regForm.confirmPassword) {
    alert('请输入密码并确认')
    return
  }
  if (regForm.password !== regForm.confirmPassword) {
    alert('两次输入的密码不一致')
    return
  }
  
  registerSuccess.value = false
  registerError.value = null // 清除之前的错误

  registerMutation({
    input: {
      fullName: regForm.fullName, 
      phone: regForm.phone,       
      password: regForm.password,
    }
  })
  // 【已修复】回调参数是 result，不是 { data }
  .then((result) => {
    // 注册成功后
    // 【已修复】从 result.data 中获取数据
    if (result?.data?.registerStudent?.token) {
      // 自动登录
      const token = result.data.registerStudent.token
      
      // 【!!】请和后端确认，'roles' 和 'name' 字段是否正确
      const userRole = result.data.registerStudent.user?.roles[0]?.name || 'user'
      
      userStore.setUserInfo(token, userRole)
      router.push('/') // 跳转到首页
    } else {
      // 注册成功，但不自动登录 (例如后端需要邮箱验证)
      registerSuccess.value = true
      regForm.fullName = ''
      regForm.phone = ''
      regForm.password = ''
      regForm.confirmPassword = ''
    }
  })
  .catch(e => {
    // GQL 错误或网络错误
    // 错误已由 registerError 自动捕获和显示
    console.error('Register error:', e)
  });
}

// --- 统一错误处理 ---
const getErrorMessage = (err: ApolloError | null | undefined): string => {
  if (!err) {
    return '发生未知错误';
  }

  // 1. 网络错误 (Failed to fetch 等)
  if (err.networkError) {
    return `网络错误: ${err.networkError.message}。请检查后端是否在 ${'http://192.168.3.164:8080/graphql'} 运行，以及 CORS 设置。`;
  }

  // 2. GraphQL 错误 (后端返回的 errors 数组)
  if (err.graphQLErrors && err.graphQLErrors.length > 0) {
    const firstError = err.graphQLErrors[0];
    if (firstError?.message) { // 使用可选链
      return firstError.message; 
    }
    return 'GraphQL 返回了一个未知错误';
  }

  return '发生未知错误';
}

</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background-color: #f5f7fa; /* 浅色背景 */
}
.login-card {
  width: 450px;
  padding: 20px;
}
.login-card h2 {
  text-align: center;
  margin-bottom: 24px;
}
/* 确保 el-alert 也有上边距 */
.el-alert {
  margin-top: 20px;
}
</style>

