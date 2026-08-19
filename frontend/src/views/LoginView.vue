<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AuthFrame from '@/components/AuthFrame.vue'
import { login } from '@/api/auth'
import type { LoginRequest } from '@/api/types'
import { useAuthStore } from '@/stores/auth'
import { getApiErrorMessage } from '@/utils/errors'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const form = ref<LoginRequest>({ username: '', password: '' })
const loading = ref(false)

const handleSubmit = async (): Promise<void> => {
  if (!form.value.username.trim() || !form.value.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    const response = await login(form.value)
    if (response.code === 200 && response.data) {
      authStore.setSession(response.data)
      ElMessage.success('登录成功')
      const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
      await router.replace(redirect)
    } else {
      ElMessage.error(response.message || '登录失败')
    }
  } catch (error: unknown) {
    ElMessage.error(getApiErrorMessage(error, '登录失败，请稍后重试'))
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <AuthFrame title="登录" description="继续阅读、写作和管理你的内容。">
    <el-form class="auth-form" :model="form" label-position="top" @submit.prevent="handleSubmit">
      <el-form-item label="用户名" prop="username">
        <el-input v-model="form.username" autocomplete="username" placeholder="请输入用户名" />
      </el-form-item>
      <el-form-item label="密码" prop="password">
        <el-input
          v-model="form.password"
          type="password"
          autocomplete="current-password"
          placeholder="请输入密码"
          show-password
        />
      </el-form-item>
      <el-form-item>
        <el-button native-type="submit" type="primary" :loading="loading" :disabled="loading">
          登录
        </el-button>
      </el-form-item>
    </el-form>
    <p class="account-switch">还没有账号？<RouterLink to="/register">创建账号</RouterLink></p>
  </AuthFrame>
</template>

<style scoped>
.account-switch {
  margin: var(--space-5) 0 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  text-align: center;
}

.account-switch a {
  color: var(--color-brand);
  font-weight: 600;
  text-decoration: none;
}

.account-switch a:hover {
  text-decoration: underline;
}
</style>
