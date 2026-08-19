<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight, ChatDotSquare, DataAnalysis, EditPen, Search } from '@element-plus/icons-vue'
import SiteFooter from '@/components/SiteFooter.vue'
import SiteHeader from '@/components/SiteHeader.vue'
import { getLatestPosts } from '@/api/post'
import type { PostResponse } from '@/api/types'

type NewsletterState = 'idle' | 'loading' | 'success' | 'error'

const router = useRouter()
const posts = ref<PostResponse[]>([])
const loading = ref(false)
const loadError = ref('')
const searchKeyword = ref('')
const newsletterEmail = ref('')
const newsletterState = ref<NewsletterState>('idle')

const loadLatestPosts = async (): Promise<void> => {
  loading.value = true
  loadError.value = ''
  try {
    const response = await getLatestPosts()
    if (response.code === 200) posts.value = response.data ?? []
    else loadError.value = response.message || '最新文章暂时不可用'
  } catch {
    loadError.value = '文章加载失败，请确认后端服务已启动'
  } finally {
    loading.value = false
  }
}

const openSearch = async (): Promise<void> => {
  const keyword = searchKeyword.value.trim()
  await router.push({ path: '/articles', query: keyword ? { q: keyword } : { focus: 'search' } })
}

const subscribe = (): void => {
  const validEmail = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(newsletterEmail.value.trim())
  if (!validEmail) {
    newsletterState.value = 'error'
    return
  }
  newsletterState.value = 'loading'
  window.setTimeout(() => {
    localStorage.setItem('newsletterEmail', newsletterEmail.value.trim())
    newsletterState.value = 'success'
  }, 350)
}

const formatDate = (timestamp: number): string => new Date(timestamp).toLocaleDateString('zh-CN', {
  year: 'numeric', month: 'long', day: 'numeric'
})

const excerpt = (post: PostResponse): string => {
  const source = post.summary || post.content
  return source.replace(/[#*`>\[\]()\r\n-]+/g, ' ').replace(/\s+/g, ' ').trim().slice(0, 150)
}

onMounted(() => void loadLatestPosts())
</script>

<template>
  <div class="home-page">
    <SiteHeader />
    <main>
      <section class="masthead" aria-labelledby="home-title">
        <div>
          <h1 id="home-title">AI博客系统</h1>
        </div>
        <div class="masthead-side">
          <p>记录模型应用、工程实践与产品判断，关注真实问题和可复用的方法。</p>
          <form class="home-search" role="search" @submit.prevent="openSearch">
            <el-icon aria-hidden="true"><Search /></el-icon>
            <input v-model="searchKeyword" type="search" aria-label="搜索文章" placeholder="搜索文章" />
            <button type="submit">搜索</button>
          </form>
        </div>
      </section>

      <nav class="function-nav" aria-label="核心功能">
        <RouterLink to="/write">
          <el-icon><EditPen /></el-icon><span>开始写作</span><el-icon class="arrow"><ArrowRight /></el-icon>
        </RouterLink>
        <RouterLink to="/analytics">
          <el-icon><DataAnalysis /></el-icon><span>查看标签热度</span><el-icon class="arrow"><ArrowRight /></el-icon>
        </RouterLink>
        <RouterLink to="/agent">
          <el-icon><ChatDotSquare /></el-icon><span>检索站内文章</span><el-icon class="arrow"><ArrowRight /></el-icon>
        </RouterLink>
      </nav>

      <section class="latest-section" aria-labelledby="latest-heading">
        <div class="section-heading">
          <div><p>LATEST</p><h2 id="latest-heading">最新文章</h2></div>
          <RouterLink to="/articles">全部文章 <el-icon><ArrowRight /></el-icon></RouterLink>
        </div>

        <div v-if="loading" class="loading-state"><el-skeleton :rows="6" animated /></div>
        <div v-else-if="loadError" class="error-state" role="alert">
          <p>{{ loadError }}</p><el-button @click="loadLatestPosts">重新加载</el-button>
        </div>
        <div v-else-if="posts.length" class="article-list">
          <article v-for="post in posts" :key="post.id" class="article-row">
            <time :datetime="new Date(post.createdAt).toISOString()">{{ formatDate(post.createdAt) }}</time>
            <div class="article-content">
              <div class="article-labels">
                <span v-for="tag in post.tags?.slice(0, 2)" :key="tag.id">{{ tag.name }}</span>
                <span v-if="post.isAiGenerated">AI 辅助</span>
              </div>
              <RouterLink class="article-title" :to="`/post/${post.id}`">{{ post.title }}</RouterLink>
              <p>{{ excerpt(post) }}</p>
              <div class="article-meta">
                <span>{{ post.author.nickname }}</span>
                <span>{{ post.viewCount }} 阅读</span>
                <span>{{ post.commentCount }} 评论</span>
              </div>
            </div>
            <RouterLink class="article-arrow" :to="`/post/${post.id}`" :aria-label="`阅读《${post.title}》`">
              <el-icon><ArrowRight /></el-icon>
            </RouterLink>
          </article>
        </div>
        <el-empty v-else description="目前还没有已发布文章" />
      </section>

      <section class="newsletter" aria-labelledby="newsletter-heading">
        <div><p>MONTHLY DIGEST</p><h2 id="newsletter-heading">每月技术摘要</h2></div>
        <form novalidate @submit.prevent="subscribe">
          <label class="visually-hidden" for="home-newsletter-email">邮箱</label>
          <input
            id="home-newsletter-email"
            v-model="newsletterEmail"
            type="email"
            autocomplete="email"
            placeholder="name@example.com"
            :aria-invalid="newsletterState === 'error'"
            :disabled="newsletterState === 'loading' || newsletterState === 'success'"
            @input="newsletterState = 'idle'"
          />
          <el-button native-type="submit" type="primary" :loading="newsletterState === 'loading'" :disabled="newsletterState === 'success'">
            {{ newsletterState === 'success' ? '已记录' : '订阅' }}
          </el-button>
          <span v-if="newsletterState === 'error'" class="form-error" role="alert">请输入有效邮箱</span>
          <span v-if="newsletterState === 'success'" class="form-success" aria-live="polite">已保存在此设备</span>
        </form>
      </section>
    </main>
    <SiteFooter />
  </div>
</template>

<style scoped>
.home-page { min-height: 100vh; background: var(--color-canvas); }
main { width: min(calc(100% - var(--space-8)), var(--content-width)); margin: 0 auto; }
.masthead { display: grid; grid-template-columns: minmax(0, 1fr) minmax(320px, 460px); align-items: end; gap: var(--space-12); padding: var(--space-12) 0 var(--space-8); border-bottom: 2px solid var(--color-text); }
.section-heading p, .newsletter > div > p { margin: 0 0 var(--space-2); color: var(--color-brand); font-size: var(--font-size-xs); font-weight: 700; }
.masthead h1 { margin: 0; font-family: var(--font-serif); font-size: var(--font-size-3xl); line-height: var(--line-height-tight); }
.masthead-side > p { margin: 0 0 var(--space-5); color: var(--color-text-secondary); font-family: var(--font-serif); font-size: var(--font-size-lg); }
.home-search { display: grid; grid-template-columns: auto minmax(0, 1fr) auto; align-items: center; gap: var(--space-3); border-bottom: 1px solid var(--color-border-strong); }
.home-search input { min-width: 0; padding: var(--space-3) 0; border: 0; background: transparent; color: var(--color-text); outline: 0; }
.home-search button { padding: var(--space-2); border: 0; background: transparent; color: var(--color-brand); cursor: pointer; font-weight: 650; }
.function-nav { display: grid; grid-template-columns: repeat(3, 1fr); border-bottom: 1px solid var(--color-border); }
.function-nav a { display: grid; grid-template-columns: auto minmax(0, 1fr) auto; align-items: center; gap: var(--space-3); padding: var(--space-5); border-right: 1px solid var(--color-border); color: var(--color-text); text-decoration: none; transition: background-color var(--transition-fast), color var(--transition-fast); }
.function-nav a:last-child { border-right: 0; }
.function-nav a > .el-icon:first-child { color: var(--color-brand); font-size: var(--font-size-xl); }
.function-nav a:hover { background: var(--color-brand-soft); color: var(--color-brand); }
.function-nav .arrow { transition: transform var(--transition-fast); }
.function-nav a:hover .arrow { transform: translateX(var(--space-1)); }
.latest-section { padding: var(--space-10) 0 var(--space-12); }
.section-heading { display: flex; align-items: end; justify-content: space-between; gap: var(--space-4); margin-bottom: var(--space-5); }
.section-heading h2, .newsletter h2 { margin: 0; font-family: var(--font-serif); font-size: var(--font-size-2xl); }
.section-heading > a { display: inline-flex; align-items: center; gap: var(--space-2); color: var(--color-brand); font-size: var(--font-size-sm); text-decoration: none; }
.article-list { border-top: 2px solid var(--color-text); }
.article-row { display: grid; grid-template-columns: 140px minmax(0, 1fr) auto; gap: var(--space-6); padding: var(--space-6) 0; border-bottom: 1px solid var(--color-border); }
.article-row > time { color: var(--color-text-tertiary); font-size: var(--font-size-xs); }
.article-labels { display: flex; flex-wrap: wrap; gap: var(--space-3); margin-bottom: var(--space-2); color: var(--color-brand); font-size: var(--font-size-xs); font-weight: 700; }
.article-title { display: inline-block; color: var(--color-text); font-family: var(--font-serif); font-size: var(--font-size-xl); font-weight: 650; line-height: 1.35; text-decoration: none; overflow-wrap: anywhere; }
.article-title:hover { color: var(--color-brand); }
.article-content > p { display: -webkit-box; margin: var(--space-2) 0 var(--space-3); overflow: hidden; color: var(--color-text-secondary); font-size: var(--font-size-sm); -webkit-box-orient: vertical; -webkit-line-clamp: 2; }
.article-meta { display: flex; flex-wrap: wrap; gap: var(--space-4); color: var(--color-text-tertiary); font-size: var(--font-size-xs); }
.article-arrow { align-self: center; padding: var(--space-3); color: var(--color-text-tertiary); text-decoration: none; }
.article-arrow:hover { color: var(--color-brand); transform: translateX(var(--space-1)); }
.loading-state, .error-state { max-width: 760px; padding: var(--space-8) 0; }
.error-state { color: var(--color-danger); }
.newsletter { display: grid; grid-template-columns: minmax(220px, 1fr) minmax(320px, 1fr); align-items: center; gap: var(--space-8); padding: var(--space-8) 0; border-top: 2px solid var(--color-text); }
.newsletter form { position: relative; display: grid; grid-template-columns: minmax(0, 1fr) auto; gap: var(--space-2); }
.newsletter input { min-width: 0; padding: var(--space-3); border: 1px solid var(--color-border-strong); border-radius: var(--radius-sm); background: var(--color-surface); color: var(--color-text); }
.form-error, .form-success { position: absolute; top: calc(100% + var(--space-1)); left: 0; font-size: var(--font-size-xs); }
.form-error { color: var(--color-danger); }
.form-success { color: var(--color-brand); }
.visually-hidden { position: absolute; width: 1px; height: 1px; overflow: hidden; clip-path: inset(50%); }
@media (max-width: 760px) {
  main { width: calc(100% - var(--space-6)); }
  .masthead { grid-template-columns: 1fr; gap: var(--space-5); padding: var(--space-8) 0 var(--space-6); }
  .masthead h1 { font-size: var(--font-size-2xl); }
  .function-nav { grid-template-columns: 1fr; }
  .function-nav a { border-right: 0; border-bottom: 1px solid var(--color-border); }
  .function-nav a:last-child { border-bottom: 0; }
  .latest-section { padding-top: var(--space-8); }
  .article-row { grid-template-columns: minmax(0, 1fr) auto; gap: var(--space-3); }
  .article-row > time { grid-column: 1 / -1; }
  .article-title { font-size: var(--font-size-lg); }
  .newsletter { grid-template-columns: 1fr; gap: var(--space-5); }
}
</style>
