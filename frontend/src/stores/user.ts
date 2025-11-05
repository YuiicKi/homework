// src/stores/user.ts

import { defineStore } from 'pinia'

export const useUserStore = defineStore('user', {
  // 1. 状态 (State)
  state: () => ({
    // 
    // ⬇️ 【已修改】 ⬇️
    // 不再从 localStorage 读取初始 token，确保每次刷新都为空
    token: '',
    
    // ⬇️ 【已修改】 ⬇️
    // 不再从 localStorage 读取初始 role，确保每次都为默认值
    role: 'user', 
  }),
  
  // 2. 动作 (Actions)
  // (保持不变 - 登录时仍需设置 localStorage，以便在当前会话中刷新路由守卫)
  actions: {
    // 登录/注册成功时调用
    setUserInfo(newToken: string, newRole: string) {
      this.token = newToken
      this.role = newRole
      localStorage.setItem('token', newToken)
      localStorage.setItem('role', newRole)
    },
    
    // 退出登录时调用
    logout() {
      this.token = ''
      this.role = 'user' // 重置为默认角色
      localStorage.removeItem('token')
      localStorage.removeItem('role')
    }
  },

  // 3. 计算属性 (Getters)
  // (保持不变)
  getters: {
    // 方便侧边栏判断是否为管理员
    isAdmin: (state) => state.role === 'admin',
  },
})