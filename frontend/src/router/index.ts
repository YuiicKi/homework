import { createRouter, createWebHistory } from 'vue-router'
// 【重要】单独导入 RouteRecordRaw 类型
import type { RouteRecordRaw } from 'vue-router'

// 【路径修正】根据您的截图，Layout 在 views/layout 目录下
import Layout from '../layout/index.vue'
import pagesConfig from '../pages.json'

// 【关键改动】使用 Vite 的 glob 导入来建立组件映射表
// 这会找到 ../views/ 下所有的 .vue 文件
const viewModules = import.meta.glob('../views/*.vue')

/**
 * 动态生成子路由
 * 遍历 pages.json 中的配置，自动匹配 views 目录下的组件
 */
const dynamicRoutes: Array<RouteRecordRaw> = pagesConfig.pages.map(page => {
  // 假设 pages.json 里的 component 字段只是文件名 (如 "Dashboard")
  // 我们需要拼接成完整的路径 key (如 "../views/Dashboard.vue")
  // 如果您的 json 里已经带了 .vue 后缀，请去掉这里的 .vue
  const moduleKey = `../views/${page.component}.vue`
  
  // 检查组件是否存在，避免报错
  if (!viewModules[moduleKey]) {
    console.error(`❌ 路由错误: 找不到组件 ${moduleKey}，请检查 pages.json 配置`)
  }

  return {
    path: page.path,
    name: page.name, // 使用中文名称或 ID 作为路由名称
    // 动态加载组件
    component: viewModules[moduleKey]
  } as RouteRecordRaw
})

const routes: Array<RouteRecordRaw> = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue') 
  },
  {
    path: '/',
    component: Layout, // 主布局
    redirect: '/', // 默认跳转（如果 pages.json 里有 path: '/' 的页面，这里会自动匹配）
    children: dynamicRoutes // 注入动态生成的子路由
  },
  // 404 路由 (可选)
  {
    path: '/:pathMatch(.*)*',
    redirect: '/'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

export default router