<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'
import { isAxiosError } from 'axios'
import { Clock, Delete, Plus, Promotion, Refresh, User } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import SiteHeader from '@/components/SiteHeader.vue'
import { getAgentOverview, getRecommendedQuestions, sendMessage } from '@/api/agent'
import type { AgentArticleSource, AgentOverview } from '@/api/agent'
import { getPosts } from '@/api/post'
import type { PostResponse } from '@/api/types'
import { getApiErrorMessage } from '@/utils/errors'
import { renderMarkdown } from '@/utils/markdown'

interface Message {
  id: string
  role: 'user' | 'assistant'
  content: string
  timestamp: number
  sources: AgentArticleSource[]
}

interface StoredConversation {
  id: string
  title: string
  createdAt: number
  updatedAt: number
  messages: Message[]
}

const HISTORY_STORAGE_KEY = 'agentConversationHistory'
const ACTIVE_CONVERSATION_KEY = 'agentConversationId'
const ARTICLE_PAGE_SIZE = 10
const MAX_STORED_CONVERSATIONS = 30
const MAX_STORED_MESSAGES = 80

const readStoredConversations = (): StoredConversation[] => {
  try {
    const value: unknown = JSON.parse(localStorage.getItem(HISTORY_STORAGE_KEY) || '[]')
    if (!Array.isArray(value)) return []
    return (value as StoredConversation[])
      .filter(item => item && typeof item.id === 'string' && Array.isArray(item.messages))
      .sort((a, b) => b.updatedAt - a.updatedAt)
      .slice(0, MAX_STORED_CONVERSATIONS)
  } catch {
    return []
  }
}

const cloneMessages = (items: Message[]): Message[] => items.map(item => ({
  ...item,
  sources: (item.sources || []).map(source => ({ ...source }))
}))

const initialConversations = readStoredConversations()
const storedActiveId = localStorage.getItem(ACTIVE_CONVERSATION_KEY) || ''
const initialConversation = initialConversations.find(item => item.id === storedActiveId) || null

const conversations = ref<StoredConversation[]>(initialConversations)
const messages = ref<Message[]>(initialConversation ? cloneMessages(initialConversation.messages) : [])
const conversationId = ref(initialConversation?.id || '')
const recommendedQuestions = ref<string[]>([])
const overview = ref<AgentOverview | null>(null)
const inputText = ref('')
const loading = ref(false)
const overviewLoading = ref(false)
const visibleArticleCount = ref(ARTICLE_PAGE_SIZE)
const historyOpen = ref(false)
const chatContainer = ref<HTMLDivElement | null>(null)
let abortController: AbortController | null = null

const articleIndex = computed(() => {
  const articles = overview.value?.articles
  return articles?.length ? articles : (overview.value?.recentArticles || [])
})
const visibleArticles = computed(() => articleIndex.value.slice(0, visibleArticleCount.value))
const remainingArticleCount = computed(() => Math.max(0, articleIndex.value.length - visibleArticles.value.length))

const persistConversations = (): void => {
  const stored = conversations.value
    .sort((a, b) => b.updatedAt - a.updatedAt)
    .slice(0, MAX_STORED_CONVERSATIONS)
    .map(item => ({ ...item, messages: cloneMessages(item.messages.slice(-MAX_STORED_MESSAGES)) }))
  conversations.value = stored
  localStorage.setItem(HISTORY_STORAGE_KEY, JSON.stringify(stored))
}

const scrollToBottom = (): void => {
  void nextTick(() => {
    if (chatContainer.value) chatContainer.value.scrollTop = chatContainer.value.scrollHeight
  })
}

const saveActiveConversation = (): void => {
  if (!conversationId.value) return
  const conversation = conversations.value.find(item => item.id === conversationId.value)
  if (!conversation) return
  conversation.messages = cloneMessages(messages.value)
  conversation.updatedAt = Date.now()
  persistConversations()
}

const createConversation = (firstQuestion: string): StoredConversation => {
  const now = Date.now()
  const conversation: StoredConversation = {
    id: `web-${now.toString(36)}-${Math.random().toString(36).slice(2, 7)}`,
    title: firstQuestion.slice(0, 28),
    createdAt: now,
    updatedAt: now,
    messages: []
  }
  conversations.value.unshift(conversation)
  conversationId.value = conversation.id
  localStorage.setItem(ACTIVE_CONVERSATION_KEY, conversation.id)
  persistConversations()
  return conversation
}

const ensureConversation = (firstQuestion: string): void => {
  if (conversationId.value && conversations.value.some(item => item.id === conversationId.value)) return
  createConversation(firstQuestion)
}

const addMessage = (role: Message['role'], content: string, sources: AgentArticleSource[] = []): void => {
  const timestamp = Date.now()
  messages.value.push({ id: `${role}-${timestamp}-${messages.value.length}`, role, content, timestamp, sources })
  saveActiveConversation()
  scrollToBottom()
}

const handleSend = async (): Promise<void> => {
  const question = inputText.value.trim()
  if (!question || loading.value) return
  ensureConversation(question)
  addMessage('user', question)
  inputText.value = ''
  loading.value = true
  abortController = new AbortController()
  const controller = abortController
  try {
    const response = await sendMessage(question, conversationId.value, controller.signal)
    if (response.code === 200 && response.data) {
      addMessage('assistant', response.data.answer, response.data.sources ?? [])
    } else {
      addMessage('assistant', response.message || '服务暂时不可用')
    }
  } catch (error: unknown) {
    if (isAxiosError(error) && error.code === 'ERR_CANCELED') return
    addMessage('assistant', getApiErrorMessage(error, '服务暂时不可用，请稍后重试'))
  } finally {
    if (abortController === controller) abortController = null
    loading.value = false
  }
}

const handleStop = (): void => {
  abortController?.abort()
  abortController = null
  loading.value = false
}

const handleKeyDown = (event: KeyboardEvent): void => {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    void handleSend()
  }
}

const askRecommended = (question: string): void => {
  inputText.value = question
  void handleSend()
}

const startNewConversation = (): void => {
  handleStop()
  messages.value = []
  conversationId.value = ''
  inputText.value = ''
  historyOpen.value = false
  localStorage.removeItem(ACTIVE_CONVERSATION_KEY)
  ElMessage.success('已新建对话')
}

const selectConversation = (id: string): void => {
  const conversation = conversations.value.find(item => item.id === id)
  if (!conversation) return
  handleStop()
  conversationId.value = id
  messages.value = cloneMessages(conversation.messages)
  localStorage.setItem(ACTIVE_CONVERSATION_KEY, id)
  historyOpen.value = false
  scrollToBottom()
}

const deleteConversation = (id: string): void => {
  const wasActive = conversationId.value === id
  if (wasActive) handleStop()
  conversations.value = conversations.value.filter(item => item.id !== id)
  persistConversations()
  if (wasActive) {
    messages.value = []
    conversationId.value = ''
    localStorage.removeItem(ACTIVE_CONVERSATION_KEY)
  }
  ElMessage.success('对话记录已删除')
}

const clearConversation = (): void => {
  if (conversationId.value) deleteConversation(conversationId.value)
}

const formatTime = (timestamp: number): string => new Date(timestamp).toLocaleTimeString('zh-CN', {
  hour: '2-digit', minute: '2-digit'
})

const formatHistoryTime = (timestamp: number): string => {
  const date = new Date(timestamp)
  const today = new Date()
  if (date.toDateString() === today.toDateString()) return formatTime(timestamp)
  return date.toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' })
}

const postToArticleSource = (post: PostResponse): AgentArticleSource => {
  const summary = (post.summary || post.content || '')
    .replace(/[#*`>\[\]()\r\n-]+/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
  return { id: post.id, title: post.title, summary: summary.slice(0, 150) || '该文章暂未提供摘要。' }
}

const fetchCompleteArticleIndex = async (expectedCount: number): Promise<AgentArticleSource[]> => {
  const response = await getPosts(0, Math.max(10, expectedCount))
  if (response.code !== 200 || !response.data) return []
  return (response.data.content || []).map(postToArticleSource)
}

const loadAgentOverview = async (): Promise<boolean> => {
  overviewLoading.value = true
  try {
    const response = await getAgentOverview()
    if (response.code === 200 && response.data) {
      let data = response.data
      if ((data.articles?.length || 0) < data.publishedCount) {
        try {
          const completeArticles = await fetchCompleteArticleIndex(data.publishedCount)
          if (completeArticles.length) data = { ...data, articles: completeArticles }
        } catch {
          // 兼容尚未重启的旧版后端；补齐失败时仍保留最近文章。
        }
      }
      overview.value = data
      recommendedQuestions.value = data.recommendedQuestions ?? []
      return true
    }
  } catch {
    overview.value = null
  } finally {
    overviewLoading.value = false
  }
  return false
}

const refreshArticleIndex = async (): Promise<void> => {
  visibleArticleCount.value = ARTICLE_PAGE_SIZE
  const loaded = await loadAgentOverview()
  if (loaded) ElMessage.success(`文章索引已更新，共 ${overview.value?.publishedCount || 0} 篇`)
  else ElMessage.error('文章索引更新失败')
}

const loadMoreArticles = (): void => {
  visibleArticleCount.value += ARTICLE_PAGE_SIZE
}

onMounted(async () => {
  const loaded = await loadAgentOverview()
  if (!loaded) {
    try {
      const response = await getRecommendedQuestions()
      if (response.code === 200) recommendedQuestions.value = response.data ?? []
    } catch {
      recommendedQuestions.value = []
    }
  }
  if (messages.value.length) scrollToBottom()
})
</script>

<template>
  <div class="agent-page">
    <SiteHeader />
    <main class="agent-layout">
      <aside class="agent-sidebar">
        <div class="sidebar-intro">
          <h1>AI 检索</h1>
          <p class="sidebar-copy">从本站已发布文章中检索、比较和归纳内容。</p>
          <p v-if="overview" class="article-count"><strong>{{ overview.publishedCount }}</strong> 篇内容已纳入检索</p>
        </div>

        <button class="mobile-history-button" type="button" @click="historyOpen = true">
          <el-icon><Clock /></el-icon>对话记录<span>{{ conversations.length }}</span>
        </button>

        <div class="sidebar-toolbar">
          <el-button type="primary" @click="startNewConversation"><el-icon><Plus /></el-icon>新对话</el-button>
          <el-tooltip content="刷新文章索引" placement="top">
            <el-button aria-label="刷新文章索引" :loading="overviewLoading" @click="refreshArticleIndex">
              <el-icon><Refresh /></el-icon>
            </el-button>
          </el-tooltip>
        </div>

        <section class="conversation-history" aria-labelledby="history-heading">
          <div class="sidebar-section-heading">
            <h2 id="history-heading">对话记录</h2><span>{{ conversations.length }}</span>
          </div>
          <p v-if="conversations.length === 0" class="history-empty">暂无历史对话</p>
          <div v-else class="history-list">
            <div v-for="conversation in conversations" :key="conversation.id" :class="['history-item', { active: conversation.id === conversationId }]">
              <button class="history-select" type="button" @click="selectConversation(conversation.id)">
                <strong>{{ conversation.title }}</strong>
                <span>{{ formatHistoryTime(conversation.updatedAt) }} · {{ conversation.messages.length }} 条消息</span>
              </button>
              <button class="history-delete" type="button" aria-label="删除这条对话" @click="deleteConversation(conversation.id)">
                <el-icon><Delete /></el-icon>
              </button>
            </div>
          </div>
        </section>

        <section v-if="recommendedQuestions.length" class="question-list" aria-labelledby="question-heading">
          <h2 id="question-heading">可检索的问题</h2>
          <button v-for="question in recommendedQuestions" :key="question" type="button" :disabled="loading" @click="askRecommended(question)">
            {{ question }}
          </button>
        </section>

        <el-button class="clear-button" :disabled="!conversationId" @click="clearConversation">
          <el-icon><Delete /></el-icon>删除当前对话
        </el-button>
      </aside>

      <section class="chat-panel" aria-label="AI 检索对话">
        <div ref="chatContainer" class="chat-messages" aria-live="polite">
          <div v-if="messages.length === 0" class="library-overview">
            <header>
              <p>站内内容索引</p>
              <h2>从现有文章开始提问</h2>
              <div v-if="overview" class="index-meta">
                <span>当前收录 {{ overview.publishedCount }} 篇已发布文章，回答会注明所依据的内容。</span>
                <button type="button" :disabled="overviewLoading" @click="refreshArticleIndex">
                  <el-icon><Refresh /></el-icon>{{ overviewLoading ? '更新中' : '刷新索引' }}
                </button>
              </div>
              <span v-else>回答会基于站内已发布文章，并注明所依据的内容。</span>
            </header>

            <ol v-if="visibleArticles.length" class="article-index">
              <li v-for="(article, index) in visibleArticles" :key="article.id">
                <span class="article-number">{{ String(index + 1).padStart(2, '0') }}</span>
                <RouterLink :to="`/post/${article.id}`">
                  <strong>{{ article.title }}</strong>
                  <span>{{ article.summary }}</span>
                </RouterLink>
              </li>
            </ol>
            <button v-if="remainingArticleCount" class="load-more-button" type="button" @click="loadMoreArticles">
              再显示 {{ Math.min(ARTICLE_PAGE_SIZE, remainingArticleCount) }} 篇
              <span>还有 {{ remainingArticleCount }} 篇</span>
            </button>
          </div>

          <article v-for="message in messages" :key="message.id" :class="['message-row', message.role]">
            <div class="message-avatar" aria-hidden="true">
              <el-icon v-if="message.role === 'user'"><User /></el-icon>
              <span v-else>AI</span>
            </div>
            <div class="message-content-wrap">
              <div v-if="message.role === 'assistant'" class="message-content markdown" v-html="renderMarkdown(message.content).html"></div>
              <p v-else class="message-content">{{ message.content }}</p>
              <nav v-if="message.role === 'assistant' && message.sources.length" class="answer-sources" aria-label="回答依据">
                <span>依据文章</span>
                <RouterLink v-for="source in message.sources" :key="source.id" :to="`/post/${source.id}`">
                  {{ source.title }}
                </RouterLink>
              </nav>
              <time :datetime="new Date(message.timestamp).toISOString()">{{ formatTime(message.timestamp) }}</time>
            </div>
          </article>

          <article v-if="loading" class="message-row assistant loading-row">
            <div class="message-avatar" aria-hidden="true"><span>AI</span></div>
            <div class="thinking"><span></span><span></span><span></span><span class="sr-only">正在生成回答</span></div>
          </article>
        </div>

        <div class="chat-composer">
          <el-input
            v-model="inputText"
            type="textarea"
            :rows="2"
            resize="none"
            maxlength="1000"
            placeholder="询问文章数量、主题、观点或内容对比"
            :disabled="loading"
            @keydown="handleKeyDown"
          />
          <el-button v-if="loading" type="danger" plain @click="handleStop">停止</el-button>
          <el-button v-else type="primary" :disabled="!inputText.trim()" @click="handleSend">
            <el-icon><Promotion /></el-icon>发送
          </el-button>
        </div>
      </section>
    </main>

    <el-drawer v-model="historyOpen" title="对话记录" direction="ltr" size="min(88vw, 340px)" class="history-drawer">
      <el-button class="drawer-new-button" type="primary" @click="startNewConversation">
        <el-icon><Plus /></el-icon>新对话
      </el-button>
      <p v-if="conversations.length === 0" class="history-empty">暂无历史对话，发送问题后会自动保存在这里。</p>
      <div v-else class="history-list drawer-history-list">
        <div v-for="conversation in conversations" :key="conversation.id" :class="['history-item', { active: conversation.id === conversationId }]">
          <button class="history-select" type="button" @click="selectConversation(conversation.id)">
            <strong>{{ conversation.title }}</strong>
            <span>{{ formatHistoryTime(conversation.updatedAt) }} · {{ conversation.messages.length }} 条消息</span>
          </button>
          <button class="history-delete" type="button" aria-label="删除这条对话" @click="deleteConversation(conversation.id)">
            <el-icon><Delete /></el-icon>
          </button>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
.agent-page { height: 100vh; background: var(--color-canvas); overflow: hidden; }
.agent-layout { display: grid; grid-template-columns: 300px minmax(0, 1fr); width: min(100%, var(--content-width)); height: calc(100vh - var(--header-height)); margin: 0 auto; border-right: 1px solid var(--color-border); border-left: 1px solid var(--color-border); background: var(--color-surface); }
.agent-sidebar { display: flex; flex-direction: column; gap: var(--space-5); min-height: 0; padding: var(--space-8) var(--space-6); border-right: 1px solid var(--color-border); background: var(--color-surface-subtle); }
.agent-sidebar h1 { margin: 0; font-family: var(--font-serif); font-size: var(--font-size-2xl); }
.sidebar-copy { margin: var(--space-3) 0 0; color: var(--color-text-secondary); font-size: var(--font-size-sm); }
.article-count { margin: var(--space-5) 0 0; padding-top: var(--space-4); border-top: 1px solid var(--color-border); color: var(--color-text-secondary); font-size: var(--font-size-xs); }
.article-count strong { color: var(--color-text); font-family: var(--font-serif); font-size: var(--font-size-lg); }
.sidebar-toolbar { display: grid; grid-template-columns: minmax(0, 1fr) auto; gap: var(--space-2); }
.sidebar-toolbar .el-button { margin: 0; }
.conversation-history { min-height: 0; }
.sidebar-section-heading { display: flex; align-items: center; justify-content: space-between; margin-bottom: var(--space-2); }
.sidebar-section-heading h2, .question-list h2 { margin: 0; font-size: var(--font-size-xs); text-transform: uppercase; }
.sidebar-section-heading span { min-width: 22px; padding: 1px var(--space-2); border-radius: 99px; background: var(--color-brand-soft); color: var(--color-brand); font-size: var(--font-size-xs); text-align: center; }
.history-list { display: flex; flex-direction: column; gap: var(--space-1); max-height: 220px; overflow-y: auto; }
.history-item { display: grid; grid-template-columns: minmax(0, 1fr) 30px; align-items: center; border: 1px solid transparent; border-radius: var(--radius-sm); }
.history-item:hover, .history-item.active { border-color: var(--color-border); background: var(--color-surface); }
.history-item.active { border-color: color-mix(in srgb, var(--color-brand) 28%, var(--color-border)); }
.history-select { min-width: 0; padding: var(--space-2) var(--space-3); border: 0; background: transparent; cursor: pointer; text-align: left; }
.history-select strong, .history-select span { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.history-select strong { color: var(--color-text-secondary); font-size: var(--font-size-sm); font-weight: 600; }
.history-item.active .history-select strong { color: var(--color-brand); }
.history-select span { margin-top: 2px; color: var(--color-text-tertiary); font-size: .6875rem; }
.history-delete { display: grid; width: 28px; height: 28px; padding: 0; place-items: center; border: 0; border-radius: var(--radius-sm); background: transparent; color: var(--color-text-tertiary); cursor: pointer; opacity: 0; }
.history-item:hover .history-delete, .history-delete:focus-visible { opacity: 1; }
.history-delete:hover { background: var(--color-danger-soft); color: var(--color-danger); }
.history-empty { margin: var(--space-3) 0; color: var(--color-text-tertiary); font-size: var(--font-size-xs); }
.question-list { min-height: 100px; flex: 1; overflow-y: auto; }
.question-list h2 { margin: 0 0 var(--space-3); font-size: var(--font-size-xs); text-transform: uppercase; }
.question-list button { width: 100%; padding: var(--space-3) 0; border: 0; border-bottom: 1px solid var(--color-border); background: transparent; color: var(--color-text-secondary); cursor: pointer; font-size: var(--font-size-sm); line-height: 1.45; text-align: left; }
.question-list button:hover { color: var(--color-brand); }
.question-list button:disabled { cursor: not-allowed; opacity: 0.55; }
.clear-button { width: 100%; margin-top: auto; }
.mobile-history-button { display: none; }
.chat-panel { display: flex; min-width: 0; min-height: 0; flex-direction: column; }
.chat-messages { flex: 1; min-height: 0; padding: var(--space-8); overflow-y: auto; }
.library-overview { width: min(100%, 780px); margin: 0 auto; }
.library-overview header { padding: var(--space-3) 0 var(--space-6); border-bottom: 2px solid var(--color-text); }
.library-overview header p { margin: 0 0 var(--space-2); color: var(--color-brand); font-size: var(--font-size-xs); font-weight: 700; }
.library-overview header h2 { margin: 0 0 var(--space-3); font-family: var(--font-serif); font-size: var(--font-size-2xl); }
.library-overview header span { color: var(--color-text-secondary); font-size: var(--font-size-sm); }
.index-meta { display: flex; align-items: center; justify-content: space-between; gap: var(--space-4); }
.index-meta button { display: inline-flex; flex: 0 0 auto; align-items: center; gap: var(--space-1); padding: var(--space-1) 0; border: 0; background: transparent; color: var(--color-brand); cursor: pointer; font-size: var(--font-size-xs); }
.index-meta button:disabled { cursor: wait; opacity: .6; }
.article-index { margin: 0; padding: 0; list-style: none; }
.article-index li { display: grid; grid-template-columns: 40px minmax(0, 1fr); gap: var(--space-4); padding: var(--space-5) 0; border-bottom: 1px solid var(--color-border); }
.article-number { color: var(--color-text-tertiary); font-family: var(--font-serif); }
.article-index a { min-width: 0; color: var(--color-text); text-decoration: none; }
.article-index a strong { display: block; margin-bottom: var(--space-2); font-family: var(--font-serif); font-size: var(--font-size-lg); }
.article-index a > span { display: -webkit-box; color: var(--color-text-secondary); font-size: var(--font-size-sm); line-height: 1.6; overflow: hidden; -webkit-box-orient: vertical; -webkit-line-clamp: 2; }
.article-index a:hover strong { color: var(--color-brand); }
.load-more-button { display: flex; align-items: center; justify-content: space-between; width: 100%; padding: var(--space-4) 0; border: 0; border-bottom: 1px solid var(--color-border); background: transparent; color: var(--color-brand); cursor: pointer; font-size: var(--font-size-sm); font-weight: 650; }
.load-more-button span { color: var(--color-text-tertiary); font-size: var(--font-size-xs); font-weight: 400; }
.message-avatar { display: grid; place-items: center; border-radius: 50%; background: var(--color-brand-soft); color: var(--color-brand); font-family: var(--font-serif); font-weight: 700; }
.message-row { display: grid; grid-template-columns: 36px minmax(0, 720px); gap: var(--space-3); margin: 0 auto var(--space-6); }
.message-row.user { grid-template-columns: minmax(0, 720px) 36px; justify-content: end; }
.message-row.user .message-avatar { grid-column: 2; background: var(--color-text); color: var(--color-surface); }
.message-row.user .message-content-wrap { grid-column: 1; grid-row: 1; justify-self: end; background: var(--color-brand-soft); }
.message-avatar { width: 36px; height: 36px; font-size: var(--font-size-xs); }
.message-content-wrap { min-width: 0; width: fit-content; max-width: 100%; padding: var(--space-3) var(--space-4); border: 1px solid var(--color-border); border-radius: var(--radius-md); background: var(--color-surface); }
.message-row.assistant .message-content-wrap { width: 100%; padding: var(--space-1) 0 var(--space-5); border: 0; border-bottom: 1px solid var(--color-border); border-radius: 0; }
.message-content { margin: 0; white-space: pre-wrap; overflow-wrap: anywhere; }
.message-content-wrap time { display: block; margin-top: var(--space-2); color: var(--color-text-tertiary); font-size: var(--font-size-xs); text-align: right; }
.answer-sources { display: flex; flex-wrap: wrap; gap: var(--space-2) var(--space-3); margin-top: var(--space-4); padding-top: var(--space-3); border-top: 1px solid var(--color-border); font-size: var(--font-size-xs); }
.answer-sources > span { color: var(--color-text-tertiary); }
.answer-sources a { color: var(--color-brand); text-decoration: none; }
.answer-sources a:hover { text-decoration: underline; text-underline-offset: var(--space-1); }
.markdown :deep(p) { margin: 0 0 var(--space-2); }
.markdown :deep(p:last-child) { margin-bottom: 0; }
.markdown :deep(pre) { max-width: 100%; padding: var(--space-4); border-radius: var(--radius-sm); background: var(--color-code); color: var(--color-code-text); overflow-x: auto; }
.markdown :deep(code) { font-family: var(--font-mono); }
.thinking { display: flex; align-items: center; gap: var(--space-2); min-height: 44px; padding: 0 var(--space-4); }
.thinking > span:not(.sr-only) { width: 6px; height: 6px; border-radius: 50%; background: var(--color-text-tertiary); animation: pulse 1.2s infinite ease-in-out; }
.thinking > span:nth-child(2) { animation-delay: 160ms; }
.thinking > span:nth-child(3) { animation-delay: 320ms; }
.sr-only { position: absolute; width: 1px; height: 1px; overflow: hidden; clip-path: inset(50%); }
.chat-composer { display: grid; grid-template-columns: minmax(0, 1fr) auto; align-items: end; gap: var(--space-3); padding: var(--space-4) var(--space-8) var(--space-6); border-top: 1px solid var(--color-border); background: var(--color-surface); }
.drawer-new-button { width: 100%; margin-bottom: var(--space-4); }
.drawer-history-list { max-height: calc(100vh - 150px); }
.drawer-history-list .history-delete { opacity: 1; }
@keyframes pulse { 0%, 70%, 100% { opacity: 0.35; transform: translateY(0); } 35% { opacity: 1; transform: translateY(calc(var(--space-1) * -1)); } }
@media (max-width: 780px) {
  .agent-page { min-height: 100vh; height: auto; overflow: visible; }
  .agent-layout { grid-template-columns: 1fr; height: calc(100vh - 60px); border: 0; }
  .agent-layout { grid-template-rows: auto minmax(0, 1fr); }
  .agent-sidebar { display: flex; align-items: center; flex-direction: row; gap: var(--space-3); padding: var(--space-4) var(--space-3); border-right: 0; border-bottom: 1px solid var(--color-border); }
  .sidebar-intro { display: flex; min-width: 0; align-items: baseline; gap: var(--space-3); }
  .agent-sidebar h1 { font-size: var(--font-size-xl); }
  .sidebar-copy, .article-count, .sidebar-toolbar, .conversation-history, .question-list, .clear-button { display: none; }
  .mobile-history-button { display: inline-flex; flex: 0 0 auto; align-items: center; gap: var(--space-1); margin-left: auto; padding: var(--space-2) var(--space-3); border: 1px solid var(--color-border); border-radius: var(--radius-sm); background: var(--color-surface); color: var(--color-text-secondary); cursor: pointer; font-size: var(--font-size-xs); }
  .mobile-history-button span { display: inline-grid; min-width: 18px; height: 18px; place-items: center; border-radius: 99px; background: var(--color-brand-soft); color: var(--color-brand); }
  .chat-messages { padding: var(--space-5) var(--space-3); }
  .chat-composer { padding: var(--space-3); }
  .message-row, .message-row.user { grid-template-columns: 30px minmax(0, 1fr); }
  .message-row.user { grid-template-columns: minmax(0, 1fr) 30px; }
  .message-avatar { width: 30px; height: 30px; }
  .library-overview header { padding-top: 0; }
  .library-overview header h2 { font-size: var(--font-size-xl); }
  .index-meta { align-items: flex-start; flex-direction: column; gap: var(--space-2); }
  .article-index li { grid-template-columns: 30px minmax(0, 1fr); gap: var(--space-2); }
  .article-index a strong { font-size: var(--font-size-base); }
}
</style>
