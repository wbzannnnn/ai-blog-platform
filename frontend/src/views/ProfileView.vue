<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Camera, EditPen } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import SiteHeader from '@/components/SiteHeader.vue'
import SiteFooter from '@/components/SiteFooter.vue'
import { getProfile, updateProfile, uploadAvatar } from '@/api/user'
import type { UpdateProfileRequest, UserResponse } from '@/api/types'
import { useAuthStore } from '@/stores/auth'
import { getApiErrorMessage } from '@/utils/errors'

const authStore = useAuthStore()
const user = ref<UserResponse | null>(null)
const loading = ref(false)
const saving = ref(false)
const uploading = ref(false)
const fileInput = ref<HTMLInputElement | null>(null)
const editForm = ref<UpdateProfileRequest>({ nickname: '', email: '' })
const isEditing = ref(false)

const syncUser = (nextUser: UserResponse): void => {
  user.value = nextUser
  authStore.setUser(nextUser)
}

const loadProfile = async (): Promise<void> => {
  loading.value = true
  try {
    const response = await getProfile()
    if (response.code === 200 && response.data) {
      syncUser(response.data)
      editForm.value = { nickname: response.data.nickname || '', email: response.data.email || '' }
    }
  } catch (error: unknown) {
    ElMessage.error(getApiErrorMessage(error, '加载个人信息失败'))
  } finally {
    loading.value = false
  }
}

const startEdit = (): void => {
  if (!user.value) return
  editForm.value = { nickname: user.value.nickname || '', email: user.value.email || '' }
  isEditing.value = true
}

const handleSave = async (): Promise<void> => {
  if (!editForm.value.nickname?.trim()) {
    ElMessage.warning('昵称不能为空')
    return
  }
  saving.value = true
  try {
    const response = await updateProfile({
      nickname: editForm.value.nickname.trim(),
      email: editForm.value.email?.trim()
    })
    if (response.code === 200 && response.data) {
      syncUser(response.data)
      isEditing.value = false
      ElMessage.success('个人信息已更新')
    } else {
      ElMessage.error(response.message || '更新失败')
    }
  } catch (error: unknown) {
    ElMessage.error(getApiErrorMessage(error, '更新失败，请稍后重试'))
  } finally {
    saving.value = false
  }
}

const handleAvatarUpload = async (event: Event): Promise<void> => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  if (!file.type.startsWith('image/')) {
    ElMessage.warning('请选择图片文件')
    return
  }
  if (file.size > 2 * 1024 * 1024) {
    ElMessage.warning('图片大小不能超过 2MB')
    return
  }
  uploading.value = true
  try {
    const response = await uploadAvatar(file)
    if (response.code === 200 && response.data) {
      syncUser(response.data)
      ElMessage.success('头像已更新')
    } else {
      ElMessage.error(response.message || '上传失败')
    }
  } catch (error: unknown) {
    ElMessage.error(getApiErrorMessage(error, '头像上传失败'))
  } finally {
    uploading.value = false
    input.value = ''
  }
}

const formatDate = (timestamp: number): string => new Date(timestamp).toLocaleDateString('zh-CN', {
  year: 'numeric', month: 'long', day: 'numeric'
})

onMounted(() => void loadProfile())
</script>

<template>
  <div class="profile-page">
    <SiteHeader />
    <main>
      <div class="page-heading">
        <p>ACCOUNT</p>
        <h1>个人资料</h1>
      </div>

      <div v-if="loading" class="loading-state"><el-skeleton :rows="6" animated /></div>
      <div v-else-if="user" class="profile-layout">
        <aside class="identity-panel">
          <div class="avatar-wrap">
            <el-avatar :size="96" :src="user.avatar" />
            <input
              ref="fileInput"
              class="visually-hidden"
              type="file"
              accept="image/*"
              :disabled="uploading"
              @change="handleAvatarUpload"
            />
            <el-tooltip content="更换头像" placement="bottom">
              <button class="avatar-button" type="button" :disabled="uploading" aria-label="更换头像" @click="fileInput?.click()">
                <el-icon><Camera /></el-icon>
              </button>
            </el-tooltip>
          </div>
          <h2>{{ user.nickname || user.username }}</h2>
          <p>@{{ user.username }}</p>
          <span class="role-label">{{ user.role === 'ADMIN' ? '管理员' : '作者' }}</span>
        </aside>

        <section class="details-panel" aria-labelledby="details-heading">
          <div class="section-heading">
            <h2 id="details-heading">账户信息</h2>
            <el-button v-if="!isEditing" text @click="startEdit"><el-icon><EditPen /></el-icon>编辑</el-button>
          </div>

          <dl v-if="!isEditing" class="details-list">
            <div><dt>昵称</dt><dd>{{ user.nickname || '未设置' }}</dd></div>
            <div><dt>邮箱</dt><dd>{{ user.email || '未设置' }}</dd></div>
            <div><dt>注册时间</dt><dd>{{ formatDate(user.createdAt) }}</dd></div>
            <div><dt>账号角色</dt><dd>{{ user.role === 'ADMIN' ? '管理员' : '普通用户' }}</dd></div>
          </dl>

          <el-form v-else :model="editForm" label-position="top" class="profile-form" @submit.prevent="handleSave">
            <el-form-item label="昵称">
              <el-input v-model="editForm.nickname" maxlength="20" show-word-limit />
            </el-form-item>
            <el-form-item label="邮箱">
              <el-input v-model="editForm.email" type="email" />
            </el-form-item>
            <div class="form-actions">
              <el-button @click="isEditing = false">取消</el-button>
              <el-button native-type="submit" type="primary" :loading="saving">保存</el-button>
            </div>
          </el-form>
        </section>
      </div>
    </main>
    <SiteFooter />
  </div>
</template>

<style scoped>
.profile-page { min-height: 100vh; background: var(--color-canvas); }
main { width: min(calc(100% - var(--space-8)), 900px); min-height: calc(100vh - 180px); margin: 0 auto; padding: var(--space-10) 0 var(--space-16); }
.page-heading { padding-bottom: var(--space-6); border-bottom: 2px solid var(--color-text); }
.page-heading p { margin: 0 0 var(--space-2); color: var(--color-brand); font-size: var(--font-size-xs); font-weight: 700; }
.page-heading h1 { margin: 0; font-family: var(--font-serif); font-size: var(--font-size-2xl); }
.loading-state { max-width: 680px; margin: var(--space-10) auto; }
.profile-layout { display: grid; grid-template-columns: minmax(220px, 1fr) minmax(0, 2fr); gap: var(--space-10); padding-top: var(--space-8); }
.identity-panel { padding-right: var(--space-8); border-right: 1px solid var(--color-border); text-align: center; }
.avatar-wrap { position: relative; display: inline-block; }
.avatar-button { position: absolute; right: 0; bottom: 0; display: grid; place-items: center; width: 34px; height: 34px; padding: 0; border: 1px solid var(--color-border-strong); border-radius: 50%; background: var(--color-surface); color: var(--color-text); cursor: pointer; box-shadow: var(--shadow-subtle); }
.avatar-button:hover { border-color: var(--color-brand); color: var(--color-brand); }
.avatar-button:disabled { cursor: wait; opacity: 0.6; }
.visually-hidden { position: absolute; width: 1px; height: 1px; overflow: hidden; clip: rect(0 0 0 0); clip-path: inset(50%); white-space: nowrap; }
.identity-panel h2 { margin: var(--space-4) 0 0; font-family: var(--font-serif); font-size: var(--font-size-xl); overflow-wrap: anywhere; }
.identity-panel p { margin: var(--space-1) 0 var(--space-3); color: var(--color-text-tertiary); font-size: var(--font-size-sm); overflow-wrap: anywhere; }
.role-label { padding: var(--space-1) var(--space-2); border-radius: var(--radius-sm); background: var(--color-brand-soft); color: var(--color-brand); font-size: var(--font-size-xs); }
.details-panel { min-width: 0; }
.section-heading { display: flex; align-items: center; justify-content: space-between; padding-bottom: var(--space-3); border-bottom: 1px solid var(--color-border); }
.section-heading h2 { margin: 0; font-size: var(--font-size-base); }
.details-list { margin: 0; }
.details-list div { display: grid; grid-template-columns: 120px minmax(0, 1fr); gap: var(--space-4); padding: var(--space-4) 0; border-bottom: 1px solid var(--color-border); }
.details-list dt { color: var(--color-text-tertiary); font-size: var(--font-size-sm); }
.details-list dd { min-width: 0; margin: 0; overflow-wrap: anywhere; }
.profile-form { padding-top: var(--space-5); }
.form-actions { display: flex; justify-content: flex-end; gap: var(--space-3); }
@media (max-width: 680px) {
  main { width: calc(100% - var(--space-6)); padding-top: var(--space-8); }
  .profile-layout { grid-template-columns: 1fr; gap: var(--space-8); }
  .identity-panel { padding: 0 0 var(--space-8); border-right: 0; border-bottom: 1px solid var(--color-border); }
  .details-list div { grid-template-columns: 96px minmax(0, 1fr); }
}
</style>
