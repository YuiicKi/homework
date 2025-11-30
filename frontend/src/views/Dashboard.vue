<template>
  <div class="dashboard-container">
    <el-card shadow="never" class="welcome-card">
      <div class="welcome-content">
        <div class="avatar-section">
          <el-avatar :size="64" :style="{ backgroundColor: getAvatarColor(userStore.role) }">
            {{ userStore.fullName?.[0] || 'U' }}
          </el-avatar>
        </div>
        <div class="text-section">
          <h2>你好，{{ userStore.fullName || '用户' }}，祝你开心每一天！</h2>
          <p class="role-text">当前身份：
            <el-tag effect="dark" :type="getRoleType(userStore.role)">
              {{ getRoleName(userStore.role) }}
            </el-tag>
          </p>
        </div>
      </div>
    </el-card>

    <div class="actions-section">
      <h3>快捷入口</h3>
      <el-row :gutter="20">
        
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

// --- 角色判断逻辑 ---
const isAdmin = computed(() => userStore.role === 'ADMIN')
const isTeacher = computed(() => userStore.role === 'TEACHER')
const isStudent = computed(() => userStore.role === 'STUDENT')
const isFinance = computed(() => userStore.role === 'FINANCE')

// --- UI 辅助函数 ---
const getRoleName = (role: string) => {
  const map: Record<string, string> = {
    'ADMIN': '系统管理员',
    'TEACHER': '考务教师',
    'STUDENT': '学生',
    'FINANCE': '财务管理员'
  }
  return map[role] || role
}

const getRoleType = (role: string) => {
  if (role === 'ADMIN') return 'danger'
  if (role === 'TEACHER') return 'warning'
  if (role === 'FINANCE') return 'info'
  return 'success'
}

const getAvatarColor = (role: string) => {
  if (role === 'ADMIN') return '#f56c6c'
  if (role === 'TEACHER') return '#e6a23c'
  if (role === 'FINANCE') return '#909399'
  return '#409eff'
}
</script>

<style scoped>
.dashboard-container { padding: 20px; }
.welcome-card { margin-bottom: 20px; }
.welcome-content { display: flex; align-items: center; gap: 20px; }
.text-section h2 { margin: 0 0 10px 0; font-size: 20px; color: #303133; }
.role-text { margin: 0; color: #909399; font-size: 14px; }
.actions-section h3 { margin-bottom: 15px; color: #303133; }
.action-card { cursor: pointer; transition: transform 0.2s; height: 100px; margin-bottom: 20px; }
.action-card:hover { transform: translateY(-3px); }
:deep(.el-card__body) { display: flex; align-items: center; padding: 15px !important; width: 100%; }
.card-icon { width: 48px; height: 48px; border-radius: 8px; display: flex; align-items: center; justify-content: center; font-size: 24px; margin-right: 15px; flex-shrink: 0; }
.card-info h4 { margin: 0 0 5px 0; font-size: 16px; font-weight: 600; }
.card-info p { margin: 0; font-size: 12px; color: #999; }
</style>