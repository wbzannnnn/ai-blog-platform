<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { DocumentChecked, MagicStick, Promotion, View } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import SiteHeader from '@/components/SiteHeader.vue'
import SiteFooter from '@/components/SiteFooter.vue'
import { generateArticle, generateSummary, generateTags } from '@/api/ai'
import { createPost } from '@/api/post'
import type { AiGenerateRequest, PostCreateRequest } from '@/api/types'
import { getApiErrorMessage } from '@/utils/errors'
import { renderMarkdown } from '@/utils/markdown'

const router = useRouter()
const form = ref<PostCreateRequest>({ title: '', content: '', summary: '', isAiGenerated: false, tags: [] })
const aiAction = ref<'article' | 'summary' | 'tags' | null>(null)
const posting = ref(false)
const showTagDialog = ref(false)
const newTagValue = ref('')
const editorMode = ref<'edit' | 'preview'>('edit')
const previewDocument = computed(() => renderMarkdown(form.value.content))

const handleAiGenerate = async (): Promise<void> => {
  if (!form.value.title.trim()) {
    ElMessage.warning('请先输入文章标题')
    return
  }
  aiAction.value = 'article'
  try {
    const request: AiGenerateRequest = { topic: form.value.title, length: 1000 }
    const response = await generateArticle(request)
    if (response.code === 200 && response.data) {
      form.value.content = response.data.content
      form.value.summary = response.data.summary
      form.value.tags = response.data.tags
      form.value.isAiGenerated = true
      ElMessage.success('文章内容已生成')
    }
  } catch (error: unknown) {
    ElMessage.error(getApiErrorMessage(error, 'AI 生成失败'))
  } finally {
    aiAction.value = null
  }
}

const handleGenerateSummary = async (): Promise<void> => {
  if (!form.value.content.trim()) {
    ElMessage.warning('请先填写文章内容')
    return
  }
  aiAction.value = 'summary'
  try {
    const response = await generateSummary(form.value.content)
    if (response.code === 200) form.value.summary = response.data
  } catch (error: unknown) {
    ElMessage.error(getApiErrorMessage(error, '摘要生成失败'))
  } finally {
    aiAction.value = null
  }
}

const handleGenerateTags = async (): Promise<void> => {
  if (!form.value.content.trim()) {
    ElMessage.warning('请先填写文章内容')
    return
  }
  aiAction.value = 'tags'
  try {
    const response = await generateTags(form.value.content)
    if (response.code === 200) form.value.tags = response.data
  } catch (error: unknown) {
    ElMessage.error(getApiErrorMessage(error, '标签生成失败'))
  } finally {
    aiAction.value = null
  }
}

const submitPost = async (status: 'DRAFT' | 'PUBLISHED'): Promise<void> => {
  if (!form.value.title.trim() || (status === 'PUBLISHED' && !form.value.content.trim())) {
    ElMessage.warning(status === 'DRAFT' ? '请输入文章标题' : '请填写文章标题和内容')
    return
  }
  posting.value = true
  try {
    const response = await createPost({ ...form.value, status })
    if (response.code === 200 && response.data) {
      ElMessage.success(status === 'DRAFT' ? '草稿已保存' : '文章已提交')
      await router.push(`/post/${response.data.id}`)
    } else {
      ElMessage.error(response.message || '提交失败')
    }
  } catch (error: unknown) {
    ElMessage.error(getApiErrorMessage(error, '提交失败'))
  } finally {
    posting.value = false
  }
}

const addTag = (): void => {
  newTagValue.value = ''
  showTagDialog.value = true
}

const confirmAddTag = (): void => {
  const tag = newTagValue.value.trim()
  if (!tag) return
  if (form.value.tags?.includes(tag)) {
    ElMessage.warning('该标签已存在')
    return
  }
  form.value.tags = [...(form.value.tags ?? []), tag]
  showTagDialog.value = false
}

const removeTag = (tag: string): void => {
  form.value.tags = form.value.tags?.filter((item) => item !== tag)
}
</script>

<template>
  <div class="write-page">
    <SiteHeader />
    <main>
      <header class="editor-header">
        <div><p>EDITOR</p><h1>新文章</h1></div>
        <div class="publish-actions">
          <el-button :loading="posting" @click="submitPost('DRAFT')"><el-icon><DocumentChecked /></el-icon>保存草稿</el-button>
          <el-button type="primary" :loading="posting" @click="submitPost('PUBLISHED')"><el-icon><Promotion /></el-icon>发布文章</el-button>
        </div>
      </header>

      <section class="ai-toolbar" aria-label="AI 写作工具">
        <span>AI 辅助</span>
        <el-button :loading="aiAction === 'article'" :disabled="aiAction !== null" @click="handleAiGenerate"><el-icon><MagicStick /></el-icon>生成初稿</el-button>
        <el-button :loading="aiAction === 'summary'" :disabled="aiAction !== null" @click="handleGenerateSummary">生成摘要</el-button>
        <el-button :loading="aiAction === 'tags'" :disabled="aiAction !== null" @click="handleGenerateTags">生成标签</el-button>
      </section>

      <form class="editor-form" @submit.prevent>
        <label for="article-title">标题</label>
        <input id="article-title" v-model="form.title" class="title-input" maxlength="120" placeholder="文章标题" />

        <div class="meta-grid">
          <div class="field-group">
            <label for="article-summary">摘要</label>
            <el-input id="article-summary" v-model="form.summary" type="textarea" :rows="3" maxlength="300" show-word-limit placeholder="简要概括文章内容" />
          </div>
          <div class="field-group">
            <span class="field-label">创作方式</span>
            <el-switch v-model="form.isAiGenerated" active-text="AI 辅助" inactive-text="原创" />
          </div>
        </div>

        <div class="tags-section">
          <span class="field-label">标签</span>
          <div class="tag-list">
            <el-tag v-for="tag in form.tags" :key="tag" closable @close="removeTag(tag)">{{ tag }}</el-tag>
            <el-button size="small" @click="addTag">添加标签</el-button>
          </div>
        </div>

        <div class="mode-tabs" role="tablist" aria-label="编辑模式">
          <button type="button" role="tab" :aria-selected="editorMode === 'edit'" :class="{ active: editorMode === 'edit' }" @click="editorMode = 'edit'">编辑</button>
          <button type="button" role="tab" :aria-selected="editorMode === 'preview'" :class="{ active: editorMode === 'preview' }" @click="editorMode = 'preview'"><el-icon><View /></el-icon>预览</button>
        </div>

        <el-input
          v-if="editorMode === 'edit'"
          v-model="form.content"
          type="textarea"
          :rows="24"
          resize="vertical"
          class="content-editor"
          placeholder="开始写作"
        />
        <article v-else class="preview-panel" v-html="previewDocument.html"></article>
      </form>
    </main>

    <el-dialog v-model="showTagDialog" title="添加标签" width="400px" :close-on-click-modal="false">
      <el-input v-model="newTagValue" maxlength="20" placeholder="标签名称" @keyup.enter="confirmAddTag" />
      <template #footer><el-button @click="showTagDialog = false">取消</el-button><el-button type="primary" :disabled="!newTagValue.trim()" @click="confirmAddTag">添加</el-button></template>
    </el-dialog>
    <SiteFooter />
  </div>
</template>

<style scoped>
.write-page { min-height: 100vh; background: var(--color-canvas); }
main { width: min(calc(100% - var(--space-8)), 1040px); margin: 0 auto; padding: var(--space-8) 0 var(--space-16); }
.editor-header { display: flex; align-items: flex-end; justify-content: space-between; gap: var(--space-6); padding-bottom: var(--space-5); border-bottom: 2px solid var(--color-text); }
.editor-header p { margin: 0 0 var(--space-1); color: var(--color-brand); font-size: var(--font-size-xs); font-weight: 700; }
.editor-header h1 { margin: 0; font-family: var(--font-serif); font-size: var(--font-size-2xl); }
.publish-actions { display: flex; gap: var(--space-3); }
.ai-toolbar { display: flex; flex-wrap: wrap; align-items: center; gap: var(--space-3); padding: var(--space-4) 0; border-bottom: 1px solid var(--color-border); }
.ai-toolbar > span { margin-right: auto; color: var(--color-text-secondary); font-size: var(--font-size-sm); font-weight: 600; }
.editor-form { display: grid; gap: var(--space-4); padding-top: var(--space-8); }
.editor-form > label, .field-label, .field-group label { color: var(--color-text-secondary); font-size: var(--font-size-xs); font-weight: 700; }
.title-input { width: 100%; padding: var(--space-2) 0 var(--space-4); border: 0; border-bottom: 1px solid var(--color-border-strong); background: transparent; color: var(--color-text); font-family: var(--font-serif); font-size: var(--font-size-2xl); outline: 0; }
.title-input:focus { border-color: var(--color-brand); }
.meta-grid { display: grid; grid-template-columns: minmax(0, 3fr) minmax(170px, 1fr); gap: var(--space-6); padding: var(--space-5) 0; }
.field-group { display: flex; flex-direction: column; align-items: flex-start; gap: var(--space-2); }
.tags-section { display: grid; grid-template-columns: 80px minmax(0, 1fr); align-items: start; gap: var(--space-3); padding-bottom: var(--space-5); border-bottom: 1px solid var(--color-border); }
.tag-list { display: flex; flex-wrap: wrap; gap: var(--space-2); }
.mode-tabs { display: inline-flex; width: fit-content; padding: var(--space-1); border: 1px solid var(--color-border); border-radius: var(--radius-sm); background: var(--color-surface-subtle); }
.mode-tabs button { display: inline-flex; align-items: center; gap: var(--space-1); padding: var(--space-2) var(--space-4); border: 0; border-radius: var(--radius-sm); background: transparent; color: var(--color-text-secondary); cursor: pointer; }
.mode-tabs button.active { background: var(--color-surface); color: var(--color-brand); box-shadow: 0 1px 2px rgb(28 33 30 / 8%); }
.content-editor :deep(.el-textarea__inner) { min-height: 560px !important; padding: var(--space-5); border-radius: var(--radius-sm); background: var(--color-surface); color: var(--color-text); font-family: var(--font-mono); line-height: var(--line-height-base); box-shadow: 0 0 0 1px var(--color-border) inset; }
.preview-panel { min-height: 560px; padding: var(--space-8); border: 1px solid var(--color-border); border-radius: var(--radius-sm); background: var(--color-surface); font-family: var(--font-serif); line-height: var(--line-height-reading); overflow-wrap: anywhere; }
.preview-panel :deep(h1), .preview-panel :deep(h2), .preview-panel :deep(h3) { font-family: var(--font-sans); line-height: var(--line-height-tight); }
.preview-panel :deep(h1) { font-size: var(--font-size-2xl); }
.preview-panel :deep(h2) { margin-top: var(--space-8); font-size: var(--font-size-xl); }
.preview-panel :deep(h3) { margin-top: var(--space-6); font-size: var(--font-size-lg); }
.preview-panel :deep(a) { color: var(--color-brand); }
.preview-panel :deep(blockquote) { margin: var(--space-5) 0; padding: var(--space-3) var(--space-5); border-left: 3px solid var(--color-brand); background: var(--color-surface-subtle); color: var(--color-text-secondary); }
.preview-panel :deep(pre) { max-width: 100%; padding: var(--space-5); border-radius: var(--radius-md); background: var(--color-code); color: var(--color-code-text); overflow-x: auto; }
.preview-panel :deep(code) { font-family: var(--font-mono); }
.preview-panel :deep(img) { height: auto; border-radius: var(--radius-sm); }
.preview-panel :deep(table) { display: block; max-width: 100%; border-collapse: collapse; overflow-x: auto; }
.preview-panel :deep(th), .preview-panel :deep(td) { padding: var(--space-3); border: 1px solid var(--color-border); }
@media (max-width: 720px) {
  main { width: calc(100% - var(--space-6)); }
  .editor-header { align-items: flex-start; flex-direction: column; }
  .publish-actions { width: 100%; }
  .publish-actions .el-button { flex: 1; }
  .meta-grid { grid-template-columns: 1fr; }
  .ai-toolbar > span { width: 100%; }
  .title-input { font-size: var(--font-size-xl); }
  .preview-panel { padding: var(--space-5); }
}
</style>
