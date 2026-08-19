<script setup lang="ts">
import { nextTick, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowRight, ChatLineRound, Pointer, Search, View } from '@element-plus/icons-vue'
import SiteFooter from '@/components/SiteFooter.vue'
import SiteHeader from '@/components/SiteHeader.vue'
import { getPosts, searchPosts } from '@/api/post'
import type { PostResponse } from '@/api/types'

const route = useRoute()
const router = useRouter()
const posts = ref<PostResponse[]>([])
const searchKeyword = ref(typeof route.query.q === 'string' ? route.query.q : '')
const searchInput = ref<HTMLInputElement | null>(null)
const currentPage = ref(0)
const totalElements = ref(0)
const totalPages = ref(0)
const loading = ref(false)
const loadError = ref('')

const loadPosts = async (page = 0, keyword = ''): Promise<void> => {
  loading.value = true
  loadError.value = ''
  try {
    const response = keyword ? await searchPosts(keyword, page) : await getPosts(page)
    if (response.code === 200 && response.data) {
      posts.value = response.data.content
      totalElements.value = response.data.totalElements
      totalPages.value = response.data.totalPages
    } else {
      loadError.value = response.message || '文章列表暂时不可用'
    }
  } catch {
    loadError.value = '文章加载失败，请确认后端服务已启动'
  } finally {
    loading.value = false
  }
}

const handleSearch = async (): Promise<void> => {
  const keyword = searchKeyword.value.trim()
  currentPage.value = 0
  const currentQuery = typeof route.query.q === 'string' ? route.query.q : ''
  if (keyword === currentQuery) await loadPosts(0, keyword)
  else await router.push({ path: '/articles', query: keyword ? { q: keyword } : {} })
}

const clearSearch = async (): Promise<void> => {
  searchKeyword.value = ''
  await router.push('/articles')
}

const handlePageChange = (page: number): void => {
  currentPage.value = page - 1
  void loadPosts(currentPage.value, searchKeyword.value.trim())
  document.querySelector('.articles-list')?.scrollIntoView({ behavior: 'smooth' })
}

const focusSearch = async (): Promise<void> => {
  if (route.query.focus !== 'search') return
  await nextTick()
  searchInput.value?.focus()
}

const formatDate = (timestamp: number): string => new Date(timestamp).toLocaleDateString('zh-CN', {
  year: 'numeric', month: 'long', day: 'numeric'
})

const excerpt = (post: PostResponse): string => {
  const source = post.summary || post.content
  return source.replace(/[#*`>\[\]()\r\n-]+/g, ' ').replace(/\s+/g, ' ').trim().slice(0, 180)
}

onMounted(() => {
  void loadPosts(0, searchKeyword.value.trim())
  void focusSearch()
})

watch(() => route.query.q, (value) => {
  const keyword = typeof value === 'string' ? value : ''
  searchKeyword.value = keyword
  currentPage.value = 0
  void loadPosts(0, keyword)
})
</script>

<template>
  <div class="articles-page">
    <SiteHeader />
    <main>
      <header class="page-header">
        <div><p>ARTICLES</p><h1>文章</h1></div>
        <p>共 {{ totalElements }} 篇已发布文章</p>
      </header>

      <form class="search-bar" role="search" @submit.prevent="handleSearch">
        <el-icon aria-hidden="true"><Search /></el-icon>
        <input ref="searchInput" v-model="searchKeyword" type="search" aria-label="搜索文章" placeholder="搜索标题或正文" />
        <button v-if="searchKeyword" type="button" class="clear-button" @click="clearSearch">清除</button>
        <el-button native-type="submit" type="primary" :loading="loading">搜索</el-button>
      </form>

      <p v-if="route.query.q && !loading" class="result-note">“{{ route.query.q }}”的搜索结果：{{ totalElements }} 篇</p>

      <div v-if="loading" class="page-state"><el-skeleton :rows="9" animated /></div>
      <div v-else-if="loadError" class="page-state error-state" role="alert">
        <p>{{ loadError }}</p><el-button @click="loadPosts(currentPage, searchKeyword.trim())">重新加载</el-button>
      </div>
      <section v-else class="articles-list" aria-label="文章列表">
        <article v-for="post in posts" :key="post.id" class="article-row">
          <div class="article-index">{{ String(post.id).padStart(3, '0') }}</div>
          <div class="article-content">
            <div class="article-labels">
              <span v-for="tag in post.tags?.slice(0, 3)" :key="tag.id">{{ tag.name }}</span>
              <span v-if="post.isAiGenerated">AI 辅助</span>
            </div>
            <RouterLink class="article-title" :to="`/post/${post.id}`">{{ post.title }}</RouterLink>
            <p>{{ excerpt(post) }}</p>
            <div class="article-meta">
              <span>{{ post.author.nickname }}</span>
              <time :datetime="new Date(post.createdAt).toISOString()">{{ formatDate(post.createdAt) }}</time>
              <span><el-icon><View /></el-icon>{{ post.viewCount }}</span>
              <span><el-icon><ChatLineRound /></el-icon>{{ post.commentCount }}</span>
              <span><el-icon><Pointer /></el-icon>{{ post.likeCount }}</span>
            </div>
          </div>
          <RouterLink class="article-arrow" :to="`/post/${post.id}`" :aria-label="`阅读《${post.title}》`"><el-icon><ArrowRight /></el-icon></RouterLink>
        </article>
        <el-empty v-if="posts.length === 0" :description="searchKeyword ? '没有匹配的文章' : '目前还没有已发布文章'" />
      </section>

      <div v-if="totalPages > 1" class="pagination-wrap">
        <el-pagination :current-page="currentPage + 1" :page-size="10" :total="totalElements" layout="prev, pager, next" @current-change="handlePageChange" />
      </div>
    </main>
    <SiteFooter />
  </div>
</template>

<style scoped>
.articles-page { min-height: 100vh; background: var(--color-canvas); }
main { width: min(calc(100% - var(--space-8)), 980px); min-height: calc(100vh - 170px); margin: 0 auto; padding: var(--space-10) 0 var(--space-16); }
.page-header { display: flex; align-items: end; justify-content: space-between; gap: var(--space-6); padding-bottom: var(--space-5); border-bottom: 2px solid var(--color-text); }
.page-header div > p { margin: 0 0 var(--space-2); color: var(--color-brand); font-size: var(--font-size-xs); font-weight: 700; }
.page-header h1 { margin: 0; font-family: var(--font-serif); font-size: var(--font-size-2xl); }
.page-header > p { margin: 0; color: var(--color-text-secondary); font-size: var(--font-size-sm); }
.search-bar { display: grid; grid-template-columns: auto minmax(0, 1fr) auto auto; align-items: center; gap: var(--space-3); margin: var(--space-5) 0; padding-bottom: var(--space-4); border-bottom: 1px solid var(--color-border); }
.search-bar input { width: 100%; padding: var(--space-2) 0; border: 0; background: transparent; color: var(--color-text); outline: 0; }
.clear-button { padding: var(--space-2); border: 0; background: transparent; color: var(--color-text-secondary); cursor: pointer; }
.result-note { margin: 0 0 var(--space-4); color: var(--color-text-secondary); font-size: var(--font-size-sm); }
.articles-list { border-top: 2px solid var(--color-text); }
.article-row { display: grid; grid-template-columns: 64px minmax(0, 1fr) auto; gap: var(--space-5); padding: var(--space-6) 0; border-bottom: 1px solid var(--color-border); }
.article-index { color: var(--color-text-tertiary); font-family: var(--font-serif); font-size: var(--font-size-sm); }
.article-labels { display: flex; flex-wrap: wrap; gap: var(--space-3); margin-bottom: var(--space-2); color: var(--color-brand); font-size: var(--font-size-xs); font-weight: 700; }
.article-title { color: var(--color-text); font-family: var(--font-serif); font-size: var(--font-size-xl); font-weight: 650; line-height: 1.35; text-decoration: none; overflow-wrap: anywhere; }
.article-title:hover { color: var(--color-brand); }
.article-content > p { display: -webkit-box; margin: var(--space-2) 0 var(--space-3); overflow: hidden; color: var(--color-text-secondary); font-size: var(--font-size-sm); -webkit-box-orient: vertical; -webkit-line-clamp: 2; }
.article-meta { display: flex; flex-wrap: wrap; gap: var(--space-4); color: var(--color-text-tertiary); font-size: var(--font-size-xs); }
.article-meta span { display: inline-flex; align-items: center; gap: var(--space-1); }
.article-arrow { align-self: center; padding: var(--space-3); color: var(--color-text-tertiary); text-decoration: none; }
.article-arrow:hover { color: var(--color-brand); transform: translateX(var(--space-1)); }
.page-state { max-width: 720px; padding: var(--space-8) 0; }
.error-state { color: var(--color-danger); }
.pagination-wrap { display: flex; justify-content: center; padding-top: var(--space-8); }
@media (max-width: 640px) {
  main { width: calc(100% - var(--space-6)); padding-top: var(--space-8); }
  .page-header { align-items: flex-start; flex-direction: column; gap: var(--space-2); }
  .search-bar { grid-template-columns: auto minmax(0, 1fr) auto; }
  .search-bar .el-button { grid-column: 1 / -1; width: 100%; }
  .article-row { grid-template-columns: minmax(0, 1fr) auto; gap: var(--space-2); }
  .article-index { grid-column: 1 / -1; }
  .article-title { font-size: var(--font-size-lg); }
}
</style>
