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

    <!-- 准考证预览弹窗 -->
    <el-dialog v-model="showCardDialog" title="电子准考证" width="600px" destroy-on-close>
      <div class="admit-card-preview" v-loading="cardLoading">
        <div v-if="cardData" class="card-box">
          <div class="card-header">
            <h3>{{ cardData.subjectName }} - 准考证</h3>
            <span class="ticket-no">NO. {{ cardData.ticketNumber }}</span>
          </div>
          
          <div class="card-body">
            <el-descriptions :column="2" border>
              <el-descriptions-item label="考生姓名">{{ cardData.fullName }}</el-descriptions-item>
              <el-descriptions-item label="身份证号">{{ cardData.idCardNumber }}</el-descriptions-item>
              <el-descriptions-item label="考场信息" :span="2">
                {{ cardData.roomName }} ({{ cardData.roomNumber }}) - 座位: <b>{{ cardData.seatNumber }}</b>
              </el-descriptions-item>
              <el-descriptions-item label="考试时间" :span="2">
                {{ formatDate(cardData.sessionStartTime) }} 至 {{ formatDate(cardData.sessionEndTime) }}
              </el-descriptions-item>
            </el-descriptions>

            <div class="exam-notice" v-if="cardData.examNotice">
              <h4>考生须知：</h4>
              <p>{{ cardData.examNotice }}</p>
            </div>
          </div>
        </div>
        <el-empty v-else description="暂无准考证数据" />
      </div>
      <template #footer>
        <el-button @click="showCardDialog = false">关闭</el-button>
        <el-button type="primary" @click="printCard" :disabled="!cardData">打印</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { gql } from '@apollo/client/core'
import { useQuery, useLazyQuery } from '@vue/apollo-composable'
import { Ticket } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'

// 1. 获取考试列表
// 对应 Schema: myExamSchedules(subjectId: ID): [MyExamSchedule!]!
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
// 对应 Schema: admitCard(registrationInfoId: ID!, templateId: ID): AdmitCard
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

const handleViewAdmitCard = async (row: any) => {
  if (!row.registrationInfoId) {
    ElMessage.warning('报名信息ID缺失')
    return
  }
  showCardDialog.value = true
  // 懒加载查询
  loadCard(GET_ADMIT_CARD, { regId: row.registrationInfoId })
}

const printCard = () => {
  window.print() // 简单调用浏览器打印，实际项目中可使用 print-js 打印局部
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

/* 准考证样式 */
.card-box { border: 2px solid #303133; padding: 20px; border-radius: 8px; margin-top: 10px; }
.card-header { display: flex; justify-content: space-between; border-bottom: 1px solid #eee; padding-bottom: 15px; margin-bottom: 15px; }
.card-header h3 { margin: 0; }
.ticket-no { font-family: monospace; font-weight: bold; font-size: 18px; }
.exam-notice { margin-top: 20px; background: #f4f4f5; padding: 10px; border-radius: 4px; }
.exam-notice h4 { margin: 0 0 5px 0; font-size: 14px; }
.exam-notice p { margin: 0; font-size: 12px; color: #606266; line-height: 1.5; }

/* 打印时的样式控制 */
@media print {
  body * { visibility: hidden; }
  .admit-card-preview, .admit-card-preview * { visibility: visible; }
  .admit-card-preview { position: absolute; left: 0; top: 0; width: 100%; }
}
</style>