<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ChatLineRound, Clock, Document, Pointer, User, View } from '@element-plus/icons-vue'
import { getPendingPosts, getUsers } from '@/api/admin'
import { getPosts } from '@/api/post'
import type { PostResponse } from '@/api/types'

interface DashboardStats {
  users: number
  posts: number
  pending: number
  pageViews: number
  pageLikes: number
  pageComments: number
}

const stats = ref<DashboardStats>({ users: 0, posts: 0, pending: 0, pageViews: 0, pageLikes: 0, pageComments: 0 })
const recentPosts = ref<PostResponse[]>([])
const loading = ref(false)
const loadError = ref('')

const loadDashboard = async (): Promise<void> => {
  loading.value = true
  loadError.value = ''
  try {
    const [usersResponse, postsResponse, pendingResponse] = await Promise.all([
      getUsers(0, 1),
      getPosts(0, 5),
      getPendingPosts(0, 1)
    ])
    const currentPosts = postsResponse.data?.content ?? []
    recentPosts.value = currentPosts
    stats.value = {
      users: usersResponse.data?.totalElements ?? 0,
      posts: postsResponse.data?.totalElements ?? 0,
      pending: pendingResponse.data?.totalElements ?? 0,
      pageViews: currentPosts.reduce((sum, post) => sum + post.viewCount, 0),
      pageLikes: currentPosts.reduce((sum, post) => sum + post.likeCount, 0),
      pageComments: currentPosts.reduce((sum, post) => sum + post.commentCount, 0)
    }
  } catch {
    loadError.value = '数据加载失败，请确认后端服务可用'
  } finally {
    loading.value = false
  }
}

const formatDate = (timestamp: number): string => new Date(timestamp).toLocaleDateString('zh-CN')

const statusText = (status: string): string => ({
  DRAFT: '草稿', PUBLISHED: '已发布', ARCHIVED: '已归档'
}[status] ?? status)

onMounted(() => void loadDashboard())
</script>

<template>
  <div class="dashboard">
    <div class="page-header">
      <h2 class="page-title">内容概览</h2>
      <p class="page-subtitle">统计数据来自当前后台接口，不包含估算值。</p>
    </div>

    <div v-if="loading" class="loading-state"><el-skeleton :rows="8" animated /></div>
    <div v-else-if="loadError" class="error-state" role="alert"><p>{{ loadError }}</p><el-button @click="loadDashboard">重试</el-button></div>
    <template v-else>
      <section class="stats-grid" aria-label="站点统计">
        <div class="stat-item"><el-icon><User /></el-icon><div><strong>{{ stats.users }}</strong><span>用户总数</span></div></div>
        <div class="stat-item"><el-icon><Document /></el-icon><div><strong>{{ stats.posts }}</strong><span>已发布文章</span></div></div>
        <div class="stat-item attention"><el-icon><Clock /></el-icon><div><strong>{{ stats.pending }}</strong><span>待审核文章</span></div></div>
        <div class="stat-item"><el-icon><View /></el-icon><div><strong>{{ stats.pageViews }}</strong><span>本页文章阅读</span></div></div>
        <div class="stat-item"><el-icon><Pointer /></el-icon><div><strong>{{ stats.pageLikes }}</strong><span>本页文章点赞</span></div></div>
        <div class="stat-item"><el-icon><ChatLineRound /></el-icon><div><strong>{{ stats.pageComments }}</strong><span>本页文章评论</span></div></div>
      </section>

      <section class="recent-section" aria-labelledby="recent-heading">
        <div class="section-heading">
          <h3 id="recent-heading">最近发布</h3>
          <RouterLink to="/admin/posts">查看全部</RouterLink>
        </div>
        <div class="table-wrap">
          <el-table :data="recentPosts" empty-text="暂无已发布文章">
            <el-table-column prop="title" label="标题" min-width="260">
              <template #default="scope"><RouterLink class="post-link" :to="`/post/${scope.row.id}`">{{ scope.row.title }}</RouterLink></template>
            </el-table-column>
            <el-table-column prop="author.nickname" label="作者" width="130" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="scope"><el-tag size="small" effect="plain">{{ statusText(scope.row.status || 'PUBLISHED') }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="viewCount" label="阅读" width="90" />
            <el-table-column prop="createdAt" label="发布日期" width="140">
              <template #default="scope">{{ formatDate(scope.row.createdAt) }}</template>
            </el-table-column>
          </el-table>
        </div>
      </section>
    </template>
  </div>
</template>

<style scoped>
.dashboard { max-width: 1180px; margin: 0 auto; }
.loading-state { max-width: 760px; padding: var(--space-8) 0; }
.error-state { padding: var(--space-8); border: 1px solid var(--color-border); background: var(--color-surface); color: var(--color-danger); text-align: center; }
.stats-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); margin-bottom: var(--space-8); border-top: 2px solid var(--color-text); border-left: 1px solid var(--color-border); }
.stat-item { display: flex; align-items: center; gap: var(--space-4); min-width: 0; padding: var(--space-5); border-right: 1px solid var(--color-border); border-bottom: 1px solid var(--color-border); background: var(--color-surface); }
.stat-item > .el-icon { flex: 0 0 auto; color: var(--color-brand); font-size: var(--font-size-xl); }
.stat-item.attention > .el-icon { color: var(--color-warning); }
.stat-item div { display: flex; min-width: 0; flex-direction: column; }
.stat-item strong { font-family: var(--font-serif); font-size: var(--font-size-2xl); font-weight: 650; line-height: var(--line-height-tight); }
.stat-item span { color: var(--color-text-secondary); font-size: var(--font-size-xs); }
.recent-section { border-top: 2px solid var(--color-text); }
.section-heading { display: flex; align-items: center; justify-content: space-between; padding: var(--space-4) 0; }
.section-heading h3 { margin: 0; font-size: var(--font-size-sm); }
.section-heading a { color: var(--color-brand); font-size: var(--font-size-sm); text-decoration: none; }
.table-wrap { border: 1px solid var(--color-border); overflow-x: auto; }
.post-link { color: var(--color-text); font-weight: 600; text-decoration: none; }
.post-link:hover { color: var(--color-brand); }
@media (max-width: 900px) { .stats-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
@media (max-width: 520px) { .stats-grid { grid-template-columns: 1fr; } .stat-item { padding: var(--space-4); } }
</style>
