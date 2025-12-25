<template>
  <div class="grade-container">
    <el-card shadow="never">
      <div class="toolbar">
        <div class="filters">
          <h3>成绩录入与管理</h3>

          <el-select
            v-model="searchParams.subjectId"
            placeholder="选择考试科目"
            style="width: 200px; margin-right: 15px;"
            @change="handleSubjectChange"
          >
            <el-option
              v-for="sub in subjects"
              :key="sub.id"
              :label="sub.name + ' (' + sub.code + ')'"
              :value="sub.id"
            />
          </el-select>

          <el-select
            v-model="searchParams.sessionId"
            placeholder="选择考试场次"
            style="width: 200px; margin-right: 15px;"
            :disabled="!searchParams.subjectId"
            @change="fetchStudents"
          >
            <el-option
              v-for="ses in sessions"
              :key="ses.id"
              :label="ses.name"
              :value="ses.id"
            />
          </el-select>

          <el-button type="primary" :icon="Search" @click="fetchStudents" :disabled="!canSearch">查询考生</el-button>
        </div>

        <div class="actions">
          <el-button
            type="info"
            plain
            :icon="Bell"
            @click="openPreNotificationDialog"
          >
            成绩预告管理
          </el-button>

          <el-button
            type="warning"
            plain
            :icon="Timer"
            :disabled="!canSearch"
            @click="openTimeSettings"
          >
            发布时间设置
          </el-button>

          <el-button :icon="Download" @click="downloadTemplate">下载预填模板</el-button>

          <el-upload
            class="upload-btn"
            action=""
            :auto-upload="false"
            :show-file-list="false"
            :on-change="handleFileImport"
            accept=".xlsx, .csv"
          >
            <el-button type="success" :icon="Upload" :loading="importing">Excel 导入</el-button>
          </el-upload>
        </div>
      </div>

      <el-table :data="studentList" border v-loading="loading" stripe style="width: 100%; margin-top: 20px;">
        <el-table-column prop="ticketNumber" label="准考证号" width="180" sortable />
        
        <el-table-column label="姓名" width="150">
           <template #default><span>-</span></template>
        </el-table-column>
        <el-table-column label="证件号" width="180">
           <template #default><span>-</span></template>
        </el-table-column>
        
        <el-table-column label="当前状态" width="120">
          <template #default="{ row }">
             <el-tag :type="row.isAbsent ? 'danger' : 'success'" effect="plain">
               {{ row.isAbsent ? '缺考' : '正常' }}
             </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="卷面成绩 (0-100)" width="200">
          <template #default="{ row }">
            <el-input-number 
              v-model="row.score" 
              :min="0" 
              :max="100" 
              :precision="1" 
              :disabled="row.isAbsent"
              size="small"
              controls-position="right"
              @change="markAsDirty(row)"
            />
          </template>
        </el-table-column>

        <el-table-column prop="memo" label="备注/评语">
          <template #default="{ row }">
            <el-input v-model="row.memo" size="small" placeholder="本地备注" @input="markAsDirty(row)" />
          </template>
        </el-table-column>

        <el-table-column label="操作" width="100" align="center" fixed="right">
          <template #default="{ row }">
             <el-button 
               link 
               type="danger" 
               size="small" 
               @click="toggleAbsent(row)"
             >
               {{ row.isAbsent ? '取消缺考' : '标记缺考' }}
             </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="footer-actions" v-if="studentList.length > 0">
        <div class="info">
          共 {{ studentList.length }} 人，{{ dirtyCount }} 条待保存
        </div>
        <el-button type="primary" size="large" @click="saveGrades" :loading="saving" :disabled="dirtyCount === 0">
          保存更改
        </el-button>
      </div>
    </el-card>

    <el-dialog v-model="showTimeDialog" title="设置成绩发布时间" width="500px">
      <el-form label-width="120px">
        <el-form-item label="当前科目">
          <strong>{{ currentSubjectName }}</strong>
        </el-form-item>
        <el-form-item label="当前场次">
          <strong>{{ currentSessionName }}</strong>
        </el-form-item>
        <el-form-item label="成绩发布时间">
          <el-date-picker
            v-model="gradeReleaseTime"
            type="datetime"
            placeholder="选择发布日期和时间"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
          <div class="tips">到达该时间后，学生端将自动可见成绩。</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showTimeDialog = false">取消</el-button>
        <el-button type="primary" @click="saveReleaseTime" :loading="savingTime">保存设置</el-button>
      </template>
    </el-dialog>

    <!-- 成绩预告管理对话框 -->
    <el-dialog v-model="showPreNotificationDialog" title="成绩查询预告管理" width="800px">
      <div class="pre-notification-header">
        <el-button type="primary" size="small" @click="openCreatePreNotification">新建预告</el-button>
        <el-button size="small" @click="refetchPreNotifications">刷新列表</el-button>
      </div>

      <el-table :data="preNotifications" border v-loading="preNotificationsLoading" style="margin-top: 15px;">
        <el-table-column prop="examType" label="考试类型" width="100" />
        <el-table-column prop="examYear" label="年份" width="80" />
        <el-table-column prop="title" label="预告标题" min-width="180" />
        <el-table-column label="查询开放时间" width="160">
          <template #default="{ row }">{{ formatDateTime(row.queryTime) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'PUBLISHED' ? 'success' : 'info'" size="small">
              {{ row.status === 'PUBLISHED' ? '已发布' : '草稿' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" align="center">
          <template #default="{ row }">
            <el-button size="small" link type="primary" @click="editPreNotification(row)">编辑</el-button>
            <el-button
              size="small"
              link
              type="success"
              @click="publishPreNotification(row)"
              :disabled="row.status === 'PUBLISHED'"
            >
              发布
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- 新建/编辑预告对话框 -->
    <el-dialog v-model="showPreNotificationForm" :title="editingPreNotificationId ? '编辑预告' : '新建预告'" width="550px">
      <el-form :model="preNotificationForm" label-width="120px">
        <el-form-item label="考试类型" required>
          <el-select v-model="preNotificationForm.examType" placeholder="请选择" style="width: 100%">
            <el-option label="期末考试" value="FINAL" />
            <el-option label="期中考试" value="MIDTERM" />
            <el-option label="补考" value="MAKEUP" />
          </el-select>
        </el-form-item>
        <el-form-item label="考试年份" required>
          <el-input-number v-model="preNotificationForm.examYear" :min="2020" :max="2030" style="width: 100%" />
        </el-form-item>
        <el-form-item label="预告标题" required>
          <el-input v-model="preNotificationForm.title" placeholder="如：2025年期末考试成绩即将公布" />
        </el-form-item>
        <el-form-item label="查询开放时间" required>
          <el-date-picker
            v-model="preNotificationForm.queryTime"
            type="datetime"
            placeholder="选择成绩查询开放时间"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="预告内容" required>
          <el-input
            v-model="preNotificationForm.content"
            type="textarea"
            :rows="4"
            placeholder="通知内容，将推送给所有考生"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showPreNotificationForm = false">取消</el-button>
        <el-button type="primary" @click="savePreNotification" :loading="savingPreNotification">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive } from 'vue'
import { Search, Download, Upload, Timer, Bell } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, type UploadFile } from 'element-plus'
import { gql } from '@apollo/client/core'
import { useQuery, useMutation, useApolloClient } from '@vue/apollo-composable'
import dayjs from 'dayjs'

const { client } = useApolloClient()

// --- GraphQL Definitions ---

// 1. 获取科目
const GET_SUBJECTS = gql`
  query GetSubjectsForGrade {
    examSubjects(status: ENABLED) {
      id
      code
      name
    }
  }
`

// 2. 获取场次
const GET_SESSIONS = gql`
  query GetSessionsForGrade {
    examSessions {
      id
      name
    }
  }
`

// 3. 获取考生列表 (必须包含 registrationInfoId 用于 CSV 生成)
const GET_STUDENTS_GRADES = gql`
  query GetStudentGrades($subjectId: ID!, $sessionId: ID!) {
    seatAssignments(subjectId: $subjectId, sessionId: $sessionId) {
      id
      ticketNumber
      registrationInfoId
    }
  }
`

// 3.5 批量获取已录入的成绩
const GET_EXAM_RESULTS_BY_IDS = gql`
  query GetExamResultsByIds($registrationInfoIds: [ID!]!) {
    examResultsByRegistrationIds(registrationInfoIds: $registrationInfoIds) {
      registrationInfoId
      subjects {
        subjectId
        score
      }
    }
  }
`

// 4. 获取成绩发布时间
const GET_RESULT_RELEASE_SETTING = gql`
  query GetResultReleaseSetting($subjectId: ID, $examYear: Int) {
    resultReleaseSettings(subjectId: $subjectId, examYear: $examYear) {
      id
      releaseTime
      subjectName
    }
  }
`

// 5. 录入成绩 (单条)
const UPSERT_EXAM_RESULT = gql`
  mutation UpsertExamResult($input: UpsertExamResultInput!) {
    upsertExamResult(input: $input) {
      resultId
    }
  }
`

// 6. 设置发布时间
const UPSERT_RELEASE_SETTING = gql`
  mutation UpsertResultReleaseSetting($input: ExamResultReleaseSettingInput!) {
    upsertResultReleaseSetting(input: $input) {
      id
      releaseTime
    }
  }
`

// 7. 导入成绩 Mutation
const IMPORT_EXAM_RESULTS = gql`
  mutation ImportExamResults($input: ExamResultImportInput!) {
    importExamResults(input: $input) {
      id
      status
      totalCount
      successCount
      failureCount
    }
  }
`

// 8. 成绩预告相关 GraphQL
const GET_PRE_NOTIFICATIONS = gql`
  query GetResultPreNotifications {
    resultPreNotifications {
      id
      examType
      examYear
      queryTime
      title
      content
      status
      lastPublishedAt
    }
  }
`

const CREATE_PRE_NOTIFICATION = gql`
  mutation CreateResultPreNotification($input: ExamResultPreNotificationInput!) {
    createResultPreNotification(input: $input) {
      id
      title
      status
    }
  }
`

const UPDATE_PRE_NOTIFICATION = gql`
  mutation UpdateResultPreNotification($id: ID!, $input: ExamResultPreNotificationInput!) {
    updateResultPreNotification(id: $id, input: $input) {
      id
      title
      status
    }
  }
`

const PUBLISH_PRE_NOTIFICATION = gql`
  mutation PublishResultPreNotification($id: ID!) {
    publishResultPreNotification(id: $id) {
      id
      status
      lastPublishedAt
    }
  }
`

// --- State ---
const searchParams = reactive({
  subjectId: '',
  sessionId: ''
})

const studentList = ref<any[]>([])
const dirtySet = ref(new Set<string>()) 
const showTimeDialog = ref(false)
const gradeReleaseTime = ref('')
const currentReleaseSettingId = ref<string | null>(null)
const importing = ref(false)

// --- Queries ---
const { result: subResult } = useQuery(GET_SUBJECTS)
const { result: sessResult } = useQuery(GET_SESSIONS)
const subjects = computed(() => subResult.value?.examSubjects || [])
const sessions = computed(() => sessResult.value?.examSessions || [])

const canSearch = computed(() => searchParams.subjectId && searchParams.sessionId)

const currentSubjectName = computed(() => subjects.value.find((s:any) => s.id === searchParams.subjectId)?.name || '-')
const currentSessionName = computed(() => sessions.value.find((s:any) => s.id === searchParams.sessionId)?.name || '-')

// --- Methods ---

const handleSubjectChange = () => {
  studentList.value = []
  searchParams.sessionId = ''
}

// 获取学生列表 - 使用响应式 enabled 控制查询触发
const queryEnabled = ref(false)
const { onResult, loading } = useQuery(GET_STUDENTS_GRADES,
  () => ({
    subjectId: searchParams.subjectId,
    sessionId: searchParams.sessionId
  }),
  () => ({ enabled: queryEnabled.value, fetchPolicy: 'network-only' })
)

onResult(async (res) => {
  if (res.data?.seatAssignments) {
    const seats = res.data.seatAssignments
    const registrationIds = seats.map((s: any) => s.registrationInfoId).filter(Boolean)

    // 查询已有成绩
    let scoreMap: Record<string, number> = {}
    if (registrationIds.length > 0) {
      try {
        const gradeRes = await client.query({
          query: GET_EXAM_RESULTS_BY_IDS,
          variables: { registrationInfoIds: registrationIds },
          fetchPolicy: 'network-only'
        })
        const results = gradeRes.data?.examResultsByRegistrationIds || []
        for (const r of results) {
          // 取第一个科目的成绩（当前查询场景下只有一个科目）
          const subjectScore = r.subjects?.[0]?.score
          if (subjectScore != null && r.registrationInfoId) {
            scoreMap[r.registrationInfoId] = subjectScore
          }
        }
      } catch (e) {
        console.warn('获取已有成绩失败:', e)
      }
    }

    studentList.value = seats.map((item: any) => ({
      ...item,
      score: scoreMap[item.registrationInfoId] ?? 0,
      isAbsent: false,
      memo: ''
    }))
    dirtySet.value.clear()
  }
  // 查询完成后重置，以便下次可以重新触发
  queryEnabled.value = false
})

// 获取发布时间 (查询)
const { onResult: onSettingResult, refetch: refetchSetting } = useQuery(GET_RESULT_RELEASE_SETTING,
  () => ({ subjectId: searchParams.subjectId, examYear: 2025 }),
  { enabled: false, fetchPolicy: 'network-only' }
)

onSettingResult((res) => {
  const settings = res.data?.resultReleaseSettings || []
  if (settings.length > 0) {
    const rawTime = settings[0].releaseTime
    if (rawTime) {
      const t = dayjs(Number(rawTime) ? Number(rawTime) : rawTime)
      gradeReleaseTime.value = t.isValid() ? t.format('YYYY-MM-DD HH:mm:ss') : ''
    }
    currentReleaseSettingId.value = settings[0].id
  } else {
    gradeReleaseTime.value = ''
    currentReleaseSettingId.value = null
  }
})

const fetchStudents = () => {
  if (!canSearch.value) return
  // 通过设置 enabled 为 true 来触发查询
  queryEnabled.value = true
}

const markAsDirty = (row: any) => {
  dirtySet.value.add(row.id)
}

const dirtyCount = computed(() => dirtySet.value.size)

const toggleAbsent = (row: any) => {
  row.isAbsent = !row.isAbsent
  if (row.isAbsent) row.score = 0
  markAsDirty(row)
}

// 保存成绩 (手动保存)
const { mutate: upsertResult, loading: saving } = useMutation(UPSERT_EXAM_RESULT)

const saveGrades = async () => {
  const dirtyItems = studentList.value.filter(row => dirtySet.value.has(row.id))
  if (dirtyItems.length === 0) return

  try {
    const promises = dirtyItems.map(row => {
      return upsertResult({
        input: {
          registrationInfoId: row.registrationInfoId,
          examType: 'FINAL',
          examYear: 2025,
          ticketNumber: row.ticketNumber,
          subjects: [{
            subjectId: searchParams.subjectId,
            subjectName: currentSubjectName.value,
            score: row.score,
            isPass: row.score >= 60
          }]
        }
      })
    })

    await Promise.all(promises)
    ElMessage.success(`成功保存 ${dirtyItems.length} 条成绩记录`)
    dirtySet.value.clear()
  } catch (e: any) {
    ElMessage.error('保存失败: ' + (e.message || '未知错误'))
  }
}

// 发布时间设置
const { mutate: upsertReleaseSetting, loading: savingTime } = useMutation(UPSERT_RELEASE_SETTING)

const openTimeSettings = () => {
  if (!canSearch.value) return
  refetchSetting()
  showTimeDialog.value = true
}

const saveReleaseTime = async () => {
  if (!gradeReleaseTime.value) {
    return ElMessage.warning('请选择时间')
  }
  
  try {
    const formattedTime = dayjs(gradeReleaseTime.value).format('YYYY-MM-DDTHH:mm:ssZ')
    await upsertReleaseSetting({
      input: {
        subjectId: searchParams.subjectId,
        examYear: 2025,
        releaseTime: formattedTime
      }
    })
    ElMessage.success('发布时间设置成功')
    showTimeDialog.value = false
  } catch (e: any) {
    ElMessage.error('设置失败: ' + e.message)
  }
}

// ==========================================
// ====== [核心修改] 下载预填模板逻辑 ======
// ==========================================

// 1. 定义后端严格要求的 CSV Header (必须匹配后端 Bean 字段顺序)
const CSV_HEADER = [
  'registrationInfoId', // 核心字段：必须存在，否则后端无法关联
  'examType',           
  'examYear',           
  'ticketNumber',       
  'subjectId',
  'subjectName',
  'subjectScore',       // 老师主要填这一列
  'subjectPassLine',
  'subjectIsPass',
  'subjectRemark',      // 选填
  'subjectNationalRank',
  'totalScore',
  'totalPassLine',
  'qualificationStatus',
  'qualificationNote'
].join(',') + '\n'

const downloadTemplate = () => {
  // 必须先有数据，否则下载下来的模板没有 ID，还是没法导入
  if (studentList.value.length === 0) {
    ElMessage.warning('请先点击"查询考生"加载名单，才能生成预填模板')
    return
  }

  ElMessage.info('正在生成预填模板...')
  
  // 2. 遍历当前列表，生成 CSV 数据行
  const rows = studentList.value.map(student => {
    return [
      student.registrationInfoId || '', // 填入数据库 ID
      'FINAL',                          // 考试类型
      '2025',                           // 考试年份 (按需调整)
      student.ticketNumber || '',       // 考号
      searchParams.subjectId,           // 科目ID
      currentSubjectName.value,         // 科目名称
      '',                               // 分数留空，让老师填
      '60',                             // 默认及格线
      '',                               // 是否通过 (留空)
      '',                               // 备注 (留空)
      // 其他非必填字段全部留空
      '', '', '', '', ''             
    ].join(',')
  })

  // 3. 拼接
  const csvContent = CSV_HEADER + rows.join('\n')
  
  // 4. 创建 Blob 下载
  // 注意：添加 '\uFEFF' (BOM) 是为了让 Excel 正确识别中文
  const blob = new Blob(['\uFEFF' + csvContent], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  // 文件名带上科目，避免混淆
  a.download = `${currentSubjectName.value}_成绩录入表.csv` 
  a.click()
  URL.revokeObjectURL(url)
}

// ==========================================
// ====== 文件导入处理逻辑 ======
// ==========================================

const { mutate: importMutate } = useMutation(IMPORT_EXAM_RESULTS)
const MAX_FILE_SIZE = 20 * 1024 * 1024 

const fileToBase64 = (file: File): Promise<string> => {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.readAsDataURL(file)
    reader.onload = () => resolve(reader.result as string)
    reader.onerror = (error) => reject(error)
  })
}

const handleFileImport = async (uploadFile: UploadFile) => {
  const rawFile = uploadFile.raw
  if (!rawFile) return

  if (rawFile.size > MAX_FILE_SIZE) {
    ElMessage.error('文件大小不能超过 20MB')
    return
  }

  const isExcel = rawFile.type === 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' || 
                  rawFile.type === 'application/vnd.ms-excel' ||
                  rawFile.name.endsWith('.xlsx') || 
                  rawFile.name.endsWith('.csv')
  
  if (!isExcel) {
    ElMessage.error('请上传 .xlsx 或 .csv 格式的文件')
    return
  }

  try {
    const base64String = await fileToBase64(rawFile)
    const parts = base64String.split(',')
    const pureBase64 = parts.length > 1 ? parts[1] : ''

    if (!pureBase64) throw new Error('Base64 转换结果异常')

    ElMessageBox.confirm(
      `检测到文件 "${rawFile.name}"，确定导入吗？\n请确保使用的是通过"下载模板"生成的预填表格。`, 
      '导入确认', 
      { confirmButtonText: '确定导入', cancelButtonText: '取消', type: 'warning' }
    ).then(async () => {
      importing.value = true
      try {
        const res = await importMutate({
          input: {
            fileName: rawFile.name,
            contentBase64: pureBase64
          }
        })
        
        const job = res?.data?.importExamResults
        if (job) {
          ElMessage.success(`导入任务提交成功，状态: ${job.status}。请稍后刷新查看结果。`)
          // 可以在这里触发一次 refetch() 刷新列表
          fetchStudents()
        } else {
          ElMessage.success('导入请求已发送')
        }
      } catch (e: any) {
        ElMessage.error('导入失败: ' + e.message)
      } finally {
        importing.value = false
      }
    }).catch(() => {
      ElMessage.info('已取消导入')
    })

  } catch (error) {
    console.error(error)
    ElMessage.error('文件读取失败')
  }
}

// ==========================================
// ====== 成绩预告管理逻辑 ======
// ==========================================

const showPreNotificationDialog = ref(false)
const showPreNotificationForm = ref(false)
const editingPreNotificationId = ref<string | null>(null)
const savingPreNotification = ref(false)

const preNotificationForm = reactive({
  examType: 'FINAL',
  examYear: 2025,
  title: '',
  queryTime: '',
  content: ''
})

// 查询预告列表
const { result: preNotificationResult, loading: preNotificationsLoading, refetch: refetchPreNotifications } = useQuery(
  GET_PRE_NOTIFICATIONS,
  null,
  { fetchPolicy: 'network-only', enabled: false }
)

const preNotifications = computed(() => preNotificationResult.value?.resultPreNotifications || [])

// Mutations
const { mutate: createPreNotificationMutate } = useMutation(CREATE_PRE_NOTIFICATION)
const { mutate: updatePreNotificationMutate } = useMutation(UPDATE_PRE_NOTIFICATION)
const { mutate: publishPreNotificationMutate } = useMutation(PUBLISH_PRE_NOTIFICATION)

const openPreNotificationDialog = () => {
  showPreNotificationDialog.value = true
  refetchPreNotifications()
}

const openCreatePreNotification = () => {
  editingPreNotificationId.value = null
  preNotificationForm.examType = 'FINAL'
  preNotificationForm.examYear = 2025
  preNotificationForm.title = ''
  preNotificationForm.queryTime = ''
  preNotificationForm.content = ''
  showPreNotificationForm.value = true
}

const editPreNotification = (row: any) => {
  editingPreNotificationId.value = row.id
  preNotificationForm.examType = row.examType
  preNotificationForm.examYear = row.examYear
  preNotificationForm.title = row.title
  preNotificationForm.queryTime = row.queryTime ? dayjs(Number(row.queryTime) || row.queryTime).format('YYYY-MM-DDTHH:mm:ss') : ''
  preNotificationForm.content = row.content
  showPreNotificationForm.value = true
}

const savePreNotification = async () => {
  if (!preNotificationForm.title || !preNotificationForm.queryTime || !preNotificationForm.content) {
    return ElMessage.warning('请填写完整信息')
  }

  savingPreNotification.value = true
  try {
    const input = {
      examType: preNotificationForm.examType,
      examYear: preNotificationForm.examYear,
      title: preNotificationForm.title,
      queryTime: preNotificationForm.queryTime,
      content: preNotificationForm.content
    }

    if (editingPreNotificationId.value) {
      await updatePreNotificationMutate({ id: editingPreNotificationId.value, input })
      ElMessage.success('预告更新成功')
    } else {
      await createPreNotificationMutate({ input })
      ElMessage.success('预告创建成功')
    }

    showPreNotificationForm.value = false
    refetchPreNotifications()
  } catch (e: any) {
    ElMessage.error('操作失败: ' + e.message)
  } finally {
    savingPreNotification.value = false
  }
}

const publishPreNotification = async (row: any) => {
  try {
    await ElMessageBox.confirm(`确定发布预告「${row.title}」吗？发布后将通知所有考生。`, '发布确认', {
      confirmButtonText: '确定发布',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await publishPreNotificationMutate({ id: row.id })
    ElMessage.success('预告发布成功，已通知考生')
    refetchPreNotifications()
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error('发布失败: ' + e.message)
    }
  }
}

const formatDateTime = (ts: string) => {
  if (!ts) return '-'
  const num = Number(ts)
  const d = !isNaN(num) && num > 0 ? dayjs(num) : dayjs(ts)
  return d.isValid() ? d.format('YYYY-MM-DD HH:mm') : '-'
}
</script>

<style scoped>
.grade-container { padding: 20px; }
.toolbar { display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: 20px; }
.filters h3 { margin: 0 0 15px 0; color: #303133; }
.filters { display: flex; align-items: center; }
.actions { display: flex; gap: 10px; }
.footer-actions { 
  margin-top: 20px; 
  padding: 15px; 
  background: #fdf6ec; 
  border: 1px solid #faecd8; 
  display: flex; 
  justify-content: space-between; 
  align-items: center;
  border-radius: 4px;
}
.info { color: #e6a23c; font-weight: bold; }
.tips { font-size: 12px; color: #909399; margin-top: 5px; }
.pre-notification-header { display: flex; gap: 10px; }
</style>