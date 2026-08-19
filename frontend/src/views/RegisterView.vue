<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AuthFrame from '@/components/AuthFrame.vue'
import { register } from '@/api/auth'
import type { RegisterRequest } from '@/api/types'
import { getApiErrorMessage } from '@/utils/errors'

const router = useRouter()
const form = ref<RegisterRequest>({ username: '', password: '', email: '', nickname: '' })
const loading = ref(false)

const handleSubmit = async (): Promise<void> => {
  const values = Object.values(form.value)
  if (values.some((value) => !value.trim())) {
    ElMessage.warning('请填写所有必填项')
    return
  }
  loading.value = true
  try {
    const response = await register(form.value)
    if (response.code === 200) {
      ElMessage.success('注册成功，请登录')
      await router.replace('/login')
    } else {
      ElMessage.error(response.message || '注册失败')
    }
  } catch (error: unknown) {
    ElMessage.error(getApiErrorMessage(error, '注册失败，请稍后重试'))
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <AuthFrame title="创建账号" description="加入后可以发布文章、参与讨论并保存个人资料。">
    <el-form class="auth-form" :model="form" label-position="top" @submit.prevent="handleSubmit">
      <el-form-item label="用户名" prop="username">
        <el-input v-model="form.username" autocomplete="username" placeholder="用于登录" />
      </el-form-item>
      <el-form-item label="昵称" prop="nickname">
        <el-input v-model="form.nickname" autocomplete="nickname" placeholder="文章和评论中的署名" />
      </el-form-item>
      <el-form-item label="邮箱" prop="email">
        <el-input v-model="form.email" type="email" autocomplete="email" placeholder="name@example.com" />
      </el-form-item>
      <el-form-item label="密码" prop="password">
        <el-input
          v-model="form.password"
          type="password"
          autocomplete="new-password"
          placeholder="请输入密码"
          show-password
        />
      </el-form-item>
      <el-form-item>
        <el-button native-type="submit" type="primary" :loading="loading" :disabled="loading">
          创建账号
        </el-button>
      </el-form-item>
    </el-form>
    <p class="account-switch">已有账号？<RouterLink to="/login">直接登录</RouterLink></p>
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
