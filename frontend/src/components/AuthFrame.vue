<script setup lang="ts">
interface AuthFrameProps {
  title?: string
  description?: string
}

withDefaults(defineProps<AuthFrameProps>(), {
  title: '账户',
  description: ''
})
</script>

<template>
  <div class="auth-page">
    <header class="auth-header">
      <RouterLink class="brand" to="/" aria-label="返回 AI博客系统首页">
        <span>AI</span>博客系统
      </RouterLink>
      <RouterLink class="back-link" to="/">返回首页</RouterLink>
    </header>
    <main class="auth-main">
      <section class="auth-panel">
        <p class="eyebrow">MEMBER ACCESS</p>
        <h1>{{ title }}</h1>
        <p v-if="description" class="description">{{ description }}</p>
        <slot />
      </section>
    </main>
  </div>
</template>

<style scoped>
.auth-page {
  min-height: 100vh;
  background: var(--color-canvas);
}

.auth-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: min(calc(100% - var(--space-8)), var(--content-width));
  height: var(--header-height);
  margin: 0 auto;
  border-bottom: 1px solid var(--color-border);
}

.brand,
.back-link {
  color: var(--color-text);
  text-decoration: none;
}

.brand {
  font-size: var(--font-size-lg);
  font-weight: 650;
}

.brand span {
  margin-right: var(--space-2);
  color: var(--color-brand);
  font-family: var(--font-serif);
  font-size: var(--font-size-xl);
}

.back-link {
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}

.back-link:hover {
  color: var(--color-brand);
}

.auth-main {
  display: grid;
  place-items: center;
  min-height: calc(100vh - var(--header-height));
  padding: var(--space-10) var(--space-4);
}

.auth-panel {
  width: min(100%, 440px);
  padding: var(--space-8) 0;
  border-top: 2px solid var(--color-text);
}

.eyebrow {
  margin: 0 0 var(--space-3);
  color: var(--color-brand);
  font-size: var(--font-size-xs);
  font-weight: 700;
}

h1 {
  margin: 0;
  font-family: var(--font-serif);
  font-size: var(--font-size-2xl);
  line-height: var(--line-height-tight);
}

.description {
  margin: var(--space-3) 0 var(--space-6);
  color: var(--color-text-secondary);
}

:deep(.auth-form) {
  margin-top: var(--space-6);
}

:deep(.auth-form .el-form-item) {
  margin-bottom: var(--space-5);
}

:deep(.auth-form .el-form-item__label) {
  color: var(--color-text);
  font-weight: 600;
}

:deep(.auth-form .el-input__wrapper) {
  min-height: 44px;
  background: var(--color-surface);
  box-shadow: 0 0 0 1px var(--color-border-strong) inset;
}

:deep(.auth-form .el-button) {
  width: 100%;
  min-height: 44px;
}

@media (max-width: 560px) {
  .auth-header {
    width: calc(100% - var(--space-6));
  }

  .auth-main {
    place-items: start center;
    padding-top: var(--space-8);
  }
}
</style>
