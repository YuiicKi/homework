<template>
  <el-container class="main-layout">
    
    <el-aside :width="isCollapsed ? '80px' : '220px'" class="sidebar-aside">
      <Sidebar 
        :is-collapsed="isCollapsed" 
        @toggle="toggleSidebar" 
      />
    </el-aside>
    
    <el-main class="main-content">
      <router-view></router-view>
    </el-main>

  </el-container>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import Sidebar from '../components/sidebar/Sidebar.vue'

// 1. 控制侧边栏是否收起的状态
const isCollapsed = ref(false)

// 2. 切换状态的函数
const toggleSidebar = () => {
  isCollapsed.value = !isCollapsed.value
}
</script>

<style scoped>
.main-layout {
  height: 100vh;
  width: 100vw;
  background-color: #f8f9fa; /* 匹配 Gemini 的浅灰背景 */
}

/* 侧边栏样式 */
.sidebar-aside {
  background-color: #f8f9fa; /* 侧边栏背景色 */
  transition: width 0.3s ease-in-out; /* 平滑的收起动画 */
  border-right: 1px solid #e0e0e0; /* 浅色边框 */
  padding: 8px; /* 增加内边距，让菜单项有空间显示圆角 */
  overflow-x: hidden; /* 收起时隐藏多余内容 */
}

/* 内容区样式 */
.main-content {
  background-color: #ffffff; /* 内容区白色背景 */
  padding: 24px;
  overflow-y: auto; /* 内容区允许滚动 */
}
</style>