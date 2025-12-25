<template>
  <div class="registration-container">
    <el-card shadow="never">
      <div class="header">
        <h2>考试报名</h2>
        <p class="subtitle">查看可报名的科目，追踪审核状态，审核通过后请及时缴费</p>
      </div>

      <div class="filter-bar">
        <el-alert 
          title="温馨提示：如状态为“已驳回”，请重新提交报名信息。" 
          type="info" 
          show-icon 
          :closable="false" 
        />
        <div style="flex-grow: 1;"></div>
        <el-button @click="refetchExams" :loading="loading" :icon="Refresh">刷新状态</el-button>
      </div>

      <el-table :data="myExamList" border v-loading="loading" style="width: 100%">
        <el-table-column prop="subjectName" label="考试科目" min-width="180">
           <template #default="{ row }">
              <div class="subject-info">
                <strong>{{ row.subjectName }}</strong>
                <span class="code-tag">{{ row.subjectCode }}</span>
                <span v-if="row.sessionName" class="session-tag">{{ row.sessionName }}</span>
              </div>
           </template>
        </el-table-column>
        
        <el-table-column label="相关时间" width="300">
          <template #default="{ row }">
            <div class="time-cell">
              <span class="label">报名截止:</span> 
              <span>{{ formatDate(row.registrationEndTime) }}</span>
            </div>
            <div class="time-cell">
              <span class="label">考试时间:</span> 
              <span>{{ formatDate(row.examStartTime) }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="状态/说明" align="center" width="160">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" effect="dark" size="default">
              {{ row.actionLabel || getStatusText(row.status) }}
            </el-tag>
            
            <el-tooltip 
              v-if="row.note"
              effect="dark" 
              :content="row.note" 
              placement="top"
            >
              <el-icon class="info-icon"><Warning /></el-icon>
            </el-tooltip>
          </template>
        </el-table-column>

        <el-table-column label="操作" align="center" width="180" fixed="right">
          <template #default="{ row }">
            
            <el-button 
              v-if="row.status === 'OPEN'"
              type="primary" 
              size="small"
              @click="openRegisterDialog(row)"
            >
              立即报名
            </el-button>

            <el-button 
              v-else-if="row.status === 'NOT_STARTED'"
              plain
              size="small"
              disabled
            >
              未开始
            </el-button>

            <el-button 
              v-else
              plain
              size="small"
              disabled
            >
              已结束
            </el-button>

          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog 
      v-model="showDialog" 
      title="填写报名信息" 
      width="550px"
      destroy-on-close
      :close-on-click-modal="false"
    >
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="报考科目">
          <el-input :value="currentExam.subjectName" disabled />
        </el-form-item>

        <el-form-item label="考试场次">
          <el-input :value="currentExam.sessionName" disabled />
        </el-form-item>

        <el-form-item label="考生姓名">
          <el-input :value="userStore.fullName" disabled placeholder="自动获取登录用户姓名" />
        </el-form-item>
        
        <el-form-item label="身份证号" prop="idCardNumber">
          <el-input v-model="form.idCardNumber" placeholder="请输入本人身份证号" />
        </el-form-item>

        <el-form-item label="证件照片" prop="photoUrl">
          <el-upload
            class="avatar-uploader"
            action="#"
            :show-file-list="false"
            :http-request="mockUpload"
            :before-upload="beforeAvatarUpload"
          >
            <img v-if="form.photoUrl" :src="form.photoUrl" class="avatar" />
            <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
          </el-upload>
          <div class="upload-tip">请上传本人近期免冠证件照 (JPG/PNG, &lt;2MB)</div>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">
          确认提交
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, nextTick } from 'vue'
import { Plus, Warning, Refresh } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type FormRules, type UploadProps } from 'element-plus'
import { gql } from '@apollo/client/core'
import dayjs from 'dayjs'
import { useUserStore } from '../stores/user' 
import { useQuery, useMutation, useApolloClient } from '@vue/apollo-composable' // 新增 useApolloClient
// --- GraphQL Definitions ---

// 1. 查询可报名的考试 (Schema: availableExams)
// 注意：Schema 中的 ExamRegistrationView 目前不直接返回 "用户的审核状态" (PENDING/REJECTED)
// 这是一个 Schema 设计限制，我们暂时依赖 availableExams 列表
const GET_AVAILABLE_EXAMS = gql`
  query GetAvailableExams {
    availableExams {
      registrationId # 报名窗口ID
      subjectCode
      subjectName
      sessionId      # 场次ID
      sessionName    # 场次名称
      examStartTime
      registrationEndTime
      status       # ExamRegistrationViewStatus: NOT_STARTED, OPEN, CLOSED
      actionLabel  # 后端返回的操作文本，如 "去报名"
      note
    }
  }
`

// 2. 提交基本信息 (Schema: upsertRegistrationInfo)
const UPSERT_REGISTRATION_INFO = gql`
  mutation UpsertInfo($input: RegistrationInfoInput!) {
    upsertRegistrationInfo(input: $input) {
      id
      status
      subjectId
    }
  }
`

// 3. 上传材料 (Schema: uploadRegistrationMaterial)
// 照片在 Schema 中属于材料，需要单独上传
const UPLOAD_MATERIAL = gql`
  mutation UploadMaterial($input: RegistrationMaterialInput!) {
    uploadRegistrationMaterial(input: $input) {
      id
      fileUrl
    }
  }
`// 约第 142 行（在 UPLOAD_MATERIAL 下方添加）
// 4. (方案B新增) 根据场次ID查询科目ID
const GET_WINDOW_DETAIL = gql`
  query GetWindowDetail($id: ID!) {
    examRegistrationWindow(id: $id) {
      id
      subject {
        id
        name
      }
    }
  }
`

// --- Init & State ---
const userStore = useUserStore()
const { client } = useApolloClient()
const { result, loading, refetch: refetchExams } = useQuery(GET_AVAILABLE_EXAMS)
const { mutate: upsertInfo } = useMutation(UPSERT_REGISTRATION_INFO)
const { mutate: uploadMaterial } = useMutation(UPLOAD_MATERIAL)

const showDialog = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()
const currentExam = ref<any>({}) 

// 表单模型
const form = reactive({
  idCardNumber: '',
  photoUrl: ''
})

// 表单校验规则
const rules: FormRules = {
  idCardNumber: [
    { required: true, message: '必填项', trigger: 'blur' },
    { pattern: /(^\d{15}$)|(^\d{18}$)|(^\d{17}(\d|X|x)$)/, message: '身份证格式错误', trigger: 'blur' }
  ],
  photoUrl: [{ required: true, message: '请上传照片', trigger: 'change' }]
}

// --- Data Logic ---
// 安全获取列表，移除 Mock 数据
const myExamList = computed(() => result.value?.availableExams || [])


// --- Helper Functions ---
// 增强版日期格式化：兼容 ISO 字符串和时间戳
const formatDate = (ts: string | number) => {
  if (!ts) return '-'
  
  // 1. 尝试作为数字解析 (处理时间戳字符串 "1679...")
  const numDate = Number(ts)
  if (!isNaN(numDate)) {
    return dayjs(numDate).format('YYYY-MM-DD HH:mm')
  }

  // 2. 尝试直接解析 (处理 ISO 字符串 "2025-01-01T...")
  const directDate = dayjs(ts)
  if (directDate.isValid()) {
    return directDate.format('YYYY-MM-DD HH:mm')
  }

  return '-'
}

// 同时也检查一下 isRegisteringTime 函数，确保比较逻辑正确


const getStatusText = (s: string) => ({ 'NOT_STARTED':'未开始', 'OPEN':'报名中', 'CLOSED':'已截止' }[s] || s)
const getStatusType = (s: string) => ({ 'NOT_STARTED':'info', 'OPEN':'success', 'CLOSED':'info' }[s] || 'info')

// --- Action Handlers ---

const openRegisterDialog = (row: any) => {
  currentExam.value = row
  // 重置表单
  form.idCardNumber = ''
  form.photoUrl = ''
  showDialog.value = true
  nextTick(() => formRef.value?.clearValidate())
}

// 约第 215 行左右，替换整个 handleSubmit 函数（或者修改其中的 try 块）
const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitting.value = true
      try {
        // --- 方案B 修改开始 ---
        
        // 1. 先用 registrationId (场次ID, 如8) 去查询详情，获取真正的 subjectId (科目ID, 如2)
        const windowRes = await client.query({
          query: GET_WINDOW_DETAIL,
          variables: { id: currentExam.value.registrationId },
          fetchPolicy: 'network-only' // 确保不走缓存
        })

        const realSubjectId = windowRes.data?.examRegistrationWindow?.subject?.id
        
        if (!realSubjectId) {
          throw new Error('无法获取关联的科目信息，请联系管理员')
        }

        // 2. 使用查到的 realSubjectId 和当前行的 sessionId 提交报名
        const infoRes = await upsertInfo({
          input: {
            subjectId: realSubjectId, // 这里填入刚才查到的正确ID
            sessionId: currentExam.value.sessionId, // 当前行对应的场次
            fullName: userStore.fullName,
            idCardNumber: form.idCardNumber,
          }
        })
        // --- 方案B 修改结束 ---

        const registrationInfoId = infoRes?.data?.upsertRegistrationInfo?.id

        if (!registrationInfoId) throw new Error('报名信息提交失败')

        // Step 3: 上传照片 (保持原逻辑不变)
        if (form.photoUrl) {
          await uploadMaterial({
            input: {
              registrationInfoId: registrationInfoId,
              type: 'PHOTO',
              fileUrl: form.photoUrl,
              fileFormat: 'jpg',
              fileSize: 0 
            }
          })
        }

        ElMessage.success('报名成功')
        showDialog.value = false
        refetchExams()
      } catch (e: any) {
        console.error(e)
        ElMessage.error(e.message || '提交失败')
      } finally {
        submitting.value = false
      }
    }
  })
}

// 模拟图片上传 (实际项目中应替换为真实文件上传接口，获取 HTTP URL)
const mockUpload = (opts: any) => {
  const reader = new FileReader()
  reader.onload = (e) => { 
    form.photoUrl = e.target?.result as string; 
    ElMessage.success('图片已读取 (Mock)') 
    formRef.value?.validateField('photoUrl')
  }
  reader.readAsDataURL(opts.file)
}

const beforeAvatarUpload: UploadProps['beforeUpload'] = (file) => {
  if (file.type !== 'image/jpeg' && file.type !== 'image/png') { ElMessage.error('仅支持JPG/PNG'); return false }
  if (file.size / 1024 / 1024 > 2) { ElMessage.error('图片不能超过2MB'); return false }
  return true
}
</script>

<style scoped>
.registration-container { padding: 20px; max-width: 1200px; margin: 0 auto; }
.header { margin-bottom: 20px; }
.subtitle { color: #909399; font-size: 14px; margin-top: 5px; }
.filter-bar { display: flex; align-items: center; margin-bottom: 20px; gap: 20px; }

.subject-info { display: flex; flex-direction: column; }
.code-tag { font-size: 12px; color: #909399; margin-top: 2px; font-family: monospace; }
.session-tag { font-size: 12px; color: #409eff; margin-top: 2px; font-weight: 500; }

.time-cell { font-size: 13px; line-height: 1.6; color: #606266; display: flex; }
.time-cell .label { color: #909399; width: 65px; text-align: right; margin-right: 8px; flex-shrink: 0; }

.info-icon { color: #e6a23c; margin-left: 5px; cursor: help; vertical-align: middle; }

/* Upload Styles */
.avatar-uploader .el-upload { border: 1px dashed var(--el-border-color); border-radius: 6px; cursor: pointer; position: relative; overflow: hidden; transition: .3s; }
.avatar-uploader .el-upload:hover { border-color: var(--el-color-primary); }
.avatar-uploader-icon { font-size: 28px; color: #8c939d; width: 100px; height: 100px; text-align: center; display: flex; justify-content: center; align-items: center; }
.avatar { width: 100px; height: 100px; display: block; object-fit: cover; }
.upload-tip { font-size: 12px; color: #909399; margin-top: 8px; line-height: 1.4; }
</style>