<template>
  <div class="my-exam-container">
    <el-card shadow="never">
      <div class="header">
        <h2>我的考试</h2>
        <el-button @click="handleRefresh" :loading="loading || gradesLoading" :icon="Refresh">刷新安排</el-button>
      </div>

      <el-table :data="mergedExamList" border v-loading="loading || gradesLoading" stripe style="width: 100%">
        <el-table-column prop="subjectName" label="考试科目" min-width="160" />
        
        <el-table-column label="考试时间" width="310">
          <template #default="{ row }">
            <div class="time-cell">
              <div class="session-name">{{ row.sessionName || '标准场次' }}</div>
              <div class="sub-text">
                {{ formatDate(row.sessionStartTime) }} ~ {{ formatDate(row.sessionEndTime) }}
              </div>
            </div>
          </template>
        </el-table-column>
        
        <el-table-column label="考场位置" min-width="220">
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

        <el-table-column label="成绩" width="140" align="center">
          <template #default="{ row }">
            <div v-if="row.score !== null && row.score !== undefined">
              <div class="score-text" :class="{ 'pass': row.isPassed, 'fail': !row.isPassed }">
                {{ row.score }} 分
              </div>
              <el-tag size="small" :type="row.isPassed ? 'success' : 'danger'" effect="plain">
                {{ row.isPassed ? '通过' : '未通过' }}
              </el-tag>
            </div>
            <span v-else class="text-gray">待发布</span>
          </template>
        </el-table-column>

        <el-table-column prop="ticketNumber" label="准考证号" width="160" show-overflow-tooltip font-family="monospace" />

        <el-table-column label="操作" width="220" align="center" fixed="right">
          <template #default="{ row }">
            <div class="action-buttons">
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

              <el-button 
                v-if="row.isPassed" 
                type="warning" 
                size="small" 
                :loading="generatingId === row.registrationInfoId"
                @click="handleDownloadCertificate(row)"
              >
                <el-icon class="el-icon--left"><Trophy /></el-icon>
                证书
              </el-button>
            </div>
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

    <div style="position: fixed; top: -9999px; left: -9999px; z-index: -1; visibility: visible;">
      <Certificate 
        v-if="certData"
        id="cert-node" 
        :student-name="certData.studentName"
        :exam-name="certData.examName"
        :score="certData.score"
        :certificate-number="certData.certificateNumber"
        :date="certData.date"
      />
    </div>

  </div>
</template>

<script setup lang="ts">
import { ref, computed, nextTick } from 'vue' // 已移除 watch
import { gql } from '@apollo/client/core'
import { useQuery, useLazyQuery, useApolloClient } from '@vue/apollo-composable'
import { Ticket, Printer, Trophy, Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import { useUserStore } from '../stores/user'

// 引入 PDF 生成工具
import html2canvas from 'html2canvas'
import { jsPDF } from 'jspdf'

// 引入子组件
import AdmitCard from './AdmitCard.vue'
import Certificate from './Certificate.vue'

// 常量定义 (暂硬编码，需与后端/管理端一致)
const CURRENT_EXAM_YEAR = 2025
const CURRENT_EXAM_TYPE = 'FINAL'

// --- GraphQL: 获取我的考试列表 (已移除不存在的字段) ---
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
      # score, isPassed, fullName, certificateNumber 已移除
    }
  }
`

// --- GraphQL: 单独查询成绩 (补充数据) ---
const GET_EXAM_RESULT = gql`
  query GetExamResult($input: ExamResultQueryInput!) {
    examResult(input: $input) {
      resultId
      fullName
      subjects {
        subjectName
        score
        isPass
      }
    }
  }
`

// --- GraphQL: 获取准考证详情 ---
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

// --- 状态与 Hooks ---
const userStore = useUserStore()
const { client } = useApolloClient()

// 1. 获取基础考试安排
const { result, loading, refetch: refetchSchedules, onResult } = useQuery(GET_MY_SCHEDULES, null, {
  fetchPolicy: 'network-only'
})

// 2. 本地状态存储成绩
const gradesMap = ref<Record<string, any>>({}) // Key: ticketNumber + subjectName, Value: { score, isPass, fullName }
const gradesLoading = ref(false)

// 3. 计算属性：合并数据
const mergedExamList = computed(() => {
  const list = result.value?.myExamSchedules || []
  return list.map((item: any) => {
    // 构造唯一键来匹配成绩 (假设一个准考证下可能有多个科目)
    const key = `${item.ticketNumber}_${item.subjectName}`
    const gradeInfo = gradesMap.value[key] || {}
    
    return {
      ...item,
      score: gradeInfo.score,     // undefined if not found
      isPassed: gradeInfo.isPass, // undefined if not found
      fullName: gradeInfo.fullName // 用于证书
    }
  })
})

// --- 核心逻辑：获取成绩 ---
const fetchGrades = async (schedules: any[]) => {
  const ticketNumbers = new Set(schedules.map((s: any) => s.ticketNumber).filter((t: string) => !!t))
  if (ticketNumbers.size === 0) return

  gradesLoading.value = true
  try {
    // 针对每个唯一的准考证号查询一次成绩
    const promises = Array.from(ticketNumbers).map(async (ticketNo) => {
      try {
        const res = await client.query({
          query: GET_EXAM_RESULT,
          variables: {
            input: {
              examType: CURRENT_EXAM_TYPE,
              examYear: CURRENT_EXAM_YEAR,
              ticketNumber: ticketNo
            }
          },
          fetchPolicy: 'network-only'
        })
        return { ticketNumber: ticketNo as string, data: res.data?.examResult }
      } catch (e) {
        // 忽略单个查询失败 (可能是成绩未出)
        console.warn(`Fetch grade failed for ${ticketNo}`, e)
        return null
      }
    })

    const results = await Promise.all(promises)

    // 更新本地 Map
    const newMap = { ...gradesMap.value }
    results.forEach(res => {
      if (res && res.data) {
        const { fullName, subjects } = res.data
        if (subjects && Array.isArray(subjects)) {
          subjects.forEach((sub: any) => {
            // 将成绩映射回 Map，Key = 准考证号_科目名
            const key = `${res.ticketNumber}_${sub.subjectName}`
            newMap[key] = {
              score: sub.score,
              isPass: sub.isPass,
              fullName: fullName
            }
          })
        }
      }
    })
    gradesMap.value = newMap

  } finally {
    gradesLoading.value = false
  }
}

// 监听列表变化自动获取成绩
onResult((queryResult) => {
  if (queryResult.data?.myExamSchedules) {
    fetchGrades(queryResult.data.myExamSchedules)
  }
})

const handleRefresh = () => {
  refetchSchedules()
  // fetchGrades 会在 onResult 中自动触发
}

// --- 准考证相关逻辑 ---
const showCardDialog = ref(false)
const { load: loadCard, result: cardResult, loading: cardLoading } = useLazyQuery(GET_ADMIT_CARD)
const cardData = computed(() => cardResult.value?.admitCard)

// 转换准考证数据适配组件
const transformedStudent = computed(() => {
  if (!cardData.value) return undefined
  return {
    name: cardData.value.fullName || '', 
    studentId: cardData.value.ticketNumber || '', 
    department: '本科生院', 
  }
})

const transformedExams = computed(() => {
  if (!cardData.value) return []
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

const handleViewAdmitCard = (row: any) => {
  if (!row.registrationInfoId) return ElMessage.warning('ID缺失')
  showCardDialog.value = true
  loadCard(null, { regId: row.registrationInfoId })
}

const printCard = () => {
  window.print()
}

// --- 证书下载核心逻辑 ---
const generatingId = ref('') 
const certData = ref<any>(null)

const handleDownloadCertificate = async (row: any) => {
  generatingId.value = row.registrationInfoId
  
  // 优先使用成绩单中返回的正式姓名，否则使用 Store 中的姓名
  const studentName = row.fullName || userStore.fullName || '考生'

  // 1. 准备数据供隐藏组件渲染
  certData.value = {
    studentName: studentName, 
    examName: row.subjectName,
    score: row.score,
    // 前端生成临时证书编号 (因为后端未提供)
    certificateNumber: `CERT-${CURRENT_EXAM_YEAR}-${row.registrationInfoId.substring(0,6).toUpperCase()}`,
    date: dayjs().format('YYYY年MM月DD日') 
  }

  // 2. 等待 DOM 更新
  await nextTick()
  // 额外延时，确保字体和样式渲染完成
  await new Promise(r => setTimeout(r, 200))
  
  try {
    const element = document.getElementById('cert-node')
    if (!element) throw new Error('证书渲染节点未找到')

    // 3. 截图
    const canvas = await html2canvas(element, {
      scale: 2, // 2倍缩放保证清晰度
      useCORS: true, // 允许跨域图片
      logging: false,
      backgroundColor: '#ffffff'
    })

    // 4. 生成 PDF (A4 横向)
    const pdf = new jsPDF('l', 'mm', 'a4') 
    const imgData = canvas.toDataURL('image/jpeg', 0.9)
    // A4尺寸: 297mm x 210mm
    pdf.addImage(imgData, 'JPEG', 0, 0, 297, 210)
    
    // 5. 下载
    pdf.save(`${row.subjectName}_荣誉证书.pdf`)
    ElMessage.success('证书已下载')

  } catch (e) {
    console.error(e)
    ElMessage.error('证书生成失败，请稍后重试')
  } finally {
    generatingId.value = ''
    certData.value = null // 清理 DOM
  }
}

// --- 工具 ---
const formatDate = (ts: string) => {
  if (!ts) return '-'
  const num = Number(ts)
  const d = !isNaN(num) && num > 0 ? dayjs(num) : dayjs(ts)
  return d.isValid() ? d.format('YYYY-MM-DD HH:mm') : '-'
}
</script>

<style scoped>
.my-exam-container { padding: 20px; }
.header { display: flex; justify-content: space-between; margin-bottom: 20px; align-items: center; }
.header h2 { margin: 0; color: #303133; }

.time-cell { display: flex; flex-direction: column; }
.session-name { font-weight: bold; color: #606266; }
.sub-text { font-size: 12px; color: #909399; margin-top: 2px; }

.center-name { font-weight: bold; color: #303133; }
.pending-text { color: #dcdfe6; font-style: italic; }
.text-gray { color: #909399; font-size: 13px; }

.score-text { font-size: 16px; font-weight: bold; line-height: 1.2; margin-bottom: 2px; }
.score-text.pass { color: #67C23A; }
.score-text.fail { color: #F56C6C; }

.action-buttons {
  display: flex;
  justify-content: center;
  gap: 8px;
}
</style>