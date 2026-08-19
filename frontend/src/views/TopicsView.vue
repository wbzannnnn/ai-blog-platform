<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ArrowRight, Search } from '@element-plus/icons-vue'
import SiteFooter from '@/components/SiteFooter.vue'
import SiteHeader from '@/components/SiteHeader.vue'
import { getOverview, getTagTrends } from '@/api/analytics'
import type { OverviewData, TagHotItem } from '@/api/analytics'

type SortMode = 'score' | 'articles' | 'views'

const topics = ref<TagHotItem[]>([])
const overview = ref<OverviewData | null>(null)
const keyword = ref('')
const sortMode = ref<SortMode>('score')
const loading = ref(false)
const loadError = ref('')

const maxHeatIndex = computed(() => Math.max(0, ...topics.value.map((topic) => topic.heatIndex)))

const heatBarWidth = (heatIndex: number): string => {
  if (maxHeatIndex.value <= 0) return '0%'
  return `${Math.round((heatIndex / maxHeatIndex.value) * 100)}%`
}

const filteredTopics = computed(() => {
  const search = keyword.value.trim().toLowerCase()
  const items = search ? topics.value.filter((topic) => topic.tagName.toLowerCase().includes(search)) : [...topics.value]
  return items.sort((a, b) => {
    if (sortMode.value === 'articles') return b.articles - a.articles
    if (sortMode.value === 'views') return b.views - a.views
    return b.heatIndex - a.heatIndex
  })
})

const loadTopics = async (): Promise<void> => {
  loading.value = true
  loadError.value = ''
  try {
    const [trendsResponse, overviewResponse] = await Promise.all([getTagTrends({}), getOverview()])
    if (trendsResponse.code === 200) topics.value = trendsResponse.data.allTags ?? []
    if (overviewResponse.code === 200) overview.value = overviewResponse.data
  } catch {
    loadError.value = '专题数据加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

onMounted(() => void loadTopics())
</script>

<template>
  <div class="topics-page">
    <SiteHeader />
    <main>
      <header class="page-header">
        <div><p>TOPICS</p><h1>专题</h1></div>
        <p>按真实标签数据浏览内容</p>
      </header>

      <dl v-if="overview" class="overview-strip">
        <div><dt>已发布文章</dt><dd>{{ overview.totalPosts }}</dd></div>
        <div><dt>专题数量</dt><dd>{{ overview.totalTags }}</dd></div>
        <div><dt>标签关联</dt><dd>{{ overview.totalTagRelations }}</dd></div>
        <div><dt>篇均标签</dt><dd>{{ overview.avgTagsPerPost }}</dd></div>
      </dl>

      <div class="topic-controls">
        <label class="topic-search">
          <el-icon><Search /></el-icon>
          <input v-model="keyword" type="search" placeholder="搜索专题" />
        </label>
        <div class="sort-control" role="group" aria-label="专题排序">
          <button type="button" :class="{ active: sortMode === 'score' }" @click="sortMode = 'score'">综合热度</button>
          <button type="button" :class="{ active: sortMode === 'articles' }" @click="sortMode = 'articles'">文章数</button>
          <button type="button" :class="{ active: sortMode === 'views' }" @click="sortMode = 'views'">阅读量</button>
        </div>
      </div>

      <div v-if="loading" class="page-state"><el-skeleton :rows="9" animated /></div>
      <div v-else-if="loadError" class="page-state error-state" role="alert">
        <p>{{ loadError }}</p><el-button @click="loadTopics">重新加载</el-button>
      </div>
      <section v-else class="topic-list" aria-label="专题列表">
        <RouterLink
          v-for="(topic, index) in filteredTopics"
          :key="topic.tagId"
          class="topic-row"
          :to="{ path: '/analytics', query: { tag: topic.tagId } }"
        >
          <span class="topic-index">{{ String(index + 1).padStart(2, '0') }}</span>
          <div class="topic-main">
            <div class="topic-title"><h2>{{ topic.tagName }}</h2><span><small>热度指数</small>{{ topic.heatIndex }}</span></div>
            <div class="score-track" aria-hidden="true"><span :style="{ width: heatBarWidth(topic.heatIndex) }"></span></div>
            <dl>
              <div><dt>文章</dt><dd>{{ topic.articles }}</dd></div>
              <div><dt>阅读</dt><dd>{{ topic.views }}</dd></div>
              <div><dt>点赞</dt><dd>{{ topic.likes }}</dd></div>
              <div><dt>评论</dt><dd>{{ topic.comments }}</dd></div>
            </dl>
          </div>
          <el-icon class="row-arrow"><ArrowRight /></el-icon>
        </RouterLink>
        <el-empty v-if="filteredTopics.length === 0" description="没有匹配的专题" />
      </section>
    </main>
    <SiteFooter />
  </div>
</template>

<style scoped>
.topics-page { min-height: 100vh; background: var(--color-canvas); }
main { width: min(calc(100% - var(--space-8)), 980px); min-height: calc(100vh - 170px); margin: 0 auto; padding: var(--space-10) 0 var(--space-16); }
.page-header { display: flex; align-items: end; justify-content: space-between; gap: var(--space-6); padding-bottom: var(--space-5); border-bottom: 2px solid var(--color-text); }
.page-header div > p { margin: 0 0 var(--space-2); color: var(--color-brand); font-size: var(--font-size-xs); font-weight: 700; }
.page-header h1 { margin: 0; font-family: var(--font-serif); font-size: var(--font-size-2xl); }
.page-header > p { margin: 0; color: var(--color-text-secondary); font-size: var(--font-size-sm); }
.overview-strip { display: grid; grid-template-columns: repeat(4, 1fr); margin: 0 0 var(--space-8); border-bottom: 1px solid var(--color-border); }
.overview-strip > div { display: flex; flex-direction: column-reverse; gap: var(--space-1); padding: var(--space-5); border-right: 1px solid var(--color-border); background: var(--color-surface); }
.overview-strip > div:last-child { border-right: 0; }
.overview-strip dt { color: var(--color-text-tertiary); font-size: var(--font-size-xs); }
.overview-strip dd { margin: 0; font-family: var(--font-serif); font-size: var(--font-size-2xl); }
.topic-controls { display: flex; align-items: center; justify-content: space-between; gap: var(--space-4); margin-bottom: var(--space-5); }
.topic-search { display: grid; grid-template-columns: auto minmax(0, 1fr); align-items: center; gap: var(--space-2); width: min(100%, 360px); padding: var(--space-2) var(--space-3); border: 1px solid var(--color-border-strong); border-radius: var(--radius-sm); background: var(--color-surface); }
.topic-search input { min-width: 0; border: 0; background: transparent; color: var(--color-text); outline: 0; }
.sort-control { display: inline-flex; padding: var(--space-1); border: 1px solid var(--color-border); border-radius: var(--radius-sm); background: var(--color-surface-subtle); }
.sort-control button { padding: var(--space-2) var(--space-3); border: 0; border-radius: var(--radius-sm); background: transparent; color: var(--color-text-secondary); cursor: pointer; }
.sort-control button.active { background: var(--color-surface); color: var(--color-brand); }
.topic-list { border-top: 2px solid var(--color-text); }
.topic-row { display: grid; grid-template-columns: 50px minmax(0, 1fr) auto; align-items: center; gap: var(--space-5); padding: var(--space-5) 0; border-bottom: 1px solid var(--color-border); color: var(--color-text); text-decoration: none; }
.topic-row:hover h2, .topic-row:hover .row-arrow { color: var(--color-brand); }
.topic-index { color: var(--color-text-tertiary); font-family: var(--font-serif); }
.topic-title { display: flex; align-items: baseline; justify-content: space-between; gap: var(--space-4); }
.topic-title h2 { margin: 0; font-family: var(--font-serif); font-size: var(--font-size-xl); transition: color var(--transition-fast); }
.topic-title > span { display: inline-flex; align-items: baseline; gap: var(--space-2); color: var(--color-brand); font-family: var(--font-serif); }
.topic-title small { color: var(--color-text-tertiary); font-family: var(--font-sans); font-size: var(--font-size-xs); font-weight: 400; }
.score-track { height: 4px; margin: var(--space-3) 0; background: var(--color-border); }
.score-track span { display: block; height: 100%; background: var(--color-brand); }
.topic-main dl { display: flex; flex-wrap: wrap; gap: var(--space-5); margin: 0; }
.topic-main dl div { display: flex; gap: var(--space-1); color: var(--color-text-tertiary); font-size: var(--font-size-xs); }
.topic-main dd { margin: 0; color: var(--color-text-secondary); }
.row-arrow { color: var(--color-text-tertiary); transition: color var(--transition-fast), transform var(--transition-fast); }
.topic-row:hover .row-arrow { transform: translateX(var(--space-1)); }
.page-state { max-width: 720px; padding: var(--space-8) 0; }
.error-state { color: var(--color-danger); }
@media (max-width: 680px) {
  main { width: calc(100% - var(--space-6)); padding-top: var(--space-8); }
  .page-header { align-items: flex-start; flex-direction: column; gap: var(--space-2); }
  .overview-strip { grid-template-columns: repeat(2, 1fr); }
  .overview-strip > div:nth-child(2) { border-right: 0; }
  .overview-strip > div:nth-child(-n + 2) { border-bottom: 1px solid var(--color-border); }
  .topic-controls { align-items: stretch; flex-direction: column; }
  .topic-search { width: 100%; }
  .sort-control { display: grid; grid-template-columns: repeat(3, 1fr); }
  .topic-row { grid-template-columns: 34px minmax(0, 1fr); gap: var(--space-3); }
  .row-arrow { display: none; }
  .topic-title h2 { font-size: var(--font-size-lg); }
}
</style>
