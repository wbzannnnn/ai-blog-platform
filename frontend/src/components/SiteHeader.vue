<script setup lang="ts">
import { ref, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { useRoute, useRouter } from 'vue-router'
import { Close, Menu, Search, User } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const { isLoggedIn, isAdmin, user } = storeToRefs(authStore)
const mobileOpen = ref(false)

const navItems = [
  { label: '首页', to: { path: '/' } },
  { label: '文章', to: { path: '/articles' } },
  { label: '专题', to: { path: '/topics' } },
  { label: '写文章', to: { path: '/write' } },
  { label: '数据分析', to: { path: '/analytics' } },
  { label: 'AI 检索', to: { path: '/agent' } }
]

const closeMobile = (): void => {
  mobileOpen.value = false
}

const openSearch = async (): Promise<void> => {
  closeMobile()
  await router.push({ path: '/articles', query: { focus: 'search' } })
}

const handleLogout = async (): Promise<void> => {
  authStore.logout()
  closeMobile()
  await router.push('/')
}

watch(() => route.fullPath, closeMobile)
</script>

<template>
  <header class="site-header">
    <div class="header-inner">
      <RouterLink class="brand" to="/" aria-label="AI博客系统首页">
        <span class="brand-mark" aria-hidden="true">AI</span>
        <span class="brand-name">博客系统</span>
      </RouterLink>

      <nav class="desktop-nav" aria-label="主导航">
        <RouterLink
          v-for="item in navItems"
          :key="item.label"
          :to="item.to"
          class="nav-link"
        >
          {{ item.label }}
        </RouterLink>
      </nav>

      <div class="header-actions">
        <el-tooltip content="搜索文章" placement="bottom">
          <button class="icon-button" type="button" aria-label="搜索文章" @click="openSearch">
            <el-icon><Search /></el-icon>
          </button>
        </el-tooltip>

        <template v-if="isLoggedIn">
          <el-dropdown trigger="click">
            <button class="account-button" type="button" aria-label="打开账户菜单">
              <el-avatar :size="28" :src="user?.avatar">
                <el-icon><User /></el-icon>
              </el-avatar>
              <span>{{ user?.nickname || user?.username }}</span>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="router.push('/profile')">个人资料</el-dropdown-item>
                <el-dropdown-item v-if="isAdmin" @click="router.push('/admin/dashboard')">管理后台</el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
        <RouterLink v-else class="login-link desktop-action" to="/login">登录</RouterLink>

        <button
          class="icon-button mobile-toggle"
          type="button"
          :aria-expanded="mobileOpen"
          aria-controls="mobile-navigation"
          :aria-label="mobileOpen ? '关闭导航' : '打开导航'"
          @click="mobileOpen = !mobileOpen"
        >
          <el-icon><Close v-if="mobileOpen" /><Menu v-else /></el-icon>
        </button>
      </div>
    </div>

    <nav v-if="mobileOpen" id="mobile-navigation" class="mobile-nav" aria-label="移动端导航">
      <RouterLink v-for="item in navItems" :key="item.label" :to="item.to" class="mobile-link" @click="closeMobile">
        {{ item.label }}
      </RouterLink>
      <RouterLink v-if="isLoggedIn" class="mobile-link" to="/profile" @click="closeMobile">个人资料</RouterLink>
      <RouterLink v-if="isAdmin" class="mobile-link" to="/admin/dashboard" @click="closeMobile">管理后台</RouterLink>
      <RouterLink v-if="!isLoggedIn" class="mobile-link" to="/login" @click="closeMobile">登录</RouterLink>
      <button v-else class="mobile-link mobile-logout" type="button" @click="handleLogout">退出登录</button>
    </nav>
  </header>
</template>

<style scoped>
.site-header {
  position: sticky;
  top: 0;
  z-index: 40;
  height: var(--header-height);
  border-bottom: 1px solid var(--color-border);
  background: color-mix(in srgb, var(--color-surface) 94%, transparent);
  backdrop-filter: blur(14px);
}

.header-inner {
  display: grid;
  grid-template-columns: minmax(150px, 1fr) auto minmax(150px, 1fr);
  align-items: center;
  width: min(calc(100% - var(--space-8)), var(--content-width));
  height: 100%;
  margin: 0 auto;
}

.brand {
  display: inline-flex;
  align-items: baseline;
  gap: 0;
  width: fit-content;
  color: var(--color-text);
  text-decoration: none;
}

.brand-mark {
  color: var(--color-brand);
  font-family: var(--font-serif);
  font-size: var(--font-size-xl);
  font-weight: 700;
}

.brand-name {
  font-size: var(--font-size-lg);
  font-weight: 650;
}

.desktop-nav {
  display: flex;
  align-items: center;
  gap: var(--space-6);
  height: 100%;
}

.nav-link,
.login-link {
  position: relative;
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  text-decoration: none;
  transition: color var(--transition-fast);
}

.nav-link::after {
  position: absolute;
  right: 0;
  bottom: calc(var(--space-5) * -1);
  left: 0;
  height: 2px;
  background: var(--color-brand);
  content: '';
  opacity: 0;
  transform: translateY(var(--space-1));
  transition: opacity var(--transition-fast), transform var(--transition-fast);
}

.nav-link:hover,
.nav-link.router-link-active,
.login-link:hover {
  color: var(--color-text);
}

.nav-link.router-link-exact-active::after {
  opacity: 1;
  transform: translateY(0);
}

.header-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: var(--space-3);
}

.icon-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  padding: 0;
  border: 1px solid transparent;
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--color-text-secondary);
  cursor: pointer;
  text-decoration: none;
  transition: color var(--transition-fast), border-color var(--transition-fast), background-color var(--transition-fast);
}

.icon-button:hover {
  border-color: var(--color-border);
  background: var(--color-surface-subtle);
  color: var(--color-text);
}

.account-button {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  max-width: 160px;
  padding: var(--space-1) var(--space-2);
  border: 1px solid transparent;
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--color-text-secondary);
  cursor: pointer;
}

.account-button:hover {
  border-color: var(--color-border);
  color: var(--color-text);
}

.account-button span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mobile-toggle,
.mobile-nav {
  display: none;
}

@media (max-width: 860px) {
  .site-header {
    height: 60px;
  }

  .header-inner {
    display: flex;
    justify-content: space-between;
    width: min(calc(100% - var(--space-6)), var(--content-width));
  }

  .desktop-nav,
  .desktop-action,
  .account-button {
    display: none;
  }

  .mobile-toggle {
    display: inline-flex;
  }

  .mobile-nav {
    display: flex;
    flex-direction: column;
    gap: var(--space-1);
    padding: var(--space-3);
    border-bottom: 1px solid var(--color-border);
    background: var(--color-surface);
    box-shadow: var(--shadow-subtle);
  }

  .mobile-link {
    width: 100%;
    padding: var(--space-3) var(--space-4);
    border: 0;
    border-radius: var(--radius-sm);
    background: transparent;
    color: var(--color-text-secondary);
    font-size: var(--font-size-base);
    text-align: left;
    text-decoration: none;
  }

  .mobile-link:hover,
  .mobile-link.router-link-active {
    background: var(--color-surface-subtle);
    color: var(--color-text);
  }

  .mobile-logout {
    cursor: pointer;
  }
}
</style>
