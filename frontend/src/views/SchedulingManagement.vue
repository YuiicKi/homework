<template>
  <div class="scheduling-container">
    <h1>考试安排管理</h1>

    <el-tabs v-model="activeTab" type="card" class="demo-tabs">
      
      <el-tab-pane label="考试科目库" name="subjects">
        <div class="operation-bar">
          <el-button type="primary" @click="openSubjectDialog()">新建科目</el-button>
          <el-button @click="refetchSubjects">刷新数据</el-button>
        </div>
        
        <el-table :data="subjects" border v-loading="subjectsLoading" style="width: 100%">
          <el-table-column prop="code" label="科目代码" width="120" sortable />
          <el-table-column prop="name" label="科目名称" width="180" />
          <el-table-column prop="durationMinutes" label="时长" width="100" align="center">
            <template #default="{ row }">{{ row.durationMinutes }}分钟</template>
          </el-table-column>
          <el-table-column prop="questionCount" label="题量" width="100" align="center" />
          <el-table-column prop="status" label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 'ENABLED' ? 'success' : 'info'">
                {{ row.status === 'ENABLED' ? '启用' : '禁用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="当前报名安排" width="240" align="center">
            <template #default="{ row }">
              <div v-if="windowMap[row.id]" style="font-size: 12px; line-height: 1.5; text-align: left;">
                <el-tag size="small" effect="plain" style="margin-bottom: 4px;">
                  {{ getSessionName(windowMap[row.id].session?.id) || '未知场次' }}
                </el-tag>
                <div style="color: #606266;">起: {{ formatDate(windowMap[row.id].startTime) }}</div>
                <div style="color: #606266;">止: {{ formatDate(windowMap[row.id].endTime) }}</div>
              </div>
              <div v-else style="color: #909399; font-size: 12px;">未配置</div>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="250" align="center" fixed="right">
            <template #default="{ row }">
              <el-button size="small" @click="handleEditSubject(row)">编辑</el-button>
              <el-button size="small" type="warning" @click="openTimeConfig(row)">配置报名</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="考点信息" name="centers">
        <div class="operation-bar">
          <el-button type="primary" @click="showCenterDialog = true">新增考点</el-button>
          <el-button @click="refetchCenters">刷新</el-button>
        </div>
        <el-table :data="examCenters" border v-loading="centersLoading">
          <el-table-column prop="name" label="考点名称" width="200" />
          <el-table-column prop="address" label="地址" />
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
          <el-table-column prop="capacity" label="容量" width="100" />
          <el-table-column label="所属考点">
            <template #default="{ row }">{{ row.center?.name || '-' }}</template>
          </el-table-column>
          <el-table-column prop="managerName" label="管理员" />
          <el-table-column label="状态">
            <template #default="{ row }"><el-tag>{{ row.status }}</el-tag></template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="考试场次" name="sessions">
        <div class="operation-bar">
          <el-button type="primary" @click="openSessionDialog()">新增场次</el-button>
          <el-button @click="refetchSessions">刷新</el-button>
        </div>
        <el-table :data="sessions" border v-loading="sessionsLoading">
          <el-table-column prop="name" label="场次名称" width="200" />
          <el-table-column label="开始时间" width="200">
            <template #default="{ row }">{{ formatDate(row.startTime) }}</template>
          </el-table-column>
          <el-table-column label="结束时间" width="200">
            <template #default="{ row }">{{ formatDate(row.endTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="150" align="center">
            <template #default="{ row }">
              <el-button size="small" @click="openSessionDialog(row)">编辑</el-button>
              <el-button size="small" type="danger" @click="handleDeleteSession(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="排考设置" name="schedules">
        <div class="toolbar">
          <div class="filters">
            <el-select v-model="filterSubject" placeholder="筛选科目" clearable style="width: 150px; margin-right: 10px" @change="refetchSchedules">
              <el-option v-for="s in subjects" :key="s.id" :label="s.name" :value="s.id" />
            </el-select>
            <el-select v-model="filterSession" placeholder="筛选场次" clearable style="width: 150px; margin-right: 10px" @change="refetchSchedules">
              <el-option v-for="s in sessions" :key="s.id" :label="s.name" :value="s.id" />
            </el-select>
            <el-button type="primary" :icon="Search" @click="refetchSchedules">查询</el-button>
          </div>
          <div class="actions">
             <el-button @click="refetchSchedules" style="margin-right: 10px">刷新列表</el-button>
             <el-button type="success" :icon="Plus" @click="handleAddSchedule">新建排考 & 分配监考</el-button>
          </div>
        </div>

        <el-alert title="请在此处将科目、场次与具体教室绑定。排座算法将仅使用已绑定的教室。" type="info" style="margin: 15px 0" :closable="false" />

        <el-table :data="schedules" border v-loading="schedulesLoading" style="width: 100%">
          <el-table-column prop="examSubject.name" label="考试科目" width="180" />
          <el-table-column label="考试场次" width="220">
            <template #default="{ row }">
              <div>{{ row.examSession.name }}</div>
              <div class="sub-text" v-if="row.examSession.startTime">{{ formatDate(row.examSession.startTime) }}</div>
            </template>
          </el-table-column>
          <el-table-column label="考场" width="200">
            <template #default="{ row }">
              <b>{{ row.examRoom.name }}</b> 
              <span style="color: #999; margin-left: 5px">({{ row.examRoom.roomNumber }})</span>
              <div class="sub-text">容量: {{ row.examRoom.capacity }}</div>
            </template>
          </el-table-column>
          
         <el-table-column label="监考信息" min-width="200">
            <template #default="{ row }">
              <div v-if="getInvigilatorNames(row.id).length > 0" style="margin-bottom: 4px;">
                <el-tag
                  v-for="(name, index) in getInvigilatorNames(row.id)"
                  :key="index"
                  size="small"
                  effect="plain"
                  style="margin-right: 4px; margin-bottom: 2px;"
                >
                  {{ name }}
                </el-tag>
              </div>
              <div v-else class="text-gray" style="font-size: 12px; margin-bottom: 4px;">
                <span style="color: #E6A23C" v-if="!row.note">未分配且无备注</span>
                <span style="color: #909399" v-else>未分配老师</span>
              </div>

              <div v-if="row.note" style="font-size: 12px; color: #606266; border-top: 1px dashed #eee; padding-top: 4px; margin-top: 2px;">
                <span style="color: #909399;">备注：</span>{{ row.note }}
              </div>
            </template>
          </el-table-column>

          <el-table-column prop="status" label="状态" width="100" align="center">
            <template #default>
              <el-tag type="success">已排考</el-tag>
            </template>
          </el-table-column>
          
          <el-table-column label="操作" width="180" align="center" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="handleEditSchedule(row)">编辑/分配</el-button>
              <el-popconfirm title="确定删除该安排吗？" @confirm="handleDeleteSchedule(row.id)">
                <template #reference>
                  <el-button link type="danger" size="small">删除</el-button>
                </template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="报名审核" name="audits">
        <div class="operation-bar">
          <el-button @click="refetchAudits">刷新列表</el-button>
          <el-tag type="warning" style="margin-left: 10px">待审核: {{ pendingList.length }} 人</el-tag>
        </div>
        <el-table :data="pendingList" border v-loading="auditLoading">
          <el-table-column prop="fullName" label="考生姓名" width="120" />
          <el-table-column prop="idCardNumber" label="身份证号" width="180" />
          <el-table-column label="报考科目" width="180">
            <template #default="{ row }">{{ getSubjectName(row.subjectId) }}</template>
          </el-table-column>
          <el-table-column label="材料状态" width="120">
              <template #default="{ row }">
                <el-tag v-if="row.materials && row.materials.length > 0" type="success">已上传 {{ row.materials.length }} 份</el-tag>
                <el-tag v-else type="danger">无材料</el-tag>
              </template>
          </el-table-column>
          <el-table-column label="操作" width="150" align="center" fixed="right">
            <template #default="{ row }">
              <el-button size="small" type="primary" @click="openAuditDialog(row)">审核</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="座位分配" name="seats">
        <div class="seat-control-panel">
           <el-form :inline="true">
             <el-form-item label="选择场次">
               <el-select v-model="seatQuery.sessionId" placeholder="选择考试场次" style="width: 180px">
                 <el-option v-for="s in sessions" :key="s.id" :label="s.name" :value="s.id" />
               </el-select>
             </el-form-item>
             <el-form-item label="选择科目">
               <el-select v-model="seatQuery.subjectId" placeholder="选择科目" style="width: 180px" @change="fetchSeatStats">
                 <el-option v-for="s in subjects" :key="s.id" :label="s.name" :value="s.id" />
               </el-select>
             </el-form-item>
             <el-form-item>
               <el-button type="primary" @click="fetchSeatStats" :disabled="!canQuerySeats">查询状态</el-button>
             </el-form-item>
           </el-form>
        </div>

        <div v-if="seatStats" class="seat-stats-card">
           <el-row :gutter="20">
             <el-col :span="6">
               <div class="stat-item">
                 <div class="label">已报名人数</div>
                 <div class="value">{{ seatStats.registrationCount }}</div>
               </div>
             </el-col>
             <el-col :span="6">
               <div class="stat-item">
                 <div class="label">已分配座位</div>
                 <div class="value" :style="{color: isFullyAssigned ? '#67C23A' : '#E6A23C'}">
                    {{ seatStats.assignmentCount }}
                 </div>
               </div>
             </el-col>
             <el-col :span="12" style="display: flex; align-items: center; justify-content: flex-end;">
                <el-button 
                   type="success" 
                   size="large" 
                   :disabled="isFullyAssigned || seatStats.registrationCount === 0"
                   :loading="assigningSeats"
                   @click="handleAssignSeats"
                >
                   <el-icon style="margin-right:5px"><Tools /></el-icon> 执行一键排座
                </el-button>
                <el-button 
                   type="danger" 
                   plain 
                   size="large"
                   :disabled="seatStats.assignmentCount === 0"
                   @click="handleResetSeats"
                >
                   清空重置
                </el-button>
             </el-col>
           </el-row>
        </div>
        <el-empty v-else description="请选择场次和科目以查看排座状态" />

        <div class="operation-bar" style="margin-top: 20px;">
           <span>分配结果预览</span>
           <el-button size="small" style="float: right" @click="fetchAssignments" :disabled="!seatStats">刷新列表</el-button>
        </div>
        <el-table :data="assignments" border height="400" v-loading="assignmentsLoading">
           <el-table-column prop="ticketNumber" label="准考证号" width="180" />
           <el-table-column label="座位号" width="100" align="center">
             <template #default="{ row }">
               <el-tag effect="dark">{{ row.seatNumber }}</el-tag>
             </template>
           </el-table-column>
           <el-table-column label="考场信息">
             <template #default="{ row }">
                {{ getRoomName(row.roomId) }}
             </template>
           </el-table-column>
           <el-table-column prop="status" label="状态" />
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="通知公告" name="notifications">
        <div class="operation-bar">
          <el-button type="primary" @click="showNotifyDialog = true">新建普通通知</el-button>
          
          <el-button-group style="margin-left: 10px;">
            <el-button type="success" plain :icon="Postcard" @click="openQuickNotify('INVIGILATION')">
              发布监考安排通知
            </el-button>
            <el-button type="warning" plain :icon="Timer" @click="openQuickNotify('GRADE_RELEASE')">
              发布成绩查询预告
            </el-button>
          </el-button-group>

          <el-button @click="refetchNotifications" style="margin-left: 10px;">刷新</el-button>
        </div>

        <el-table :data="notifications" border v-loading="notifyLoading" style="margin-top: 15px;">
          <el-table-column prop="title" label="标题" min-width="200" />
          <el-table-column prop="type" label="类型" width="120">
            <template #default="{ row }"><el-tag effect="plain">{{ row.type }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="channel" label="渠道" width="100" />
          <el-table-column label="状态" width="120" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 'PUBLISHED' ? 'success' : 'warning'">
                {{ row.status === 'PUBLISHED' ? '已发布' : '草稿' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="创建时间" width="180">
             <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="200" align="center" fixed="right">
            <template #default="{ row }">
              <el-button v-if="row.status !== 'PUBLISHED'" size="small" type="success" @click="handlePublish(row)">发布</el-button>
              <el-button v-else size="small" type="warning" @click="handleWithdraw(row)">撤回</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-dialog 
          v-model="showQuickNotifyDialog" 
          :title="quickNotifyType === 'INVIGILATION' ? '生成监考安排通知' : '生成成绩发布预告'" 
          width="500px"
        >
          <el-form label-width="100px" :model="quickForm">
            
            <el-form-item label="考试场次" required>
              <el-select v-model="quickForm.sessionId" placeholder="选择关联的考试场次" style="width: 100%">
                <el-option 
                  v-for="s in sessions" 
                  :key="s.id" 
                  :label="s.name" 
                  :value="s.id" 
                />
              </el-select>
            </el-form-item>

            <el-form-item label="考试科目" v-if="quickNotifyType === 'GRADE_RELEASE'" required>
              <el-select v-model="quickForm.subjectId" placeholder="选择科目" style="width: 100%">
                <el-option 
                  v-for="sub in subjects" 
                  :key="sub.id" 
                  :label="sub.name" 
                  :value="sub.id" 
                />
              </el-select>
            </el-form-item>

            <el-form-item label="预计发布" v-if="quickNotifyType === 'GRADE_RELEASE'" required>
              <el-date-picker
                v-model="quickForm.releaseTime"
                type="datetime"
                placeholder="选择预计查分时间"
                format="YYYY-MM-DD HH:mm"
                value-format="YYYY-MM-DD HH:mm:ss"
                style="width: 100%"
              />
              <div class="tips">学生将在收到通知后知晓该时间点。</div>
            </el-form-item>

            <el-divider content-position="left">预览生成内容</el-divider>
            
            <div class="preview-box">
              <p><strong>发送给：</strong> {{ quickNotifyType === 'INVIGILATION' ? '全体教师 (TEACHER)' : '全体考生 (STUDENT)' }}</p>
              <p><strong>标题：</strong> {{ generatedTitle }}</p>
              <p><strong>内容：</strong> {{ generatedContent }}</p>
            </div>

          </el-form>
          <template #footer>
            <el-button @click="showQuickNotifyDialog = false">取消</el-button>
            <el-button type="primary" @click="sendQuickNotify" :loading="quickNotifying">立即发布</el-button>
          </template>
        </el-dialog>
      </el-tab-pane>

      <el-tab-pane label="准考证模板" name="admitCards">
        <div class="operation-bar">
          <el-button type="primary" @click="openTemplateDialog()">新建模板</el-button>
          <el-button @click="refetchTemplates">刷新</el-button>
        </div>
        <el-table :data="templates" border v-loading="templatesLoading">
          <el-table-column prop="name" label="模板名称" width="200" />
          <el-table-column prop="examNotice" label="考生须知预览" show-overflow-tooltip />
          <el-table-column label="Logo" width="100" align="center">
             <template #default="{ row }">
                <el-image v-if="row.logoUrl" :src="row.logoUrl" style="width: 40px; height: 40px" fit="contain" />
                <span v-else>-</span>
             </template>
          </el-table-column>
          <el-table-column label="操作" width="220" align="center">
             <template #default="{ row }">
                <el-button size="small" type="success" @click="handlePreviewTemplate(row)">预览</el-button>
                <el-button size="small" @click="handleEditTemplate(row)">编辑</el-button>
                <el-button size="small" type="danger" @click="handleDeleteTemplate(row)">删除</el-button>
             </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

    </el-tabs>

    <el-dialog v-model="showScheduleDialog" :title="isEditSchedule ? '编辑考试安排' : '新建考试安排'" width="550px">
      <el-form :model="scheduleForm" label-width="100px" v-loading="scheduleFormLoading">
        <el-form-item label="考试科目" required>
          <el-select v-model="scheduleForm.subjectId" placeholder="请选择科目" style="width: 100%" :disabled="isEditSchedule">
            <el-option v-for="s in subjects" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="考试场次" required>
          <el-select v-model="scheduleForm.sessionId" placeholder="请选择场次" style="width: 100%">
            <el-option v-for="s in sessions" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>

        <el-form-item label="考场教室" required>
           <el-select v-model="scheduleForm.roomId" placeholder="请选择考场" style="width: 100%">
            <el-option v-for="r in examRooms" :key="r.id" :label="`${r.center?.name || ''} ${r.name} (${r.roomNumber}, ${r.capacity}人)`" :value="r.id" />
          </el-select>
        </el-form-item>

        <el-form-item label="监考老师">
          <el-select 
            v-model="scheduleForm.selectedInvigilators" 
            multiple 
            filterable
            placeholder="请选择监考老师（可多选）" 
            style="width: 100%"
            no-data-text="暂无教师数据"
          >
            <el-option 
              v-for="t in teachers" 
              :key="t.id" 
              :label="t.fullName + (t.phone ? ' (' + t.phone + ')' : '')" 
              :value="t.id" 
            />
          </el-select>
          <div v-if="teachers.length === 0" style="font-size: 12px; color: #F56C6C; margin-top: 5px; line-height: 1.2;">
             提示：未检测到角色为 "TEACHER" 的用户。请在“用户管理”中确认用户角色是否正确。
          </div>
        </el-form-item>

        <el-form-item label="备注">
          <el-input v-model="scheduleForm.note" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showScheduleDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitSchedule" :loading="scheduleSubmitting">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showSubjectDialog" :title="isEditSubject ? '编辑科目' : '新建科目'" width="500px">
      <el-form :model="subjectForm" :rules="subjectRules" ref="subjectFormRef" label-width="100px">
        <el-form-item label="科目代码" prop="code"><el-input v-model="subjectForm.code" :disabled="isEditSubject" /></el-form-item>
        <el-form-item label="科目名称" prop="name"><el-input v-model="subjectForm.name" /></el-form-item>
        <el-form-item label="考试时长"><el-input-number v-model="subjectForm.durationMinutes" :min="30" :step="10" /> 分钟</el-form-item>
        <el-form-item label="题目数量"><el-input-number v-model="subjectForm.questionCount" :min="1" /></el-form-item>
        <el-form-item label="科目描述"><el-input v-model="subjectForm.description" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showSubjectDialog=false">取消</el-button>
        <el-button type="primary" @click="handleSubmitSubject" :loading="subjectSubmitting">保存科目</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showTimeDialog" title="配置报名与考试场次" width="500px" top="5vh">
      <el-form :model="timeForm" label-width="100px" v-loading="timeLoading">
        <el-form-item label="当前科目"><el-tag>{{ currentSubject?.name }}</el-tag></el-form-item>
        <el-form-item label="所属场次" required>
          <el-select v-model="timeForm.sessionId" placeholder="请选择场次">
            <el-option v-for="s in sessions" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="报名时间" required>
          <div style="display: flex; gap: 10px; align-items: center;">
            <el-date-picker v-model="timeForm.range[0]" type="datetime" placeholder="开始时间" value-format="x" style="width: 48%" />
            <span style="color: #909399">至</span>
            <el-date-picker v-model="timeForm.range[1]" type="datetime" placeholder="结束时间" value-format="x" style="width: 48%" />
          </div>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="timeForm.note" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showTimeDialog=false">取消</el-button>
        <el-button type="success" @click="handleSubmitTime" :loading="timeSubmitting">{{ currentWindowId ? '更新配置' : '创建配置' }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showCenterDialog" title="新增考点" width="500px">
      <el-form :model="centerForm" label-width="80px">
        <el-form-item label="名称"><el-input v-model="centerForm.name" /></el-form-item>
        <el-form-item label="地址"><el-input v-model="centerForm.address" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="centerForm.description" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCenterDialog=false">取消</el-button>
        <el-button type="primary" @click="handleCreateCenter">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showRoomDialog" title="新增考场" width="500px">
      <el-form :model="roomForm" label-width="100px">
        <el-form-item label="所属考点">
          <el-select v-model="roomForm.centerId" placeholder="选择考点"><el-option v-for="c in examCenters" :key="c.id" :label="c.name" :value="c.id" /></el-select>
        </el-form-item>
        <el-form-item label="门牌号"><el-input v-model="roomForm.roomNumber" /></el-form-item>
        <el-form-item label="考场名"><el-input v-model="roomForm.name" /></el-form-item>
        <el-form-item label="容量"><el-input-number v-model="roomForm.capacity" :min="10" :max="500" /></el-form-item>
        <el-form-item label="管理员"><el-input v-model="roomForm.managerName" /></el-form-item>
        <el-form-item label="联系电话"><el-input v-model="roomForm.managerPhone" /></el-form-item>
        <el-form-item label="具体位置"><el-input v-model="roomForm.location" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showRoomDialog=false">取消</el-button>
        <el-button type="primary" @click="handleCreateRoom">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showSessionDialog" :title="editingSession ? '编辑场次' : '新增场次'" width="500px">
      <el-form :model="sessionForm" label-width="100px">
        <el-form-item label="场次名称" required>
          <el-input v-model="sessionForm.name" placeholder="如：2025年上半年第一场" />
        </el-form-item>
        <el-form-item label="开始时间" required>
          <el-date-picker v-model="sessionForm.startTime" type="datetime" placeholder="选择开始时间" style="width: 100%" />
        </el-form-item>
        <el-form-item label="结束时间" required>
          <el-date-picker v-model="sessionForm.endTime" type="datetime" placeholder="选择结束时间" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showSessionDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSaveSession" :loading="sessionSaving">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showNotifyDialog" title="发布新通知" width="600px">
      <el-form :model="notifyForm" label-width="80px">
        <el-form-item label="类型" required>
          <el-select v-model="notifyForm.type" placeholder="选择类型" @change="handleNotifyTypeChange">
            <el-option label="考试通知" value="EXAM_NOTICE" />
            <el-option label="系统公告" value="SYSTEM_NOTICE" />
            <el-option label="成绩发布" value="GRADE_NOTICE" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="notifyForm.type === 'EXAM_NOTICE'" label="关联科目" required>
           <el-select v-model="notifyForm.subjectId" placeholder="请选择科目" style="width: 100%" @change="handleSubjectChange">
             <el-option v-for="sub in subjects" :key="sub.id" :label="sub.name + ' (' + sub.code + ')'" :value="sub.id" />
           </el-select>
        </el-form-item>
        <el-form-item label="标题" required>
          <el-input v-model="notifyForm.title" placeholder="标题" :disabled="notifyForm.type === 'EXAM_NOTICE' && !notifyForm.subjectId" />
        </el-form-item>
        <el-form-item label="渠道">
          <el-select v-model="notifyForm.channel"><el-option label="站内信" value="WEB" /><el-option label="邮件" value="EMAIL" /></el-select>
        </el-form-item>
        <el-form-item label="内容" required><el-input v-model="notifyForm.content" type="textarea" :rows="5" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showNotifyDialog=false">取消</el-button>
        <el-button type="primary" @click="handleCreateNotify" :loading="notifySubmitting">创建通知</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showAuditDialog" title="考生报名审核" width="600px">
      <div v-if="currentAudit" style="padding: 0 20px;">
        <el-descriptions title="基本信息" :column="2" border>
          <el-descriptions-item label="姓名">{{ currentAudit.fullName }}</el-descriptions-item>
          <el-descriptions-item label="电话">{{ currentAudit.phone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="身份证">{{ currentAudit.idCardNumber }}</el-descriptions-item>
          <el-descriptions-item label="报考科目">{{ getSubjectName(currentAudit.subjectId) }}</el-descriptions-item>
        </el-descriptions>
        <el-divider content-position="left">证明材料</el-divider>
        <div v-if="currentAudit.materials && currentAudit.materials.length">
          <div v-for="mat in currentAudit.materials" :key="mat.id" style="margin-bottom: 10px;">
            <div style="font-weight: bold; margin-bottom: 5px;">{{ mat.type }}</div>
            <el-image v-if="mat.fileUrl && (mat.fileUrl.endsWith('.jpg') || mat.fileUrl.endsWith('.png'))" style="width: 100px; height: 100px; border-radius: 4px;" :src="mat.fileUrl" :preview-src-list="[mat.fileUrl]" fit="cover" />
            <a v-else :href="mat.fileUrl" target="_blank" style="color: #409EFF;"><el-icon><Document /></el-icon> 查看文件</a>
          </div>
        </div>
        <el-empty v-else description="无材料" :image-size="60" />
        <el-divider content-position="left">审核操作</el-divider>
        <el-input v-model="rejectReason" type="textarea" placeholder="驳回原因（必填）" :rows="3" style="margin-bottom: 15px;" />
        <div style="text-align: right;">
          <el-button type="danger" @click="handleReject">驳回申请</el-button>
          <el-button type="success" @click="handleApprove" :disabled="!!rejectReason">通过申请</el-button>
        </div>
      </div>
    </el-dialog>

    <el-dialog v-model="showTemplateDialog" :title="isEditTemplate ? '编辑模板' : '新建模板'" width="500px">
       <el-form :model="templateForm" label-width="100px">
         <el-form-item label="模板名称"><el-input v-model="templateForm.name" placeholder="例如：2024期末通用模板" /></el-form-item>
         <el-form-item label="Logo链接"><el-input v-model="templateForm.logoUrl" placeholder="输入学校Logo图片地址" /></el-form-item>
         <el-form-item label="考生须知"><el-input v-model="templateForm.examNotice" type="textarea" :rows="6" placeholder="输入打印在准考证上的注意事项..." /></el-form-item>
       </el-form>
       <template #footer>
         <el-button @click="showTemplateDialog=false">取消</el-button>
         <el-button type="primary" @click="handleSubmitTemplate">保存模板</el-button>
       </template>
    </el-dialog>

    <el-dialog v-model="showPreviewDialog" title="准考证样式预览" width="850px" top="5vh">
      <div v-if="previewTemplate" class="preview-container">
        <AdmitCard 
          :template-data="previewTemplate" 
          :student="mockStudent"
          :exams="mockExams"
          exam-name="2024-2025第一学期期末考试（预览）"
        />
      </div>
      <template #footer>
        <el-button @click="showPreviewDialog = false">关闭</el-button>
        <el-button type="primary" @click="triggerPrint">
          <el-icon style="margin-right: 5px"><Printer /></el-icon> 模拟打印 (A4)
        </el-button>
      </template>
    </el-dialog>

  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue' 
import { gql } from '@apollo/client/core'
import { useQuery, useMutation, useApolloClient } from '@vue/apollo-composable'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { Document, Tools, Printer, Plus, Search, Postcard, Timer } from '@element-plus/icons-vue'
import dayjs from 'dayjs' 
import AdmitCard from './AdmitCard.vue' 

const activeTab = ref('subjects')
const { client } = useApolloClient()
const formatDate = (isoStr: string) => isoStr ? dayjs(Number(isoStr) ? Number(isoStr) : isoStr).format('YYYY-MM-DD HH:mm') : '-'

// --- 新增：JSON Note 解析辅助函数 ---

// 2. 获取监考老师名字数组（从监考分配表中查询）
// 注意：这个函数在后面会被重新定义，这里只是占位
let getInvigilatorNames = (_scheduleId: string): string[] => []

// ================== GraphQL Definitions ==================
// 1. Subjects
const GET_SUBJECTS = gql`query { examSubjects { id, code, name, durationMinutes, questionCount, description, status } }`
const CREATE_SUBJECT = gql`mutation($input: ExamSubjectInput!) { createExamSubject(input: $input) { id } }`
const UPDATE_SUBJECT = gql`mutation($id: ID!, $input: ExamSubjectInput!) { updateExamSubject(id: $id, input: $input) { id } }`

// 2. Windows & Sessions
const GET_ALL_WINDOWS = gql`query GetAllWindows { examRegistrationWindows { id, subject { id }, session { id }, startTime, endTime } }`
const GET_WINDOW_LIST = gql`query GetWindows($subjectId: ID) { examRegistrationWindows(subjectId: $subjectId) { id, session { id }, startTime, endTime, note } }`
const UPSERT_WINDOW = gql`mutation UpsertWindow($id: ID, $input: ExamRegistrationWindowInput!) { upsertExamRegistrationWindow(id: $id, input: $input) { id } }`
const GET_SESSIONS = gql`query { examSessions { id, name, startTime, endTime } }`
const CREATE_SESSION = gql`mutation($input: ExamSessionInput!) { createExamSession(input: $input) { id, name } }`
const UPDATE_SESSION = gql`mutation($id: ID!, $input: ExamSessionInput!) { updateExamSession(id: $id, input: $input) { id, name } }`
const DELETE_SESSION = gql`mutation($id: ID!) { deleteExamSession(id: $id) }`

// 3. Centers & Rooms & Teachers
const GET_CENTERS = gql`query { examCenters { id, name, address, description } }`
const CREATE_CENTER = gql`mutation($input: ExamCenterInput!) { createExamCenter(input: $input) { id } }`
const DELETE_CENTER = gql`mutation($id: ID!) { deleteExamCenter(id: $id) }`
const GET_ROOMS = gql`query { examRooms { id, roomNumber, name, capacity, status, managerName, center { id, name } } }`
const CREATE_ROOM = gql`mutation($input: ExamRoomInput!) { createExamRoom(input: $input) { id } }`
// 数据库角色名是小写 teacher
const GET_TEACHERS = gql`query { users(role: "teacher") { id, fullName, phone } }`

// 4. Notifications
const GET_NOTIFICATIONS = gql`query { notifications { id, title, type, content, channel, status, createdAt } }`
const CREATE_NOTIFICATION = gql`mutation($input: NotificationInput!) { createNotification(input: $input) { id } }`
const PUBLISH_NOTIFICATION = gql`mutation($input: PublishNotificationInput!) { publishNotification(input: $input) { id, status } }`
const WITHDRAW_NOTIFICATION = gql`mutation($input: PublishNotificationInput!) { withdrawNotification(input: $input) { id, status } }`

// 5. Audits
const GET_PENDING_REGISTRATIONS = gql`query { pendingRegistrations { id, fullName, idCardNumber, phone, subjectId, status, materials { id, type, fileUrl, status } } }`
const APPROVE_REGISTRATION = gql`mutation($id: ID!) { approveRegistration(registrationInfoId: $id) { id, status } }`
const REJECT_REGISTRATION = gql`mutation($input: RegistrationRejectInput!) { rejectRegistration(input: $input) { id, status } }`

// 6. Seats
const GET_SEAT_STATS = gql`query Stats($subjectId: ID!, $sessionId: ID!) { seatAssignmentStats(subjectId: $subjectId, sessionId: $sessionId) { subjectId, sessionId, registrationCount, assignmentCount, availableRooms } }`
const ASSIGN_SEATS = gql`mutation Assign($input: SeatAssignmentInput!) { assignSeats(input: $input) { id } }`
const RESET_SEATS = gql`mutation Reset($subjectId: ID!, $sessionId: ID!) { resetSeats(subjectId: $subjectId, sessionId: $sessionId) }`
const GET_ASSIGNMENTS = gql`query List($subjectId: ID, $sessionId: ID) { seatAssignments(subjectId: $subjectId, sessionId: $sessionId) { id, ticketNumber, seatNumber, roomId, status } }`

// 7. Admit Cards
const GET_TEMPLATES = gql`query { admitCardTemplates { id, name, logoUrl, examNotice } }`
const UPSERT_TEMPLATE = gql`mutation($input: AdmitCardTemplateInput!) { upsertAdmitCardTemplate(input: $input) { id } }`
const DELETE_TEMPLATE = gql`mutation($id: ID!) { deleteAdmitCardTemplate(id: $id) }`

// 8. Exam Schedules & Invigilators
const GET_Schedules = gql`
  query GetSchedules($subjectId: ID, $sessionId: ID) {
    examSchedules(subjectId: $subjectId, sessionId: $sessionId) {
      id, status, note
      examSubject { id name }
      examSession { id name startTime }
      examRoom { id roomNumber name capacity center { name } }
    }
  }
`
const GET_ALL_INVIGILATOR_ASSIGNMENTS = gql`
  query GetAllInvigilatorAssignments {
    invigilatorAssignments(scheduleId: null) {
      id, scheduleId, teacherUserId, teacherName
    }
  }
`
const CREATE_SCHEDULE = gql`mutation CreateSchedule($input: ExamScheduleInput!) { createExamSchedule(input: $input) { id } }`
const UPDATE_SCHEDULE = gql`mutation UpdateSchedule($id: ID!, $input: ExamScheduleInput!) { updateExamSchedule(id: $id, input: $input) { id } }`
const DELETE_SCHEDULE = gql`mutation($id: ID!) { deleteExamSchedule(id: $id) }`
const ASSIGN_INVIGILATORS = gql`mutation AssignInvigilators($input: AssignInvigilatorsInput!) { assignInvigilators(input: $input) { id } }`

// ================== Hooks & State ==================

// Common Data
const { result: subRes, loading: subjectsLoading, refetch: refetchSubjects } = useQuery(GET_SUBJECTS)
const subjects = computed(() => subRes.value?.examSubjects || [])
const { result: sessionRes, loading: sessionsLoading, refetch: refetchSessions } = useQuery(GET_SESSIONS)
const sessions = computed(() => sessionRes.value?.examSessions || [])
const { mutate: createSessionMutate } = useMutation(CREATE_SESSION)
const { mutate: updateSessionMutate } = useMutation(UPDATE_SESSION)
const { mutate: deleteSessionMutate } = useMutation(DELETE_SESSION)
const { result: roomRes, loading: roomsLoading, refetch: refetchRooms } = useQuery(GET_ROOMS)
const examRooms = computed(() => roomRes.value?.examRooms || [])
// [修复点]：获取 refetch 方法以便手动刷新老师列表
const { result: teacherRes, refetch: refetchTeachers } = useQuery(GET_TEACHERS)
const teachers = computed(() => teacherRes.value?.users || [])

// Window Map
const { result: allWindowsRes, refetch: refetchWindows } = useQuery(GET_ALL_WINDOWS)
const windowMap = computed(() => {
  const map: Record<string, any> = {}
  const list = allWindowsRes.value?.examRegistrationWindows || []
  list.forEach((w: any) => { if (w && w.subject && w.subject.id) map[w.subject.id] = w })
  return map
})

// Mutations
const { mutate: createSub } = useMutation(CREATE_SUBJECT)
const { mutate: updateSub } = useMutation(UPDATE_SUBJECT)
const { mutate: upsertWindow } = useMutation(UPSERT_WINDOW)
const { result: centerRes, loading: centersLoading, refetch: refetchCenters } = useQuery(GET_CENTERS)
const examCenters = computed(() => centerRes.value?.examCenters || [])
const { mutate: createCenterMutate } = useMutation(CREATE_CENTER)
const { mutate: deleteCenterMutate } = useMutation(DELETE_CENTER)
const { mutate: createRoomMutate } = useMutation(CREATE_ROOM)
const { result: notifyRes, loading: notifyLoading, refetch: refetchNotifications } = useQuery(GET_NOTIFICATIONS)
const notifications = computed(() => notifyRes.value?.notifications || [])
const { mutate: createNotifyMutate } = useMutation(CREATE_NOTIFICATION)
const { mutate: publishNotifyMutate } = useMutation(PUBLISH_NOTIFICATION)
const { mutate: withdrawNotifyMutate } = useMutation(WITHDRAW_NOTIFICATION)
const { result: auditRes, loading: auditLoading, refetch: refetchAudits } = useQuery(GET_PENDING_REGISTRATIONS)
const pendingList = computed(() => auditRes.value?.pendingRegistrations || [])
const { mutate: approveMutate } = useMutation(APPROVE_REGISTRATION)
const { mutate: rejectMutate } = useMutation(REJECT_REGISTRATION)

// Seats Hooks
const { mutate: assignSeatsMutate } = useMutation(ASSIGN_SEATS)
const { mutate: resetSeatsMutate } = useMutation(RESET_SEATS)

// Templates Hooks
const { result: templateRes, loading: templatesLoading, refetch: refetchTemplates } = useQuery(GET_TEMPLATES)
const templates = computed(() => templateRes.value?.admitCardTemplates || [])
const { mutate: upsertTemplateMutate } = useMutation(UPSERT_TEMPLATE)
const { mutate: deleteTemplateMutate } = useMutation(DELETE_TEMPLATE)

// Schedule State & Hooks
const filterSubject = ref('')
const filterSession = ref('')
const { result: scheduleRes, loading: schedulesLoading, refetch: refetchSchedules } = useQuery(GET_Schedules, () => ({
  subjectId: filterSubject.value || undefined,
  sessionId: filterSession.value || undefined
}))
const schedules = computed(() => scheduleRes.value?.examSchedules || [])
const { result: allAssignmentsRes, refetch: refetchAllAssignments } = useQuery(GET_ALL_INVIGILATOR_ASSIGNMENTS, null, { fetchPolicy: 'network-only' })
const allAssignments = computed(() => allAssignmentsRes.value?.invigilatorAssignments || [])

// 根据 scheduleId 获取监考老师名字数组
getInvigilatorNames = (scheduleId: string): string[] => {
  if (!scheduleId) return []
  const assignments = allAssignments.value.filter((a: any) => String(a.scheduleId) === String(scheduleId))
  return assignments.map((a: any) => a.teacherName || '未知教师')
}

const { mutate: createScheduleMutate } = useMutation(CREATE_SCHEDULE)
const { mutate: updateScheduleMutate } = useMutation(UPDATE_SCHEDULE)
const { mutate: deleteScheduleMutate } = useMutation(DELETE_SCHEDULE)
const { mutate: assignInvigilatorsMutate } = useMutation(ASSIGN_INVIGILATORS)


// ================== Logic Implementation ==================

// 1. Subject
const showSubjectDialog = ref(false)
const isEditSubject = ref(false)
const subjectSubmitting = ref(false)
const subjectFormRef = ref<FormInstance>()
const subjectForm = reactive({ id: '', code: '', name: '', durationMinutes: 90, questionCount: 100, description: '' })
const subjectRules = { code: [{ required: true, message: '必填', trigger: 'blur' }], name: [{ required: true, message: '必填', trigger: 'blur' }] }
const openSubjectDialog = () => { isEditSubject.value = false; Object.assign(subjectForm, { id: '', code: '', name: '', durationMinutes: 90, questionCount: 100, description: '' }); showSubjectDialog.value = true }
const handleEditSubject = (row: any) => { isEditSubject.value = true; Object.assign(subjectForm, { ...row }); showSubjectDialog.value = true }
const handleSubmitSubject = async () => { if (!subjectFormRef.value) return; await subjectFormRef.value.validate(async (valid) => { if (valid) { subjectSubmitting.value = true; try { const input = { code: subjectForm.code, name: subjectForm.name, durationMinutes: subjectForm.durationMinutes, questionCount: subjectForm.questionCount, description: subjectForm.description }; if (isEditSubject.value) await updateSub({ id: subjectForm.id, input }); else await createSub({ input }); ElMessage.success('科目保存成功！'); showSubjectDialog.value = false; refetchSubjects() } catch (e: any) { ElMessage.error(e.message) } finally { subjectSubmitting.value = false } } }) }

// 2. Time Config
const showTimeDialog = ref(false)
const timeSubmitting = ref(false)
const timeLoading = ref(false)
const currentSubject = ref<any>(null)
const currentWindowId = ref<string | null>(null)
const timeForm = reactive({ sessionId: '', range: [] as string[], note: '' })
const getSessionName = (sessionId: string) => { const s = sessions.value.find((item: any) => item.id === sessionId); return s ? s.name : '' }

const openTimeConfig = async (row: any) => { 
  currentSubject.value = row; 
  timeForm.sessionId = ''; 
  timeForm.range = []; 
  timeForm.note = ''; 
  currentWindowId.value = null; 
  showTimeDialog.value = true; 
  timeLoading.value = true; 
  try { 
    refetchSessions(); 
    const { data } = await client.query({ query: GET_WINDOW_LIST, variables: { subjectId: row.id }, fetchPolicy: 'network-only' }); 
    const history = data?.examRegistrationWindows?.[0]; 
    if (history) { 
      currentWindowId.value = history.id; 
      timeForm.sessionId = history.session?.id || ''; 
      timeForm.note = history.note; 
      if (history.startTime && history.endTime) timeForm.range = [String(dayjs(history.startTime).valueOf()), String(dayjs(history.endTime).valueOf())] 
    } 
  } catch (e) { console.error(e) } finally { timeLoading.value = false } 
}

const handleSubmitTime = async () => {
  if (!timeForm.sessionId || !timeForm.range || timeForm.range.length < 2) {
    ElMessage.warning('请选择场次和完整的起止时间')
    return
  }
  timeSubmitting.value = true
  try {
    const startObj = dayjs(Number(timeForm.range[0]))
    const endObj = dayjs(Number(timeForm.range[1]))
    if (!startObj.isValid() || !endObj.isValid()) throw new Error('时间格式解析失败')
    const fmt = 'YYYY-MM-DDTHH:mm:ssZ' 
    await upsertWindow({
      id: currentWindowId.value,
      input: {
        subjectId: currentSubject.value.id,
        sessionId: timeForm.sessionId,
        startTime: startObj.format(fmt),
        endTime: endObj.format(fmt),
        note: timeForm.note
      }
    })
    ElMessage.success('报名时间配置成功！')
    showTimeDialog.value = false
    await refetchWindows()
  } catch (e: any) { ElMessage.error(e.message || '提交失败') } finally { timeSubmitting.value = false }
}

// 3. Center & Room
const showCenterDialog = ref(false)
const centerForm = reactive({ name: '', address: '', description: '' })
const handleCreateCenter = async () => { try { await createCenterMutate({ input: { ...centerForm } }); ElMessage.success('创建成功'); showCenterDialog.value = false; refetchCenters() } catch(e:any){ ElMessage.error(e.message) } }
const handleDeleteCenter = async (row: any) => { try { await ElMessageBox.confirm('确认删除?'); await deleteCenterMutate({ id: row.id }); refetchCenters() } catch {} }
const showRoomDialog = ref(false)
const roomForm = reactive({ centerId: '', roomNumber: '', name: '', capacity: 30, managerName: '', managerPhone: '', location: '' })
const openRoomDialog = () => { refetchCenters(); showRoomDialog.value = true }
const handleCreateRoom = async () => { try { await createRoomMutate({ input: { ...roomForm } }); ElMessage.success('创建成功'); showRoomDialog.value = false; refetchRooms() } catch(e:any){ if (e.message.includes('INTERNAL_ERROR')) ElMessage.error('门牌号重复，请更换'); else ElMessage.error(e.message) } }
const getRoomName = (roomId: string) => { const r = examRooms.value.find((i: any) => i.id === roomId); return r ? `${r.name} (${r.roomNumber})` : roomId }

// 3.5 Sessions
const showSessionDialog = ref(false)
const editingSession = ref<any>(null)
const sessionSaving = ref(false)
const sessionForm = reactive({ name: '', startTime: null as Date | null, endTime: null as Date | null })
const openSessionDialog = (row?: any) => {
  if (row) {
    editingSession.value = row
    sessionForm.name = row.name
    sessionForm.startTime = row.startTime ? new Date(Number(row.startTime) || row.startTime) : null
    sessionForm.endTime = row.endTime ? new Date(Number(row.endTime) || row.endTime) : null
  } else {
    editingSession.value = null
    sessionForm.name = ''
    sessionForm.startTime = null
    sessionForm.endTime = null
  }
  showSessionDialog.value = true
}
const handleSaveSession = async () => {
  if (!sessionForm.name) return ElMessage.warning('请输入场次名称')
  if (!sessionForm.startTime) return ElMessage.warning('请选择开始时间')
  if (!sessionForm.endTime) return ElMessage.warning('请选择结束时间')
  sessionSaving.value = true
  try {
    const input = {
      name: sessionForm.name,
      startTime: sessionForm.startTime.toISOString(),
      endTime: sessionForm.endTime.toISOString()
    }
    if (editingSession.value) {
      await updateSessionMutate({ id: editingSession.value.id, input })
      ElMessage.success('更新成功')
    } else {
      await createSessionMutate({ input })
      ElMessage.success('创建成功')
    }
    showSessionDialog.value = false
    refetchSessions()
  } catch (e: any) {
    ElMessage.error(e.message)
  } finally {
    sessionSaving.value = false
  }
}
const handleDeleteSession = async (row: any) => {
  try {
    await ElMessageBox.confirm('确认删除该场次？')
    await deleteSessionMutate({ id: row.id })
    ElMessage.success('删除成功')
    refetchSessions()
  } catch {}
}

// 4. Notification (Enhanced)
const showNotifyDialog = ref(false)
const notifySubmitting = ref(false)
const notifyForm = reactive({ title: '', type: 'EXAM_NOTICE', channel: 'WEB', content: '', subjectId: '' })
const handleNotifyTypeChange = (val: string) => { if (val !== 'EXAM_NOTICE') { notifyForm.subjectId = ''; notifyForm.title = ''; notifyForm.content = '' } else { notifyForm.title = ''; notifyForm.content = '请先选择关联科目...' } }
const handleSubjectChange = (subjectId: string) => { const sub = subjects.value.find((s: any) => s.id === subjectId); if (sub) { notifyForm.title = `【考试通知】${sub.name} 期末考试安排`; notifyForm.content = `各位同学：\n\n${sub.name} (${sub.code}) 考试将于近期举行。请登录系统查看安排。\n\n祝考试顺利！` } }
const handleCreateNotify = async () => { if (notifyForm.type === 'EXAM_NOTICE' && !notifyForm.subjectId) return ElMessage.warning('请选科目'); if(!notifyForm.title) return ElMessage.warning('写标题'); notifySubmitting.value = true; try { await createNotifyMutate({ input: { title: notifyForm.title, type: notifyForm.type, channel: notifyForm.channel, content: notifyForm.content, targets: [{ targetType: 'SUBJECT', targetValue: notifyForm.subjectId || 'ALL' }] } }); ElMessage.success('草稿创建成功'); showNotifyDialog.value = false; refetchNotifications(); notifyForm.title = '' } catch(e:any) { ElMessage.error(e.message) } finally { notifySubmitting.value = false } }
const handlePublish = async (row: any) => { try { await ElMessageBox.confirm('确认发布?'); await publishNotifyMutate({ input: { notificationId: row.id } }); ElMessage.success('已发布'); refetchNotifications() } catch {} }
const handleWithdraw = async (row: any) => { try { await ElMessageBox.confirm('确认撤回?'); await withdrawNotifyMutate({ input: { notificationId: row.id } }); ElMessage.success('已撤回'); refetchNotifications() } catch {} }

// --- Quick Notify Logic (New) ---
const showQuickNotifyDialog = ref(false)
const quickNotifyType = ref<'INVIGILATION' | 'GRADE_RELEASE'>('INVIGILATION')
const quickForm = reactive({ sessionId: '', subjectId: '', releaseTime: '' })
const quickNotifying = ref(false)

// Computed for Auto-generating Content
const currentSessionName = computed(() => sessions.value.find((s:any) => s.id === quickForm.sessionId)?.name || '本次考试')
const currentSubjectName = computed(() => subjects.value.find((s:any) => s.id === quickForm.subjectId)?.name || '该科目')

const generatedTitle = computed(() => {
  if (quickNotifyType.value === 'INVIGILATION') return `${currentSessionName.value} 监考任务安排通知`
  else return `${currentSubjectName.value} 成绩查询预告`
})

const generatedContent = computed(() => {
  if (quickNotifyType.value === 'INVIGILATION') {
    return `各位老师好，${currentSessionName.value} 的监考安排已发布。请登录系统，在“考试安排管理”或“我的任务”中查看具体考场和时间。请务必准时到岗。`
  } else {
    const timeStr = quickForm.releaseTime || '待定时间'
    return `各位同学，${currentSessionName.value} - ${currentSubjectName.value} 科目的考试成绩预计将于 【${timeStr}】 发布。届时请登录系统查看成绩。`
  }
})

const openQuickNotify = (type: 'INVIGILATION' | 'GRADE_RELEASE') => {
  quickNotifyType.value = type
  quickForm.sessionId = ''
  quickForm.subjectId = ''
  quickForm.releaseTime = ''
  showQuickNotifyDialog.value = true
}

const sendQuickNotify = async () => {
  if (!quickForm.sessionId) return ElMessage.warning('请选择考试场次')
  if (quickNotifyType.value === 'GRADE_RELEASE') {
    if (!quickForm.subjectId) return ElMessage.warning('请选择科目')
    if (!quickForm.releaseTime) return ElMessage.warning('请选择预计发布时间')
  }

  const targetRole = quickNotifyType.value === 'INVIGILATION' ? 'TEACHER' : 'STUDENT'
  
  const input = {
    title: generatedTitle.value,
    type: 'SYSTEM_NOTICE',
    content: generatedContent.value,
    channel: 'WEB',
    targets: [{ targetType: 'ROLE', targetValue: targetRole }]
  }

  quickNotifying.value = true
  try {
    // 1. Create
    const res = await createNotifyMutate({ input })
    const newId = res?.data?.createNotification?.id
    if (newId) {
      // 2. Publish Immediately
      await publishNotifyMutate({ input: { notificationId: newId } })
      ElMessage.success('通知发布成功')
      showQuickNotifyDialog.value = false
      refetchNotifications()
    }
  } catch (e: any) {
    ElMessage.error(e.message || '发布失败')
  } finally {
    quickNotifying.value = false
  }
}

// 5. Audit
const showAuditDialog = ref(false)
const currentAudit = ref<any>(null)
const rejectReason = ref('')
const openAuditDialog = (row: any) => { currentAudit.value = row; rejectReason.value = ''; showAuditDialog.value = true }
const getSubjectName = (subjectId: string) => { const sub = subjects.value.find((s: any) => s.id === subjectId); return sub ? `${sub.name}` : subjectId }
const handleApprove = async () => { try { await ElMessageBox.confirm('确认通过?'); await approveMutate({ id: currentAudit.value.id }); ElMessage.success('已通过'); showAuditDialog.value = false; refetchAudits() } catch {} }
const handleReject = async () => { if (!rejectReason.value) return ElMessage.warning('写原因'); try { await rejectMutate({ input: { registrationInfoId: currentAudit.value.id, reason: rejectReason.value } }); ElMessage.warning('已驳回'); showAuditDialog.value = false; refetchAudits() } catch(e:any) { ElMessage.error(e.message) } }

// 6. Seat Assignment Logic
const seatQuery = reactive({ subjectId: '', sessionId: '' })
const seatStats = ref<any>(null)
const assignments = ref<any[]>([])
const assignmentsLoading = ref(false)
const assigningSeats = ref(false)
const canQuerySeats = computed(() => seatQuery.subjectId && seatQuery.sessionId)
const isFullyAssigned = computed(() => seatStats.value && seatStats.value.registrationCount > 0 && seatStats.value.registrationCount === seatStats.value.assignmentCount)
const fetchSeatStats = async () => { if (!canQuerySeats.value) return; try { const { data } = await client.query({ query: GET_SEAT_STATS, variables: { subjectId: seatQuery.subjectId, sessionId: seatQuery.sessionId }, fetchPolicy: 'network-only' }); seatStats.value = data.seatAssignmentStats; fetchAssignments() } catch (e) { console.error(e) } }
const fetchAssignments = async () => { if (!canQuerySeats.value) return; assignmentsLoading.value = true; try { const { data } = await client.query({ query: GET_ASSIGNMENTS, variables: { subjectId: seatQuery.subjectId, sessionId: seatQuery.sessionId }, fetchPolicy: 'network-only' }); assignments.value = data.seatAssignments || [] } finally { assignmentsLoading.value = false } }
const handleAssignSeats = async () => { try { await ElMessageBox.confirm('确定要执行自动排座算法吗？'); assigningSeats.value = true; await assignSeatsMutate({ input: { subjectId: seatQuery.subjectId, sessionId: seatQuery.sessionId } }); ElMessage.success('排座完成！'); fetchSeatStats() } catch(e:any) { ElMessage.error(e.message) } finally { assigningSeats.value = false } }
const handleResetSeats = async () => { try { await ElMessageBox.confirm('危险操作：确定要清空所有座次分配吗？', { type: 'error' }); await resetSeatsMutate({ subjectId: seatQuery.subjectId, sessionId: seatQuery.sessionId }); ElMessage.success('已清空分配'); fetchSeatStats() } catch {} }

// 7. Schedule Logic
const showScheduleDialog = ref(false)
const isEditSchedule = ref(false)
const scheduleSubmitting = ref(false)
const scheduleFormLoading = ref(false)
const currentScheduleId = ref('')
const scheduleForm = reactive({ sessionId: '', subjectId: '', roomId: '', note: '', selectedInvigilators: [] as string[] })

const handleAddSchedule = () => {
  isEditSchedule.value = false
  currentScheduleId.value = ''
  scheduleForm.subjectId = ''
  scheduleForm.sessionId = ''
  scheduleForm.roomId = ''
  scheduleForm.note = ''
  scheduleForm.selectedInvigilators = []
  
  // [修复点] 打开弹窗时，强制刷新老师列表，避免缓存或未加载导致为空
  refetchTeachers()
  showScheduleDialog.value = true
}

const handleEditSchedule = (row: any) => {
  isEditSchedule.value = true
  currentScheduleId.value = row.id

  // 基础字段回显
  scheduleForm.subjectId = row.examSubject.id
  scheduleForm.sessionId = row.examSession.id
  scheduleForm.roomId = row.examRoom.id
  scheduleForm.note = row.note || ''

  // 从监考分配表中获取已分配的监考老师
  const assignments = allAssignments.value.filter((a: any) => String(a.scheduleId) === String(row.id))
  scheduleForm.selectedInvigilators = assignments.map((a: any) => String(a.teacherUserId))

  // 刷新老师列表以确保下拉框有数据
  refetchTeachers()
  showScheduleDialog.value = true
}

// === 核心逻辑修改：handleSubmitSchedule ===
// 保存排考后，再调用监考分配API
const handleSubmitSchedule = async () => {
  if(!scheduleForm.sessionId || !scheduleForm.subjectId || !scheduleForm.roomId) return ElMessage.warning('请补全考试、场次和教室信息')

  scheduleSubmitting.value = true

  try {
    const scheduleInput = {
      sessionId: scheduleForm.sessionId,
      subjectId: scheduleForm.subjectId,
      roomId: scheduleForm.roomId,
      note: scheduleForm.note || ''
    }

    let scheduleId: string
    if (isEditSchedule.value) {
        await updateScheduleMutate({ id: currentScheduleId.value, input: scheduleInput })
        scheduleId = currentScheduleId.value
    } else {
        const result = await createScheduleMutate({ input: scheduleInput })
        scheduleId = (result?.data as any)?.createExamSchedule?.id
    }

    // 如果选择了监考老师，调用监考分配API
    if (scheduleId && scheduleForm.selectedInvigilators.length > 0) {
      try {
        await assignInvigilatorsMutate({
          input: {
            scheduleId: scheduleId,
            teacherUserIds: scheduleForm.selectedInvigilators,
            replaceExisting: true
          }
        })
      } catch (e: any) {
        console.error('分配监考失败:', e.message)
        ElMessage.warning('排考保存成功，但监考分配失败: ' + e.message)
      }
    }

    // 自动创建/更新报名窗口（使用当前时间到场次结束时间作为报名时间）
    try {
      const selectedSession = sessions.value.find((s: any) => s.id === scheduleForm.sessionId)
      if (selectedSession) {
        const now = dayjs()
        const sessionEnd = dayjs(selectedSession.endTime)
        await upsertWindow({
          input: {
            subjectId: scheduleForm.subjectId,
            sessionId: scheduleForm.sessionId,
            startTime: now.format('YYYY-MM-DDTHH:mm:ssZ'),
            endTime: sessionEnd.format('YYYY-MM-DDTHH:mm:ssZ'),
            note: '自动创建的报名窗口'
          }
        })
      }
    } catch (e: any) {
      console.error('创建报名窗口失败:', e.message)
    }

    ElMessage.success(isEditSchedule.value ? '更新成功' : '创建成功')
    showScheduleDialog.value = false
    refetchSchedules()
    refetchAllAssignments()
  } catch(e:any) {
    ElMessage.error(e.message || '操作失败')
  } finally {
    scheduleSubmitting.value = false
  }
}

const handleDeleteSchedule = async (id: string) => { try { await deleteScheduleMutate({ id }); ElMessage.success('已删除'); refetchSchedules() } catch {} }

// 8. Template Logic
const showTemplateDialog = ref(false)
const isEditTemplate = ref(false)
const templateForm = reactive({ id: '', name: '', logoUrl: '', examNotice: '' })
const openTemplateDialog = () => { isEditTemplate.value = false; Object.assign(templateForm, { id:'', name:'', logoUrl:'', examNotice:'' }); showTemplateDialog.value = true }
const handleEditTemplate = (row: any) => { isEditTemplate.value = true; Object.assign(templateForm, row); showTemplateDialog.value = true }
const handleDeleteTemplate = async (row: any) => { try { await ElMessageBox.confirm('确认删除?'); await deleteTemplateMutate({ id: row.id }); refetchTemplates() } catch {} }
const handleSubmitTemplate = async () => { try { await upsertTemplateMutate({ input: { id: isEditTemplate.value ? templateForm.id : null, name: templateForm.name, logoUrl: templateForm.logoUrl, examNotice: templateForm.examNotice } }); ElMessage.success('模板保存成功'); showTemplateDialog.value = false; refetchTemplates() } catch(e:any) { ElMessage.error(e.message) } }

// Preview Logic
const showPreviewDialog = ref(false)
const previewTemplate = ref<any>(null)
const mockStudent = { name: '李华', studentId: '2024001', department: '演示学院 / 计算机科学与技术', avatar: '' }
const mockExams = [ { subjectName: '高等数学 (A)', startTime: String(dayjs().add(1, 'day').hour(9).minute(0).valueOf()), endTime: String(dayjs().add(1, 'day').hour(11).minute(0).valueOf()), roomName: '第一教学楼 101', seatNumber: '01' }, { subjectName: '大学英语 (IV)', startTime: String(dayjs().add(2, 'day').hour(14).minute(30).valueOf()), endTime: String(dayjs().add(2, 'day').hour(16).minute(30).valueOf()), roomName: '综合实验楼 B205', seatNumber: '12' } ]
const handlePreviewTemplate = (row: any) => { previewTemplate.value = row; showPreviewDialog.value = true }
const triggerPrint = () => { window.print() }

</script>

<style scoped>
.scheduling-container { padding: 20px; background: #fff; }
.operation-bar { margin-bottom: 15px; display: flex; align-items: center; justify-content: space-between; }
/* New Toolbar Styles */
.toolbar { display: flex; justify-content: space-between; margin-bottom: 20px; flex-wrap: wrap; gap: 10px; }
.filters { display: flex; align-items: center; }
.sub-text { font-size: 12px; color: #909399; margin-top: 4px; }
.text-gray { color: #dcdfe6; }
/* Seat Styles */
.seat-control-panel { padding: 15px; background: #f5f7fa; border-radius: 4px; margin-bottom: 20px; }
.seat-stats-card { background: #fff; padding: 20px; border: 1px solid #ebeef5; border-radius: 4px; text-align: center; }
.stat-item .label { color: #909399; font-size: 14px; margin-bottom: 8px; }
.stat-item .value { font-size: 24px; font-weight: bold; color: #303133; }
/* Preview Styles */
.preview-container { background: #f0f2f5; padding: 20px; max-height: 70vh; overflow-y: auto; display: flex; justify-content: center; }
/* Quick Notify Styles */
.preview-box { background: #f4f4f5; padding: 15px; border-radius: 4px; margin-top: 10px; border: 1px dashed #dcdfe6; }
.preview-box p { margin: 5px 0; font-size: 13px; color: #606266; line-height: 1.5; }
.tips { font-size: 12px; color: #909399; line-height: 1.2; margin-top: 4px; }
</style>