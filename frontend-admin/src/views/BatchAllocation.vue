<template>
  <div class="page-container">
    <el-card class="upload-card">
      <template #header>
        <div class="card-header">
          <span>新生批量导入</span>
          <div style="display: flex; gap: 8px;">
            <el-button type="success" :icon="Download" @click="downloadTemplate">下载模板</el-button>
            <el-button type="info" :icon="Document" @click="downloadTestData">下载测试数据(20条)</el-button>
          </div>
        </div>
      </template>
      <el-upload
        class="upload-area"
        drag
        :show-file-list
        :auto-upload="false"
        :on-change="handleFileChange"
        accept=".xlsx,.xls"
        :disabled="importLoading"
      >
        <el-icon class="upload-icon"><UploadFilled /></el-icon>
        <div class="el-upload__text">将 Excel 文件拖到此处，或 <em>点击上传</em></div>
        <template #tip>
          <div class="el-upload__tip">
            仅支持 .xlsx / .xls 格式文件，字段包含：学号、姓名、性别、专业、手机号、作息偏好、是否吸烟
          </div>
        </template>
      </el-upload>
      <el-button 
        v-if="selectedFile" 
        type="primary" 
        class="import-btn" 
        :loading="importLoading"
        @click="handleImport"
      >
        开始导入
      </el-button>
    </el-card>

    <el-card v-if="importResult" class="result-card">
      <template #header>
        <div class="card-header">
          <span>导入结果</span>
          <div>
            <el-tag type="success">成功 {{ importResult.successCount }} 条</el-tag>
            <el-tag type="danger" style="margin-left: 8px">失败 {{ importResult.errorCount }} 条</el-tag>
          </div>
        </div>
      </template>
      
      <el-tabs v-if="importResult.errors.length > 0">
        <el-tab-pane label="错误数据" name="errors">
          <el-alert type="error" title="以下数据存在错误，请修正后重新导入" :closable="false" style="margin-bottom: 16px" />
          <el-table :data="importResult.errors" stripe border>
            <el-table-column prop="rowNum" label="行号" width="80" align="center" />
            <el-table-column prop="studentNo" label="学号" width="120" />
            <el-table-column prop="name" label="姓名" width="100" />
            <el-table-column prop="errorMessage" label="错误原因" min-width="200" />
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="待分配学生" name="pending">
          <div class="action-bar">
            <el-button type="primary" :icon="MagicStick" @click="startAllocation" :loading="allocationLoading">
              开始智能分配
            </el-button>
            <el-button :icon="Delete" @click="handleClear">清空待分配</el-button>
          </div>
          <el-table :data="importResult.pendingStudents" stripe border>
            <el-table-column prop="studentNo" label="学号" width="120" />
            <el-table-column prop="name" label="姓名" width="100" />
            <el-table-column prop="gender" label="性别" width="80" align="center">
              <template #default="{ row }">
                <el-tag :type="row.gender === '男' ? 'primary' : 'danger'" size="small">{{ row.gender }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="major" label="专业" min-width="120" />
            <el-table-column prop="phone" label="手机号" width="130" />
            <el-table-column prop="sleepPreference" label="作息偏好" width="100" align="center">
              <template #default="{ row }">
                <el-tag type="info" size="small">{{ row.sleepPreference || '-' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="smoking" label="是否吸烟" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="row.smoking === '是' ? 'warning' : 'success'" size="small">{{ row.smoking }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
      
      <div v-else class="action-bar">
        <el-button type="primary" :icon="MagicStick" @click="startAllocation" :loading="allocationLoading">
          开始智能分配
        </el-button>
        <el-button :icon="Delete" @click="handleClear">清空待分配</el-button>
      </div>
      <el-table v-if="importResult.errors.length === 0" :data="importResult.pendingStudents" stripe border>
        <el-table-column prop="studentNo" label="学号" width="120" />
        <el-table-column prop="name" label="姓名" width="100" />
        <el-table-column prop="gender" label="性别" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.gender === '男' ? 'primary' : 'danger'" size="small">{{ row.gender }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="major" label="专业" min-width="120" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="sleepPreference" label="作息偏好" width="100" align="center">
          <template #default="{ row }">
            <el-tag type="info" size="small">{{ row.sleepPreference || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="smoking" label="是否吸烟" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.smoking === '是' ? 'warning' : 'success'" size="small">{{ row.smoking }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card v-if="allocationResult" class="allocation-card">
      <template #header>
        <div class="card-header">
          <span>分配结果预览</span>
          <div>
            <el-tag type="success">已分配 {{ allocationResult.allocatedCount }} 人</el-tag>
            <el-tag type="warning" style="margin-left: 8px">未分配 {{ allocationResult.unallocatedCount }} 人</el-tag>
            <el-button type="success" style="margin-left: 16px" :icon="Check" @click="confirmAllocation" :loading="confirmLoading">
              确认入住
            </el-button>
          </div>
        </div>
      </template>

      <el-alert 
        v-if="allocationResult.unallocatedStudents.length > 0"
        type="warning" 
        title="以下学生未能分配到床位，请检查是否有足够的空余床位"
        :closable="false"
        style="margin-bottom: 16px"
      />

      <el-collapse accordion>
        <el-collapse-item 
          v-for="building in allocationResult.buildings" 
          :key="building.id" 
          :name="building.id"
        >
          <template #title>
            <span class="building-title">
              <el-icon><OfficeBuilding /></el-icon>
              {{ building.name }} ({{ building.gender }})
            </span>
          </template>
          
          <el-collapse accordion>
            <el-collapse-item 
              v-for="floor in building.floors" 
              :key="floor.floorNumber" 
              :name="floor.floorNumber"
            >
              <template #title>
                <span class="floor-title">
                  <el-icon><Grid /></el-icon>
                  {{ floor.floorName }}
                </span>
              </template>
              
              <div class="rooms-container">
                <el-card 
                  v-for="room in floor.rooms" 
                  :key="room.id" 
                  class="room-card"
                  :class="{ 'room-has-student': room.currentCount > 0 }"
                >
                  <div class="room-header">
                    <span class="room-number">{{ room.roomNumber }}室</span>
                    <el-tag size="small">{{ room.currentCount }}/{{ room.capacity }}</el-tag>
                  </div>
                  <div class="beds-list">
                    <div 
                      v-for="bed in room.beds" 
                      :key="bed.id" 
                      class="bed-item"
                      :class="{ 'bed-occupied': bed.occupied }"
                    >
                      <div class="bed-number">{{ bed.bedNumber }}床</div>
                      <div v-if="bed.student" class="student-info">
                        <div class="student-name">{{ bed.student.name }}</div>
                        <div class="student-major">{{ bed.student.major }}</div>
                        <el-tag v-if="bed.student.matchReason" type="info" size="small" class="match-reason">
                          {{ bed.student.matchReason }}
                        </el-tag>
                      </div>
                      <div v-else-if="bed.occupied" class="bed-occupied-text">已占用</div>
                      <div v-else class="bed-empty-text">空床位</div>
                    </div>
                  </div>
                </el-card>
              </div>
            </el-collapse-item>
          </el-collapse>
        </el-collapse-item>
      </el-collapse>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { batchAllocationApi } from '../api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  Download, UploadFilled, MagicStick, Delete, Check, OfficeBuilding, Grid, Document
} from '@element-plus/icons-vue'

const selectedFile = ref(null)
const importLoading = ref(false)
const allocationLoading = ref(false)
const confirmLoading = ref(false)
const importResult = ref(null)
const allocationResult = ref(null)
const currentBatchNo = ref('')

const handleFileChange = (file) => {
  selectedFile.value = file.raw
}

const downloadTemplate = () => {
  batchAllocationApi.downloadTemplate()
}

const downloadTestData = () => {
  batchAllocationApi.downloadTestData()
}

const handleImport = async () => {
  if (!selectedFile.value) {
    ElMessage.warning('请先选择文件')
    return
  }
  importLoading.value = true
  try {
    const res = await batchAllocationApi.importStudents(selectedFile.value)
    importResult.value = res
    currentBatchNo.value = res.batchNo
    allocationResult.value = null
    ElMessage.success('导入完成')
  } finally {
    importLoading.value = false
  }
}

const startAllocation = async () => {
  if (!currentBatchNo.value) {
    ElMessage.warning('请先导入数据')
    return
  }
  allocationLoading.value = true
  try {
    const res = await batchAllocationApi.startAllocation(currentBatchNo.value)
    allocationResult.value = res
    ElMessage.success('分配完成')
  } finally {
    allocationLoading.value = false
  }
}

const confirmAllocation = async () => {
  try {
    await ElMessageBox.confirm(
      '确认后将正式写入学生和床位数据，确定要执行吗？',
      '确认入住',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    const studentIds = []
    allocationResult.value.buildings.forEach(building => {
      building.floors.forEach(floor => {
        floor.rooms.forEach(room => {
          room.beds.forEach(bed => {
            if (bed.student) {
              studentIds.push(bed.student.id)
            }
          })
        })
      })
    })

    confirmLoading.value = true
    await batchAllocationApi.confirmAllocation(currentBatchNo.value, studentIds)
    ElMessage.success('确认入住成功')
    importResult.value = null
    allocationResult.value = null
    currentBatchNo.value = ''
    selectedFile.value = null
  } catch {
    // 用户取消
  } finally {
    confirmLoading.value = false
  }
}

const handleClear = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要清空待分配数据吗？',
      '提示',
      { type: 'warning' }
    )
    await batchAllocationApi.clearPending(currentBatchNo.value)
    importResult.value = null
    allocationResult.value = null
    currentBatchNo.value = ''
    selectedFile.value = null
    ElMessage.success('清空成功')
  } catch {
    // 用户取消
  }
}
</script>

<style lang="scss" scoped>
.page-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.upload-area {
  :deep(.el-upload-dragger) {
    width: 100%;
  }
  
  .upload-icon {
    font-size: 67px;
    color: #409EFF;
  }
}

.import-btn {
  margin-top: 16px;
  width: 100%;
}

.result-card {
  .action-bar {
    margin-bottom: 16px;
  }
}

.allocation-card {
  .building-title {
    display: flex;
    align-items: center;
    gap: 8px;
    font-weight: 500;
  }
  
  .floor-title {
    display: flex;
    align-items: center;
    gap: 8px;
  }
  
  .rooms-container {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
    gap: 16px;
    padding: 16px 0;
  }
  
  .room-card {
    transition: all 0.3s;
    
    &.room-has-student {
      border-color: #409EFF;
    }
    
    .room-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 12px;
      padding-bottom: 8px;
      border-bottom: 1px solid #eee;
      
      .room-number {
        font-weight: 600;
        font-size: 16px;
      }
    }
    
    .beds-list {
      display: flex;
      flex-direction: column;
      gap: 8px;
    }
    
    .bed-item {
      padding: 8px;
      border-radius: 4px;
      background: #f5f7fa;
      border: 1px solid #e4e7ed;
      
      &.bed-occupied {
        background: #ecf5ff;
        border-color: #b3d8ff;
      }
      
      .bed-number {
        font-size: 12px;
        color: #909399;
        margin-bottom: 4px;
      }
      
      .student-info {
        .student-name {
          font-weight: 500;
          font-size: 14px;
        }
        
        .student-major {
          font-size: 12px;
          color: #606266;
          margin: 2px 0;
        }
        
        .match-reason {
          margin-top: 4px;
        }
      }
      
      .bed-occupied-text,
      .bed-empty-text {
        font-size: 12px;
        color: #909399;
      }
      
      .bed-occupied-text {
        color: #e6a23c;
      }
    }
  }
}
</style>
