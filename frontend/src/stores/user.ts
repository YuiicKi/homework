// src/stores/user.ts
import { defineStore } from 'pinia'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: '',
    role: 'user',
    fullName: '' // 新增：存储用户姓名
  }),
  
  actions: {
    // 修改：添加 fullName 参数
    setUserInfo(newToken: string, newRole: string, fullName: string = '') {
      this.token = newToken
      this.role = newRole
      this.fullName = fullName // 存储用户姓名
      
      localStorage.setItem('token', newToken)
      localStorage.setItem('role', newRole)
      localStorage.setItem('fullName', fullName) // 存储到本地
    },
    
    logout() {
      this.token = ''
      this.role = 'user'
      this.fullName = '' // 清空姓名
      
      localStorage.removeItem('token')
      localStorage.removeItem('role')
      localStorage.removeItem('fullName')
    }
  },

  getters: {
    isAdmin: (state) => state.role === 'admin',
    // 新增：获取用户显示名称
    displayName: (state) => state.fullName || '用户'
  },
})