// src/stores/user.ts
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { gql } from '@apollo/client/core'
import { useApolloClient } from '@vue/apollo-composable'
import 'pinia-plugin-persistedstate'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const fullName = ref('')
  const role = ref('')
  const userId = ref('')
  
  const { resolveClient } = useApolloClient()

  const LOGIN_MUTATION = gql`
    mutation Login($phone: String!, $password: String!) {
      login(phone: $phone, password: $password) {
        token
        user {
          id
          fullName
          roles {
            name
            description
          }
        }
      }
    }
  `

  // --- 修改点：改为接收多个参数，以匹配 Login.vue 的调用方式 ---
  // 假设 Login.vue 传的是 (token, fullName, role)
  // 第4个参数 userId 设为可选，防止报错
  function setUserInfo(newToken: string, newName: string, newRole: string, newId: string = '') {
    token.value = newToken
    fullName.value = newName
    role.value = newRole
    if (newId) {
      userId.value = newId
    }
    
    if (newToken) {
      localStorage.setItem('token', newToken)
    }
  }

  async function login(phone: string, pass: string) {
    const client = resolveClient()
    try {
      const { data } = await client.mutate({
        mutation: LOGIN_MUTATION,
        variables: { phone, password: pass }
      })

      const payload = data.login
      
      const userRole = (payload.user.roles && payload.user.roles.length > 0) 
        ? payload.user.roles[0].name 
        : 'GUEST'

      // 这里的内部调用也需要同步修改为多参数形式
      setUserInfo(
        payload.token,
        payload.user.fullName || '用户',
        userRole,
        payload.user.id
      )

      return true
    } catch (error) {
      throw error
    }
  }

  function logout() {
    token.value = ''
    fullName.value = ''
    role.value = ''
    userId.value = ''
    localStorage.removeItem('token')
  }

  return { 
    token, 
    fullName, 
    role, 
    userId, 
    login, 
    logout,
    setUserInfo 
  }
}, {
  persist: true
} as any)