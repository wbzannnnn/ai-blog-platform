<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowRight, ChatLineRound, DataAnalysis, Pointer, View } from '@element-plus/icons-vue'
import SiteFooter from '@/components/SiteFooter.vue'
import SiteHeader from '@/components/SiteHeader.vue'
import {
  getEndDate,
  getOverview,
  getStartDate,
  getTagTrends,
  type CompareData,
  type OverviewData,
  type TagHotItem,
  type TimeSeriesPoint,
  type TrendData
} from '@/api/analytics'

type MetricKey = 'heat' | 'views' | 'likes' | 'comments' | 'articles'

interface ChartPoint {
  label: string
  primary: number
  comparison: number
  x: number
  primaryY: number
  comparisonY: number
}

const route = useRoute()
const router = useRouter()

const ranges = [
  { key: '7d', label: '近 7 天' },
  { key: '30d', label: '近 30 天' },
  { key: '90d', label: '近 90 天' },
  { key: '1y', label: '近 1 年' }
]

const metrics: { key: MetricKey; label: string; shortLabel: string }[] = [
  { key: 'heat', label: '综合热度', shortLabel: '热度' },
  { key: 'views', label: '累计阅读', shortLabel: '阅读' },
  { key: 'likes', label: '累计点赞', shortLabel: '点赞' },
  { key: 'comments', label: '累计评论', shortLabel: '评论' },
  { key: 'articles', label: '发布文章', shortLabel: '文章' }
]

const queryNumber = (key: string): number | null => {
  const raw = route.query[key]
  const value = Array.isArray(raw) ? raw[0] : raw
  if (!value) return null
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : null
}

const initialRange = String(route.query.range || '90d')
const initialMetric = String(route.query.metric || 'heat') as MetricKey

const selectedTagId = ref<number | null>(queryNumber('tag'))
const compareTagId = ref<number | null>(queryNumber('compare'))
const timeRange = ref(ranges.some(item => item.key === initialRange) ? initialRange : '90d')
const activeMetric = ref<MetricKey>(metrics.some(item => item.key === initialMetric) ? initialMetric : 'heat')
const overview = ref<OverviewData | null>(null)
const allTags = ref<TagHotItem[]>([])
const trendData = ref<TrendData | null>(null)
const compareData = ref<CompareData | null>(null)
const lastUpdated = ref(0)
const dataNote = ref('')
const pageLoading = ref(true)
const detailLoading = ref(false)
const pageError = ref('')
const detailError = ref('')
const hoveredPoint = ref<ChartPoint | null>(null)

const numberFormatter = new Intl.NumberFormat('zh-CN')
const compactFormatter = new Intl.NumberFormat('zh-CN', {
  notation: 'compact',
  maximumFractionDigits: 1
})

const formatNumber = (value?: number | null): string => compactFormatter.format(Number(value || 0))
const formatExact = (value?: number | null): string => numberFormatter.format(Number(value || 0))
const formatPercent = (value?: number | null): string => `${Number(value || 0).toFixed(1)}%`
const formatDate = (timestamp: number): string => new Date(timestamp).toLocaleDateString('zh-CN')
const formatUpdatedAt = (timestamp: number): string => timestamp
  ? new Date(timestamp).toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
  : '等待数据更新'

const totalHeat = computed(() => allTags.value.reduce((sum, tag) => sum + tag.heatIndex, 0))
const activeTagCount = computed(() => allTags.value.filter(tag => tag.articles > 0).length)
const inactiveTagCount = computed(() => Math.max(0, (overview.value?.totalTags || 0) - activeTagCount.value))
const maxHeat = computed(() => Math.max(1, ...allTags.value.map(tag => tag.heatIndex)))
const averageHeat = computed(() => allTags.value.length ? totalHeat.value / allTags.value.length : 0)
const topThreeShare = computed(() => {
  if (!totalHeat.value) return 0
  const topHeat = allTags.value.slice(0, 3).reduce((sum, tag) => sum + tag.heatIndex, 0)
  return topHeat * 100 / totalHeat.value
})
const activeTagRate = computed(() => {
  const total = overview.value?.totalTags || 0
  return total ? activeTagCount.value * 100 / total : 0
})

const selectedTag = computed(() => allTags.value.find(tag => tag.tagId === selectedTagId.value) || null)
const selectedTagRank = computed(() => {
  const index = allTags.value.findIndex(tag => tag.tagId === selectedTagId.value)
  return index >= 0 ? index + 1 : 0
})
const compareTagName = computed(() => allTags.value.find(tag => tag.tagId === compareTagId.value)?.tagName || '对比标签')
const metricLabel = computed(() => metrics.find(item => item.key === activeMetric.value)?.label || '综合热度')

const tagInteractions = (tag: TagHotItem): number => tag.likes + tag.comments
const tagEngagementRate = (tag: TagHotItem): number => tag.views ? tagInteractions(tag) * 100 / tag.views : 0
const heatPerArticle = (tag: TagHotItem): number => tag.articles ? tag.heatIndex / tag.articles : 0
const heatBarWidth = (tag: TagHotItem): string => `${Math.max(2, tag.heatIndex * 100 / maxHeat.value)}%`

const overviewCards = computed(() => [
  {
    label: '已发布文章',
    value: formatNumber(overview.value?.totalPosts),
    note: `篇均 ${formatNumber(overview.value?.avgViewsPerPost)} 次阅读`
  },
  {
    label: '全站阅读',
    value: formatNumber(overview.value?.totalViews),
    note: `${formatExact(overview.value?.totalViews)} 次累计访问`
  },
  {
    label: '内容互动',
    value: formatNumber(overview.value?.totalInteractions),
    note: `点赞 ${formatNumber(overview.value?.totalLikes)} · 评论 ${formatNumber(overview.value?.totalComments)}`
  },
  {
    label: '标签覆盖',
    value: `${activeTagCount.value}/${formatExact(overview.value?.totalTags)}`,
    note: `活跃率 ${formatPercent(activeTagRate.value)}`
  }
])

const selectedMetrics = computed(() => {
  const tag = selectedTag.value
  if (!tag) return []
  return [
    { label: '热度排名', value: `TOP ${selectedTagRank.value}`, note: `共 ${allTags.value.length} 个标签` },
    { label: '综合热度', value: formatExact(tag.heatIndex), note: `篇均 ${formatExact(Math.round(heatPerArticle(tag)))}` },
    { label: '关联内容', value: formatExact(tag.articles), note: '篇已发布文章' },
    { label: '累计阅读', value: formatNumber(tag.views), note: `${formatExact(tag.views)} 次` },
    { label: '互动率', value: formatPercent(tagEngagementRate(tag)), note: `${formatExact(tagInteractions(tag))} 次互动` }
  ]
})

const granularity = computed(() => {
  if (timeRange.value === '7d') return 'day'
  if (timeRange.value === '30d') return 'week'
  return 'month'
})

const pointMetricValue = (point: TimeSeriesPoint): number => {
  switch (activeMetric.value) {
    case 'views': return point.viewCount
    case 'likes': return point.likeCount
    case 'comments': return point.commentCount
    case 'articles': return point.articleCount
    default: return point.viewCount + point.likeCount * 3 + point.commentCount * 5 + point.articleCount * 10
  }
}

const chartRows = computed(() => {
  const current = trendData.value?.current || []
  const comparison = compareData.value?.current || []
  const labels = [...new Set([...current.map(item => item.label), ...comparison.map(item => item.label)])].sort()
  const currentMap = new Map(current.map(item => [item.label, pointMetricValue(item)]))
  const comparisonMap = new Map(comparison.map(item => [item.label, pointMetricValue(item)]))
  return labels.map(label => ({
    label,
    primary: currentMap.get(label) || 0,
    comparison: comparisonMap.get(label) || 0
  }))
})

const chartMaximum = computed(() => Math.max(
  1,
  ...chartRows.value.flatMap(item => compareData.value
    ? [item.primary, item.comparison]
    : [item.primary])
))

const chartPoints = computed<ChartPoint[]>(() => {
  const left = 58
  const right = 698
  const top = 22
  const bottom = 224
  const height = bottom - top
  return chartRows.value.map((item, index) => {
    const x = chartRows.value.length === 1
      ? (left + right) / 2
      : left + index * (right - left) / (chartRows.value.length - 1)
    return {
      ...item,
      x,
      primaryY: bottom - item.primary / chartMaximum.value * height,
      comparisonY: bottom - item.comparison / chartMaximum.value * height
    }
  })
})

const chartPolyline = computed(() => chartPoints.value.map(point => `${point.x},${point.primaryY}`).join(' '))
const comparePolyline = computed(() => chartPoints.value.map(point => `${point.x},${point.comparisonY}`).join(' '))
const yTicks = computed(() => [1, 0.75, 0.5, 0.25, 0].map(ratio => ({
  y: 224 - ratio * 202,
  value: Math.round(chartMaximum.value * ratio)
})))
const shouldShowChartLabel = (index: number): boolean => {
  const interval = Math.max(1, Math.ceil(chartPoints.value.length / 6))
  return index % interval === 0 || index === chartPoints.value.length - 1
}

const formatChange = (value: number | null): string => {
  if (value === null) return '暂无环比'
  if (value === 0) return '与上期持平'
  return `${value > 0 ? '↑' : '↓'} ${Math.abs(value).toFixed(1)}%`
}

const changeClass = (value: number | null): string => {
  if (value === null || value === 0) return 'is-flat'
  return value > 0 ? 'is-up' : 'is-down'
}

const syncUrl = (): void => {
  const query: Record<string, string> = {}
  if (selectedTagId.value) query.tag = String(selectedTagId.value)
  if (compareTagId.value) query.compare = String(compareTagId.value)
  if (timeRange.value !== '90d') query.range = timeRange.value
  if (activeMetric.value !== 'heat') query.metric = activeMetric.value
  void router.replace({ path: '/analytics', query })
}

const loadTrend = async (): Promise<void> => {
  if (!selectedTagId.value) return
  detailLoading.value = true
  detailError.value = ''
  hoveredPoint.value = null
  try {
    const response = await getTagTrends({
      tagId: selectedTagId.value,
      compareTagId: compareTagId.value || undefined,
      startDate: getStartDate(timeRange.value),
      endDate: getEndDate(),
      granularity: granularity.value
    })
    if (response.code !== 200 || !response.data) {
      throw new Error(response.message || '标签分析数据暂不可用')
    }
    trendData.value = response.data.trend || null
    compareData.value = response.data.compare || null
    lastUpdated.value = response.data.lastUpdated
    dataNote.value = response.data.dataNote
  } catch (error) {
    trendData.value = null
    compareData.value = null
    detailError.value = error instanceof Error ? error.message : '标签分析数据加载失败'
  } finally {
    detailLoading.value = false
  }
}

const loadDashboard = async (): Promise<void> => {
  pageLoading.value = true
  pageError.value = ''
  try {
    const [overviewResponse, tagsResponse] = await Promise.all([getOverview(), getTagTrends({})])
    if (overviewResponse.code !== 200 || tagsResponse.code !== 200) {
      throw new Error(overviewResponse.message || tagsResponse.message || '数据分析服务暂不可用')
    }
    overview.value = overviewResponse.data
    allTags.value = tagsResponse.data?.allTags || []
    lastUpdated.value = tagsResponse.data?.lastUpdated || Date.now()
    dataNote.value = tagsResponse.data?.dataNote || ''

    const selectedExists = allTags.value.some(tag => tag.tagId === selectedTagId.value)
    selectedTagId.value = selectedExists ? selectedTagId.value : (allTags.value[0]?.tagId || null)
    if (compareTagId.value === selectedTagId.value || !allTags.value.some(tag => tag.tagId === compareTagId.value)) {
      compareTagId.value = null
    }
    if (selectedTagId.value) await loadTrend()
  } catch (error) {
    pageError.value = error instanceof Error ? error.message : '数据看板加载失败，请确认后端服务已启动'
  } finally {
    pageLoading.value = false
  }
}

const selectTag = async (tagId: number | null): Promise<void> => {
  selectedTagId.value = tagId
  if (compareTagId.value === tagId) compareTagId.value = null
  trendData.value = null
  compareData.value = null
  syncUrl()
  await loadTrend()
}

const selectCompareTag = async (tagId: number | null): Promise<void> => {
  compareTagId.value = tagId === selectedTagId.value ? null : tagId
  syncUrl()
  await loadTrend()
}

const switchRange = async (range: string): Promise<void> => {
  if (timeRange.value === range) return
  timeRange.value = range
  syncUrl()
  await loadTrend()
}

const switchMetric = (metric: MetricKey): void => {
  activeMetric.value = metric
  hoveredPoint.value = null
  syncUrl()
}

onMounted(() => void loadDashboard())
</script>

<template>
  <div class="analytics-page">
    <SiteHeader />

    <main>
      <section class="hero" aria-labelledby="analytics-title">
        <div class="hero-copy">
          <p class="eyebrow"><span class="live-dot"></span> CONTENT INTELLIGENCE</p>
          <h1 id="analytics-title">标签热度分析</h1>
          <p class="hero-description">从标签热度看内容表现，用阅读、互动与发布数据发现真正值得持续投入的主题。</p>
          <span class="updated-at">数据更新于 {{ formatUpdatedAt(lastUpdated) }}</span>
        </div>
        <div class="hero-focus" aria-label="标签热度总览">
          <span>标签热度总量</span>
          <strong>{{ formatNumber(totalHeat) }}</strong>
          <div v-if="allTags[0]" class="hero-leader">
            <span>当前领跑</span>
            <button type="button" @click="selectTag(allTags[0].tagId)"># {{ allTags[0].tagName }}</button>
            <b>{{ formatExact(allTags[0].heatIndex) }}</b>
          </div>
          <div class="heat-formula">阅读 + 点赞×3 + 评论×5 + 文章×10</div>
        </div>
      </section>

      <div v-if="pageError" class="error-banner" role="alert">
        <div><strong>数据加载失败</strong><span>{{ pageError }}</span></div>
        <button type="button" @click="loadDashboard">重新加载</button>
      </div>

      <template v-if="pageLoading">
        <section class="overview-grid" aria-label="正在加载核心指标">
          <div v-for="index in 4" :key="index" class="overview-card skeleton-card">
            <span></span><b></b><i></i>
          </div>
        </section>
        <div class="panel loading-panel"><el-skeleton :rows="8" animated /></div>
      </template>

      <template v-else-if="!pageError">
        <section class="overview-grid" aria-label="核心数据指标">
          <article v-for="card in overviewCards" :key="card.label" class="overview-card">
            <span>{{ card.label }}</span>
            <strong>{{ card.value }}</strong>
            <p>{{ card.note }}</p>
          </article>
        </section>

        <section class="analysis-grid" aria-label="标签热度概览">
          <article class="panel ranking-panel">
            <div class="panel-heading">
              <div><p class="eyebrow">HOT TOPICS</p><h2>标签热度排行</h2></div>
              <span>综合热度 · 全部时间</span>
            </div>

            <div v-if="allTags.length" class="ranking-list">
              <button
                v-for="(tag, index) in allTags.slice(0, 10)"
                :key="tag.tagId"
                type="button"
                :class="['ranking-item', { selected: tag.tagId === selectedTagId }]"
                @click="selectTag(tag.tagId)"
              >
                <span :class="['rank-number', { leading: index < 3 }]">{{ String(index + 1).padStart(2, '0') }}</span>
                <span class="rank-main">
                  <span class="rank-label"><b># {{ tag.tagName }}</b><small>{{ tag.articles }} 篇内容</small></span>
                  <span class="heat-track"><i :style="{ width: heatBarWidth(tag) }"></i></span>
                </span>
                <strong>{{ formatExact(tag.heatIndex) }}</strong>
              </button>
            </div>
            <el-empty v-else description="还没有可分析的标签数据" />
          </article>

          <aside class="panel structure-panel">
            <div class="panel-heading compact">
              <div><p class="eyebrow">HEALTH CHECK</p><h2>热度结构</h2></div>
            </div>
            <div class="share-figure">
              <div class="share-ring" :style="{ '--share': `${Math.min(100, topThreeShare)}%` }">
                <div><strong>{{ formatPercent(topThreeShare) }}</strong><span>TOP 3 占比</span></div>
              </div>
              <p>头部标签贡献了 {{ formatPercent(topThreeShare) }} 的总热度，可结合长尾标签判断内容结构是否均衡。</p>
            </div>
            <dl class="structure-metrics">
              <div><dt>活跃标签率</dt><dd>{{ formatPercent(activeTagRate) }}</dd><span><i :style="{ width: `${activeTagRate}%` }"></i></span></div>
              <div><dt>平均标签热度</dt><dd>{{ formatExact(Math.round(averageHeat)) }}</dd></div>
              <div><dt>篇均标签数</dt><dd>{{ overview?.avgTagsPerPost || 0 }}</dd></div>
              <div><dt>暂无内容标签</dt><dd>{{ inactiveTagCount }}</dd></div>
            </dl>
          </aside>
        </section>

        <section v-if="allTags.length" class="detail-section" aria-labelledby="trend-title">
          <div class="section-heading">
            <div>
              <p class="eyebrow">DEEP DIVE</p>
              <h2 id="trend-title">标签趋势洞察</h2>
            </div>
            <p>选择标签与周期，查看热度由哪些内容和互动构成。</p>
          </div>

          <div class="filter-bar">
            <label>
              <span>分析标签</span>
              <el-select v-model="selectedTagId" filterable placeholder="选择标签" @change="selectTag">
                <el-option v-for="tag in allTags" :key="tag.tagId" :label="tag.tagName" :value="tag.tagId" />
              </el-select>
            </label>
            <label>
              <span>对比标签</span>
              <el-select v-model="compareTagId" filterable clearable placeholder="不对比" @change="selectCompareTag">
                <el-option
                  v-for="tag in allTags.filter(item => item.tagId !== selectedTagId)"
                  :key="tag.tagId"
                  :label="tag.tagName"
                  :value="tag.tagId"
                />
              </el-select>
            </label>
            <div class="range-filter">
              <span>分析周期</span>
              <div>
                <button
                  v-for="range in ranges"
                  :key="range.key"
                  type="button"
                  :class="{ active: timeRange === range.key }"
                  @click="switchRange(range.key)"
                >{{ range.label }}</button>
              </div>
            </div>
          </div>

          <div v-if="selectedTag" class="selected-header">
            <div><span>当前分析标签</span><h3># {{ selectedTag.tagName }}</h3></div>
            <p v-if="trendData">{{ trendData.analysis }}</p>
          </div>

          <div v-if="selectedTag" class="tag-metric-grid">
            <article v-for="item in selectedMetrics" :key="item.label">
              <span>{{ item.label }}</span><strong>{{ item.value }}</strong><small>{{ item.note }}</small>
            </article>
          </div>

          <div v-if="detailLoading" class="panel loading-panel"><el-skeleton :rows="9" animated /></div>
          <div v-else-if="detailError" class="detail-error" role="alert">
            <p>{{ detailError }}</p><el-button type="primary" @click="loadTrend">重试</el-button>
          </div>

          <template v-else-if="trendData">
            <article class="panel chart-panel">
              <div class="chart-heading">
                <div>
                  <span>内容走势</span>
                  <h3>{{ metricLabel }}</h3>
                </div>
                <div class="period-summary">
                  <span>本周期发文 {{ trendData.summary.curPeriodTotal }} 篇</span>
                  <b :class="changeClass(trendData.summary.changePercent)">{{ formatChange(trendData.summary.changePercent) }}</b>
                  <span>峰值 {{ trendData.summary.peakLabel }}</span>
                </div>
              </div>

              <div class="metric-tabs" aria-label="切换趋势指标">
                <button
                  v-for="metric in metrics"
                  :key="metric.key"
                  type="button"
                  :class="{ active: activeMetric === metric.key }"
                  @click="switchMetric(metric.key)"
                >{{ metric.label }}</button>
              </div>

              <div v-if="chartPoints.length" class="chart-wrap" @mouseleave="hoveredPoint = null">
                <svg viewBox="0 0 720 270" role="img" :aria-label="`${selectedTag?.tagName || ''}${metricLabel}趋势图`">
                  <g v-for="tick in yTicks" :key="tick.y">
                    <line class="grid-line" x1="58" :y1="tick.y" x2="698" :y2="tick.y" />
                    <text class="axis-label" x="48" :y="tick.y + 4" text-anchor="end">{{ formatNumber(tick.value) }}</text>
                  </g>
                  <polyline v-if="compareData" class="compare-line" :points="comparePolyline" />
                  <polyline class="primary-line" :points="chartPolyline" />
                  <g v-for="(point, index) in chartPoints" :key="point.label">
                    <circle
                      class="chart-point"
                      :cx="point.x"
                      :cy="point.primaryY"
                      r="5"
                      tabindex="0"
                      @mouseenter="hoveredPoint = point"
                      @focus="hoveredPoint = point"
                    />
                    <text v-if="shouldShowChartLabel(index)" class="x-label" :x="point.x" y="254" text-anchor="middle">{{ point.label }}</text>
                  </g>
                </svg>
                <div
                  v-if="hoveredPoint"
                  class="chart-tooltip"
                  :style="{ left: `${hoveredPoint.x / 7.2}%`, top: `${hoveredPoint.primaryY / 2.7}%` }"
                >
                  <b>{{ hoveredPoint.label }}</b>
                  <span>{{ selectedTag?.tagName }}：{{ formatExact(hoveredPoint.primary) }}</span>
                  <span v-if="compareData">{{ compareTagName }}：{{ formatExact(hoveredPoint.comparison) }}</span>
                </div>
                <div class="chart-legend">
                  <span><i></i>{{ selectedTag?.tagName }}</span>
                  <span v-if="compareData" class="comparison"><i></i>{{ compareTagName }}</span>
                </div>
              </div>
              <div v-else class="chart-empty">该周期暂无已发布内容，换一个时间范围试试。</div>
            </article>

            <article v-if="trendData.topArticles?.length" class="panel articles-panel">
              <div class="panel-heading">
                <div><p class="eyebrow">TOP CONTENT</p><h2>热度贡献文章</h2></div>
                <span>按阅读与互动综合排序</span>
              </div>
              <div class="article-list">
                <RouterLink v-for="(article, index) in trendData.topArticles" :key="article.id" :to="`/post/${article.id}`">
                  <span class="article-index">{{ String(index + 1).padStart(2, '0') }}</span>
                  <div><h3>{{ article.title }}</h3><time>{{ formatDate(article.createdAt) }}</time></div>
                  <ul>
                    <li><el-icon><View /></el-icon>{{ formatNumber(article.viewCount) }}</li>
                    <li><el-icon><Pointer /></el-icon>{{ formatNumber(article.likeCount) }}</li>
                    <li><el-icon><ChatLineRound /></el-icon>{{ formatNumber(article.commentCount) }}</li>
                  </ul>
                  <el-icon class="article-arrow"><ArrowRight /></el-icon>
                </RouterLink>
              </div>
            </article>
          </template>
        </section>

        <section v-if="allTags.length" class="all-tags-section" aria-labelledby="all-tags-title">
          <div class="section-heading">
            <div><p class="eyebrow">FULL RANKING</p><h2 id="all-tags-title">全部标签指标</h2></div>
            <p>互动率 =（点赞 + 评论）/ 阅读量</p>
          </div>
          <div class="table-wrap">
            <table>
              <thead><tr><th>排名</th><th>标签</th><th>综合热度</th><th>文章</th><th>阅读</th><th>互动</th><th>互动率</th><th>篇均热度</th></tr></thead>
              <tbody>
                <tr v-for="(tag, index) in allTags" :key="tag.tagId" :class="{ selected: tag.tagId === selectedTagId }">
                  <td><span :class="{ leading: index < 3 }">{{ String(index + 1).padStart(2, '0') }}</span></td>
                  <td><button type="button" @click="selectTag(tag.tagId)"># {{ tag.tagName }}</button></td>
                  <td><strong>{{ formatExact(tag.heatIndex) }}</strong></td>
                  <td>{{ formatExact(tag.articles) }}</td>
                  <td>{{ formatExact(tag.views) }}</td>
                  <td>{{ formatExact(tagInteractions(tag)) }}</td>
                  <td>{{ formatPercent(tagEngagementRate(tag)) }}</td>
                  <td>{{ formatExact(Math.round(heatPerArticle(tag))) }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>

        <div v-if="dataNote" class="data-note">
          <el-icon><DataAnalysis /></el-icon>
          <p><strong>指标口径</strong>{{ dataNote }}</p>
        </div>
      </template>
    </main>

    <SiteFooter />
  </div>
</template>

<style scoped>
.analytics-page { min-height: 100vh; background: var(--color-canvas); }
main { width: min(calc(100% - var(--space-8)), var(--content-width)); margin: 0 auto; padding-bottom: var(--space-16); }
button { color: inherit; }
.eyebrow { margin: 0 0 var(--space-2); color: var(--color-brand); font-size: var(--font-size-xs); font-weight: 750; letter-spacing: .11em; }
.hero { display: grid; grid-template-columns: minmax(0, 1.5fr) minmax(320px, .7fr); align-items: stretch; border-bottom: 2px solid var(--color-text); }
.hero-copy { padding: var(--space-12) var(--space-10) var(--space-10) 0; }
.hero h1 { margin: 0; font-family: var(--font-serif); font-size: clamp(2.5rem, 6vw, 4.75rem); font-weight: 650; letter-spacing: -.04em; line-height: 1.05; }
.hero-description { max-width: 720px; margin: var(--space-5) 0 var(--space-6); color: var(--color-text-secondary); font-family: var(--font-serif); font-size: var(--font-size-lg); }
.updated-at { color: var(--color-text-tertiary); font-size: var(--font-size-xs); }
.live-dot { display: inline-block; width: 7px; height: 7px; margin-right: var(--space-2); border-radius: 50%; background: var(--color-brand); box-shadow: 0 0 0 4px var(--color-brand-soft); }
.hero-focus { display: flex; min-width: 0; flex-direction: column; justify-content: center; padding: var(--space-8); border-left: 1px solid var(--color-border); background: var(--color-surface); }
.hero-focus > span { color: var(--color-text-secondary); font-size: var(--font-size-sm); }
.hero-focus > strong { margin: var(--space-1) 0 var(--space-5); color: var(--color-brand); font-family: var(--font-serif); font-size: clamp(2.8rem, 5vw, 4.5rem); font-weight: 650; line-height: 1; }
.hero-leader { display: grid; grid-template-columns: 1fr auto; align-items: center; gap: var(--space-1) var(--space-3); padding-top: var(--space-4); border-top: 1px solid var(--color-border); }
.hero-leader span { grid-column: 1 / -1; color: var(--color-text-tertiary); font-size: var(--font-size-xs); }
.hero-leader button { width: fit-content; padding: 0; border: 0; background: transparent; cursor: pointer; font-weight: 700; }
.hero-leader button:hover { color: var(--color-brand); }
.hero-leader b { color: var(--color-brand); font-family: var(--font-serif); }
.heat-formula { margin-top: var(--space-5); color: var(--color-text-tertiary); font-size: .6875rem; }
.overview-grid { display: grid; grid-template-columns: repeat(4, 1fr); border-bottom: 1px solid var(--color-border); }
.overview-card { min-width: 0; padding: var(--space-6); border-right: 1px solid var(--color-border); background: var(--color-surface); }
.overview-card:last-child { border-right: 0; }
.overview-card > span { color: var(--color-text-secondary); font-size: var(--font-size-xs); font-weight: 650; }
.overview-card strong { display: block; margin: var(--space-2) 0; font-family: var(--font-serif); font-size: clamp(1.8rem, 4vw, 2.6rem); font-weight: 600; line-height: 1.15; }
.overview-card p { margin: 0; overflow: hidden; color: var(--color-text-tertiary); font-size: var(--font-size-xs); text-overflow: ellipsis; white-space: nowrap; }
.panel { border: 1px solid var(--color-border); background: var(--color-surface); }
.analysis-grid { display: grid; grid-template-columns: minmax(0, 1.7fr) minmax(280px, .8fr); gap: var(--space-5); padding: var(--space-10) 0; }
.ranking-panel, .structure-panel, .chart-panel, .articles-panel { padding: var(--space-6); }
.panel-heading, .section-heading, .chart-heading { display: flex; align-items: end; justify-content: space-between; gap: var(--space-4); }
.panel-heading { margin-bottom: var(--space-5); }
.panel-heading h2, .section-heading h2 { margin: 0; font-family: var(--font-serif); font-size: var(--font-size-xl); }
.panel-heading > span, .section-heading > p { margin: 0; color: var(--color-text-tertiary); font-size: var(--font-size-xs); }
.ranking-list { border-top: 1px solid var(--color-border-strong); }
.ranking-item { display: grid; grid-template-columns: 34px minmax(0, 1fr) auto; align-items: center; gap: var(--space-3); width: 100%; padding: var(--space-3) var(--space-2); border: 0; border-bottom: 1px solid var(--color-border); background: transparent; cursor: pointer; text-align: left; transition: background var(--transition-fast); }
.ranking-item:hover, .ranking-item.selected { background: var(--color-brand-soft); }
.rank-number, .article-index { color: var(--color-text-tertiary); font-family: var(--font-mono); font-size: var(--font-size-xs); }
.rank-number.leading, td .leading { color: var(--color-brand); font-weight: 750; }
.rank-main { display: grid; min-width: 0; gap: var(--space-2); }
.rank-label { display: flex; align-items: center; justify-content: space-between; gap: var(--space-3); }
.rank-label b { overflow: hidden; font-size: var(--font-size-sm); text-overflow: ellipsis; white-space: nowrap; }
.rank-label small { flex: 0 0 auto; color: var(--color-text-tertiary); }
.heat-track { height: 4px; overflow: hidden; border-radius: 99px; background: var(--color-surface-subtle); }
.heat-track i { display: block; height: 100%; border-radius: inherit; background: var(--color-brand); }
.ranking-item > strong { min-width: 58px; color: var(--color-brand); font-family: var(--font-serif); font-size: var(--font-size-lg); text-align: right; }
.share-figure { padding-bottom: var(--space-6); border-bottom: 1px solid var(--color-border); }
.share-ring { display: grid; width: 150px; height: 150px; margin: var(--space-4) auto var(--space-5); place-items: center; border-radius: 50%; background: conic-gradient(var(--color-brand) var(--share), var(--color-brand-soft) 0); }
.share-ring::before { grid-area: 1 / 1; width: 112px; height: 112px; border-radius: 50%; background: var(--color-surface); content: ''; }
.share-ring div { z-index: 1; display: flex; grid-area: 1 / 1; flex-direction: column; align-items: center; }
.share-ring strong { font-family: var(--font-serif); font-size: var(--font-size-xl); }
.share-ring span { color: var(--color-text-tertiary); font-size: .6875rem; }
.share-figure p { margin: 0; color: var(--color-text-secondary); font-size: var(--font-size-xs); }
.structure-metrics { margin: var(--space-5) 0 0; }
.structure-metrics > div { display: grid; grid-template-columns: minmax(0, 1fr) auto; gap: var(--space-2); padding: var(--space-3) 0; border-bottom: 1px solid var(--color-border); }
.structure-metrics > div:last-child { border-bottom: 0; }
.structure-metrics dt { color: var(--color-text-secondary); font-size: var(--font-size-sm); }
.structure-metrics dd { margin: 0; font-family: var(--font-serif); font-weight: 700; }
.structure-metrics span { grid-column: 1 / -1; height: 3px; overflow: hidden; border-radius: 99px; background: var(--color-surface-subtle); }
.structure-metrics i { display: block; height: 100%; background: var(--color-brand); }
.detail-section, .all-tags-section { padding: var(--space-10) 0; border-top: 2px solid var(--color-text); }
.section-heading { margin-bottom: var(--space-6); }
.section-heading h2 { font-size: var(--font-size-2xl); }
.filter-bar { display: grid; grid-template-columns: minmax(180px, .8fr) minmax(180px, .8fr) minmax(360px, 1.6fr); gap: var(--space-5); margin-bottom: var(--space-6); padding: var(--space-5); border: 1px solid var(--color-border); background: var(--color-surface); }
.filter-bar label, .range-filter { display: flex; min-width: 0; flex-direction: column; gap: var(--space-2); }
.filter-bar label > span, .range-filter > span { color: var(--color-text-secondary); font-size: var(--font-size-xs); font-weight: 650; }
.range-filter > div { display: flex; }
.range-filter button, .metric-tabs button { flex: 1; padding: var(--space-2) var(--space-3); border: 1px solid var(--color-border); border-right: 0; background: var(--color-surface); color: var(--color-text-secondary); cursor: pointer; font-size: var(--font-size-xs); white-space: nowrap; }
.range-filter button:last-child { border-right: 1px solid var(--color-border); }
.range-filter button:hover, .metric-tabs button:hover { color: var(--color-brand); }
.range-filter button.active, .metric-tabs button.active { border-color: var(--color-brand); background: var(--color-brand); color: white; }
.selected-header { display: grid; grid-template-columns: auto minmax(0, 1fr); align-items: center; gap: var(--space-8); padding-bottom: var(--space-4); }
.selected-header span { color: var(--color-text-tertiary); font-size: var(--font-size-xs); }
.selected-header h3 { margin: 0; font-family: var(--font-serif); font-size: var(--font-size-xl); }
.selected-header p { margin: 0; color: var(--color-text-secondary); font-size: var(--font-size-sm); }
.tag-metric-grid { display: grid; grid-template-columns: repeat(5, 1fr); margin-bottom: var(--space-5); border-top: 1px solid var(--color-border-strong); border-bottom: 1px solid var(--color-border); }
.tag-metric-grid article { padding: var(--space-4); border-right: 1px solid var(--color-border); background: var(--color-surface); }
.tag-metric-grid article:last-child { border-right: 0; }
.tag-metric-grid span, .tag-metric-grid small { display: block; color: var(--color-text-tertiary); font-size: var(--font-size-xs); }
.tag-metric-grid strong { display: block; margin: var(--space-1) 0; font-family: var(--font-serif); font-size: var(--font-size-xl); }
.chart-panel { margin-bottom: var(--space-5); }
.chart-heading { align-items: center; }
.chart-heading span { color: var(--color-text-tertiary); font-size: var(--font-size-xs); }
.chart-heading h3 { margin: 0; font-family: var(--font-serif); font-size: var(--font-size-xl); }
.period-summary { display: flex; align-items: center; gap: var(--space-4); }
.period-summary b { color: var(--color-text-secondary); font-size: var(--font-size-sm); }
.period-summary .is-up { color: var(--color-brand); }
.period-summary .is-down { color: var(--color-danger); }
.metric-tabs { display: flex; width: fit-content; margin: var(--space-5) 0 var(--space-4); }
.metric-tabs button { flex: 0 0 auto; }
.metric-tabs button:last-child { border-right: 1px solid var(--color-border); }
.chart-wrap { position: relative; min-width: 0; overflow-x: auto; }
.chart-wrap svg { min-width: 680px; width: 100%; }
.grid-line { stroke: var(--color-border); stroke-width: 1; }
.axis-label, .x-label { fill: var(--color-text-tertiary); font-family: var(--font-sans); font-size: 10px; }
.primary-line, .compare-line { fill: none; stroke: var(--color-brand); stroke-linecap: round; stroke-linejoin: round; stroke-width: 3; }
.compare-line { stroke: var(--color-warning); stroke-dasharray: 7 6; stroke-width: 2; }
.chart-point { fill: var(--color-surface); stroke: var(--color-brand); stroke-width: 3; cursor: pointer; }
.chart-point:hover, .chart-point:focus { fill: var(--color-brand); outline: none; }
.chart-tooltip { position: absolute; z-index: 2; display: flex; min-width: 130px; flex-direction: column; padding: var(--space-2) var(--space-3); border-radius: var(--radius-sm); background: var(--color-text); color: white; font-size: var(--font-size-xs); pointer-events: none; transform: translate(-50%, calc(-100% - 12px)); }
.chart-tooltip span { color: #dfe7e2; }
.chart-legend { display: flex; justify-content: center; gap: var(--space-5); color: var(--color-text-secondary); font-size: var(--font-size-xs); }
.chart-legend span { display: inline-flex; align-items: center; gap: var(--space-2); }
.chart-legend i { width: 20px; height: 3px; background: var(--color-brand); }
.chart-legend .comparison i { height: 0; border-top: 2px dashed var(--color-warning); background: transparent; }
.chart-empty, .detail-error { padding: var(--space-12); color: var(--color-text-tertiary); text-align: center; }
.articles-panel { margin-top: var(--space-5); }
.article-list { border-top: 1px solid var(--color-border-strong); }
.article-list > a { display: grid; grid-template-columns: 34px minmax(0, 1fr) auto auto; align-items: center; gap: var(--space-4); padding: var(--space-4) var(--space-2); border-bottom: 1px solid var(--color-border); color: var(--color-text); text-decoration: none; }
.article-list > a:hover { background: var(--color-brand-soft); }
.article-list h3 { margin: 0; overflow: hidden; font-size: var(--font-size-sm); text-overflow: ellipsis; white-space: nowrap; }
.article-list time { color: var(--color-text-tertiary); font-size: var(--font-size-xs); }
.article-list ul { display: flex; gap: var(--space-4); margin: 0; padding: 0; color: var(--color-text-tertiary); font-size: var(--font-size-xs); list-style: none; }
.article-list li { display: flex; align-items: center; gap: var(--space-1); }
.article-arrow { color: var(--color-brand); }
.table-wrap { overflow-x: auto; border: 1px solid var(--color-border); background: var(--color-surface); }
table { width: 100%; min-width: 860px; border-collapse: collapse; font-size: var(--font-size-sm); }
th, td { padding: var(--space-3) var(--space-4); border-bottom: 1px solid var(--color-border); text-align: right; }
th { background: var(--color-surface-subtle); color: var(--color-text-secondary); font-size: var(--font-size-xs); font-weight: 650; }
th:first-child, th:nth-child(2), td:first-child, td:nth-child(2) { text-align: left; }
tbody tr:hover, tbody tr.selected { background: var(--color-brand-soft); }
td:first-child { color: var(--color-text-tertiary); font-family: var(--font-mono); font-size: var(--font-size-xs); }
td button { padding: 0; border: 0; background: transparent; cursor: pointer; font-weight: 650; }
td button:hover { color: var(--color-brand); }
td strong { color: var(--color-brand); }
.data-note { display: flex; align-items: flex-start; gap: var(--space-3); padding: var(--space-5); border-left: 3px solid var(--color-brand); background: var(--color-brand-soft); color: var(--color-text-secondary); font-size: var(--font-size-xs); }
.data-note .el-icon { flex: 0 0 auto; margin-top: 3px; color: var(--color-brand); }
.data-note p { margin: 0; }
.data-note strong { display: block; margin-bottom: var(--space-1); color: var(--color-text); }
.error-banner { display: flex; align-items: center; justify-content: space-between; gap: var(--space-4); margin: var(--space-6) 0; padding: var(--space-4) var(--space-5); border: 1px solid var(--color-danger); background: var(--color-danger-soft); color: var(--color-danger); }
.error-banner div { display: flex; flex-direction: column; }
.error-banner button { padding: var(--space-2) var(--space-4); border: 1px solid currentColor; background: transparent; cursor: pointer; }
.loading-panel { margin: var(--space-8) 0; padding: var(--space-8); }
.skeleton-card span, .skeleton-card b, .skeleton-card i { display: block; height: 12px; margin-bottom: var(--space-3); border-radius: 99px; background: var(--color-border); animation: pulse 1.4s ease-in-out infinite; }
.skeleton-card b { width: 55%; height: 34px; }
.skeleton-card i { width: 75%; margin-bottom: 0; }
@keyframes pulse { 50% { opacity: .35; } }
@media (max-width: 920px) {
  .hero { grid-template-columns: 1fr; }
  .hero-focus { border-top: 1px solid var(--color-border); border-left: 0; }
  .analysis-grid { grid-template-columns: 1fr; }
  .filter-bar { grid-template-columns: repeat(2, 1fr); }
  .range-filter { grid-column: 1 / -1; }
  .tag-metric-grid { grid-template-columns: repeat(3, 1fr); }
  .tag-metric-grid article:nth-child(3) { border-right: 0; }
  .tag-metric-grid article:nth-child(-n + 3) { border-bottom: 1px solid var(--color-border); }
}
@media (max-width: 680px) {
  main { width: calc(100% - var(--space-6)); }
  .hero-copy { padding: var(--space-8) 0 var(--space-6); }
  .hero h1 { font-size: 2.6rem; }
  .hero-focus { padding: var(--space-6); }
  .overview-grid { grid-template-columns: repeat(2, 1fr); }
  .overview-card:nth-child(2) { border-right: 0; }
  .overview-card:nth-child(-n + 2) { border-bottom: 1px solid var(--color-border); }
  .analysis-grid, .detail-section, .all-tags-section { padding: var(--space-8) 0; }
  .ranking-panel, .structure-panel, .chart-panel, .articles-panel { padding: var(--space-4); }
  .panel-heading > span, .section-heading > p { display: none; }
  .filter-bar { grid-template-columns: 1fr; }
  .range-filter { grid-column: auto; }
  .range-filter > div { display: grid; grid-template-columns: repeat(2, 1fr); }
  .range-filter button:nth-child(2) { border-right: 1px solid var(--color-border); }
  .range-filter button:nth-child(-n + 2) { border-bottom: 0; }
  .selected-header { grid-template-columns: 1fr; gap: var(--space-2); }
  .tag-metric-grid { grid-template-columns: repeat(2, 1fr); }
  .tag-metric-grid article:nth-child(3) { border-right: 1px solid var(--color-border); }
  .tag-metric-grid article:nth-child(even) { border-right: 0; }
  .tag-metric-grid article:nth-child(-n + 4) { border-bottom: 1px solid var(--color-border); }
  .chart-heading { align-items: flex-start; flex-direction: column; }
  .period-summary { flex-wrap: wrap; gap: var(--space-2) var(--space-4); }
  .metric-tabs { width: 100%; overflow-x: auto; }
  .article-list > a { grid-template-columns: 28px minmax(0, 1fr) auto; }
  .article-list ul { display: none; }
}
</style>
