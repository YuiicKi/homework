// src/stores/user.ts
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { gql } from '@apollo/client/core'
import { useApolloClient } from '@vue/apollo-composable'

// 即使不使用持久化，保留这个引入也没关系，或者您可以删除它
import 'pinia-plugin-persistedstate'

export const useUserStore = defineStore('user', () => {
  // --- 修改点 1：初始化时强制清空，不读取 localStorage ---
  const token = ref('') 
  const fullName = ref('')
  const role = ref('')
  const userId = ref('')

  // 从 sessionStorage 恢复登录状态（每个标签页独立）
  const savedToken = sessionStorage.getItem('token')
  const savedName = sessionStorage.getItem('fullName')
  const savedRole = sessionStorage.getItem('role')
  const savedUserId = sessionStorage.getItem('userId')
  if (savedToken) {
    token.value = savedToken
    fullName.value = savedName || ''
    role.value = savedRole || ''
    userId.value = savedUserId || ''
  }

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

  // 手动设置用户信息的辅助方法 (保持参数签名不变以适配 Login.vue)
  function setUserInfo(newToken: string, newName: string, newRole: string, newId: string = '') {
    token.value = newToken
    fullName.value = newName
    role.value = newRole
    if (newId) {
      userId.value = newId
    }
    
    // 保存到 sessionStorage（每个标签页独立）
    if (newToken) {
      sessionStorage.setItem('token', newToken)
      sessionStorage.setItem('fullName', newName)
      sessionStorage.setItem('role', newRole)
      sessionStorage.setItem('userId', newId)
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
    sessionStorage.removeItem('token')
    sessionStorage.removeItem('fullName')
    sessionStorage.removeItem('role')
    sessionStorage.removeItem('userId')
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
  // --- 修改点 3：关闭持久化 ---
  // 这样刷新页面后，State 会重置为初始值（空），从而强制重新登录
  persist: false
} as any)