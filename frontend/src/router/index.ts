import { createRouter, createWebHistory } from 'vue-router'
// 单独导入 RouteRecordRaw 类型
import type { RouteRecordRaw } from 'vue-router'

// 导入布局组件
import Layout from '../layout/index.vue'
// 导入页面配置
import pagesConfig from '../pages.json'

// 【关键改动 1】建议使用 **/*.vue 以支持子文件夹（递归查找）
// 例如：views/user/UserManagement.vue 也能被找到
const viewModules = import.meta.glob('../views/**/*.vue')

/**
 * 动态生成子路由
 */
const dynamicRoutes: Array<RouteRecordRaw> = pagesConfig.pages.map(page => {
  // 尝试匹配组件路径
  // 1. 优先匹配 views 下的直接文件
  let moduleKey = `../views/${page.component}.vue`
  
  // 2. 如果没找到，且你将来可能有子文件夹结构，这里可以扩展逻辑
  // 目前保持你的简单逻辑，但建议加上容错
  
  if (!viewModules[moduleKey]) {
    console.error(`❌ 路由错误: 无法在 views 目录下找到组件 ${page.component}.vue，请检查文件名拼写`)
    // 找不到组件时，为了防止崩坏，可以给一个简单的 404 占位组件或者 null
    // 这里暂时不做处理，控制台报错方便调试
  }

  return {
    path: page.path,
    name: page.name, 
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
    component: Layout,
    // 【关键修复 2】删除了 redirect: '/' 
    // 因为 dynamicRoutes 里已经有一个 path: '/' 的 Dashboard 子路由了
    // 父路由只负责渲染 Layout，具体的 '/' 页面由子路由接管
    children: dynamicRoutes 
  },
  // 404 路由
  {
    path: '/:pathMatch(.*)*',
    redirect: '/' // 找不到页面时回到首页
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

export default router