import type { Router } from 'vue-router'
import { useUserStore } from '../stores/user' 
import { ElMessage } from 'element-plus'

/**
 * 设置路由全局前置守卫
 */
export function setupPermissionGuard(router: Router) {
  
  router.beforeEach((to, _from, next) => {
    const userStore = useUserStore() 
    
    // 获取当前用户的角色 (转大写，防止大小写不匹配)
    const userRole = (userStore.role || '').toUpperCase()

    // --- 1. 判断是否已登录 ---
    if (userStore.token) {
      // [已登录]
      
      if (to.path === '/login') {
        // 如果已登录还要去登录页 -> 强制回首页
        next({ path: '/' }) 
      } else {
        // --- 2. 权限/角色验证 (核心新增逻辑) ---
        
        // 获取该路由需要的角色列表 (来源于 pages.json -> router/index.ts 的 meta)
        const requiredRoles = to.meta?.roles as string[] | undefined

        // 如果该页面配置了角色限制
        if (requiredRoles && requiredRoles.length > 0) {
          
          // 判断当前用户角色是否在允许列表中
          // (TESTER 是超级测试员，永远放行)
          const hasPermission = requiredRoles.includes(userRole) || userRole === 'TESTER'

          if (!hasPermission) {
            ElMessage.error('无权访问该页面')
            next({ path: '/' }) // 没权限 -> 踢回首页
            return
          }
        }

        // 验证通过，放行
        next() 
      }

    } else {
      // [未登录]
      
      //不仅要判断 path !== '/login'，还要防止无限重定向
      //有些路由可能有 query 参数，这里简单判断 path
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