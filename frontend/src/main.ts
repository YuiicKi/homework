import { createApp } from 'vue'
import { createPinia } from 'pinia'
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate' // 引入持久化插件
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import './style.css' 

import * as ElementPlusIconsVue from '@element-plus/icons-vue'

// Apollo
import { ApolloClients } from '@vue/apollo-composable'
import { apolloClient } from './apollo' 

import App from './App.vue'
import router from './router'

// 导入路由守卫配置函数
import { setupPermissionGuard } from './router/permission' 

const app = createApp(App)

// --- 1. 配置 Pinia (包含持久化插件) ---
const pinia = createPinia()
pinia.use(piniaPluginPersistedstate) // 注册插件，解决 TS 报错并使功能生效
app.use(pinia)

// --- 2. 注册 Element Plus 图标 ---
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// --- 3. 挂载路由 ---
app.use(router)

// --- 4. 挂载 Element Plus ---
app.use(ElementPlus) 

// --- 5. 提供 Apollo Client ---
// (注意：如果你的 apollo.ts 内部依赖了 userStore，确保 Pinia 已经挂载)
app.provide(ApolloClients, {
  default: apolloClient,
})

// --- 6. 设置路由守卫 ---
// (此时 Pinia 和 Router 都已准备就绪，可以安全地在守卫中调用 Store)
setupPermissionGuard(router)

// --- 7. 挂载应用 ---
app.mount('#app')