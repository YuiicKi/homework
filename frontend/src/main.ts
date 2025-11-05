// src/main.ts

import { createApp } from 'vue'
import { createPinia } from 'pinia' // 1. Pinia
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import './style.css' 

import * as ElementPlusIconsVue from '@element-plus/icons-vue'

// 2. Apollo
import { ApolloClients } from '@vue/apollo-composable'
import { apolloClient } from './apollo' 

import App from './App.vue'
import router from './router' // 3. Router

// 
// ⬇️ 【 关键修改 1 】 ⬇️
// 不再是 import './router/permission'
// 而是导入 setupPermissionGuard 函数
import { setupPermissionGuard } from './router/permission' 
// ⬆️ 【 关键修改 1 】 ⬆️
//

const app = createApp(App)

// 全局注册 Element Plus 图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// --- 【 关键修改 2：调整挂载顺序 】 ---

// 1. 必须先挂载 Pinia
app.use(createPinia())

// 2. 挂载路由
app.use(router)

// 3. 挂载 Element Plus
app.use(ElementPlus) 

// 4. 提供 Apollo (它必须在 Pinia 之后，以便 authLink 能工作)
app.provide(ApolloClients, {
  default: apolloClient,
})

// 5. 【关键】最后设置路由守卫
// (此时 Pinia 和 Router 都已准备就绪)
setupPermissionGuard(router)

// 6. 挂载 App
app.mount('#app')