<template>
  <div class="admit-card-container" id="printable-area">
    <div class="header">
      <div class="logo" v-if="templateData.logoUrl">
        <img :src="templateData.logoUrl" alt="Logo" crossorigin="anonymous" />
      </div>
      <div class="title">
        <h2>{{ examName || '2024-2025学年第一学期期末考试' }}</h2>
        <h3>准 考 证</h3>
      </div>
    </div>

    <div class="student-info">
      <div class="info-row">
        <div class="info-item"><span>姓名：</span>{{ student.name }}</div>
        <div class="info-item"><span>学号：</span>{{ student.studentId }}</div>
        <div class="info-item"><span>院系：</span>{{ student.department }}</div>
      </div>
      <div class="photo-area">
        <img :src="student.avatar || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'" />
      </div>
    </div>

    <div class="exam-schedule">
      <table class="schedule-table">
        <thead>
          <tr>
            <th>科目</th>
            <th>时间</th>
            <th>考场</th>
            <th>座位号</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(exam, index) in exams" :key="index">
            <td>{{ exam.subjectName }}</td>
            <td>{{ formatTime(exam.startTime) }} - {{ formatTime(exam.endTime, 'HH:mm') }}</td>
            <td>{{ exam.roomName }}</td>
            <td class="seat-cell">{{ exam.seatNumber }}</td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="instructions">
      <h4>考生须知：</h4>
      <pre>{{ templateData.examNotice }}</pre>
    </div>

    <div class="footer">
      <p>请考生携带身份证和准考证提前15分钟进入考场</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import dayjs from 'dayjs'

interface ExamItem {
  subjectName: string
  startTime: string
  endTime: string
  roomName: string
  seatNumber: string
}

interface StudentInfo {
  name: string
  studentId: string
  department: string
  avatar?: string
}

interface TemplateInfo {
  logoUrl?: string
  examNotice?: string
}

// 修改处：删除了 "const props ="，直接调用宏函数
// 这样 props 依然会在模板中可用，但不会因为 script 中没用它而报错
withDefaults(defineProps<{
  templateData?: TemplateInfo
  student?: StudentInfo
  exams?: ExamItem[]
  examName?: string
}>(), {
  templateData: () => ({ logoUrl: '', examNotice: '' }),
  student: () => ({ name: '张三', studentId: '2021001', department: '计算机学院' }),
  exams: () => []
})

// 找到这个函数
const formatTime = (time: string, fmt = 'YYYY-MM-DD HH:mm') => {
  if (!time) return '--'
  
  // 1. 尝试转为数字（处理时间戳字符串，如 "1735689..."）
  const timestamp = Number(time)
  
  let dateObj
  if (!isNaN(timestamp) && timestamp > 0) {
    // 如果成功转为数字，且不是0，说明是时间戳
    dateObj = dayjs(timestamp)
  } else {
    // 否则直接解析字符串（处理 ISO 格式，如 "2025-01-01T..."）
    dateObj = dayjs(time)
  }

  // 2. 校验并格式化
  return dateObj.isValid() ? dateObj.format(fmt) : '--'
}
</script>

<style scoped>
/* 前面的样式保持不变... */
.admit-card-container {
  width: 100%;
  max-width: 800px;
  margin: 0 auto;
  padding: 40px;
  background: #fff;
  border: 1px solid #ddd;
  box-sizing: border-box;
  font-family: "SimSun", "Songti SC", serif;
  color: #000;
}

.header {
  display: flex;
  align-items: center;
  border-bottom: 2px solid #000;
  padding-bottom: 20px;
  margin-bottom: 20px;
}

.logo img {
  height: 60px;
  margin-right: 20px;
}

.title {
  flex: 1;
  text-align: center;
}

.title h2 { margin: 0; font-size: 24px; font-weight: bold; }
.title h3 { margin: 10px 0 0; font-size: 28px; letter-spacing: 10px; }

.student-info {
  display: flex;
  justify-content: space-between;
  margin-bottom: 20px;
}

.info-row {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 15px;
  font-size: 16px;
}

.info-item span {
  font-weight: bold;
  display: inline-block;
  width: 80px;
}

.photo-area img {
  width: 100px;
  height: 140px;
  border: 1px solid #000;
  object-fit: cover;
}

.schedule-table {
  width: 100%;
  border-collapse: collapse;
  margin-bottom: 25px;
}

.schedule-table th, .schedule-table td {
  border: 1px solid #000;
  padding: 10px;
  text-align: center;
}

.schedule-table th {
  background-color: #f0f0f0;
  font-weight: bold;
}

.seat-cell {
  font-weight: bold;
  font-size: 18px;
}

.instructions {
  border-top: 2px solid #000;
  padding-top: 15px;
  font-size: 14px;
  line-height: 1.6;
}

.instructions pre {
  white-space: pre-wrap;
  font-family: inherit;
  margin: 0;
}

.footer {
  margin-top: 40px;
  text-align: center;
  font-size: 12px;
  border-top: 1px dashed #999;
  padding-top: 10px;
}

/* ============== 打印样式 ============== */
@media print {
  @page {
    size: A4;
    margin: 10mm;
  }
  
  body * {
    visibility: hidden; 
  }

  #printable-area, #printable-area * {
    visibility: visible;
  }

  #printable-area {
    position: absolute;
    left: 0;
    top: 0;
    width: 100%;
    border: none;
    margin: 0;
    padding: 0;
  }
  
  .schedule-table th {
    background-color: #ddd !important;
    /* 修复 CSS 警告：添加标准属性 */
    -webkit-print-color-adjust: exact;
    print-color-adjust: exact; 
  }
}
</style>