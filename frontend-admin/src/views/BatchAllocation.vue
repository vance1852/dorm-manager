<template>
  <div class="page-container">
    <el-card class="upload-card">
      <template #header>
        <div class="card-header">
          <span>新生批量导入与智能分配</span>
          <div>
            <el-button :icon="Download" @click="downloadTemplate">下载模板</el-button>
            <el-button type="success" :icon="Document" @click="downloadSample">下载示例数据</el-button>
            <el-button type="danger" :icon="Delete" @click="handleClear">清空数据</el-button>
          </div>
        </div>
      </template>
      <el-upload
        class="upload-demo"
        drag
        :action="uploadUrl"
        :headers="uploadHeaders"
        :show-file-list="false"
        :on-success="handleUploadSuccess"
        :on-error="handleUploadError"
        accept=".xlsx,.xls"
      >
        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
        <div class="el-upload__text">
          将 Excel 文件拖到此处，或<em>点击上传</em>
        </div>
        <template #tip>
          <div class="el-upload__tip">
            支持 .xlsx/.xls 格式，包含学号、姓名、性别、专业、手机号、作息偏好、是否吸烟字段
          </div>
        </template>
      </el-upload>
    </el-card>

    <el-card v-if="pendingStudents.length > 0" class="preview-card">
      <template #header>
        <div class="card-header">
          <span>导入结果预览</span>
          <el-button type="primary" :icon="MagicStick" @click="startAllocation" :disabled="validCount === 0">
            开始智能分配 ({{ validCount }}人)
          </el-button>
        </div>
      </template>
      <el-table :data="pendingStudents" stripe v-loading="importLoading">
        <el-table-column prop="rowNum" label="行号" width="60" align="center" />
        <el-table-column prop="studentNo" label="学号" width="120" />
        <el-table-column prop="name" label="姓名" width="100" />
        <el-table-column prop="gender" label="性别" width="80" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.gender === 1" type="primary" size="small">男</el-tag>
            <el-tag v-else-if="row.gender === 2" type="danger" size="small">女</el-tag>
            <el-tag v-else type="info" size="small">未知</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="major" label="专业" min-width="120" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="sleepPreference" label="作息偏好" width="100" />
        <el-table-column prop="isSmoker" label="是否吸烟" width="80" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.isSmoker === 1" type="warning" size="small">是</el-tag>
            <el-tag v-else type="success" size="small">否</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.status === 0" type="success" size="small">待分配</el-tag>
            <el-tag v-else-if="row.status === 1" type="primary" size="small">已分配</el-tag>
            <el-tag v-else type="danger" size="small">校验失败</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="errorMsg" label="错误信息" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.errorMsg" style="color: #f56c6c">{{ row.errorMsg }}</span>
            <span v-else style="color: #67c23a">校验通过</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card v-if="allocationResults.length > 0" class="result-card">
      <template #header>
        <div class="card-header">
          <span>分配结果预览</span>
          <el-button type="success" :icon="Check" @click="confirmAllocation" :disabled="assignedCount === 0">
            确认入住 ({{ assignedCount }}人)
          </el-button>
        </div>
      </template>
      <el-tree :data="treeData" default-expand-all>
        <template #default="{ node, data }">
          <div class="tree-node" v-if="data.type === 'building'">
            <el-icon><OfficeBuilding /></el-icon>
            <span class="node-title">{{ data.name }}</span>
            <el-tag type="info" size="small">{{ data.count }}人</el-tag>
          </div>
          <div class="tree-node" v-else-if="data.type === 'room'">
            <el-icon><House /></el-icon>
            <span class="node-title">{{ data.name }}室</span>
            <el-tag type="info" size="small">{{ data.count }}人</el-tag>
          </div>
          <div class="tree-node student-node" v-else-if="data.type === 'bed'">
            <el-icon><Coin /></el-icon>
            <span class="student-info">
              <strong>{{ data.studentName }}</strong>
              <span class="student-no">({{ data.studentNo }})</span>
            </span>
            <el-tag size="small" class="reason-tag">{{ data.matchReason }}</el-tag>
          </div>
          <div class="tree-node failed-node" v-else-if="data.type === 'failed'">
            <el-icon><Warning /></el-icon>
            <span class="student-info">
              <strong>{{ data.studentName }}</strong>
              <span class="student-no">({{ data.studentNo }})</span>
            </span>
            <el-tag type="danger" size="small">{{ data.matchReason }}</el-tag>
          </div>
        </template>
      </el-tree>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { batchAllocationApi } from '../api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  Download, Document, Delete, UploadFilled, MagicStick, Check,
  OfficeBuilding, House, Coin, Warning
} from '@element-plus/icons-vue'

const uploadUrl = '/api/batch-allocation/import'
const uploadHeaders = {
  Authorization: 'Bearer ' + localStorage.getItem('token')
}

const importLoading = ref(false)
const pendingStudents = ref([])
const allocationResults = ref([])

const validCount = computed(() => pendingStudents.value.filter(s => s.status === 0).length)
const assignedCount = computed(() => allocationResults.value.filter(r => r.bedId).length)

const treeData = computed(() => {
  const buildingMap = new Map()
  const failedList = []

  for (const result of allocationResults.value) {
    if (!result.bedId) {
      failedList.push({
        type: 'failed',
        label: result.studentName,
        studentName: result.studentName,
        studentNo: result.studentNo,
        matchReason: result.matchReason
      })
      continue
    }

    if (!buildingMap.has(result.buildingId)) {
      buildingMap.set(result.buildingId, {
        type: 'building',
        id: result.buildingId,
        label: result.buildingName,
        name: result.buildingName,
        count: 0,
        children: new Map()
      })
    }

    const building = buildingMap.get(result.buildingId)
    building.count++

    if (!building.children.has(result.roomId)) {
      building.children.set(result.roomId, {
        type: 'room',
        id: result.roomId,
        label: result.roomNumber,
        name: result.roomNumber,
        count: 0,
        children: []
      })
    }

    const room = building.children.get(result.roomId)
    room.count++
    room.children.push({
      type: 'bed',
      id: result.bedId,
      label: result.bedNumber + '床 - ' + result.studentName,
      bedNumber: result.bedNumber,
      studentName: result.studentName,
      studentNo: result.studentNo,
      matchReason: result.matchReason
    })
  }

  const result = []
  for (const building of buildingMap.values()) {
    result.push({
      ...building,
      children: Array.from(building.children.values()).map(room => ({
        ...room,
        children: room.children
      }))
    })
  }

  if (failedList.length > 0) {
    result.push({
      type: 'building',
      id: 'failed',
      label: '分配失败',
      name: '分配失败',
      count: failedList.length,
      children: failedList
    })
  }

  return result
})

const handleUploadSuccess = (response) => {
  importLoading.value = false
  if (response.code === 200) {
    pendingStudents.value = response.data
    allocationResults.value = []
    ElMessage.success('导入成功，共 ' + response.data.length + ' 条数据')
  } else {
    ElMessage.error(response.message || '导入失败')
  }
}

const handleUploadError = () => {
  importLoading.value = false
  ElMessage.error('上传失败')
}

const downloadTemplate = async () => {
  const res = await batchAllocationApi.downloadTemplate()
  const url = window.URL.createObjectURL(new Blob([res]))
  const link = document.createElement('a')
  link.href = url
  link.setAttribute('download', '学生导入模板.xlsx')
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

const downloadSample = async () => {
  const res = await batchAllocationApi.downloadSample()
  const url = window.URL.createObjectURL(new Blob([res]))
  const link = document.createElement('a')
  link.href = url
  link.setAttribute('download', '新生导入示例.xlsx')
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

const handleClear = async () => {
  await ElMessageBox.confirm('确定要清空所有待分配数据吗？', '提示', {
    type: 'warning'
  })
  await batchAllocationApi.clear()
  pendingStudents.value = []
  allocationResults.value = []
  ElMessage.success('清空成功')
}

const startAllocation = async () => {
  const res = await batchAllocationApi.allocate()
  allocationResults.value = res
  ElMessage.success('分配完成')
}

const confirmAllocation = async () => {
  await ElMessageBox.confirm('确认后将正式写入数据库，确定要继续吗？', '确认入住', {
    type: 'warning'
  })
  await batchAllocationApi.confirm(allocationResults.value)
  ElMessage.success('确认入住成功')
  pendingStudents.value = []
  allocationResults.value = []
}
</script>

<style scoped>
.upload-card {
  margin-bottom: 20px;
}

.upload-demo {
  text-align: center;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.tree-node {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 0;
}

.node-title {
  font-weight: 500;
}

.student-node {
  padding-left: 20px;
}

.student-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.student-no {
  color: #909399;
  font-size: 12px;
}

.reason-tag {
  margin-left: auto;
}

.failed-node {
  padding-left: 20px;
}
</style>
