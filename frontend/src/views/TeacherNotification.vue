<template>
  <div class="teacher-notification-container">
    <el-card shadow="never">
      <div class="header">
        <h2>我的监考任务</h2>
        <el-button @click="refetchAssignments" :loading="loading" :icon="Refresh">刷新</el-button>
      </div>

      <el-table :data="assignments" border v-loading="loading" stripe style="width: 100%">
        <el-table-column label="考试科目" min-width="150">
          <template #default="{ row }">
            <strong>{{ row.subjectName || '-' }}</strong>
          </template>
        </el-table-column>

        <el-table-column label="考试场次" min-width="200">
          <template #default="{ row }">
            <div>{{ row.sessionName || '标准场次' }}</div>
            <div class="sub-text">
              {{ formatDate(row.sessionStartTime) }} ~ {{ formatDate(row.sessionEndTime) }}
            </div>
          </template>
        </el-table-column>

        <el-table-column label="考点" min-width="180">
          <template #default="{ row }">
            <div>{{ row.centerName || '-' }}</div>
            <div class="sub-text">{{ row.centerAddress || '' }}</div>
          </template>
        </el-table-column>

        <el-table-column label="考场" width="150">
          <template #default="{ row }">
            <el-tag type="warning" effect="plain">
              {{ row.roomName || '-' }} ({{ row.roomNumber || '-' }})
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="分配时间" width="160">
          <template #default="{ row }">
            {{ formatDate(row.assignedAt) }}
          </template>
        </el-table-column>

        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row)" size="small">
              {{ getStatusLabel(row) }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && assignments.length === 0" description="暂无监考任务" />
    </el-card>

    <!-- 通知公告区域 -->
    <el-card shadow="never" style="margin-top: 20px;">
      <div class="header">
        <h2>监考相关通知</h2>
      </div>

      <div v-loading="notificationsLoading" class="notification-list">
        <template v-if="notifications.length > 0">
          <div v-for="item in notifications" :key="item.id" class="notify-item">
            <div class="item-header">
              <div class="title-row">
                <el-tag type="danger" effect="dark" size="small" style="margin-right: 10px;">监考通知</el-tag>
                <span class="title-text">{{ item.title }}</span>
              </div>
              <span class="time-text">{{ formatDate(item.createdAt) }}</span>
            </div>
            <div class="item-content">{{ item.content }}</div>
          </div>
        </template>
        <el-empty v-else description="暂无监考相关通知" />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { gql } from '@apollo/client/core'
import { useQuery } from '@vue/apollo-composable'
import { Refresh } from '@element-plus/icons-vue'
import { useUserStore } from '../stores/user'
import dayjs from 'dayjs'

const userStore = useUserStore()

// GraphQL: 获取我的监考任务
const GET_MY_INVIGILATOR_ASSIGNMENTS = gql`
  query GetMyInvigilatorAssignments($teacherUserId: ID) {
    invigilatorAssignments(scheduleId: null, teacherUserId: $teacherUserId) {
      id
      scheduleId
      teacherUserId
      teacherName
      subjectName
      sessionName
      sessionStartTime
      sessionEndTime
      centerName
      centerAddress
      roomName
      roomNumber
      assignedAt
    }
  }
`

// GraphQL: 获取监考相关通知
const GET_TEACHER_NOTIFICATIONS = gql`
  query GetTeacherNotifications {
    notifications(status: "PUBLISHED") {
      id
      title
      type
      content
      createdAt
    }
  }
`

// 监考任务查询 - 传入当前用户ID过滤
const { result, loading, refetch: refetchAssignments } = useQuery(GET_MY_INVIGILATOR_ASSIGNMENTS, () => ({
  teacherUserId: userStore.userId || null
}), {
  fetchPolicy: 'network-only'
})

const assignments = computed(() => result.value?.invigilatorAssignments || [])

// 通知查询
const { result: notificationResult, loading: notificationsLoading } = useQuery(GET_TEACHER_NOTIFICATIONS, null, {
  fetchPolicy: 'network-only',
  pollInterval: 60000
})

const notifications = computed(() => {
  const all = notificationResult.value?.notifications || []
  // 过滤只显示监考相关通知
  return all.filter((n: any) => n.type === 'EXAM_NOTICE' || n.title?.includes('监考'))
})

// 工具函数
const formatDate = (ts: string) => {
  if (!ts) return '-'
  const num = Number(ts)
  const d = !isNaN(num) && num > 0 ? dayjs(num) : dayjs(ts)
  return d.isValid() ? d.format('YYYY-MM-DD HH:mm') : '-'
}

const getStatusType = (row: any) => {
  if (!row.sessionStartTime) return 'info'
  const now = dayjs()
  const start = dayjs(Number(row.sessionStartTime) || row.sessionStartTime)
  const end = dayjs(Number(row.sessionEndTime) || row.sessionEndTime)

  if (now.isBefore(start)) return 'warning'
  if (now.isAfter(end)) return 'success'
  return 'danger'
}

const getStatusLabel = (row: any) => {
  if (!row.sessionStartTime) return '待定'
  const now = dayjs()
  const start = dayjs(Number(row.sessionStartTime) || row.sessionStartTime)
  const end = dayjs(Number(row.sessionEndTime) || row.sessionEndTime)

  if (now.isBefore(start)) return '待开始'
  if (now.isAfter(end)) return '已结束'
  return '进行中'
}
</script>

<style scoped>
.teacher-notification-container { padding: 20px; }
.header { display: flex; justify-content: space-between; margin-bottom: 20px; align-items: center; }
.header h2 { margin: 0; color: #303133; }

.sub-text { font-size: 12px; color: #909399; margin-top: 2px; }

.notification-list { margin-top: 10px; }

.notify-item {
  padding: 15px;
  margin-bottom: 12px;
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
  margin-bottom: 10px;
}

.title-row { display: flex; align-items: center; }
.title-text { font-size: 15px; font-weight: bold; color: #303133; }
.time-text { font-size: 12px; color: #909399; }

.item-content {
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
  white-space: pre-wrap;
  background-color: #fcfcfc;
  padding: 10px;
  border-radius: 4px;
}
</style>
