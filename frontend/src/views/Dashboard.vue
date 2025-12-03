<template>
  <div class="dashboard-container">
    <!-- 欢迎头部卡片 -->
    <el-card shadow="never" class="welcome-card">
      <div class="welcome-content">
        <div class="avatar-section">
          <el-avatar :size="64" :style="{ backgroundColor: getAvatarColor(currentRole) }">
            {{ userStore.fullName?.[0] || 'U' }}
          </el-avatar>
        </div>
        <div class="text-section">
          <h2>你好，{{ userStore.fullName || '用户' }}，祝你开心每一天！</h2>
          
          <div class="role-info-bar">
            <span class="role-text">当前身份：
              <el-tag effect="dark" :type="getRoleType(currentRole)">
                {{ getRoleName(currentRole) }}
              </el-tag>
            </span>
            
            <!-- 🛠️ 调试工具：快速切换身份 -->
            <div class="debug-switcher">
              <span class="label">🛠️ 调试切换:</span>
              <el-radio-group v-model="userStore.role" size="small">
                <el-radio-button label="TESTER">超级测试员</el-radio-button>
                <el-radio-button label="ADMIN">管理员</el-radio-button>
                <el-radio-button label="TEACHER">教师</el-radio-button>
                <el-radio-button label="STUDENT">学生</el-radio-button>
                <el-radio-button label="FINANCE">财务</el-radio-button>
              </el-radio-group>
            </div>
          </div>

        </div>
      </div>
    </el-card>

    <!-- 快捷入口区域 -->
    <div class="actions-section">
      <h3>快捷入口</h3>
      
      <!-- 如果没有任何权限，显示提示 -->
      <el-empty v-if="!hasAnyShortcut" description="暂无可用菜单，请检查账号角色权限" />

      <el-row :gutter="20" v-else>
        
        <!-- ================= 管理员 & 教师 (测试员可见) ================= -->
        <el-col :span="6" v-if="isAdmin || isTeacher">
          <el-card shadow="hover" class="action-card" @click="$router.push('/scheduling')">
            <div class="card-icon" style="background: #e6f7ff; color: #1890ff;">
              <el-icon><Calendar /></el-icon>
            </div>
            <div class="card-info">
              <h4>考试安排</h4>
              <p>发布科目、排考场</p>
            </div>
          </el-card>
        </el-col>

        <el-col :span="6" v-if="isAdmin || isTeacher">
          <el-card shadow="hover" class="action-card" @click="$router.push('/exam-query')">
            <div class="card-icon" style="background: #fff2e8; color: #fa541c;">
              <el-icon><DataLine /></el-icon>
            </div>
            <div class="card-info">
              <h4>考试信息查询</h4>
              <p>查询报考数据、报表</p>
            </div>
          </el-card>
        </el-col>

        <!-- ================= 仅 管理员 (测试员可见) ================= -->
        <el-col :span="6" v-if="isAdmin">
          <el-card shadow="hover" class="action-card" @click="$router.push('/users')">
            <div class="card-icon" style="background: #f6ffed; color: #52c41a;">
              <el-icon><User /></el-icon>
            </div>
            <div class="card-info">
              <h4>用户管理</h4>
              <p>人员账号管理</p>
            </div>
          </el-card>
        </el-col>

        <!-- ================= 管理员 & 财务 (测试员可见) ================= -->
        <el-col :span="6" v-if="isAdmin || isFinance">
          <el-card shadow="hover" class="action-card" @click="$router.push('/finance')">
            <div class="card-icon" style="background: #f9f0ff; color: #722ed1;">
              <el-icon><Money /></el-icon>
            </div>
            <div class="card-info">
              <h4>财务缴费管理</h4>
              <p>流水审核、对账</p>
            </div>
          </el-card>
        </el-col>

        <!-- ================= 仅 学生 (测试员可见) ================= -->
        <el-col :span="6" v-if="isStudent">
          <el-card shadow="hover" class="action-card" @click="$router.push('/my-exam')">
            <div class="card-icon" style="background: #fff7e6; color: #fa8c16;">
              <el-icon><Search /></el-icon>
            </div>
            <div class="card-info">
              <h4>我的考试</h4>
              <p>座位查询、准考证</p>
            </div>
          </el-card>
        </el-col>

        <el-col :span="6" v-if="isStudent">
          <el-card shadow="hover" class="action-card" @click="$router.push('/exam-registration')">
            <div class="card-icon" style="background: #fff0f6; color: #eb2f96;">
              <el-icon><EditPen /></el-icon>
            </div>
            <div class="card-info">
              <h4>考试报名</h4>
              <p>在线报名申请</p>
            </div>
          </el-card>
        </el-col>

        <el-col :span="6" v-if="isStudent">
          <el-card shadow="hover" class="action-card" @click="$router.push('/my-payment')">
            <div class="card-icon" style="background: #e6fffb; color: #13c2c2;">
              <el-icon><Wallet /></el-icon>
            </div>
            <div class="card-info">
              <h4>我的缴费</h4>
              <p>待支付订单处理</p>
            </div>
          </el-card>
        </el-col>

      </el-row>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useUserStore } from '../stores/user'
// 引入图标
import { Calendar, User, Search, EditPen, Money, Wallet, DataLine } from '@element-plus/icons-vue'

const userStore = useUserStore()

// --- 核心修复：统一转为大写 ---
const currentRole = computed(() => (userStore.role || '').toUpperCase())

// --- 逻辑升级：超级测试员 (TESTER) 拥有所有权限 ---
const isTester = computed(() => currentRole.value === 'TESTER')

const isAdmin = computed(() => currentRole.value === 'ADMIN' || isTester.value)
const isTeacher = computed(() => currentRole.value === 'TEACHER' || isTester.value)
const isStudent = computed(() => currentRole.value === 'STUDENT' || isTester.value)
const isFinance = computed(() => currentRole.value === 'FINANCE' || isTester.value)

// 检查是否有任何快捷方式显示
const hasAnyShortcut = computed(() => isAdmin.value || isTeacher.value || isStudent.value || isFinance.value)

// --- UI 辅助函数 ---
const getRoleName = (role: string) => {
  const map: Record<string, string> = {
    'ADMIN': '系统管理员',
    'TEACHER': '考务教师',
    'STUDENT': '学生',
    'FINANCE': '财务管理员',
    'TESTER': '超级测试员(调试)'
  }
  return map[role] || role || '未知身份'
}

const getRoleType = (role: string) => {
  if (role === 'ADMIN') return 'danger'
  if (role === 'TEACHER') return 'warning'
  if (role === 'FINANCE') return 'info'
  if (role === 'TESTER') return 'primary' // 测试员用蓝色
  return 'success'
}

const getAvatarColor = (role: string) => {
  if (role === 'ADMIN') return '#f56c6c'
  if (role === 'TEACHER') return '#e6a23c'
  if (role === 'FINANCE') return '#909399'
  if (role === 'TESTER') return '#722ed1' // 测试员用紫色
  return '#409eff'
}
</script>

<style scoped>
.dashboard-container { padding: 20px; }
.welcome-card { margin-bottom: 20px; }
.welcome-content { display: flex; align-items: center; gap: 20px; }
.text-section { flex: 1; } /* 让文本区域占据剩余空间 */
.text-section h2 { margin: 0 0 10px 0; font-size: 20px; color: #303133; }

.role-info-bar { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 10px; }
.role-text { color: #909399; font-size: 14px; display: flex; align-items: center; gap: 8px; }

/* 调试切换器样式 */
.debug-switcher { display: flex; align-items: center; background: #f5f7fa; padding: 4px 8px; border-radius: 4px; }
.debug-switcher .label { font-size: 12px; color: #606266; margin-right: 8px; font-weight: bold; }

.actions-section h3 { margin-bottom: 15px; color: #303133; }
.action-card { cursor: pointer; transition: transform 0.2s; height: 100px; margin-bottom: 20px; }
.action-card:hover { transform: translateY(-3px); }
:deep(.el-card__body) { display: flex; align-items: center; padding: 15px !important; width: 100%; }
.card-icon { width: 48px; height: 48px; border-radius: 8px; display: flex; align-items: center; justify-content: center; font-size: 24px; margin-right: 15px; flex-shrink: 0; }
.card-info h4 { margin: 0 0 5px 0; font-size: 16px; font-weight: 600; }
.card-info p { margin: 0; font-size: 12px; color: #999; }
</style>