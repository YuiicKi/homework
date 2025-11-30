<template>
  <div class="registration-container">
    <el-card shadow="never">
      <div class="header">
        <h2>我的考试报名</h2>
        <p class="subtitle">查看可报名的科目，追踪审核状态，审核通过后请及时缴费</p>
      </div>

      <div class="filter-bar">
        <el-alert 
          title="温馨提示：如状态为“已驳回”，请将鼠标悬停在红色图标上查看原因，并点击“修改重交”。" 
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

        <el-table-column label="当前状态" align="center" width="160">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" effect="dark" size="default">
              {{ getStatusText(row.status) }}
            </el-tag>
            
            <el-tooltip 
              v-if="row.status === 'REJECTED' && row.auditReason"
              effect="dark" 
              :content="'驳回原因: ' + row.auditReason" 
              placement="top"
            >
              <el-icon class="reject-icon"><Warning /></el-icon>
            </el-tooltip>
          </template>
        </el-table-column>

        <el-table-column label="操作" align="center" width="180" fixed="right">
          <template #default="{ row }">
            
            <el-button 
              v-if="row.status === 'NOT_REGISTERED'"
              type="primary" 
              size="small"
              :disabled="!isRegisteringTime(row)"
              @click="openRegisterDialog(row)"
            >
              {{ isRegisteringTime(row) ? '立即报名' : '未开放' }}
            </el-button>

            <el-button 
              v-else-if="row.status === 'PENDING'"
              plain
              size="small"
              disabled
            >
              审核中...
            </el-button>

            <el-button 
              v-else-if="row.status === 'REJECTED'"
              type="danger" 
              size="small"
              plain
              @click="openReSubmitDialog(row)"
            >
              修改重交
            </el-button>

            <el-button 
              v-else-if="row.status === 'APPROVED'"
              type="success" 
              size="small"
              @click="handlePay(row)"
            >
              去缴费
            </el-button>

             <div v-else-if="row.status === 'PAID'" style="color: #67c23a; font-size: 12px;">
              <el-icon><CircleCheckFilled /></el-icon> 报名完成
            </div>

          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog 
      v-model="showDialog" 
      :title="isResubmit ? '修改报名信息' : '填写报名信息'" 
      width="550px"
      destroy-on-close
      :close-on-click-modal="false"
    >
      <el-alert 
        v-if="isResubmit && currentExam.auditReason"
        :title="`上次驳回原因：${currentExam.auditReason}`"
        type="error"
        show-icon
        :closable="false"
        style="margin-bottom: 20px;"
      />

      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="报考科目">
          <el-input :value="currentExam.subjectName" disabled />
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

        <el-form-item label="备注说明" prop="remarks">
          <el-input v-model="form.remarks" type="textarea" :rows="2" placeholder="如有特殊情况请在此说明" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">
          {{ isResubmit ? '重新提交' : '确认提交' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, nextTick } from 'vue'
import { Plus, Warning, CircleCheckFilled, Refresh } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type FormRules, type UploadProps } from 'element-plus'
import { gql } from '@apollo/client/core'
import { useQuery, useMutation } from '@vue/apollo-composable'
import dayjs from 'dayjs'
import { useUserStore } from '../stores/user' // 确保路径正确

// --- GraphQL Definitions ---
const GET_STUDENT_EXAMS = gql`
  query GetStudentExams {
    studentExams {
      subjectId
      subjectCode
      subjectName
      registrationStartTime
      registrationEndTime
      examStartTime
      
      # 报名记录信息
      registrationId 
      status          
      auditReason
      idCardNumber
      photoUrl
      remarks
    }
  }
`

const SUBMIT_REGISTRATION = gql`
  mutation SubmitRegistration($input: RegistrationInput!) {
    submitRegistration(input: $input) {
      id
      status
    }
  }
`

// --- Init & State ---
const userStore = useUserStore()
const { result, loading, refetch: refetchExams } = useQuery(GET_STUDENT_EXAMS)
const { mutate: submitReg } = useMutation(SUBMIT_REGISTRATION)

const showDialog = ref(false)
const submitting = ref(false)
const isResubmit = ref(false)
const formRef = ref<FormInstance>()
const currentExam = ref<any>({}) 

// 表单模型
const form = reactive({
  idCardNumber: '',
  photoUrl: '',
  remarks: ''
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
const myExamList = computed(() => {
  const data = result.value?.studentExams
  if (data) return data

  // --- Mock Data ---
  // 修复点 2: 删除了未使用的 'const now = dayjs().valueOf()'
  return [
    {
      subjectId: '101', subjectCode: 'CS101', subjectName: '数据结构与算法', 
      registrationStartTime: String(dayjs().subtract(2, 'day').valueOf()),
      registrationEndTime: String(dayjs().add(5, 'day').valueOf()),
      examStartTime: String(dayjs().add(15, 'day').valueOf()),
      status: 'REJECTED', 
      auditReason: '证件照非本人，请重新上传',
      idCardNumber: '110101200001011234',
      photoUrl: '', 
      registrationId: 'reg_001',
      remarks: ''
    },
    {
      subjectId: '102', subjectCode: 'ENG202', subjectName: '大学英语六级', 
      registrationStartTime: String(dayjs().subtract(1, 'day').valueOf()),
      registrationEndTime: String(dayjs().add(3, 'day').valueOf()),
      examStartTime: String(dayjs().add(20, 'day').valueOf()),
      status: 'APPROVED', 
      registrationId: 'reg_002',
      idCardNumber: '110101200001011234',
      photoUrl: 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png',
      remarks: '无'
    },
    {
      subjectId: '103', subjectCode: 'MATH101', subjectName: '高等数学(下)', 
      registrationStartTime: String(dayjs().subtract(5, 'day').valueOf()),
      registrationEndTime: String(dayjs().add(10, 'day').valueOf()),
      examStartTime: String(dayjs().add(30, 'day').valueOf()),
      status: 'NOT_REGISTERED', 
      registrationId: null,
      idCardNumber: '', photoUrl: '', remarks: ''
    },
    {
      subjectId: '104', subjectCode: 'PHY101', subjectName: '大学物理', 
      registrationStartTime: String(dayjs().subtract(5, 'day').valueOf()),
      registrationEndTime: String(dayjs().add(10, 'day').valueOf()),
      examStartTime: String(dayjs().add(30, 'day').valueOf()),
      status: 'PENDING', 
      registrationId: 'reg_004',
      idCardNumber: '110101...', photoUrl: 'http://...', remarks: ''
    }
  ]
})

// --- Helper Functions ---
const formatDate = (ts: string) => dayjs(Number(ts)).format('YYYY-MM-DD HH:mm')

const isRegisteringTime = (row: any) => {
  const now = dayjs().valueOf()
  return now >= Number(row.registrationStartTime) && now <= Number(row.registrationEndTime)
}

const getStatusText = (s: string) => ({ 'NOT_REGISTERED':'未报名', 'PENDING':'审核中', 'APPROVED':'待缴费', 'REJECTED':'已驳回', 'PAID':'已完成' }[s] || s)
const getStatusType = (s: string) => ({ 'NOT_REGISTERED':'info', 'PENDING':'warning', 'APPROVED':'success', 'REJECTED':'danger', 'PAID':'success' }[s] || 'info')

// --- Action Handlers ---

const openRegisterDialog = (row: any) => {
  isResubmit.value = false
  currentExam.value = row
  form.idCardNumber = '' 
  form.photoUrl = ''
  form.remarks = ''
  showDialog.value = true
  nextTick(() => formRef.value?.clearValidate())
}

const openReSubmitDialog = (row: any) => {
  isResubmit.value = true
  currentExam.value = row
  form.idCardNumber = row.idCardNumber || ''
  form.photoUrl = row.photoUrl || ''
  form.remarks = row.remarks || ''
  showDialog.value = true
  nextTick(() => formRef.value?.clearValidate())
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitting.value = true
      try {
        await submitReg({
          input: {
            examId: currentExam.value.subjectId,
            studentName: userStore.fullName,
            idCardNumber: form.idCardNumber,
            photoUrl: form.photoUrl,
            remarks: form.remarks,
            registrationId: isResubmit.value ? currentExam.value.registrationId : undefined
          }
        })
        ElMessage.success(isResubmit.value ? '已重新提交' : '报名成功，等待审核')
        showDialog.value = false
        refetchExams()
      } catch (e: any) {
        ElMessage.error(e.message || '提交失败')
      } finally {
        submitting.value = false
      }
    }
  })
}

// 修复点 3: 使用了 row 参数
const handlePay = (row: any) => {
  ElMessage.success(`正在跳转 [${row.subjectName}] 的支付页面...`)
  // router.push({ name: 'Payment', params: { id: row.registrationId } })
}

// 模拟图片上传
const mockUpload = (opts: any) => {
  const reader = new FileReader()
  reader.onload = (e) => { 
    form.photoUrl = e.target?.result as string; 
    ElMessage.success('上传成功') 
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

/* Table Content Styles */
.subject-info { display: flex; flex-direction: column; }
.code-tag { font-size: 12px; color: #909399; margin-top: 2px; font-family: monospace; }

.time-cell { font-size: 13px; line-height: 1.6; color: #606266; display: flex; }
.time-cell .label { color: #909399; width: 65px; text-align: right; margin-right: 8px; flex-shrink: 0; }

.reject-icon { color: #f56c6c; margin-left: 8px; cursor: help; vertical-align: middle; font-size: 16px; }

/* Upload Styles */
.avatar-uploader .el-upload { border: 1px dashed var(--el-border-color); border-radius: 6px; cursor: pointer; position: relative; overflow: hidden; transition: .3s; }
.avatar-uploader .el-upload:hover { border-color: var(--el-color-primary); }
.avatar-uploader-icon { font-size: 28px; color: #8c939d; width: 100px; height: 100px; text-align: center; display: flex; justify-content: center; align-items: center; }
.avatar { width: 100px; height: 100px; display: block; object-fit: cover; }
.upload-tip { font-size: 12px; color: #909399; margin-top: 8px; line-height: 1.4; }
</style>