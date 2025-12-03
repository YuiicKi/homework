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

  // --- 修改点 2：Store 初始化时，主动清除本地残留 Token ---
  // 这保证了每次刷新页面或重新进入应用，Token 都是空的，强制用户重新登录
  localStorage.removeItem('token')

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
    
    // 依然写入 localStorage 是为了让 Apollo Client (HTTP Link) 在*本次会话*中能读取到 Token 发送请求
    // 但因为我们关闭了 persist 且初始化时会清理，下次刷新时这个 Token 就会被忽略并清除
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
  // --- 修改点 3：关闭持久化 ---
  // 这样刷新页面后，State 会重置为初始值（空），从而强制重新登录
  persist: false
} as any)