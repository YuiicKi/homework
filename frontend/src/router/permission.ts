// src/router/permission.ts

import type { Router } from 'vue-router'
// 【路径已更正】
import { useUserStore } from '../stores/user' 

/**
 * 设置路由全局前置守卫
 * @param router 路由实例
 */
export function setupPermissionGuard(router: Router) {
  
  // (to, from, next) -> (to, _from, next) 修复 "from 未读取" 的警告
  router.beforeEach((to, _from, next) => {
    
    // 【关键】
    // 这行代码现在是安全的，因为它在 main.ts 中被调用时，
    // Pinia 已经 100% 挂载完毕了。
    const userStore = useUserStore() 

    // --- 情况 A: 用户有 Token (已登录) ---
    if (userStore.token) {
      if (to.path === '/login') {
        next({ path: '/' }) 
      } else {
        next() 
      }
    } else {
      // --- 情况 B: 用户没有 Token (未登录) ---
      // (您修改 user.ts 后，刷新时 token 永远是 ''，所以会进入这里)
      if (to.path !== '/login') {
        // 强制重定向到登录页
        next({ path: '/login' }) 
      } else {
        // 如果就在登录页，则放行
        next() 
      }
    }
  })
}