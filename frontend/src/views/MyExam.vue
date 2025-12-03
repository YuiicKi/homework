<template>
  <div class="my-exam-container">
    <el-card shadow="never">
      <div class="header">
        <h2>我的考试</h2>
        <el-button @click="refetch" :loading="loading">刷新安排</el-button>
      </div>

      <el-table :data="examList" border v-loading="loading" style="width: 100%">
        <el-table-column prop="subjectName" label="考试科目" width="180" />
        <el-table-column label="考试时间" width="320">
          <template #default="{ row }">
            <div class="time-cell">
              <div>{{ row.sessionName || '标准场次' }}</div>
              <div class="sub-text">
                {{ formatDate(row.sessionStartTime) }} ~ {{ formatDate(row.sessionEndTime) }}
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="考场位置" min-width="250">
          <template #default="{ row }">
            <div v-if="row.roomName">
              <span class="center-name">【{{ row.centerName }}】</span>
              {{ row.roomName }} ({{ row.roomNumber }})
            </div>
            <div v-else class="pending-text">待分配</div>
          </template>
        </el-table-column>
        <el-table-column label="座位号" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.seatNumber" effect="dark" type="warning">
              {{ row.seatNumber }} 号
            </el-tag>
            <span v-else class="pending-text">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="ticketNumber" label="准考证号" width="180" font-family="monospace" />
        <el-table-column label="操作" width="150" align="center" fixed="right">
          <template #default="{ row }">
            <el-button 
              type="primary" 
              plain 
              size="small" 
              :disabled="!row.ticketNumber"
              @click="handleViewAdmitCard(row)"
            >
              <el-icon class="el-icon--left"><Ticket /></el-icon>
              准考证
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="showCardDialog" title="电子准考证" width="850px" destroy-on-close top="5vh">
      
      <div v-loading="cardLoading" style="min-height: 200px; display: flex; justify-content: center;">
        <AdmitCard 
          v-if="cardData"
          :student="transformedStudent"
          :exams="transformedExams"
          :template-data="transformedTemplate"
          :exam-name="cardData.subjectName + ' 期末考试'"
        />
        <el-empty v-else-if="!cardLoading" description="暂无数据" />
      </div>

      <template #footer>
        <el-button @click="showCardDialog = false">关闭</el-button>
        <el-button type="primary" @click="printCard" :disabled="!cardData">
          <el-icon style="margin-right:5px"><Printer /></el-icon> 打印准考证
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { gql } from '@apollo/client/core'
import { useQuery, useLazyQuery } from '@vue/apollo-composable'
import { Ticket, Printer } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
// 关键点：引入我们之前写好的组件 (请确认路径是否正确)
import AdmitCard from './AdmitCard.vue'

// 1. 获取考试列表
const GET_MY_SCHEDULES = gql`
  query GetMyExamSchedules {
    myExamSchedules {
      registrationInfoId
      subjectName
      sessionName
      sessionStartTime
      sessionEndTime
      centerName
      roomName
      roomNumber
      seatNumber
      ticketNumber
    }
  }
`

// 2. 获取准考证详情
const GET_ADMIT_CARD = gql`
  query GetAdmitCard($regId: ID!) {
    admitCard(registrationInfoId: $regId) {
      ticketNumber
      subjectName
      fullName
      idCardNumber
      roomName
      roomNumber
      seatNumber
      sessionStartTime
      sessionEndTime
      examNotice
      logoUrl
    }
  }
`

// --- 列表逻辑 ---
const { result, loading, refetch } = useQuery(GET_MY_SCHEDULES)
const examList = computed(() => result.value?.myExamSchedules || [])

// --- 准考证逻辑 ---
const showCardDialog = ref(false)
const { load: loadCard, result: cardResult, loading: cardLoading } = useLazyQuery(GET_ADMIT_CARD)
const cardData = computed(() => cardResult.value?.admitCard)

// === 核心数据转换 ===
// 因为后端接口返回的是扁平结构，但组件 AdmitCard 需要结构化 Props
// 所以我们需要在这里进行一次转换适配

// 找到 MyExam.vue 中的 transformedStudent，替换为以下代码：

const transformedStudent = computed(() => {
  // 修改 1: 如果没有数据，返回 undefined，而不是空对象 {}
  // 这样 AdmitCard 就会使用它内部定义的默认值，或者不渲染
  if (!cardData.value) return undefined

  return {
    // 修改 2: 添加 "|| ''" 防止后端返回 null 导致类型报错
    name: cardData.value.fullName || '', 
    studentId: cardData.value.ticketNumber || '', 
    department: '本科生院', 
    // 注意: idCardNumber 如果 AdmitCard 的接口里没定义，传了也没用，但也不会报错
    // 如果想要传头像，可以在这里加 avatar: ''
  }
})

const transformedExams = computed(() => {
  if (!cardData.value) return []
  // 组件期望的是一个数组，哪怕只有一个考试
  return [{
    subjectName: cardData.value.subjectName,
    startTime: cardData.value.sessionStartTime,
    endTime: cardData.value.sessionEndTime,
    roomName: `${cardData.value.roomName} (${cardData.value.roomNumber})`,
    seatNumber: cardData.value.seatNumber
  }]
})

const transformedTemplate = computed(() => {
  if (!cardData.value) return {}
  return {
    logoUrl: cardData.value.logoUrl,
    examNotice: cardData.value.examNotice
  }
})
// ===================

const handleViewAdmitCard = async (row: any) => {
  if (!row.registrationInfoId) return ElMessage.warning('ID缺失')
  showCardDialog.value = true
  loadCard(null, { regId: row.registrationInfoId })
}

const printCard = () => {
  window.print()
}

// --- 工具函数 ---
const formatDate = (ts: string) => {
  if (!ts) return '-'
  return dayjs(Number(ts)).format('YYYY-MM-DD HH:mm')
}
</script>

<style scoped>
.my-exam-container { padding: 20px; }
.header { display: flex; justify-content: space-between; margin-bottom: 20px; }
.time-cell { display: flex; flex-direction: column; }
.sub-text { font-size: 12px; color: #909399; }
.pending-text { color: #dcdfe6; font-style: italic; }
.center-name { font-weight: bold; color: #303133; }
</style>