<template>
  <div class="student-payment-container">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <h2>我的缴费中心</h2>
          <el-button @click="refetchData" :icon="Refresh">刷新状态</el-button>
        </div>
      </template>

      <el-tabs v-model="activeTab">
        <el-tab-pane label="待缴费" name="unpaid">
          <el-empty v-if="unpaidList.length === 0" description="暂无待缴费项目" />
          <div v-else class="payment-grid">
            <el-card v-for="item in unpaidList" :key="item.id" class="payment-card" shadow="hover">
              <div class="exam-title">{{ item.examName }}</div>
              <div class="amount">¥ {{ item.amount }}</div>
              <div class="info-row">报名号：{{ item.registrationId }}</div>
              <div class="info-row">截止时间：{{ formatDate(item.deadline) }}</div>
              <el-button type="primary" class="pay-btn" @click="handlePay(item)">
                立即支付
              </el-button>
            </el-card>
          </div>
        </el-tab-pane>

        <el-tab-pane label="缴费记录" name="history">
          <el-table :data="historyList" stripe style="width: 100%">
            <el-table-column prop="orderNo" label="订单号" width="180" />
            <el-table-column prop="examName" label="考试项目" />
            <el-table-column prop="amount" label="金额">
              <template #default="{ row }">¥{{ row.amount }}</template>
            </el-table-column>
            <el-table-column prop="payTime" label="支付时间" width="180">
              <template #default="{ row }">{{ formatDate(row.payTime) }}</template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default>
                <el-tag type="success">已完成</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog v-model="showPayDialog" title="收银台" width="400px" center destroy-on-close>
      <div class="pay-content">
        <p class="pay-title">正在支付：{{ currentOrder?.examName }}</p>
        <p class="pay-amount">¥ {{ currentOrder?.amount }}</p>
        
        <div class="qr-placeholder">
          <el-icon :size="100" color="#409eff"><Picture /></el-icon>
          <p>请使用 微信/支付宝 扫码支付</p>
        </div>
      </div>
      <template #footer>
        <el-button @click="showPayDialog = false">取消支付</el-button>
        <el-button type="success" @click="confirmPay" :loading="paying">模拟支付成功</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
// 修复：修正了导入语法，并使用 Picture 作为二维码占位符
import { Refresh, Picture } from '@element-plus/icons-vue'

const activeTab = ref('unpaid')
const showPayDialog = ref(false)
// 修复：修正了 refPf 为 ref
const paying = ref(false)
const currentOrder = ref<any>(null)

// --- Mock Data ---
const unpaidList = ref<any[]>([])
const historyList = ref<any[]>([])

const refetchData = () => {
  // 模拟 API 请求
  setTimeout(() => {
    if (historyList.value.length === 0) {
        // 初始数据
        unpaidList.value = [
          { id: '101', examName: '英语四级考试', amount: 30.00, registrationId: 'REG2024001', deadline: dayjs().add(3, 'day').valueOf() },
          { id: '102', examName: '计算机二级Python', amount: 80.00, registrationId: 'REG2024005', deadline: dayjs().add(5, 'day').valueOf() }
        ]
        historyList.value = [
           { id: '99', orderNo: 'ORD20231120001', examName: '普通话水平测试', amount: 50.00, payTime: dayjs().subtract(2, 'month').valueOf() }
        ]
    }
  }, 500)
}

const formatDate = (ts: number) => dayjs(ts).format('YYYY-MM-DD HH:mm')

const handlePay = (item: any) => {
  currentOrder.value = item
  showPayDialog.value = true
}

const confirmPay = () => {
  paying.value = true
  // 模拟调用后端 Mutation: payOrder
  setTimeout(() => {
    paying.value = false
    ElMessage.success('支付成功！')
    showPayDialog.value = false
    
    // 移动数据：从待支付 -> 已支付
    const paidItem = {
      ...currentOrder.value,
      orderNo: `ORD${dayjs().format('YYYYMMDDHHmmss')}`,
      payTime: dayjs().valueOf()
    }
    unpaidList.value = unpaidList.value.filter(i => i.id !== currentOrder.value.id)
    historyList.value.unshift(paidItem)
    activeTab.value = 'history'
  }, 1500)
}

onMounted(() => {
  refetchData()
})
</script>

<style scoped>
.student-payment-container { padding: 20px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.payment-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 20px; }
.payment-card { text-align: center; border-radius: 8px; transition: transform 0.2s; }
.payment-card:hover { transform: translateY(-5px); }
.exam-title { font-size: 16px; font-weight: bold; margin-bottom: 10px; color: #303133; }
.amount { font-size: 24px; color: #f56c6c; margin-bottom: 15px; font-weight: bold; }
.info-row { font-size: 13px; color: #909399; margin-bottom: 5px; }
.pay-btn { margin-top: 15px; width: 80%; }
.pay-content { text-align: center; }
.pay-title { font-size: 16px; margin-bottom: 5px; }
.pay-amount { font-size: 30px; color: #f56c6c; font-weight: bold; margin-bottom: 20px; }
.qr-placeholder { background: #f5f7fa; padding: 20px; border-radius: 8px; margin: 0 auto; width: 200px; }
</style>