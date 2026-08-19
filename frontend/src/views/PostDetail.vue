<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'
import { storeToRefs } from 'pinia'
import { useRoute } from 'vue-router'
import { ChatLineRound, Pointer, Promotion } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import SiteHeader from '@/components/SiteHeader.vue'
import SiteFooter from '@/components/SiteFooter.vue'
import { createComment, getCommentsByPost } from '@/api/comment'
import { checkLikeStatus, likeComment, likePost, unlikeComment, unlikePost } from '@/api/like'
import { getPostById } from '@/api/post'
import type { CommentCreateRequest, CommentResponse, PostResponse } from '@/api/types'
import { useAuthStore } from '@/stores/auth'
import { getApiErrorMessage } from '@/utils/errors'
import { renderMarkdown } from '@/utils/markdown'

const route = useRoute()
const authStore = useAuthStore()
const { isLoggedIn } = storeToRefs(authStore)
const post = ref<PostResponse | null>(null)
const comments = ref<CommentResponse[]>([])
const loading = ref(true)
const loadError = ref('')
const commentContent = ref('')
const liked = ref(false)
const submittingLike = ref(false)
const likedComments = ref<Set<number>>(new Set())
const submittingComment = ref(false)
const replyingTo = ref<number | null>(null)
const replyContent = ref('')
const replyTargetNickname = ref('')
const submittingReply = ref(false)

const articleDocument = computed(() => renderMarkdown(post.value?.content ?? ''))
const displaySummary = computed(() => {
  const summary = post.value?.summary ?? ''
  const plainText = summary.replace(/[#*`>\[\]()\r\n-]+/g, ' ').replace(/\s+/g, ' ').trim()
  return plainText.length > 220 ? `${plainText.slice(0, 220)}...` : plainText
})

const formatDate = (timestamp: number): string => new Date(timestamp).toLocaleDateString('zh-CN', {
  year: 'numeric',
  month: 'long',
  day: 'numeric'
})

const readingTime = computed(() => Math.max(1, Math.ceil((post.value?.content.replace(/\s/g, '').length ?? 0) / 500)))

const findComment = (list: CommentResponse[], id: number): CommentResponse | null => {
  for (const comment of list) {
    if (comment.id === id) return comment
    const nested = findComment(comment.replies ?? [], id)
    if (nested) return nested
  }
  return null
}

const getAllCommentIds = (list: CommentResponse[]): number[] => list.flatMap((comment) => [
  comment.id,
  ...getAllCommentIds(comment.replies ?? [])
])

const loadPost = async (): Promise<void> => {
  const id = Number(route.params.id)
  if (!Number.isInteger(id) || id <= 0) {
    loadError.value = '文章地址无效'
    return
  }
  const response = await getPostById(id)
  if (response.code === 200 && response.data) post.value = response.data
  else loadError.value = response.message || '文章不存在或暂时不可访问'
}

const loadComments = async (): Promise<void> => {
  const response = await getCommentsByPost(Number(route.params.id))
  if (response.code === 200) comments.value = response.data ?? []
}

const loadLikeStatus = async (): Promise<void> => {
  if (!isLoggedIn.value || !post.value) return
  try {
    const response = await checkLikeStatus(post.value.id, getAllCommentIds(comments.value))
    if (response.code === 200 && response.data) {
      liked.value = response.data.postLiked ?? false
      likedComments.value = new Set(response.data.likedCommentIds ?? [])
    }
  } catch {
    liked.value = false
    likedComments.value = new Set()
  }
}

const startReply = (commentId: number, nickname: string): void => {
  if (!isLoggedIn.value) {
    ElMessage.warning('请先登录')
    return
  }
  replyingTo.value = commentId
  replyTargetNickname.value = nickname
  replyContent.value = ''
  void nextTick(() => document.getElementById(`reply-input-${commentId}`)?.querySelector('textarea')?.focus())
}

const cancelReply = (): void => {
  replyingTo.value = null
  replyContent.value = ''
}

const handleReplySubmit = async (parentCommentId: number): Promise<void> => {
  const content = replyContent.value.trim()
  if (!content) {
    ElMessage.warning('回复内容不能为空')
    return
  }
  submittingReply.value = true
  try {
    const request: CommentCreateRequest = { postId: Number(route.params.id), content, parentId: parentCommentId }
    const response = await createComment(request)
    if (response.code === 200) {
      cancelReply()
      if (post.value) post.value.commentCount += 1
      await loadComments()
      await loadLikeStatus()
      ElMessage.success('回复成功')
    } else {
      ElMessage.error(response.message || '回复失败')
    }
  } catch (error: unknown) {
    ElMessage.error(getApiErrorMessage(error, '回复失败，请稍后重试'))
  } finally {
    submittingReply.value = false
  }
}

const handleLikePost = async (): Promise<void> => {
  if (!isLoggedIn.value) {
    ElMessage.warning('请先登录')
    return
  }
  if (!post.value || submittingLike.value) return
  submittingLike.value = true
  try {
    if (liked.value) {
      const response = await unlikePost(post.value.id)
      if (response.code !== 200) throw new Error(response.message || '取消点赞失败')
      post.value.likeCount = Math.max(0, post.value.likeCount - 1)
    } else {
      const response = await likePost(post.value.id)
      if (response.code !== 200) throw new Error(response.message || '点赞失败')
      post.value.likeCount += 1
    }
    liked.value = !liked.value
  } catch (error: unknown) {
    ElMessage.error(getApiErrorMessage(error, '操作失败，请稍后重试'))
  } finally {
    submittingLike.value = false
  }
}

const handleLikeComment = async (commentId: number): Promise<void> => {
  if (!isLoggedIn.value) {
    ElMessage.warning('请先登录')
    return
  }
  const comment = findComment(comments.value, commentId)
  try {
    if (likedComments.value.has(commentId)) {
      await unlikeComment(commentId)
      if (comment) comment.likeCount = Math.max(0, comment.likeCount - 1)
      likedComments.value.delete(commentId)
    } else {
      await likeComment(commentId)
      if (comment) comment.likeCount += 1
      likedComments.value.add(commentId)
    }
  } catch (error: unknown) {
    ElMessage.error(getApiErrorMessage(error, '操作失败，请稍后重试'))
  }
}

const handleSubmitComment = async (): Promise<void> => {
  const content = commentContent.value.trim()
  if (!content) {
    ElMessage.warning('评论内容不能为空')
    return
  }
  submittingComment.value = true
  try {
    const response = await createComment({ postId: Number(route.params.id), content })
    if (response.code === 200 && response.data) {
      comments.value.unshift(response.data)
      if (post.value) post.value.commentCount += 1
      commentContent.value = ''
      ElMessage.success('评论已发表')
    } else {
      ElMessage.error(response.message || '发表失败')
    }
  } catch (error: unknown) {
    ElMessage.error(getApiErrorMessage(error, '评论发表失败，请稍后重试'))
  } finally {
    submittingComment.value = false
  }
}

onMounted(async () => {
  loading.value = true
  try {
    await Promise.all([loadPost(), loadComments()])
    await loadLikeStatus()
  } catch (error: unknown) {
    loadError.value = getApiErrorMessage(error, '文章加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="post-page">
    <SiteHeader />
    <main>
      <div v-if="loading" class="page-state" aria-live="polite">
        <el-skeleton :rows="10" animated />
      </div>
      <div v-else-if="loadError" class="page-state error-state" role="alert">
        <p>{{ loadError }}</p>
        <RouterLink to="/">返回文章列表</RouterLink>
      </div>

      <template v-else-if="post">
        <header class="article-header">
          <RouterLink to="/" class="back-link">文章 / {{ post.tags?.[0]?.name || '技术实践' }}</RouterLink>
          <h1>{{ post.title }}</h1>
          <p v-if="displaySummary" class="article-summary">{{ displaySummary }}</p>
          <div class="article-meta">
            <div class="author-block">
              <el-avatar :size="36" :src="post.author.avatar" />
              <div><strong>{{ post.author.nickname }}</strong><span>{{ formatDate(post.createdAt) }}</span></div>
            </div>
            <span>{{ readingTime }} 分钟阅读</span>
            <span>{{ post.viewCount }} 次阅读</span>
            <span v-if="post.isAiGenerated" class="ai-label">AI 辅助创作</span>
          </div>
          <div class="article-engagement">
            <button
              type="button"
              class="like-button"
              :class="{ active: liked }"
              :aria-pressed="liked"
              :disabled="submittingLike"
              @click="handleLikePost"
            >
              <el-icon><Pointer /></el-icon>
              <span>{{ liked ? '已点赞' : '点赞' }}</span>
              <strong>{{ post.likeCount }}</strong>
            </button>
            <a href="#comments"><el-icon><ChatLineRound /></el-icon><span>评论</span><strong>{{ post.commentCount }}</strong></a>
          </div>
        </header>

        <div class="article-layout">
          <div class="article-column">
            <article class="post-body" v-html="articleDocument.html"></article>

            <div class="article-tags" aria-label="文章标签">
              <span v-for="tag in post.tags" :key="tag.id">{{ tag.name }}</span>
            </div>

            <div class="post-actions">
              <el-button :type="liked ? 'primary' : 'default'" :loading="submittingLike" :aria-pressed="liked" @click="handleLikePost">
                <el-icon><Pointer /></el-icon>
                {{ liked ? '已点赞' : '点赞' }} · {{ post.likeCount }}
              </el-button>
              <a class="comment-anchor" href="#comments">
                <el-icon><ChatLineRound /></el-icon> 评论 · {{ post.commentCount }}
              </a>
            </div>

            <section id="comments" class="comment-section" aria-labelledby="comments-heading">
              <div class="section-heading">
                <h2 id="comments-heading">讨论</h2>
                <span>{{ post.commentCount }} 条</span>
              </div>

              <div v-if="isLoggedIn" class="comment-composer">
                <el-input v-model="commentContent" type="textarea" :rows="3" maxlength="1000" placeholder="写下你的观点" />
                <el-button
                  type="primary"
                  :loading="submittingComment"
                  :disabled="!commentContent.trim()"
                  @click="handleSubmitComment"
                >
                  <el-icon><Promotion /></el-icon> 发表评论
                </el-button>
              </div>
              <p v-else class="login-prompt">请先 <RouterLink to="/login">登录</RouterLink> 后参与讨论。</p>

              <p v-if="comments.length === 0" class="empty-comments">暂时没有评论。</p>
              <div v-else class="comments-list">
                <article v-for="comment in comments" :key="comment.id" class="comment-item">
                  <header class="comment-header">
                    <el-avatar :size="34" :src="comment.author.avatar" />
                    <div><strong>{{ comment.author.nickname }}</strong><span>{{ formatDate(comment.createdAt) }}</span></div>
                  </header>
                  <p>{{ comment.content }}</p>
                  <div class="comment-actions">
                    <button type="button" :class="{ active: likedComments.has(comment.id) }" :aria-pressed="likedComments.has(comment.id)" @click="handleLikeComment(comment.id)">
                      <el-icon><Pointer /></el-icon> 点赞 {{ comment.likeCount }}
                    </button>
                    <button type="button" @click="startReply(comment.id, comment.author.nickname)">回复</button>
                  </div>

                  <div v-if="replyingTo === comment.id" :id="`reply-input-${comment.id}`" class="reply-composer">
                    <el-input v-model="replyContent" type="textarea" :rows="2" :placeholder="`回复 ${replyTargetNickname}`" />
                    <div><el-button @click="cancelReply">取消</el-button><el-button type="primary" :loading="submittingReply" @click="handleReplySubmit(comment.id)">回复</el-button></div>
                  </div>

                  <div v-if="comment.replies?.length" class="replies">
                    <article v-for="reply in comment.replies" :key="reply.id" class="reply-item">
                      <header class="comment-header">
                        <el-avatar :size="28" :src="reply.author.avatar" />
                        <div><strong>{{ reply.author.nickname }}</strong><span>{{ formatDate(reply.createdAt) }}</span></div>
                      </header>
                      <p>{{ reply.content }}</p>
                      <div class="comment-actions">
                        <button type="button" :class="{ active: likedComments.has(reply.id) }" :aria-pressed="likedComments.has(reply.id)" @click="handleLikeComment(reply.id)"><el-icon><Pointer /></el-icon> 点赞 {{ reply.likeCount }}</button>
                        <button type="button" @click="startReply(reply.id, reply.author.nickname)">回复</button>
                      </div>
                      <div v-if="replyingTo === reply.id" :id="`reply-input-${reply.id}`" class="reply-composer">
                        <el-input v-model="replyContent" type="textarea" :rows="2" :placeholder="`回复 ${replyTargetNickname}`" />
                        <div><el-button @click="cancelReply">取消</el-button><el-button type="primary" :loading="submittingReply" @click="handleReplySubmit(reply.id)">回复</el-button></div>
                      </div>
                    </article>
                  </div>
                </article>
              </div>
            </section>
          </div>

          <aside class="article-aside" aria-label="文章目录与数据">
            <nav v-if="articleDocument.toc.length" class="toc" aria-label="文章目录">
              <h2>目录</h2>
              <a
                v-for="entry in articleDocument.toc"
                :key="entry.id"
                :href="`#${entry.id}`"
                :class="{ nested: entry.level === 3 }"
              >{{ entry.text }}</a>
            </nav>
            <dl class="article-stats">
              <div><dt>阅读</dt><dd>{{ post.viewCount }}</dd></div>
              <div><dt>点赞</dt><dd>{{ post.likeCount }}</dd></div>
              <div><dt>评论</dt><dd>{{ post.commentCount }}</dd></div>
            </dl>
          </aside>
        </div>
      </template>
    </main>
    <SiteFooter />
  </div>
</template>

<style scoped>
.post-page {
  min-height: 100vh;
  background: var(--color-surface);
}

main {
  width: min(calc(100% - var(--space-8)), var(--content-width));
  margin: 0 auto;
}

.article-header {
  max-width: 900px;
  padding: var(--space-12) 0 var(--space-8);
  border-bottom: 1px solid var(--color-border-strong);
}

.back-link {
  color: var(--color-brand);
  font-size: var(--font-size-xs);
  font-weight: 700;
  text-decoration: none;
}

.article-header h1 {
  max-width: 900px;
  margin: var(--space-4) 0;
  font-family: var(--font-serif);
  font-size: var(--font-size-3xl);
  line-height: 1.2;
  overflow-wrap: anywhere;
}

.article-summary {
  max-width: var(--reading-width);
  margin: 0 0 var(--space-6);
  color: var(--color-text-secondary);
  font-family: var(--font-serif);
  font-size: var(--font-size-lg);
}

.article-meta,
.author-block,
.comment-header {
  display: flex;
  align-items: center;
}

.article-meta {
  flex-wrap: wrap;
  gap: var(--space-3) var(--space-5);
  color: var(--color-text-tertiary);
  font-size: var(--font-size-xs);
}

.article-engagement {
  display: flex;
  align-items: center;
  gap: var(--space-5);
  margin-top: var(--space-6);
  padding-top: var(--space-4);
  border-top: 1px solid var(--color-border);
}

.article-engagement button,
.article-engagement a {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  min-height: 40px;
  padding: 0 var(--space-3);
  border: 1px solid var(--color-border-strong);
  border-radius: var(--radius-sm);
  background: var(--color-surface);
  color: var(--color-text-secondary);
  cursor: pointer;
  font-size: var(--font-size-sm);
  text-decoration: none;
  transition: color var(--transition-fast), border-color var(--transition-fast), background-color var(--transition-fast);
}

.article-engagement button:hover,
.article-engagement a:hover,
.article-engagement button.active {
  border-color: var(--color-brand);
  color: var(--color-brand);
}

.article-engagement button.active { background: var(--color-brand-soft); }
.article-engagement button:disabled { cursor: wait; opacity: 0.6; }
.article-engagement strong { color: inherit; font-family: var(--font-serif); font-size: var(--font-size-base); }

.author-block,
.comment-header {
  gap: var(--space-3);
}

.author-block div,
.comment-header div {
  display: flex;
  flex-direction: column;
}

.author-block strong,
.comment-header strong {
  color: var(--color-text);
  font-size: var(--font-size-sm);
}

.ai-label {
  padding: var(--space-1) var(--space-2);
  border-radius: var(--radius-sm);
  background: var(--color-warning-soft);
  color: var(--color-warning);
}

.article-layout {
  display: grid;
  grid-template-columns: minmax(0, var(--reading-width)) minmax(200px, 260px);
  gap: var(--space-16);
  align-items: start;
  padding: var(--space-10) 0 var(--space-16);
}

.post-body {
  color: var(--color-text);
  font-family: var(--font-serif);
  font-size: var(--font-size-lg);
  line-height: var(--line-height-reading);
  overflow-wrap: anywhere;
}

.post-body :deep(h1),
.post-body :deep(h2),
.post-body :deep(h3),
.post-body :deep(h4) {
  scroll-margin-top: calc(var(--header-height) + var(--space-5));
  color: var(--color-text);
  font-family: var(--font-sans);
  line-height: var(--line-height-tight);
}

.post-body :deep(h1) { margin: var(--space-10) 0 var(--space-5); font-size: var(--font-size-2xl); }
.post-body :deep(h2) { margin: var(--space-10) 0 var(--space-4); font-size: var(--font-size-xl); }
.post-body :deep(h3) { margin: var(--space-8) 0 var(--space-3); font-size: var(--font-size-lg); }
.post-body :deep(h4) { margin: var(--space-6) 0 var(--space-2); font-size: var(--font-size-base); }
.post-body :deep(p) { margin: 0 0 var(--space-5); }
.post-body :deep(ul), .post-body :deep(ol) { margin: 0 0 var(--space-5); padding-left: var(--space-6); }
.post-body :deep(li) { margin-bottom: var(--space-2); }
.post-body :deep(a) { color: var(--color-brand); text-decoration-thickness: 1px; text-underline-offset: var(--space-1); }
.post-body :deep(blockquote) { margin: var(--space-6) 0; padding: var(--space-2) var(--space-5); border-left: 3px solid var(--color-brand); background: var(--color-surface-subtle); color: var(--color-text-secondary); }
.post-body :deep(code) { padding: var(--space-1) var(--space-2); border-radius: var(--radius-sm); background: var(--color-surface-subtle); color: var(--color-brand-strong); font-family: var(--font-mono); font-size: var(--font-size-sm); }
.post-body :deep(pre) { max-width: 100%; margin: var(--space-6) 0; padding: var(--space-5); border-radius: var(--radius-md); background: var(--color-code); color: var(--color-code-text); overflow-x: auto; }
.post-body :deep(pre code) { padding: 0; background: transparent; color: inherit; }
.post-body :deep(img) { height: auto; margin: var(--space-6) 0; border-radius: var(--radius-sm); }
.post-body :deep(table) { display: block; width: 100%; margin: var(--space-6) 0; border-collapse: collapse; overflow-x: auto; font-family: var(--font-sans); font-size: var(--font-size-sm); }
.post-body :deep(th), .post-body :deep(td) { padding: var(--space-3); border: 1px solid var(--color-border); text-align: left; }
.post-body :deep(th) { background: var(--color-surface-subtle); }

.article-aside {
  position: sticky;
  top: calc(var(--header-height) + var(--space-6));
  padding-top: var(--space-4);
  border-top: 2px solid var(--color-text);
}

.toc h2 {
  margin: 0 0 var(--space-3);
  font-size: var(--font-size-sm);
}

.toc a {
  display: block;
  padding: var(--space-2) 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  line-height: 1.45;
  text-decoration: none;
}

.toc a:hover {
  color: var(--color-brand);
}

.toc a.nested {
  padding-left: var(--space-4);
  font-size: var(--font-size-xs);
}

.article-stats {
  margin: var(--space-6) 0 0;
}

.article-stats div {
  display: flex;
  justify-content: space-between;
  padding: var(--space-2) 0;
  border-bottom: 1px solid var(--color-border);
}

.article-stats dt { color: var(--color-text-secondary); font-size: var(--font-size-xs); }
.article-stats dd { margin: 0; font-family: var(--font-serif); }

.article-tags {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
  margin-top: var(--space-8);
}

.article-tags span {
  padding: var(--space-1) var(--space-2);
  border: 1px solid var(--color-border-strong);
  border-radius: var(--radius-sm);
  color: var(--color-text-secondary);
  font-size: var(--font-size-xs);
}

.post-actions {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  margin: var(--space-8) 0;
  padding: var(--space-6) 0;
  border-top: 1px solid var(--color-border);
  border-bottom: 1px solid var(--color-border);
}

.comment-anchor {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  text-decoration: none;
}

.comment-section {
  scroll-margin-top: calc(var(--header-height) + var(--space-5));
}

.section-heading {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  padding-bottom: var(--space-3);
  border-bottom: 2px solid var(--color-text);
}

.section-heading h2 { margin: 0; font-family: var(--font-serif); font-size: var(--font-size-xl); }
.section-heading span { color: var(--color-text-tertiary); font-size: var(--font-size-xs); }

.comment-composer {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: var(--space-3);
  padding: var(--space-6) 0;
  border-bottom: 1px solid var(--color-border);
}

.login-prompt,
.empty-comments {
  padding: var(--space-6) 0;
  color: var(--color-text-secondary);
}

.login-prompt a { color: var(--color-brand); }

.comment-item {
  padding: var(--space-6) 0;
  border-bottom: 1px solid var(--color-border);
}

.comment-header span {
  color: var(--color-text-tertiary);
  font-size: var(--font-size-xs);
}

.comment-item > p,
.reply-item > p {
  margin: var(--space-3) 0;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.comment-actions {
  display: flex;
  gap: var(--space-3);
}

.comment-actions button {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  padding: var(--space-1) 0;
  border: 0;
  background: transparent;
  color: var(--color-text-tertiary);
  cursor: pointer;
  font-size: var(--font-size-xs);
}

.comment-actions button:hover,
.comment-actions button.active { color: var(--color-brand); }

.reply-composer {
  display: grid;
  gap: var(--space-2);
  margin-top: var(--space-4);
}

.reply-composer > div { display: flex; justify-content: flex-end; gap: var(--space-2); }

.replies {
  margin-top: var(--space-5);
  padding-left: var(--space-5);
  border-left: 2px solid var(--color-border);
}

.reply-item {
  padding: var(--space-4) 0;
  border-bottom: 1px solid var(--color-border);
}

.reply-item:last-child { border-bottom: 0; }

.page-state {
  max-width: var(--reading-width);
  margin: var(--space-12) auto;
  padding: var(--space-8);
}

.error-state { border-top: 2px solid var(--color-danger); text-align: center; }
.error-state a { color: var(--color-brand); }

@media (max-width: 980px) {
  .article-layout {
    grid-template-columns: minmax(0, var(--reading-width));
    justify-content: center;
    gap: var(--space-8);
  }

  .article-aside {
    position: static;
    grid-row: 1;
  }
}

@media (max-width: 640px) {
  main { width: calc(100% - var(--space-6)); }
  .article-header { padding: var(--space-8) 0 var(--space-6); }
  .article-header h1 { font-size: var(--font-size-2xl); }
  .article-layout { padding-top: var(--space-6); }
  .post-body { font-size: var(--font-size-base); }
  .article-engagement { gap: var(--space-2); }
  .article-engagement button, .article-engagement a { flex: 1; justify-content: center; }
  .post-actions { align-items: flex-start; flex-direction: column; }
  .replies { padding-left: var(--space-3); }
}
</style>
