<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { useRoute, useRouter } from 'vue-router'
import { ChatDotRound, Close, Clock, Document, HomeFilled, Menu, SwitchButton, User } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const { user } = storeToRefs(authStore)
const mobileMenuOpen = ref(false)

const pageTitle = computed(() => {
  const titles: Record<string, string> = {
    '/admin/dashboard': '工作台',
    '/admin/users': '用户管理',
    '/admin/posts': '文章管理',
    '/admin/pending': '待审核文章',
    '/admin/ai-chat': '管理助手'
  }
  return titles[route.path] ?? '管理后台'
})

const menuItems = [
  { path: '/admin/dashboard', icon: HomeFilled, label: '工作台' },
  { path: '/admin/users', icon: User, label: '用户管理' },
  { path: '/admin/posts', icon: Document, label: '文章管理' },
  { path: '/admin/pending', icon: Clock, label: '待审核' },
  { path: '/admin/ai-chat', icon: ChatDotRound, label: '管理助手' }
]

const handleLogout = async (): Promise<void> => {
  authStore.logout()
  await router.replace('/login')
}

watch(() => route.path, () => {
  mobileMenuOpen.value = false
})
</script>

<template>
  <div class="admin-shell">
    <button v-if="mobileMenuOpen" class="menu-overlay" type="button" aria-label="关闭导航" @click="mobileMenuOpen = false"></button>
    <aside :class="['sidebar', { open: mobileMenuOpen }]">
      <div class="sidebar-brand">
        <RouterLink to="/admin/dashboard"><span>AI</span>博客系统</RouterLink>
        <button class="mobile-close" type="button" aria-label="关闭导航" @click="mobileMenuOpen = false"><el-icon><Close /></el-icon></button>
      </div>
      <nav class="side-nav" aria-label="管理后台导航">
        <RouterLink v-for="item in menuItems" :key="item.path" :to="item.path" class="side-link">
          <el-icon><component :is="item.icon" /></el-icon><span>{{ item.label }}</span>
        </RouterLink>
      </nav>
      <div class="sidebar-footer">
        <RouterLink to="/" class="site-link">查看站点</RouterLink>
        <button type="button" @click="handleLogout"><el-icon><SwitchButton /></el-icon>退出登录</button>
      </div>
    </aside>

    <div class="admin-workspace">
      <header class="admin-header">
        <div class="header-title-wrap">
          <button class="mobile-menu" type="button" aria-label="打开导航" @click="mobileMenuOpen = true"><el-icon><Menu /></el-icon></button>
          <div><p>ADMINISTRATION</p><h1>{{ pageTitle }}</h1></div>
        </div>
        <RouterLink class="account-link" to="/profile">
          <el-avatar :size="30" :src="user?.avatar" />
          <span>{{ user?.nickname || user?.username }}</span>
        </RouterLink>
      </header>
      <main class="admin-main"><RouterView /></main>
    </div>
  </div>
</template>

<style scoped>
.admin-shell { min-height: 100vh; background: var(--color-canvas); }
.sidebar { position: fixed; inset: 0 auto 0 0; z-index: 100; display: flex; width: 236px; flex-direction: column; border-right: 1px solid var(--color-border); background: var(--color-surface); }
.sidebar-brand { display: flex; align-items: center; justify-content: space-between; height: 72px; padding: 0 var(--space-6); border-bottom: 1px solid var(--color-border); }
.sidebar-brand a { color: var(--color-text); font-size: var(--font-size-lg); font-weight: 650; text-decoration: none; }
.sidebar-brand a span { margin-right: var(--space-2); color: var(--color-brand); font-family: var(--font-serif); font-size: var(--font-size-xl); }
.side-nav { display: flex; flex-direction: column; gap: var(--space-1); padding: var(--space-5) var(--space-3); }
.side-link { display: flex; align-items: center; gap: var(--space-3); padding: var(--space-3); border-radius: var(--radius-sm); color: var(--color-text-secondary); font-size: var(--font-size-sm); text-decoration: none; transition: color var(--transition-fast), background-color var(--transition-fast); }
.side-link:hover { background: var(--color-surface-subtle); color: var(--color-text); }
.side-link.router-link-active { background: var(--color-brand-soft); color: var(--color-brand); font-weight: 650; }
.sidebar-footer { display: grid; gap: var(--space-2); margin-top: auto; padding: var(--space-4) var(--space-3); border-top: 1px solid var(--color-border); }
.sidebar-footer a, .sidebar-footer button { display: flex; align-items: center; gap: var(--space-2); width: 100%; padding: var(--space-2) var(--space-3); border: 0; border-radius: var(--radius-sm); background: transparent; color: var(--color-text-secondary); cursor: pointer; font-size: var(--font-size-sm); text-align: left; text-decoration: none; }
.sidebar-footer a:hover, .sidebar-footer button:hover { background: var(--color-surface-subtle); color: var(--color-text); }
.admin-workspace { min-height: 100vh; margin-left: 236px; }
.admin-header { position: sticky; top: 0; z-index: 50; display: flex; align-items: center; justify-content: space-between; height: 72px; padding: 0 var(--space-8); border-bottom: 1px solid var(--color-border); background: color-mix(in srgb, var(--color-surface) 94%, transparent); backdrop-filter: blur(12px); }
.header-title-wrap { display: flex; align-items: center; gap: var(--space-3); }
.header-title-wrap p { margin: 0; color: var(--color-brand); font-size: var(--font-size-xs); font-weight: 700; }
.header-title-wrap h1 { margin: 0; font-family: var(--font-serif); font-size: var(--font-size-xl); }
.account-link { display: flex; align-items: center; gap: var(--space-2); max-width: 180px; color: var(--color-text-secondary); font-size: var(--font-size-sm); text-decoration: none; }
.account-link span { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.admin-main { min-width: 0; padding: var(--space-6) var(--space-8) var(--space-12); }
.mobile-menu, .mobile-close, .menu-overlay { display: none; }

:deep(.page-header) { margin-bottom: var(--space-6); padding-bottom: var(--space-4); border-bottom: 1px solid var(--color-border); }
:deep(.page-title) { margin: 0; color: var(--color-text); font-family: var(--font-serif); font-size: var(--font-size-xl); }
:deep(.page-subtitle) { margin: var(--space-1) 0 0; color: var(--color-text-secondary); font-size: var(--font-size-sm); }
:deep(.content-card), :deep(.section-card), :deep(.stat-card) { border-color: var(--color-border); border-radius: var(--radius-sm); box-shadow: none; }
:deep(.el-table) { --el-table-border-color: var(--color-border); --el-table-header-bg-color: var(--color-surface-subtle); }
:deep(.el-dialog) { max-width: calc(100vw - var(--space-6)); }

@media (max-width: 820px) {
  .sidebar { width: min(82vw, 280px); transform: translateX(-100%); transition: transform var(--transition-fast); }
  .sidebar.open { transform: translateX(0); }
  .mobile-close, .mobile-menu { display: inline-flex; align-items: center; justify-content: center; width: 36px; height: 36px; padding: 0; border: 0; border-radius: var(--radius-sm); background: transparent; color: var(--color-text-secondary); cursor: pointer; }
  .menu-overlay { position: fixed; inset: 0; z-index: 90; display: block; width: 100%; border: 0; background: rgb(28 33 30 / 32%); }
  .admin-workspace { margin-left: 0; }
  .admin-header { height: 64px; padding: 0 var(--space-3); }
  .header-title-wrap p { display: none; }
  .header-title-wrap h1 { font-size: var(--font-size-lg); }
  .account-link span { display: none; }
  .admin-main { padding: var(--space-5) var(--space-3) var(--space-10); overflow-x: hidden; }
  :deep(.el-card__body) { padding: var(--space-3); }
  :deep(.el-table) { min-width: 720px; }
  :deep(.content-card) { overflow-x: auto; }
}
</style>
