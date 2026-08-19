<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Delete, Edit, Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getUsers, updateUser, deleteUser } from '../../api/admin'
import type { UserResponse } from '../../api/types'

const users = ref<UserResponse[]>([])
const totalElements = ref(0)
const currentPage = ref(0)
const showDialog = ref(false)
const editMode = ref(false)
const searchKeyword = ref('')

interface UserForm {
  id: number
  username: string
  email: string
  nickname: string
  role: string
  enabled: boolean
}

const form = ref<UserForm>({
  id: 0,
  username: '',
  email: '',
  nickname: '',
  role: 'USER',
  enabled: true
})

const loadUsers = async (page = 0) => {
  try {
    const response = await getUsers(page)
    if (response.code === 200) {
      users.value = response.data.content
      totalElements.value = response.data.totalElements
    }
  } catch (error) {
    console.error('加载用户失败', error)
  }
}

const handleEdit = (user: UserResponse) => {
  editMode.value = true
  form.value = {
    id: user.id,
    username: user.username,
    email: user.email,
    nickname: user.nickname,
    role: user.role,
    enabled: true
  }
  showDialog.value = true
}

const filteredUsers = computed(() => {
  const keyword = searchKeyword.value.trim().toLowerCase()
  if (!keyword) return users.value
  return users.value.filter((user) => [user.username, user.email, user.nickname].some((value) => value?.toLowerCase().includes(keyword)))
})

const handleSave = async () => {
  try {
    if (editMode.value) {
      const response = await updateUser(form.value.id, {
        username: form.value.username,
        email: form.value.email,
        nickname: form.value.nickname,
        role: form.value.role,
        enabled: form.value.enabled
      })
      if (response.code === 200) {
        ElMessage.success('用户更新成功')
      }
    }
    showDialog.value = false
    loadUsers(currentPage.value)
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

const handleDelete = async (id: number) => {
  if (!confirm('确定要删除该用户吗？')) return
  try {
    const response = await deleteUser(id)
    if (response.code === 200) {
      ElMessage.success('删除成功')
      loadUsers(currentPage.value)
    }
  } catch (error) {
    ElMessage.error('删除失败')
  }
}

const handlePageChange = (page: number) => {
  currentPage.value = page - 1
  loadUsers(page - 1)
}

const formatDate = (timestamp: number) => {
  return new Date(timestamp).toLocaleString('zh-CN')
}

const getRoleType = (role: string) => {
  return role === 'ADMIN' ? 'warning' : 'success'
}

const getRoleText = (role: string) => {
  return role === 'ADMIN' ? '管理员' : '普通用户'
}

onMounted(() => {
  loadUsers()
})
</script>

<template>
  <div class="user-management">
    <div class="page-header">
      <h2 class="page-title">用户管理</h2>
      <p class="page-subtitle">管理系统用户账户</p>
    </div>

    <el-card class="content-card">
      <div class="card-header">
        <el-input 
          v-model="searchKeyword" 
          placeholder="搜索用户名或邮箱..." 
          class="search-input"
          :prefix-icon="Search"
        />
      </div>

      <el-table :data="filteredUsers" border="false" class="data-table">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" min-width="120">
          <template #default="scope">
            <span class="username-cell">{{ scope.row.username }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="email" label="邮箱" min-width="180" />
        <el-table-column prop="nickname" label="昵称" width="120" />
        <el-table-column prop="role" label="角色" width="100">
          <template #default="scope">
            <el-tag :type="getRoleType(scope.row.role)" size="small">
              {{ getRoleText(scope.row.role) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180">
          <template #default="scope">
            {{ formatDate(scope.row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160">
          <template #default="scope">
            <div class="action-buttons">
              <el-button size="small" @click="handleEdit(scope.row as unknown as UserResponse)">
                <Edit /> 编辑
              </el-button>
              <el-button size="small" type="danger" @click="handleDelete((scope.row as unknown as UserResponse).id)">
                <Delete /> 删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          :current-page="currentPage + 1"
          :page-size="10"
          :total="totalElements"
          layout="prev, pager, next"
          @current-change="handlePageChange"
          class="pagination"
        />
      </div>
    </el-card>

    <el-dialog v-model="showDialog" title="编辑用户" width="500px">
      <el-form :model="form" label-width="100px" class="form">
        <el-form-item label="用户名">
          <el-input v-model="form.username" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="form.nickname" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.role" class="role-select">
            <el-option label="普通用户" value="USER" />
            <el-option label="管理员" value="ADMIN" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.enabled" active-text="启用" inactive-text="禁用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSave">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.user-management {
  padding: 0;
}

.page-header {
  margin-bottom: 24px;
}

.page-title {
  font-size: 24px;
  font-weight: bold;
  color: var(--color-text);
  margin: 0 0 8px 0;
}

.page-subtitle {
  font-size: 14px;
  color: var(--color-text-secondary);
  margin: 0;
}

.content-card {
  border-radius: 12px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--color-border);
  margin-bottom: 16px;
}

.search-input {
  width: 300px;
}

.add-btn {
  display: flex;
  align-items: center;
  gap: 6px;
}

.data-table {
  width: 100%;
}

.username-cell {
  color: var(--color-text);
  font-weight: 500;
}

.username-cell:hover {
  color: var(--color-brand);
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  padding-top: 16px;
  border-top: 1px solid var(--color-border);
}

.role-select {
  width: 100%;
}

.form {
  padding: 16px 0;
}

:deep(.el-table) {
  border: none;
}

:deep(.el-table th) {
  background-color: var(--color-surface-subtle);
  color: var(--color-text-secondary);
  font-weight: 500;
  padding: 12px 16px;
}

:deep(.el-table td) {
  padding: 12px 16px;
}

:deep(.el-table tr:hover) {
  background-color: var(--color-surface-subtle);
}

:deep(.el-table::before) {
  display: none;
}

.action-buttons {
  display: flex;
  gap: 8px;
}
</style>
