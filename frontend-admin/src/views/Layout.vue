<template>
  <el-container class="layout-container">
    <el-aside :width="isCollapse ? '64px' : '220px'" class="aside">
      <div class="logo">
        <el-icon :size="22"><HomeFilled /></el-icon>
        <span v-show="!isCollapse">宿舍管理系统</span>
      </div>
      <el-menu 
        :default-active="route.path" 
        router 
        :collapse="isCollapse"
        :collapse-transition="false"
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
      >
        <el-menu-item index="/dashboard">
          <el-icon><DataAnalysis /></el-icon>
          <template #title>数据概览</template>
        </el-menu-item>
        <el-menu-item v-if="userInfo?.role === 1" index="/users">
          <el-icon><UserFilled /></el-icon>
          <template #title>用户管理</template>
        </el-menu-item>
        <el-menu-item v-if="userInfo?.role === 1" index="/buildings">
          <el-icon><OfficeBuilding /></el-icon>
          <template #title>楼栋管理</template>
        </el-menu-item>
        <el-menu-item v-if="[1,2].includes(userInfo?.role)" index="/rooms">
          <el-icon><House /></el-icon>
          <template #title>房间管理</template>
        </el-menu-item>
        <el-menu-item v-if="[1,2].includes(userInfo?.role)" index="/students">
          <el-icon><Avatar /></el-icon>
          <template #title>学生管理</template>
        </el-menu-item>
        <el-menu-item v-if="[1,2].includes(userInfo?.role)" index="/batch-allocation">
          <el-icon><SetUp /></el-icon>
          <template #title>批量分配</template>
        </el-menu-item>
        <el-menu-item index="/repairs">
          <el-icon><Tools /></el-icon>
          <template #title>维修管理</template>
        </el-menu-item>
        <el-menu-item v-if="[1,2].includes(userInfo?.role)" index="/visitors">
          <el-icon><Tickets /></el-icon>
          <template #title>访客管理</template>
        </el-menu-item>
        <el-menu-item index="/announcements">
          <el-icon><Bell /></el-icon>
          <template #title>公告管理</template>
        </el-menu-item>
        <el-menu-item v-if="userInfo?.role === 1" index="/logs">
          <el-icon><Document /></el-icon>
          <template #title>操作日志</template>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <div class="header-left">
          <el-icon class="collapse-btn" @click="isCollapse = !isCollapse">
            <Expand v-if="isCollapse" />
            <Fold v-else />
          </el-icon>
        </div>
        <div class="header-right">
          <span class="username">{{ userInfo?.realName || userInfo?.username }}</span>
          <el-dropdown @command="handleCommand">
            <el-icon class="dropdown-icon"><ArrowDown /></el-icon>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="password">修改密码</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
  
  <el-dialog v-model="pwdVisible" title="修改密码" width="400px">
    <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="80px">
      <el-form-item label="原密码" prop="oldPassword">
        <el-input v-model="pwdForm.oldPassword" type="password" show-password />
      </el-form-item>
      <el-form-item label="新密码" prop="newPassword">
        <el-input v-model="pwdForm.newPassword" type="password" show-password />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="pwdVisible = false">取消</el-button>
      <el-button type="primary" @click="handleChangePwd">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { authApi } from '../api'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const userInfo = computed(() => userStore.userInfo)
const isCollapse = ref(false)
const pwdVisible = ref(false)
const pwdFormRef = ref()
const pwdForm = reactive({ oldPassword: '', newPassword: '' })
const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [{ required: true, message: '请输入新密码', trigger: 'blur' }, { min: 6, message: '密码至少6位', trigger: 'blur' }]
}

onMounted(() => { userStore.getInfo() })

const handleCommand = (cmd) => {
  if (cmd === 'logout') {
    userStore.logout()
    router.push('/login')
  } else if (cmd === 'password') {
    pwdForm.oldPassword = ''
    pwdForm.newPassword = ''
    pwdVisible.value = true
  }
}

const handleChangePwd = async () => {
  await pwdFormRef.value.validate()
  await authApi.changePassword(pwdForm)
  ElMessage.success('密码修改成功，请重新登录')
  pwdVisible.value = false
  userStore.logout()
  router.push('/login')
}
</script>

<style lang="scss" scoped>
.layout-container {
  height: 100vh;
}

.aside {
  background: #304156;
  transition: width 0.3s;
  overflow: hidden;
  
  :deep(.el-menu) {
    border-right: none;
  }
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: #fff;
  font-size: 16px;
  font-weight: bold;
  background: #263445;
  white-space: nowrap;
  overflow: hidden;
}

.header {
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}

.header-left {
  display: flex;
  align-items: center;
}

.collapse-btn {
  font-size: 20px;
  cursor: pointer;
  color: #606266;
  
  &:hover {
    color: #409EFF;
  }
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.username {
  color: #606266;
  font-size: 14px;
}

.dropdown-icon {
  cursor: pointer;
  font-size: 14px;
  color: #606266;
}

.main {
  background: #f5f7fa;
}
</style>
