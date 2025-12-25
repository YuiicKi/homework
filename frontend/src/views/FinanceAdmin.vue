<template>
  <div class="finance-container">
    <h2>财务缴费管理</h2>
    
    <div class="filter-bar">
      <el-input v-model="searchQuery" placeholder="搜索考生姓名/订单号" style="width: 240px; margin-right: 15px;" />
      <el-select v-model="statusFilter" placeholder="支付状态" style="width: 140px; margin-right: 15px;">
        <el-option label="全部" value="ALL" />
        <el-option label="已支付" value="PAID" />
        <el-option label="未支付" value="UNPAID" />
      </el-select>
      <el-button type="primary" @click="handleSearch">查询</el-button>
      <el-button type="success" plain @click="exportData">导出报表</el-button>
    </div>

    <el-row :gutter="20" style="margin-bottom: 20px;">
      <el-col :span="6">
        <el-statistic title="今日收款总额" :value="stats.todayAmount" precision="2" prefix="¥" />
      </el-col>
      <el-col :span="6">
        <el-statistic title="待缴费人数" :value="stats.pendingCount" />
      </el-col>
    </el-row>

    <el-table :data="tableData" border style="width: 100%" v-loading="loading">
      <el-table-column prop="studentName" label="考生姓名" width="120" />
      <el-table-column prop="examName" label="报考科目" width="180" />
      <el-table-column prop="amount" label="金额" width="120" sortable>
        <template #default="{ row }">¥{{ row.amount }}</template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="180">
        <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 'PAID' ? 'success' : 'danger'">
            {{ row.status === 'PAID' ? '已支付' : '待缴费' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="payMethod" label="支付方式" width="120">
         <template #default="{ row }">{{ row.payMethod || '-' }}</template>
      </el-table-column>
      <el-table-column prop="transactionId" label="流水号" show-overflow-tooltip />
      
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-popconfirm 
            v-if="row.status === 'UNPAID'"
            title="确认该学生已通过线下或其他方式缴费？"
            @confirm="handleManualConfirm(row)"
          >
            <template #reference>
              <el-button link type="primary" size="small">人工确认</el-button>
            </template>
          </el-popconfirm>
          <el-button link type="primary" size="small" @click="viewDetails(row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'

const searchQuery = ref('')
const statusFilter = ref('ALL')
const loading = ref(false)
const tableData = ref<any[]>([])
const stats = reactive({ todayAmount: 1250.00, pendingCount: 45 })

const formatDate = (ts: number) => dayjs(ts).format('YYYY-MM-DD HH:mm')

// Mock Data Generation
const generateMockData = () => {
  loading.value = true
  setTimeout(() => {
    tableData.value = [
      { id: 1, studentName: '张三', examName: '英语四级', amount: 30, createTime: Date.now(), status: 'PAID', payMethod: 'WeChat', transactionId: 'TXN123456789' },
      { id: 2, studentName: '李四', examName: '计算机二级', amount: 80, createTime: Date.now() - 86400000, status: 'UNPAID', payMethod: '', transactionId: '' },
      { id: 3, studentName: '王五', examName: '英语六级', amount: 30, createTime: Date.now() - 100000, status: 'PAID', payMethod: 'Alipay', transactionId: 'TXN987654321' },
      // ... 更多数据
    ]
    loading.value = false
  }, 600)
}

const handleSearch = () => {
  ElMessage.info('查询功能正在对接后端API...')
  generateMockData()
}

const exportData = () => {
  ElMessage.success('报表导出请求已发送，请留意下载任务')
}

const handleManualConfirm = (row: any) => {
  // 模拟 API: mutation ManualConfirmPayment
  row.status = 'PAID'
  row.payMethod = 'Offline/Manual'
  row.transactionId = 'MANUAL_' + Date.now()
  ElMessage.success(`${row.studentName} 的缴费已人工确认`)
  // 实际场景应刷新列表
}

const viewDetails = (row: any) => {
  ElMessage.info(`查看订单 ${row.id} 详情`)
}

onMounted(() => {
  generateMockData()
})
</script>

<style scoped>
.finance-container { padding: 20px; background: #fff; min-height: 100%; }
.filter-bar { margin: 20px 0; display: flex; align-items: center; }
</style>