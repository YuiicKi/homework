import { createRouter, createWebHistory } from 'vue-router'
// 【重要】我们把 RouteRecordRaw 单独拿出来，并加上 'type'
import type { RouteRecordRaw } from 'vue-router' 

import Layout from '../layout/index.vue'
import pagesConfig from '../pages.json'

// 【关键改动】使用 Vite 独有的 import.meta.glob 来动态导入
// 这会创建一个映射表，Vite 在构建时能正确识别
const viewModules = import.meta.glob('../views/*.vue')

// 动态从 pages.json 生成子路由
const dynamicRoutes: Array<RouteRecordRaw> = pagesConfig.pages.map(page => {
  // 构建模块的路径键，例如 '../views/Dashboard.vue'
  const moduleKey = `../views/${page.component}`
  
  return {
    path: page.path,
    name: page.id,
    // 从映射表中查找对应的模块
    component: viewModules[moduleKey]
  } as RouteRecordRaw // 【关键修正】在这里添加类型断言
})

const routes: Array<RouteRecordRaw> = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue') 
  },
  {
    path: '/',
    component: Layout, // 使用我们的主布局
    children: dynamicRoutes // 注入所有动态页面
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

export default router

