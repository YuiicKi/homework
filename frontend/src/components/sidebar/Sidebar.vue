<template>
  <div class="sidebar-wrapper">
    <!-- 
      顶部：收起按钮
      它会发出 'toggle' 事件，被 layout/index.vue 监听到 
    -->
    <div class="sidebar-header">
      <el-icon @click="$emit('toggle')" class="collapse-icon">
        <component :is="isCollapsed ? 'Expand' : 'Fold'" />
      </el-icon>
      <span v-if="!isCollapsed" class="sidebar-title">在线考试系统</span>
    </div>

    <!-- 菜单 -->
    <!-- 
      【关键改动】
      v-for 循环的对象从 'pages' 变成了 'visiblePages'
    -->
    <el-menu
      :default-active="$route.path"
      class="sidebar-menu"
      :collapse="isCollapsed"
      :collapse-transition="false"
      router
    >
      <el-menu-item 
        v-for="page in visiblePages" 
        :key="page.id" 
        :index="page.path"
      >
        <!-- 
          将来您可以从 pages.json 里读取 icon 名称
          <el-icon><component :is="page.meta.icon" /></el-icon> 
        -->
        <span class="menu-text">{{ page.name }}</span>
      </el-menu-item>
    </el-menu>

    <!-- 
      底部：退出登录按钮
      使用 flex-grow: 1 的 spacer 把它推到底部
    -->
    <div class="spacer"></div>
    <el-menu
      class="sidebar-menu logout-menu"
      :collapse="isCollapsed"
      :collapse-transition="false"
    >
      <el-menu-item @click="handleLogout" index="logout" class="logout-item">
        <!-- 【已修复】确保图标库已在 main.ts 全局注册 -->
        <el-icon><SwitchButton /></el-icon>
        <span class="menu-text">退出登录</span>
      </el-menu-item>
    </el-menu>

  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue' // 【改动】导入 computed
import { useRouter } from 'vue-router'
// 【已修复】路径从 ../stores/user 改为 ../../stores/user
import { useUserStore } from '../../stores/user' 
import pagesConfig from '../../pages.json'

// 1. 接收来自 layout 的 isCollapsed 属性
// 【已修复】移除了 'const props ='，以修复 'props' 未读取的警告
defineProps<{
  isCollapsed: boolean
}>()

// 2. 定义 $emit 事件，用于通知 layout 切换状态
const emit = defineEmits(['toggle'])

const pages = ref(pagesConfig.pages)
const router = useRouter()
const userStore = useUserStore()

// src/components/sidebar/Sidebar.vue (script setup 部分)

// 3. 【已修正】计算属性：根据角色过滤菜单
const visiblePages = computed(() => {
  // 统一转为大写进行比较（因为数据库存的是小写，pages.json用的是大写）
  const normalizedRole = (userStore.role || '').toUpperCase()

  // 如果是管理员，直接拥有上帝视角，看到所有菜单
  if (normalizedRole === 'ADMIN' || normalizedRole === 'TESTER') {
    return pages.value
  }

  // 如果不是管理员，则按规则过滤
  return pages.value.filter(page => {
    // 1. 如果页面没有 meta 配置，或者 meta.roles 为空，说明是公开页面
    if (!page.meta || !page.meta.roles) {
      return true
    }

    // 2. 检查普通用户的角色是否在允许列表中（大写比较）
    return page.meta.roles.includes(normalizedRole)
  })
})

// 4. 退出登录逻辑
const handleLogout = () => {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
/* 强制覆盖 el-menu 的默认背景色 */
.sidebar-menu,
.sidebar-menu .el-menu-item {
  background-color: transparent !important;
  border: none;
}

/* 整个侧边栏容器 */
.sidebar-wrapper {
  height: 100%;
  display: flex;
  flex-direction: column;
}

/* 顶部收起按钮区域 */
.sidebar-header {
  display: flex;
  align-items: center;
  height: 50px;
  padding: 0 10px;
  margin-bottom: 8px;
  flex-shrink: 0; /* 防止被压缩 */
}

.collapse-icon {
  font-size: 20px;
  cursor: pointer;
  padding: 8px;
  border-radius: 50%;
  color: #444746;
}
.collapse-icon:hover {
  background-color: #e0e0e0;
}

.sidebar-title {
  font-size: 1.1rem;
  font-weight: 500;
  margin-left: 16px;
  color: #1f1f1f;
  white-space: nowrap; /* 防止标题换行 */
}

/* 菜单项基础样式 */
.el-menu-item {
  height: 40px;
  line-height: 40px;
  margin-bottom: 4px;
  color: #1f1f1f; /* 默认文字颜色 */
  padding: 0 16px !important; /* 统一内边距 */
  border-radius: 99px; /* 胶囊圆角 */
  white-space: nowrap; /* 防止文字换行 */
}

/* 鼠标悬停在未选中项上的样式 */
.el-menu-item:not(.is-active):not(.logout-item):hover {
  background-color: #e0e0e0 !important; /* 悬停背景色 */
}

/* 【关键】Gemini 选中项的样式 
*/
.el-menu-item.is-active {
  background-color: #e6f0ff !important; /* 浅蓝色背景 */
  color: #001f33 !important; /* 深色文字 */
  font-weight: 500;
}

/* --- 收起状态下的样式 --- */

/* 菜单收起时，让图标居中 */
.el-menu--collapse {
  padding: 0 4px;
}
.el-menu--collapse .el-menu-item {
  padding: 0 !important;
  justify-content: center; /* 图标居中 */
}

/* 菜单收起时，菜单项的文字（如果还在）隐藏 */
.el-menu--collapse .menu-text {
  display: none;
}

/* --- 退出登录 --- */
.spacer {
  flex-grow: 1; /* 占满所有剩余空间，把退出按钮推到底部 */
}

.logout-menu {
  flex-shrink: 0; /* 防止被压缩 */
  margin-bottom: 8px;
}

.logout-menu .el-menu-item {
  color: #d9001b; /* 红色文字 */
}

.logout-menu .el-menu-item:hover {
  background-color: #fdebee !important; /* 浅红色背景 */
}

/* 选中退出按钮时的样式 (虽然它不会被选中) */
.logout-menu .el-menu-item.is-active {
  color: #d9001b !important;
  background-color: #fdebee !important;
}
</style>

