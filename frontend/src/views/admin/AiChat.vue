<script setup lang="ts">
import { nextTick, onMounted, ref } from 'vue'
import { Delete, Loading, Promotion, Service, User } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getAdminCapabilities, sendAdminMessage } from '@/api/adminAi'
import { getApiErrorMessage } from '@/utils/errors'
import { renderMarkdown } from '@/utils/markdown'

interface Message {
  id: string
  role: 'user' | 'assistant'
  content: string
  timestamp: number
}

const initialMessage = '我是博客管理助手，可以查询站点数据、定位文章和协助处理管理任务。'
const messages = ref<Message[]>([])
const examples = ref<string[]>([])
const inputMessage = ref('')
const isLoading = ref(false)
const messageListRef = ref<HTMLElement | null>(null)

const addMessage = (role: Message['role'], content: string, timestamp = Date.now()): void => {
  messages.value.push({ id: `${role}-${timestamp}-${messages.value.length}`, role, content, timestamp })
}

const scrollToBottom = (): void => {
  void nextTick(() => {
    if (messageListRef.value) messageListRef.value.scrollTop = messageListRef.value.scrollHeight
  })
}

const sendMessage = async (): Promise<void> => {
  const message = inputMessage.value.trim()
  if (!message || isLoading.value) return
  addMessage('user', message)
  inputMessage.value = ''
  isLoading.value = true
  scrollToBottom()
  try {
    const response = await sendAdminMessage(message)
    if (response.code === 200 && response.data) {
      addMessage('assistant', response.data.response, response.data.timestamp)
    } else {
      ElMessage.error(response.message || 'AI 响应失败')
    }
  } catch (error: unknown) {
    ElMessage.error(getApiErrorMessage(error, '发送失败，请稍后重试'))
  } finally {
    isLoading.value = false
    scrollToBottom()
  }
}

const askExample = (question: string): void => {
  inputMessage.value = question
  void sendMessage()
}

const clearChat = (): void => {
  messages.value = []
  addMessage('assistant', initialMessage)
  scrollToBottom()
}

const handleKeyDown = (event: KeyboardEvent): void => {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    void sendMessage()
  }
}

const formatTime = (timestamp: number): string => new Date(timestamp).toLocaleTimeString('zh-CN', {
  hour: '2-digit', minute: '2-digit'
})

onMounted(async () => {
  addMessage('assistant', initialMessage)
  try {
    const response = await getAdminCapabilities()
    if (response.code === 200) examples.value = response.data.exampleQuestions ?? []
  } catch {
    examples.value = []
  }
})
</script>

<template>
  <div class="admin-chat">
    <aside class="chat-context">
      <div><p>MANAGEMENT AI</p><h2>管理助手</h2><span>通过自然语言查询和处理博客管理任务。</span></div>
      <section v-if="examples.length" aria-labelledby="examples-heading">
        <h3 id="examples-heading">常用问题</h3>
        <button v-for="question in examples.slice(0, 6)" :key="question" type="button" :disabled="isLoading" @click="askExample(question)">{{ question }}</button>
      </section>
      <el-button :disabled="messages.length <= 1" @click="clearChat"><el-icon><Delete /></el-icon>清空对话</el-button>
    </aside>

    <section class="chat-workspace" aria-label="管理助手对话">
      <div ref="messageListRef" class="message-list" aria-live="polite">
        <article v-for="message in messages" :key="message.id" :class="['message', message.role]">
          <div class="avatar"><el-icon><Service v-if="message.role === 'assistant'" /><User v-else /></el-icon></div>
          <div class="bubble-wrap">
            <div class="bubble" v-html="renderMarkdown(message.content).html"></div>
            <time :datetime="new Date(message.timestamp).toISOString()">{{ formatTime(message.timestamp) }}</time>
          </div>
        </article>
        <article v-if="isLoading" class="message assistant">
          <div class="avatar"><el-icon><Service /></el-icon></div>
          <div class="bubble loading"><el-icon class="is-loading"><Loading /></el-icon><span>正在处理</span></div>
        </article>
      </div>

      <div class="composer">
        <el-input v-model="inputMessage" type="textarea" :rows="2" resize="none" maxlength="1000" placeholder="输入管理问题" :disabled="isLoading" @keydown="handleKeyDown" />
        <el-button type="primary" :loading="isLoading" :disabled="!inputMessage.trim()" @click="sendMessage"><el-icon><Promotion /></el-icon>发送</el-button>
      </div>
    </section>
  </div>
</template>

<style scoped>
.admin-chat { display: grid; grid-template-columns: 260px minmax(0, 1fr); height: calc(100vh - 120px); max-width: 1100px; margin: 0 auto; overflow: hidden; border: 1px solid var(--color-border); background: var(--color-surface); }
.chat-context { display: flex; min-height: 0; flex-direction: column; gap: var(--space-6); padding: var(--space-6); border-right: 1px solid var(--color-border); background: var(--color-surface-subtle); }
.chat-context p { margin: 0; color: var(--color-brand); font-size: var(--font-size-xs); font-weight: 700; }
.chat-context h2 { margin: var(--space-1) 0; font-family: var(--font-serif); font-size: var(--font-size-xl); }
.chat-context div > span { color: var(--color-text-secondary); font-size: var(--font-size-sm); }
.chat-context section { min-height: 0; overflow-y: auto; }
.chat-context h3 { margin: 0 0 var(--space-2); font-size: var(--font-size-xs); }
.chat-context section button { width: 100%; padding: var(--space-3) 0; border: 0; border-bottom: 1px solid var(--color-border); background: transparent; color: var(--color-text-secondary); cursor: pointer; font-size: var(--font-size-sm); line-height: 1.45; text-align: left; }
.chat-context section button:hover { color: var(--color-brand); }
.chat-context > .el-button { margin-top: auto; }
.chat-workspace { display: flex; min-width: 0; min-height: 0; flex-direction: column; }
.message-list { flex: 1; min-height: 0; padding: var(--space-6); overflow-y: auto; }
.message { display: grid; grid-template-columns: 32px minmax(0, 680px); gap: var(--space-3); margin-bottom: var(--space-5); }
.message.user { grid-template-columns: minmax(0, 680px) 32px; justify-content: end; }
.message.user .avatar { grid-column: 2; }
.message.user .bubble-wrap { grid-row: 1; justify-self: end; }
.avatar { display: grid; place-items: center; width: 32px; height: 32px; border-radius: 50%; background: var(--color-brand-soft); color: var(--color-brand); }
.message.user .avatar { background: var(--color-text); color: var(--color-surface); }
.bubble-wrap { width: fit-content; max-width: 100%; }
.bubble { min-width: 0; padding: var(--space-3) var(--space-4); border: 1px solid var(--color-border); border-radius: var(--radius-md); background: var(--color-surface); overflow-wrap: anywhere; }
.message.user .bubble { border-color: var(--color-brand-soft); background: var(--color-brand-soft); }
.bubble :deep(p) { margin: 0 0 var(--space-2); }
.bubble :deep(p:last-child) { margin-bottom: 0; }
.bubble :deep(pre) { max-width: 100%; padding: var(--space-4); background: var(--color-code); color: var(--color-code-text); overflow-x: auto; }
.bubble-wrap time { display: block; margin-top: var(--space-1); color: var(--color-text-tertiary); font-size: var(--font-size-xs); text-align: right; }
.bubble.loading { display: flex; align-items: center; gap: var(--space-2); color: var(--color-text-secondary); }
.composer { display: grid; grid-template-columns: minmax(0, 1fr) auto; align-items: end; gap: var(--space-3); padding: var(--space-4); border-top: 1px solid var(--color-border); }
@media (max-width: 760px) {
  .admin-chat { grid-template-columns: 1fr; height: calc(100vh - 100px); }
  .chat-context { display: none; }
  .message-list { padding: var(--space-4) var(--space-3); }
  .composer { padding: var(--space-3); }
}
</style>
