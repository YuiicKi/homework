<template>
  <div class="notification-container">
    <el-card shadow="never">
      <div class="header">
        <h2><el-icon style="vertical-align: middle; margin-right: 8px"><Bell /></el-icon>通知公告</h2>
        <el-button link type="primary" @click="refetch" :loading="loading">
          <el-icon><Refresh /></el-icon> 刷新
        </el-button>
      </div>

      <div v-loading="loading" class="notification-list">
        <template v-if="notifications.length > 0">
          <div v-for="item in notifications" :key="item.id" class="notify-item">
            <div class="item-header">
              <div class="title-row">
                <el-tag :type="getTypeTag(item.type)" effect="dark" size="small" style="margin-right: 10px;">
                  {{ getTypeLabel(item.type) }}
                </el-tag>
                <span class="title-text">{{ item.title }}</span>
              </div>
              <span class="time-text">{{ formatDate(item.createdAt) }}</span>
            </div>
            
            <div class="item-content">
              {{ item.content }}
            </div>
          </div>
        </template>
        
        <el-empty v-else description="暂时没有新通知" />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { gql } from '@apollo/client/core'
import { useQuery } from '@vue/apollo-composable'
import { Bell, Refresh } from '@element-plus/icons-vue'
import dayjs from 'dayjs'

// --- GraphQL Query ---
// 对应后端 Schema: notifications(keyword: String, status: String): [Notification!]!
// 我们只查询 status 为 "PUBLISHED" 的数据
const GET_NOTIFICATIONS = gql`
  query GetStudentNotifications {
    notifications(status: "PUBLISHED") {
      id
      title
      type    # EXAM_NOTICE, SYSTEM_NOTICE, GRADE_NOTICE
      content
      createdAt
    }
  }
`

// --- Logic ---
// 设置 pollInterval: 30000 表示每30秒自动刷新一次，保证通知实时性
const { result, loading, refetch } = useQuery(GET_NOTIFICATIONS, null, {
  pollInterval: 30000,
  fetchPolicy: 'network-only'
})

const notifications = computed(() => result.value?.notifications || [])

// --- Helpers ---
const formatDate = (ts: string) => {
  if (!ts) return ''
  const num = Number(ts)
  const d = !isNaN(num) && num > 0 ? dayjs(num) : dayjs(ts)
  return d.isValid() ? d.format('YYYY-MM-DD HH:mm') : '-'
}

const getTypeTag = (type: string) => {
  const map: Record<string, string> = {
    'EXAM_NOTICE': 'danger',   // 红色，显眼
    'SYSTEM_NOTICE': 'primary', // 蓝色，常规
    'GRADE_NOTICE': 'success'   // 绿色，喜庆
  }
  return map[type] || 'info'
}

const getTypeLabel = (type: string) => {
  const map: Record<string, string> = {
    'EXAM_NOTICE': '考试安排',
    'SYSTEM_NOTICE': '系统公告',
    'GRADE_NOTICE': '成绩发布'
  }
  return map[type] || '通知'
}
</script>

<style scoped>
.notification-container { padding: 20px; max-width: 1000px; margin: 0 auto; }
.header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; border-bottom: 1px solid #EBEEF5; padding-bottom: 15px; }
.header h2 { margin: 0; font-size: 20px; color: #303133; }

.notify-item {
  padding: 20px;
  margin-bottom: 15px;
  background: #fff;
  border: 1px solid #EBEEF5;
  border-radius: 8px;
  transition: all 0.3s;
}

.notify-item:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  border-color: #dcdfe6;
}

.item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.title-row {
  display: flex;
  align-items: center;
}

.title-text {
  font-size: 16px;
  font-weight: bold;
  color: #303133;
}

.time-text {
  font-size: 13px;
  color: #909399;
}

.item-content {
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
  white-space: pre-wrap; /* 保留换行符 */
  background-color: #fcfcfc;
  padding: 10px;
  border-radius: 4px;
}
</style>