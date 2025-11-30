<template>
  <div class="scheduling-container">
    <h1>考试安排与教务管理系统</h1>

    <el-tabs v-model="activeTab" type="card" class="demo-tabs">

      <el-tab-pane label="科目管理" name="subjects">
        <div class="operation-bar">
          <el-button type="primary" @click="openSubjectDialog()">发布新科目</el-button>
          <el-button @click="refetchSubjects">刷新数据</el-button>
        </div>
        <el-table :data="subjects" border v-loading="subjectsLoading" style="width: 100%">
          <el-table-column prop="code" label="代码" width="100" />
          <el-table-column prop="name" label="科目名称" width="150" />
          <el-table-column prop="durationMinutes" label="时长(分)" width="90" align="center" />
          <el-table-column label="报名时间段" width="280">
            <template #default="{ row }">
              <div class="time-cell">起：{{ formatDate(row.registrationStartTime) }}</div>
              <div class="time-cell">止：{{ formatDate(row.registrationEndTime) }}</div>
            </template>
          </el-table-column>
          <el-table-column label="考试时间段" width="280">
            <template #default="{ row }">
              <div class="time-cell">起：{{ formatDate(row.examStartTime) }}</div>
              <div class="time-cell">止：{{ formatDate(row.examEndTime) }}</div>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="isRegistering(row) ? 'success' : 'info'" effect="dark">
                {{ isRegistering(row) ? '报名中' : '非报名期' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150" align="center" fixed="right">
            <template #default="{ row }">
              <el-button size="small" type="primary" @click="handleEditSubject(row)">编辑</el-button>
              <el-button size="small" @click="handleViewSubjectDetail(row)">详情</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="报名审核" name="audits">
        <div class="operation-bar">
          <el-select v-model="auditExamId" placeholder="请选择考试科目" style="width: 250px; margin-right: 15px;" @change="refetchAuditList">
             <el-option v-for="sub in subjects" :key="sub.id" :label="sub.name" :value="sub.id" />
          </el-select>
          <el-radio-group v-model="auditStatusFilter" @change="refetchAuditList">
            <el-radio-button label="PENDING">待审核</el-radio-button>
            <el-radio-button label="ALL">全部</el-radio-button>
          </el-radio-group>
          <el-button style="margin-left: 15px;" @click="refetchAuditList" :disabled="!auditExamId">刷新</el-button>
        </div>

        <el-empty v-if="!auditExamId" description="请先选择科目以开始审核" />
        
        <el-table v-else :data="auditList" border v-loading="auditLoading">
          <el-table-column prop="studentName" label="姓名" width="100" />
          <el-table-column label="照片" width="100" align="center">
            <template #default="{ row }">
              <el-image 
                style="width: 60px; height: 60px; border-radius: 4px;"
                :src="row.photoUrl" 
                :preview-src-list="[row.photoUrl]"
                preview-teleported>
                <template #error><div class="image-slot">无图</div></template>
              </el-image>
            </template>
          </el-table-column>
          <el-table-column prop="idCardNumber" label="身份证号" width="170" />
          <el-table-column prop="remarks" label="备注" show-overflow-tooltip />
          <el-table-column label="提交时间" width="150">
            <template #default="{ row }">{{ formatDate(row.submitTime) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }">
               <el-tag :type="getAuditStatusType(row.status)">{{ getAuditStatusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="160" align="center" fixed="right">
            <template #default="{ row }">
              <div v-if="row.status === 'PENDING'">
                <el-button type="success" size="small" @click="handleAuditPass(row)">通过</el-button>
                <el-button type="danger" size="small" @click="openRejectDialog(row)">驳回</el-button>
              </div>
              <div v-else class="audited-info">
                <span v-if="row.status === 'REJECTED'" style="color: #f56c6c; font-size: 12px;">{{ row.auditReason }}</span>
                <span v-else style="color: #909399; font-size: 12px;">已处理</span>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="考点管理" name="centers">
        <div class="operation-bar">
          <el-button type="primary" @click="showCenterDialog = true">新增考点</el-button>
          <el-button @click="refetchCenters">刷新</el-button>
        </div>
        <el-table :data="examCenters" border v-loading="centersLoading">
          <el-table-column prop="name" label="考点名称" width="200" />
          <el-table-column prop="address" label="地址" />
          <el-table-column prop="contactPhone" label="联系电话" width="150" />
          <el-table-column prop="description" label="描述" />
          <el-table-column label="操作" width="100" align="center">
            <template #default="{ row }">
              <el-button size="small" type="danger" @click="handleDeleteCenter(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="考场资源" name="rooms">
        <div class="operation-bar">
          <el-button type="primary" @click="openRoomDialog">新增考场</el-button>
          <el-button @click="refetchRooms">刷新</el-button>
        </div>
        <el-table :data="examRooms" border v-loading="roomsLoading">
          <el-table-column prop="roomNumber" label="门牌号" width="120" />
          <el-table-column prop="name" label="考场名" width="150" />
          <el-table-column prop="capacity" label="容量" width="100" align="center" />
          <el-table-column label="所属考点">
            <template #default="{ row }">{{ row.center?.name || '-' }}</template>
          </el-table-column>
          <el-table-column prop="location" label="详细位置" />
          <el-table-column label="操作" width="100" align="center">
            <template #default="{ row }">
              <el-button size="small" type="danger" @click="handleDeleteRoom(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="通知公告" name="notifications">
        <div class="operation-bar">
          <el-button type="primary" @click="showNotifDialog = true">发布通知</el-button>
          <el-button @click="refetchNotifications">刷新</el-button>
        </div>
        <el-table :data="notifications" border v-loading="notifLoading">
          <el-table-column prop="title" label="标题" width="250" />
          <el-table-column prop="content" label="内容" show-overflow-tooltip />
          <el-table-column prop="publishTime" label="时间" width="180">
            <template #default="{ row }">{{ formatDate(row.publishTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="100" align="center">
            <template #default="{ row }">
              <el-button size="small" type="danger" @click="handleDeleteNotif(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="准考证/座位" name="tickets">
        <div class="operation-bar">
          <el-select v-model="selectedExamId" placeholder="选择考试科目" style="width: 240px; margin-right: 15px;" @change="fetchTickets">
            <el-option v-for="sub in subjects" :key="sub.id" :label="sub.name" :value="sub.id" />
          </el-select>
          <el-button type="success" :disabled="!selectedExamId" :loading="generatingTickets" @click="handleGenerateTickets">
            <el-icon style="margin-right: 5px"><MagicStick /></el-icon> 自动分配座位
          </el-button>
          <el-button @click="fetchTickets" :disabled="!selectedExamId">刷新列表</el-button>
        </div>
        <el-table :data="ticketList" border v-loading="ticketsLoading">
          <el-table-column prop="ticketNumber" label="准考证号" width="180" font-family="monospace" />
          <el-table-column prop="studentName" label="考生" width="120" />
          <el-table-column prop="examName" label="科目" />
          <el-table-column prop="roomName" label="考场" />
          <el-table-column prop="seatNumber" label="座位" width="100" align="center">
            <template #default="{ row }"><el-tag type="warning">{{ row.seatNumber }}</el-tag></template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

    </el-tabs>

    <el-dialog v-model="showSubjectDialog" :title="isEditSubject ? '编辑科目' : '新建科目'" width="650px">
      <el-form :model="subjectForm" :rules="subjectRules" ref="subjectFormRef" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
             <el-form-item label="科目代码" prop="code"><el-input v-model="subjectForm.code" placeholder="如: CS101" /></el-form-item>
          </el-col>
          <el-col :span="12">
             <el-form-item label="科目名称" prop="name"><el-input v-model="subjectForm.name" /></el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="考试时长" prop="durationMinutes">
               <el-input-number v-model="subjectForm.durationMinutes" :min="30" label="分钟" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
             <el-form-item label="题目数量" prop="questionCount">
               <el-input-number v-model="subjectForm.questionCount" :min="1" style="width:100%" />
             </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="描述" prop="description">
          <el-input v-model="subjectForm.description" type="textarea" />
        </el-form-item>
        <el-divider content-position="left">排期设置</el-divider>
        <el-form-item label="报名时间" prop="registrationRange">
          <el-date-picker v-model="subjectForm.registrationRange" type="datetimerange" value-format="x" range-separator="至" start-placeholder="报名开始" end-placeholder="报名截止" />
        </el-form-item>
        <el-form-item label="考试时间" prop="examRange">
          <el-date-picker v-model="subjectForm.examRange" type="datetimerange" value-format="x" range-separator="至" start-placeholder="考试开始" end-placeholder="考试截止" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showSubjectDialog=false">取消</el-button>
        <el-button type="primary" @click="handleSubmitSubject" :loading="subjectSubmitting">提交</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showCenterDialog" title="新增考点" width="500px">
      <el-form :model="centerForm" :rules="centerRules" ref="centerFormRef" label-width="90px">
        <el-form-item label="考点名称" prop="name"><el-input v-model="centerForm.name" /></el-form-item>
        <el-form-item label="地址" prop="address"><el-input v-model="centerForm.address" /></el-form-item>
        <el-form-item label="联系电话" prop="contactPhone"><el-input v-model="centerForm.contactPhone" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="centerForm.description" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCenterDialog=false">取消</el-button>
        <el-button type="primary" @click="handleCreateCenter">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showRoomDialog" title="新增考场" width="500px">
      <el-form :model="roomForm" :rules="roomRules" ref="roomFormRef" label-width="100px">
        <el-form-item label="所属考点" prop="centerId">
          <el-select v-model="roomForm.centerId" placeholder="选择考点" style="width:100%">
            <el-option v-for="c in examCenters" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="门牌号" prop="roomNumber"><el-input v-model="roomForm.roomNumber" placeholder="例如: 302" /></el-form-item>
        <el-form-item label="考场名" prop="name"><el-input v-model="roomForm.name" placeholder="例如: 第三多媒体教室" /></el-form-item>
        <el-form-item label="详细位置" prop="location"><el-input v-model="roomForm.location" placeholder="例如: 教学楼A栋3层" /></el-form-item>
        <el-form-item label="容量" prop="capacity"><el-input-number v-model="roomForm.capacity" :min="10" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showRoomDialog=false">取消</el-button>
        <el-button type="primary" @click="handleCreateRoom">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showRejectDialog" title="驳回申请" width="400px">
      <el-input v-model="rejectReason" type="textarea" :rows="3" placeholder="请输入驳回原因" />
      <template #footer>
        <el-button @click="showRejectDialog=false">取消</el-button>
        <el-button type="danger" @click="confirmReject" :loading="auditProcessing">确认驳回</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showNotifDialog" title="发布通知" width="500px">
       <el-form :model="notifForm" :rules="notifRules" ref="notifFormRef" label-width="80px">
        <el-form-item label="标题" prop="title"><el-input v-model="notifForm.title" /></el-form-item>
        <el-form-item label="内容" prop="content"><el-input v-model="notifForm.content" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showNotifDialog=false">取消</el-button>
        <el-button type="primary" @click="handlePublishNotif" :loading="notifPublishing">发布</el-button>
      </template>
    </el-dialog>

     <el-dialog v-model="showSubjectDetailDialog" title="科目详情" width="500px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="科目名称">{{ currentSubjectDetail.name }} ({{ currentSubjectDetail.code }})</el-descriptions-item>
        <el-descriptions-item label="时长/题量">{{ currentSubjectDetail.durationMinutes }}分钟 / {{ currentSubjectDetail.questionCount }}题</el-descriptions-item>
        <el-descriptions-item label="报名时间">{{ formatDate(currentSubjectDetail.registrationStartTime) }} 至 {{ formatDate(currentSubjectDetail.registrationEndTime) }}</el-descriptions-item>
        <el-descriptions-item label="考试时间">{{ formatDate(currentSubjectDetail.examStartTime) }} 至 {{ formatDate(currentSubjectDetail.examEndTime) }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, nextTick } from 'vue'
import { gql } from '@apollo/client/core'
import { useQuery, useMutation } from '@vue/apollo-composable'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { MagicStick } from '@element-plus/icons-vue'
import dayjs from 'dayjs'

const activeTab = ref('subjects')
const formatDate = (ts: string) => ts ? dayjs(Number(ts)).format('YYYY-MM-DD HH:mm') : '-'

// --- 1. 科目管理 (ExamSubject - Merged) ---
// 包含了 Code, Duration, QuestionCount (Code 1) 和 Time Ranges (Code 2)
const GET_SUBJECTS = gql`
  query GetExamSubjects { 
    examSubjects { 
      id, code, name, description, durationMinutes, questionCount
      registrationStartTime, registrationEndTime, examStartTime, examEndTime 
    } 
  }
`
const CREATE_SUBJECT = gql`mutation CreateSubject($input: ExamSubjectInput!) { createExamSubject(input: $input) { id } }`
const UPDATE_SUBJECT = gql`mutation UpdateSubject($id: ID!, $input: ExamSubjectInput!) { updateExamSubject(id: $id, input: $input) { id } }`

const { result: subRes, loading: subjectsLoading, refetch: refetchSubjects } = useQuery(GET_SUBJECTS)
const subjects = computed(() => subRes.value?.examSubjects || [])
const { mutate: createSub } = useMutation(CREATE_SUBJECT)
const { mutate: updateSub } = useMutation(UPDATE_SUBJECT)

const showSubjectDialog = ref(false)
const showSubjectDetailDialog = ref(false)
const subjectSubmitting = ref(false)
const isEditSubject = ref(false)
const subjectFormRef = ref<FormInstance>()
const currentSubjectDetail = ref<any>({})
const subjectForm = reactive({ 
  id: '', code: '', name: '', description: '', durationMinutes: 90, questionCount: 100,
  registrationRange: [] as string[], examRange: [] as string[] 
})
const subjectRules: FormRules = { 
  code: [{ required: true, message: '必填', trigger: 'blur' }],
  name: [{ required: true, message: '必填', trigger: 'blur' }],
  registrationRange: [{ required: true, message: '必填', trigger: 'change' }],
  examRange: [{ required: true, message: '必填', trigger: 'change' }] 
}

const isRegistering = (row: any) => { const now = dayjs().valueOf(); return now >= Number(row.registrationStartTime) && now <= Number(row.registrationEndTime) }

const openSubjectDialog = () => { 
  isEditSubject.value = false; 
  showSubjectDialog.value = true;
  nextTick(() => {
    subjectFormRef.value?.resetFields()
    Object.assign(subjectForm, { id: '', code: '', name: '', description: '', durationMinutes: 90, questionCount: 100, registrationRange: [], examRange: [] })
  })
}

const handleEditSubject = (row: any) => { 
  isEditSubject.value = true; 
  Object.assign(subjectForm, { 
    ...row, 
    registrationRange: [row.registrationStartTime, row.registrationEndTime], 
    examRange: [row.examStartTime, row.examEndTime] 
  }); 
  showSubjectDialog.value = true 
}

const handleViewSubjectDetail = (row: any) => { currentSubjectDetail.value = row; showSubjectDetailDialog.value = true }

const handleSubmitSubject = async () => {
  if (!subjectFormRef.value) return
  await subjectFormRef.value.validate(async (valid) => {
    if (valid) {
      subjectSubmitting.value = true
      try {
        const input = { 
          code: subjectForm.code, name: subjectForm.name, description: subjectForm.description,
          durationMinutes: Number(subjectForm.durationMinutes), questionCount: Number(subjectForm.questionCount),
          registrationStartTime: String(subjectForm.registrationRange[0]), registrationEndTime: String(subjectForm.registrationRange[1]), 
          examStartTime: String(subjectForm.examRange[0]), examEndTime: String(subjectForm.examRange[1]) 
        }
        isEditSubject.value ? await updateSub({ id: subjectForm.id, input }) : await createSub({ input })
        ElMessage.success('保存成功'); showSubjectDialog.value = false; refetchSubjects()
      } catch (e: any) { ElMessage.error(e.message) } finally { subjectSubmitting.value = false }
    }
  })
}

// --- 2. 考点管理 (ExamCenter - Renamed from TestSite) ---
// 统一使用 ExamCenter 命名，但保留了 ContactPhone 字段
const GET_CENTERS = gql`query { examCenters { id, name, address, contactPhone, description } }`
const CREATE_CENTER = gql`mutation($input: ExamCenterInput!) { createExamCenter(input: $input) { id } }`
const DELETE_CENTER = gql`mutation($id: ID!) { deleteExamCenter(id: $id) }`

const { result: centerRes, loading: centersLoading, refetch: refetchCenters } = useQuery(GET_CENTERS)
const examCenters = computed(() => centerRes.value?.examCenters || [])
const { mutate: createCenterMutate } = useMutation(CREATE_CENTER)
const { mutate: deleteCenterMutate } = useMutation(DELETE_CENTER)

const showCenterDialog = ref(false)
const centerFormRef = ref<FormInstance>()
const centerForm = reactive({ name: '', address: '', contactPhone: '', description: '' })
const centerRules = { name: [{ required: true, message: '必填' }] }

const handleCreateCenter = async () => {
  if (!centerFormRef.value) return
  await centerFormRef.value.validate(async (valid) => {
    if(valid) {
      try { await createCenterMutate({ input: { ...centerForm } }); ElMessage.success('考点创建成功'); showCenterDialog.value = false; refetchCenters(); centerFormRef.value?.resetFields() } catch(e:any){ ElMessage.error(e.message) }
    }
  })
}
const handleDeleteCenter = async (row: any) => {
  try { await ElMessageBox.confirm('确认删除?'); await deleteCenterMutate({ id: row.id }); refetchCenters() } catch {}
}

// --- 3. 考场管理 (ExamRoom - Renamed from TestVenue) ---
// 统一使用 ExamRoom，关联 centerId，增加 roomNumber 字段
const GET_ROOMS = gql`query { examRooms { id, roomNumber, name, capacity, location, center { id, name } } }`
const CREATE_ROOM = gql`mutation($input: ExamRoomInput!) { createExamRoom(input: $input) { id } }`
const DELETE_ROOM = gql`mutation($id: ID!) { deleteExamRoom(id: $id) }`

const { result: roomRes, loading: roomsLoading, refetch: refetchRooms } = useQuery(GET_ROOMS)
const examRooms = computed(() => roomRes.value?.examRooms || [])
const { mutate: createRoomMutate } = useMutation(CREATE_ROOM)
const { mutate: deleteRoomMutate } = useMutation(DELETE_ROOM)

const showRoomDialog = ref(false)
const roomFormRef = ref<FormInstance>()
const roomForm = reactive({ centerId: '', roomNumber: '', name: '', location: '', capacity: 30 })
const roomRules = { centerId: [{ required: true, message: '必填' }], roomNumber: [{ required: true, message: '必填' }], name: [{ required: true, message: '必填' }] }

const openRoomDialog = () => { refetchCenters(); showRoomDialog.value = true; nextTick(() => roomFormRef.value?.resetFields()) }
const handleCreateRoom = async () => {
  if (!roomFormRef.value) return
  await roomFormRef.value.validate(async (valid) => {
    if(valid) {
      try { await createRoomMutate({ input: { ...roomForm } }); ElMessage.success('考场创建成功'); showRoomDialog.value = false; refetchRooms() } catch(e:any){ ElMessage.error(e.message) }
    }
  })
}
const handleDeleteRoom = async (row: any) => { try { await ElMessageBox.confirm('确认删除?'); await deleteRoomMutate({ id: row.id }); refetchRooms() } catch {} }

// --- 4. 审核与通知 (保持原逻辑，变量名微调) ---
const auditExamId = ref(''); const auditStatusFilter = ref('PENDING'); const auditLoading = ref(false); const auditList = ref<any[]>([])
const showRejectDialog = ref(false); const rejectReason = ref(''); const currentAuditRow = ref<any>(null); const auditProcessing = ref(false)

// GraphQL Definitions
const GET_REGISTRATIONS = gql`query($examId: ID!, $status: String) { registrations(examId: $examId, status: $status) { id, studentName, idCardNumber, photoUrl, remarks, submitTime, status, auditReason } }`
const AUDIT_MUTATION = gql`mutation($id: ID!, $status: String!, $reason: String) { auditRegistration(id: $id, status: $status, reason: $reason) }`
const { refetch: fetchRegs } = useQuery(GET_REGISTRATIONS, {}, { enabled: false })
const { mutate: doAudit } = useMutation(AUDIT_MUTATION)

// Audit Logic
const getAuditStatusText = (s:string) => ({'PENDING':'待审核','APPROVED':'已通过','REJECTED':'已驳回'}[s] || s)
const getAuditStatusType = (s:string) => ({'PENDING':'warning','APPROVED':'success','REJECTED':'danger'}[s] || 'info')

const refetchAuditList = async () => {
  if (!auditExamId.value) return
  auditLoading.value = true
  try {
    const res = await fetchRegs({ examId: auditExamId.value, status: auditStatusFilter.value === 'ALL' ? undefined : auditStatusFilter.value })
    if (res?.data?.registrations) auditList.value = res.data.registrations
    else {
      // Mock Data 保持不变
      const mock = [
        { id: '1', studentName: '张三', idCardNumber: '110101200001011234', photoUrl: '', remarks: '无', submitTime: String(dayjs().valueOf()), status: 'PENDING' },
        { id: '2', studentName: '李四', idCardNumber: '110101200001015678', photoUrl: '', remarks: '', submitTime: String(dayjs().subtract(1,'day').valueOf()), status: 'REJECTED', auditReason: '照片模糊' }
      ]
      auditList.value = auditStatusFilter.value === 'ALL' ? mock : mock.filter(i => i.status === 'PENDING')
    }
  } catch(e) { ElMessage.error('加载失败') } finally { auditLoading.value = false }
}
const handleAuditPass = async (row:any) => { try { await ElMessageBox.confirm('确认通过?'); await doAudit({ id: row.id, status: 'APPROVED' }); ElMessage.success('已通过'); refetchAuditList() } catch{} }
const openRejectDialog = (row:any) => { currentAuditRow.value = row; rejectReason.value = ''; showRejectDialog.value = true }
const confirmReject = async () => { try { auditProcessing.value = true; await doAudit({ id: currentAuditRow.value.id, status: 'REJECTED', reason: rejectReason.value }); ElMessage.success('已驳回'); showRejectDialog.value=false; refetchAuditList() } catch(e:any){ElMessage.error(e.message)} finally{auditProcessing.value=false} }

// Notifications
const GET_NOTIF = gql`query { notifications { id, title, content, publishTime } }`
const PUB_NOTIF = gql`mutation($input: NotificationInput!) { publishNotification(input: $input) { id } }`
const DEL_NOTIF = gql`mutation($id: ID!) { deleteNotification(id: $id) }`
const { result: notifRes, loading: notifLoading, refetch: refetchNotifications } = useQuery(GET_NOTIF)
const notifications = computed(() => notifRes.value?.notifications || [])
const { mutate: pubNotif } = useMutation(PUB_NOTIF); const { mutate: delNotif } = useMutation(DEL_NOTIF)
const showNotifDialog = ref(false); const notifPublishing = ref(false); const notifFormRef = ref<FormInstance>(); const notifForm = reactive({ title: '', content: '' }); const notifRules = { title: [{required:true}], content: [{required:true}] }
const handlePublishNotif = async () => { if(!notifFormRef.value)return; await notifFormRef.value.validate(async v=>{ if(v){ notifPublishing.value=true; try{ await pubNotif({input:{...notifForm, publishTime:String(dayjs().valueOf())}}); showNotifDialog.value=false; refetchNotifications(); notifFormRef.value?.resetFields()}catch(e:any){ElMessage.error(e.message)}finally{notifPublishing.value=false}}}) }
const handleDeleteNotif = async (row:any) => { try{await ElMessageBox.confirm('删除?'); await delNotif({id:row.id}); refetchNotifications()}catch{} }

// Ticket Generation
const selectedExamId = ref(''); const generatingTickets = ref(false); const ticketsLoading = ref(false); const ticketList = ref<any[]>([])
const GEN_SEATS = gql`mutation($examId: ID!) { generateSeatAssignment(examId: $examId) }`
const GET_TICKETS = gql`query($examId: ID!) { admissionTickets(examId: $examId) { id, ticketNumber, studentName, examName, venueName: roomName, seatNumber } }`
const { mutate: genSeats } = useMutation(GEN_SEATS); const { refetch: fetchTix } = useQuery(GET_TICKETS, {}, { enabled: false })
const handleGenerateTickets = async () => { try{ await ElMessageBox.confirm('确认分配?'); generatingTickets.value=true; await genSeats({examId:selectedExamId.value}); ElMessage.success('分配成功'); fetchTickets() }catch(e:any){if(e!=='cancel')ElMessage.error(e.message)}finally{generatingTickets.value=false} }
const fetchTickets = async () => { if(!selectedExamId.value)return; ticketsLoading.value=true; try{ const res=await fetchTix({examId:selectedExamId.value}); ticketList.value=res?.data?.admissionTickets||[] }catch{ElMessage.error('失败')}finally{ticketsLoading.value=false} }
</script>

<style scoped>
.scheduling-container { padding: 20px; background: #fff; }
.operation-bar { margin-bottom: 15px; display: flex; align-items: center; }
.time-cell { font-size: 12px; color: #666; }
.image-slot { display: flex; justify-content: center; align-items: center; height: 100%; background: #f5f7fa; color: #909399; font-size: 12px; }
.audited-info { display: flex; flex-direction: column; align-items: center; }
</style>