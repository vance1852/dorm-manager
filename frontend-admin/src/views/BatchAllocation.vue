<template>
  <div class="page-container">
    <el-card class="search-card">
      <div class="step-header">
        <el-steps :active="currentStep" align-center finish-status="success">
          <el-step title="导入数据" description="上传Excel文件" />
          <el-step title="智能分配" description="自动匹配床位" />
          <el-step title="确认入住" description="审核并确认" />
        </el-steps>
      </div>
    </el-card>

    <el-card v-if="currentStep === 0" class="table-card">
      <template #header>
        <div class="card-header">
          <span>第一步：导入新生数据</span>
        </div>
      </template>
      <div class="upload-section">
        <div class="upload-actions">
          <el-button type="primary" @click="downloadTemplate" :icon="Download">下载导入模板</el-button>
          <el-button type="warning" @click="downloadTestData" :icon="Download">下载测试数据(20条)</el-button>
          <el-upload
            ref="uploadRef"
            class="upload-inline"
            :auto-upload="false"
            :limit="1"
            accept=".xlsx,.xls"
            :on-change="handleFileChange"
            :on-exceed="handleExceed"
            :file-list="fileList"
            :show-file-list="false"
          >
            <el-button type="success" :icon="Upload">选择Excel文件</el-button>
          </el-upload>
          <el-button type="primary" @click="handleImport" :loading="importLoading" :disabled="!selectedFile" :icon="UploadFilled">
            开始导入
          </el-button>
        </div>
        <div v-if="selectedFile" class="file-info">
          <el-icon><Document /></el-icon>
          <span>{{ selectedFile.name }}</span>
          <el-button type="danger" link @click="clearFile">移除</el-button>
        </div>
        <el-alert type="info" :closable="false" style="margin-top: 12px">
          模板字段说明：学号（必填）、姓名（必填）、性别（必填，男/女）、专业（必填）、手机号、作息偏好（必填，早睡型/晚睡型）、是否吸烟（必填，是/否）
        </el-alert>
      </div>

      <div v-if="importResult" style="margin-top: 20px">
        <el-row :gutter="20" style="margin-bottom: 16px">
          <el-col :span="8">
            <el-statistic title="总行数" :value="importResult.totalCount" />
          </el-col>
          <el-col :span="8">
            <el-statistic title="校验通过" :value="importResult.successCount" style="color: #67c23a" />
          </el-col>
          <el-col :span="8">
            <el-statistic title="校验失败" :value="importResult.failCount" style="color: #f56c6c" />
          </el-col>
        </el-row>

        <el-tabs v-model="importTab">
          <el-tab-pane label="校验通过" name="success">
            <el-table :data="importResult.successList" stripe max-height="400" border>
              <el-table-column prop="rowNum" label="行号" width="70" align="center" />
              <el-table-column prop="studentNo" label="学号" width="130" />
              <el-table-column prop="name" label="姓名" width="100" />
              <el-table-column prop="genderStr" label="性别" width="80" align="center" />
              <el-table-column prop="major" label="专业" min-width="120" show-overflow-tooltip />
              <el-table-column prop="phone" label="手机号" width="130" />
              <el-table-column prop="schedulePreferenceStr" label="作息偏好" width="100" align="center" />
              <el-table-column prop="smokingStr" label="是否吸烟" width="90" align="center" />
            </el-table>
          </el-tab-pane>
          <el-tab-pane v-if="importResult.failCount > 0" label="校验失败" name="fail">
            <el-table :data="importResult.failList" stripe max-height="400" border>
              <el-table-column prop="rowNum" label="行号" width="70" align="center" />
              <el-table-column prop="studentNo" label="学号" width="130" />
              <el-table-column prop="name" label="姓名" width="100" />
              <el-table-column prop="genderStr" label="性别" width="80" align="center" />
              <el-table-column prop="major" label="专业" min-width="120" show-overflow-tooltip />
              <el-table-column prop="errorMsg" label="错误信息" min-width="200">
                <template #default="{ row }">
                  <span style="color: #f56c6c">{{ row.errorMsg }}</span>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
        </el-tabs>

        <div style="margin-top: 16px; text-align: right" v-if="importResult.successCount > 0">
          <el-button type="primary" @click="goToStep(1)" :icon="Right">下一步：智能分配</el-button>
        </div>
      </div>
    </el-card>

    <el-card v-if="currentStep === 1" class="table-card">
      <template #header>
        <div class="card-header">
          <span>第二步：智能分配</span>
          <div>
            <el-button @click="goToStep(0)">上一步</el-button>
            <el-button type="primary" @click="handleAllocate" :loading="allocateLoading" :icon="MagicStick">开始分配</el-button>
          </div>
        </div>
      </template>

      <el-alert v-if="!allocationPreview" type="warning" :closable="false" style="margin-bottom: 16px">
        点击"开始分配"按钮，系统将根据以下规则自动匹配床位：<br/>
        硬约束：男女必须分开（男生只能分到男生楼、女生只能分到女生楼）、不能超过房间床位容量<br/>
        软约束：同专业的学生优先分配到同一楼层、作息偏好相同的优先分配到同一房间、吸烟学生集中分配到每栋楼的指定楼层（顶层）
      </el-alert>

      <div v-if="allocationPreview">
        <el-row :gutter="20" style="margin-bottom: 16px">
          <el-col :span="8">
            <el-statistic title="已分配" :value="allocationPreview.totalAllocated" style="color: #67c23a" />
          </el-col>
          <el-col :span="8">
            <el-statistic title="未分配" :value="allocationPreview.totalUnallocated" style="color: #f56c6c" />
          </el-col>
        </el-row>

        <div v-if="allocationPreview.unallocatedStudents && allocationPreview.unallocatedStudents.length > 0" style="margin-bottom: 16px">
          <el-alert type="warning" :closable="false">
            <template #title>以下学生未能分配床位</template>
            <div v-for="u in allocationPreview.unallocatedStudents" :key="u.studentNo" style="margin-top: 4px">
              {{ u.studentNo }} {{ u.name }} - {{ u.reason }}
            </div>
          </el-alert>
        </div>

        <div class="allocation-tree">
          <el-collapse v-model="expandedBuildings">
            <el-collapse-item v-for="building in allocationPreview.buildings" :key="building.buildingId" :name="'b_' + building.buildingId">
              <template #title>
                <div class="building-title">
                  <el-icon><OfficeBuilding /></el-icon>
                  <span>{{ building.buildingName }}</span>
                  <el-tag :type="building.gender === 1 ? 'primary' : 'danger'" size="small" style="margin-left: 8px">
                    {{ building.gender === 1 ? '男生宿舍' : '女生宿舍' }}
                  </el-tag>
                </div>
              </template>

              <el-collapse v-model="expandedFloors" style="margin-left: 24px">
                <el-collapse-item v-for="floor in building.floors" :key="'f_' + building.buildingId + '_' + floor.floor" :name="'f_' + building.buildingId + '_' + floor.floor">
                  <template #title>
                    <div class="floor-title">
                      <el-icon><House /></el-icon>
                      <span>{{ floor.floor }}层</span>
                    </div>
                  </template>

                  <div class="room-grid" style="margin-left: 24px">
                    <el-card v-for="room in floor.rooms" :key="room.roomId" class="room-card" shadow="hover">
                      <template #header>
                        <div class="room-header">
                          <span>{{ room.roomNumber }}室</span>
                          <el-tag size="small" type="info">{{ room.currentCount || 0 }}/{{ room.capacity }}</el-tag>
                        </div>
                      </template>
                      <div class="bed-list">
                        <div v-for="bed in room.beds" :key="bed.bedId"
                             class="bed-item"
                             :class="{ 'bed-occupied': bed.studentName, 'bed-empty': !bed.studentName, 'bed-drag-over': dragOverBedId === bed.bedId }"
                             :draggable="bed.studentName ? 'true' : 'false'"
                          @dragstart="handleDragStart($event, bed)"
                          @dragover.prevent="handleDragOver($event, bed)"
                          @dragleave="handleDragLeave($event, bed)"
                          @drop="handleDrop($event, bed, room)">
                          <div class="bed-number">{{ bed.bedNumber }}床</div>
                          <div v-if="bed.studentName" class="bed-student">
                            <span class="student-name">{{ bed.studentName }}</span>
                            <span class="student-no">({{ bed.studentNo }})</span>
                          </div>
                          <div v-else class="bed-empty-text">空闲</div>
                          <div v-if="bed.matchReason" class="match-reason">
                            <el-tag size="small" type="success" effect="plain">{{ bed.matchReason }}</el-tag>
                          </div>
                        </div>
                      </div>
                    </el-card>
                  </div>
                </el-collapse-item>
              </el-collapse>
            </el-collapse-item>
          </el-collapse>
        </div>

        <div style="margin-top: 16px; text-align: right">
          <el-button @click="goToStep(0)">上一步</el-button>
          <el-button type="primary" @click="goToStep(2)" :disabled="allocationPreview.totalAllocated === 0" :icon="Right">下一步：确认入住</el-button>
        </div>
      </div>
    </el-card>

    <el-card v-if="currentStep === 2" class="table-card">
      <template #header>
        <div class="card-header">
          <span>第三步：确认入住</span>
        </div>
      </template>

      <el-alert type="warning" :closable="false" style="margin-bottom: 16px">
        请仔细核对分配方案，确认无误后点击"确认入住"按钮。此操作将把学生和床位信息写入数据库，不可撤销！
      </el-alert>

      <el-descriptions :column="2" border style="margin-bottom: 16px">
        <el-descriptions-item label="已分配学生数">{{ allocationPreview?.totalAllocated || 0 }}</el-descriptions-item>
        <el-descriptions-item label="未分配学生数">{{ allocationPreview?.totalUnallocated || 0 }}</el-descriptions-item>
      </el-descriptions>

      <div style="text-align: center; padding: 20px 0">
        <el-button @click="goToStep(1)">返回调整</el-button>
        <el-button type="danger" size="large" @click="handleConfirm" :loading="confirmLoading" :icon="CircleCheck">
          确认入住
        </el-button>
      </div>
    </el-card>

    <el-dialog v-model="swapDialogVisible" title="调整学生床位" width="500px" :close-on-click-modal="false">
      <el-form label-width="100px">
        <el-form-item label="学生1">
          <span>{{ swapInfo.student1Name }} ({{ swapInfo.student1No }}) → {{ swapInfo.bed1Number }}床</span>
        </el-form-item>
        <el-form-item label="学生2">
          <span>{{ swapInfo.student2Name }} ({{ swapInfo.student2No }}) → {{ swapInfo.bed2Number }}床</span>
        </el-form-item>
        <el-form-item>
          <el-alert type="info" :closable="false">将交换两名学生的床位位置</el-alert>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="swapDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSwapConfirm" :loading="swapLoading">确认交换</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { batchAllocationApi } from '../api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Download, Upload, UploadFilled, Right, MagicStick, OfficeBuilding, House, CircleCheck, Document } from '@element-plus/icons-vue'

const currentStep = ref(0)
const batchId = ref('')
const selectedFile = ref(null)
const fileList = ref([])
const importLoading = ref(false)
const allocateLoading = ref(false)
const confirmLoading = ref(false)
const swapLoading = ref(false)
const importResult = ref(null)
const importTab = ref('success')
const allocationPreview = ref(null)
const uploadRef = ref()
const expandedBuildings = ref([])
const expandedFloors = ref([])
const swapDialogVisible = ref(false)
const dragSourceBed = ref(null)
const dragOverBedId = ref(null)

const swapInfo = reactive({
  student1Name: '',
  student1No: '',
  bed1Id: null,
  bed1Number: '',
  student2Name: '',
  student2No: '',
  bed2Id: null,
  bed2Number: ''
})

onMounted(async () => {
  const res = await batchAllocationApi.createBatchId()
  batchId.value = res
})

const goToStep = (step) => {
  currentStep.value = step
  if (step === 1 && allocationPreview.value) {
    const buildingIds = allocationPreview.value.buildings.map(b => 'b_' + b.buildingId)
    expandedBuildings.value = buildingIds
  }
}

const downloadTemplate = () => {
  const token = localStorage.getItem('token')
  const url = batchAllocationApi.downloadTemplate()
  const link = document.createElement('a')
  link.href = url + '?token=' + token
  link.download = '新生导入模板.xlsx'
  link.click()
}

const downloadTestData = () => {
  const token = localStorage.getItem('token')
  const url = batchAllocationApi.downloadTestData()
  const link = document.createElement('a')
  link.href = url + '?token=' + token
  link.download = '新生测试数据.xlsx'
  link.click()
}

const handleFileChange = (file) => {
  selectedFile.value = file.raw
  fileList.value = [file]
}

const handleExceed = () => {
  ElMessage.warning('只能上传一个文件，请先移除已选文件')
}

const clearFile = () => {
  selectedFile.value = null
  fileList.value = []
}

const handleImport = async () => {
  if (!selectedFile.value) {
    ElMessage.warning('请先选择Excel文件')
    return
  }
  importLoading.value = true
  try {
    const formData = new FormData()
    formData.append('file', selectedFile.value)
    const res = await batchAllocationApi.importExcel(formData, batchId.value)
    importResult.value = res
    if (res.failCount > 0) {
      importTab.value = 'fail'
    }
    ElMessage.success(`导入完成：成功${res.successCount}条，失败${res.failCount}条`)
  } finally {
    importLoading.value = false
  }
}

const handleAllocate = async () => {
  allocateLoading.value = true
  try {
    const res = await batchAllocationApi.allocate(batchId.value)
    allocationPreview.value = res
    if (res.buildings && res.buildings.length > 0) {
      expandedBuildings.value = res.buildings.map(b => 'b_' + b.buildingId)
    }
    ElMessage.success(`分配完成：已分配${res.totalAllocated}人，未分配${res.totalUnallocated}人`)
  } finally {
    allocateLoading.value = false
  }
}

const handleConfirm = async () => {
  try {
    await ElMessageBox.confirm(
      '确认入住后将把学生和床位信息写入数据库，此操作不可撤销，是否继续？',
      '确认入住',
      { confirmButtonText: '确认入住', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  confirmLoading.value = true
  try {
    await batchAllocationApi.confirm(batchId.value)
    ElMessage.success('入住确认成功！所有学生已分配到指定床位')
    currentStep.value = 0
    importResult.value = null
    allocationPreview.value = null
    selectedFile.value = null
    fileList.value = []
    const res = await batchAllocationApi.createBatchId()
    batchId.value = res
  } finally {
    confirmLoading.value = false
  }
}

const handleDragStart = (event, bed) => {
  if (!bed.studentName) return
  dragSourceBed.value = bed
  event.dataTransfer.effectAllowed = 'move'
  event.dataTransfer.setData('text/plain', bed.bedId.toString())
}

const handleDragOver = (event, bed) => {
  event.preventDefault()
  if (dragSourceBed.value && dragSourceBed.value.bedId !== bed.bedId && bed.studentName) {
    dragOverBedId.value = bed.bedId
    event.dataTransfer.dropEffect = 'move'
  }
}

const handleDragLeave = () => {
  dragOverBedId.value = null
}

const handleDrop = async (event, targetBed) => {
  dragOverBedId.value = null
  if (!dragSourceBed.value || dragSourceBed.value.bedId === targetBed.bedId) return

  if (!targetBed.studentName) {
    ElMessage.warning('只能与已有学生的床位交换')
    return
  }

  swapInfo.student1Name = dragSourceBed.value.studentName
  swapInfo.student1No = dragSourceBed.value.studentNo
  swapInfo.bed1Id = dragSourceBed.value.bedId
  swapInfo.bed1Number = dragSourceBed.value.bedNumber
  swapInfo.student2Name = targetBed.studentName
  swapInfo.student2No = targetBed.studentNo
  swapInfo.bed2Id = targetBed.bedId
  swapInfo.bed2Number = targetBed.bedNumber
  swapDialogVisible.value = true
}

const handleSwapConfirm = async () => {
  swapLoading.value = true
  try {
    const res = await batchAllocationApi.swap(batchId.value, {
      studentId1: null,
      bedId1: swapInfo.bed1Id,
      studentId2: null,
      bedId2: swapInfo.bed2Id
    })
    allocationPreview.value = res
    swapDialogVisible.value = false
    ElMessage.success('床位交换成功')
  } finally {
    swapLoading.value = false
  }
}
</script>

<style scoped>
.step-header {
  padding: 10px 0;
}
.upload-section {
  padding: 10px 0;
}
.upload-actions {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}
.upload-inline {
  display: inline-flex;
  align-items: center;
}
.upload-inline :deep(.el-upload) {
  display: inline-flex;
}
.file-info {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 8px;
  padding: 6px 12px;
  background: #f0f9eb;
  border-radius: 6px;
  font-size: 13px;
  color: #606266;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.building-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 15px;
  font-weight: bold;
}
.floor-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 500;
}
.room-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 12px;
}
.room-card {
  margin-bottom: 0;
}
.room-card :deep(.el-card__header) {
  padding: 8px 12px;
}
.room-card :deep(.el-card__body) {
  padding: 8px 12px;
}
.room-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 500;
}
.bed-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.bed-item {
  padding: 6px 10px;
  border-radius: 6px;
  border: 1px solid #e4e7ed;
  cursor: default;
  transition: all 0.2s;
}
.bed-occupied {
  background: #f0f9eb;
  border-color: #c2e7b0;
  cursor: grab;
}
.bed-occupied:hover {
  border-color: #67c23a;
  box-shadow: 0 1px 4px rgba(103, 194, 58, 0.3);
}
.bed-empty {
  background: #f5f7fa;
}
.bed-drag-over {
  border-color: #409eff !important;
  background: #ecf5ff !important;
}
.bed-number {
  font-size: 12px;
  color: #909399;
}
.bed-student {
  display: flex;
  align-items: center;
  gap: 4px;
}
.student-name {
  font-weight: 500;
  color: #303133;
}
.student-no {
  font-size: 12px;
  color: #909399;
}
.bed-empty-text {
  color: #c0c4cc;
  font-size: 13px;
}
.match-reason {
  margin-top: 2px;
}
:deep(.el-tag) { transition: none !important; }
</style>
