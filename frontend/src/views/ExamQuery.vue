<template>
  <div class="query-container">
    <el-card shadow="never">
      <div class="search-bar">
        <h3>考试信息综合查询</h3>
        <div class="filters">
          <el-input 
            v-model="keyword" 
            placeholder="输入科目代码或名称" 
            style="width: 240px; margin-right: 15px;" 
            clearable 
            @clear="handleSearch"
            @keyup.enter="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
          
          <el-select 
            v-model="status" 
            placeholder="科目状态" 
            style="width: 140px; margin-right: 15px;" 
            clearable 
            @clear="handleSearch"
          >
            <el-option label="启用" value="ENABLED" />
            <el-option label="禁用" value="DISABLED" />
          </el-select>
          
          <el-button type="primary" @click="handleSearch" :loading="loading">查询</el-button>
        </div>
      </div>

      <el-table :data="tableData" border v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="code" label="科目代码" width="120" sortable />
        <el-table-column prop="name" label="科目名称" width="200" />
        
        <el-table-column label="考试配置" width="200">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ row.durationMinutes }}分钟</el-tag>
            <span style="margin: 0 5px; color: #dcdfe6">|</span>
            <el-tag size="small" type="info">{{ row.questionCount }}道题</el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="description" label="科目描述" show-overflow-tooltip />

        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ENABLED' ? 'success' : 'danger'" effect="plain">
              {{ row.status === 'ENABLED' ? '正常' : '已停用' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="100" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="viewDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 详情弹窗 -->
    <el-dialog v-model="showDetail" title="科目详情" width="500px">
      <el-descriptions :column="1" border v-if="currentRow">
        <el-descriptions-item label="科目ID">{{ currentRow.id }}</el-descriptions-item>
        <el-descriptions-item label="科目代码">{{ currentRow.code }}</el-descriptions-item>
        <el-descriptions-item label="科目名称">{{ currentRow.name }}</el-descriptions-item>
        <el-descriptions-item label="考试时长">{{ currentRow.durationMinutes }} 分钟</el-descriptions-item>
        <el-descriptions-item label="题目数量">{{ currentRow.questionCount }}</el-descriptions-item>
        <el-descriptions-item label="当前状态">
          {{ currentRow.status === 'ENABLED' ? '启用' : '禁用' }}
        </el-descriptions-item>
        <el-descriptions-item label="详细描述">{{ currentRow.description || '无' }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="showDetail = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { gql } from '@apollo/client/core'
import { useQuery } from '@vue/apollo-composable'

// --- GraphQL Query ---
// 对应 Schema: examSubjects(keyword: String, status: ExamSubjectStatus): [ExamSubject!]!
const GET_EXAM_SUBJECTS = gql`
  query QueryExamSubjects($keyword: String, $status: ExamSubjectStatus) {
    examSubjects(keyword: $keyword, status: $status) {
      id
      code
      name
      status
      durationMinutes
      questionCount
      description
    }
  }
`

// --- State ---
const keyword = ref('')
const status = ref('') // ENABLED, DISABLED or ''
const showDetail = ref(false)
const currentRow = ref<any>(null)

// --- Query Hook ---
const { result, loading, refetch } = useQuery(GET_EXAM_SUBJECTS, () => ({
  keyword: keyword.value || undefined,
  // 如果 status 为空字符串，传 undefined 给 GraphQL，避免类型错误
  status: status.value || undefined 
}))

const tableData = computed(() => result.value?.examSubjects || [])

// --- Methods ---
const handleSearch = () => {
  refetch()
}

const viewDetail = (row: any) => {
  currentRow.value = row
  showDetail.value = true
}
</script>

<style scoped>
.query-container { padding: 20px; }
.search-bar { margin-bottom: 20px; }
.search-bar h3 { margin-top: 0; margin-bottom: 15px; color: #303133; }
.filters { display: flex; align-items: center; }
</style>