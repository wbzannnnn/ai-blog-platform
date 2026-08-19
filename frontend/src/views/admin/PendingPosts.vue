<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Check, Close, View, Warning } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getPendingPosts, approvePost, rejectPost } from '../../api/admin'
import type { PostResponse } from '../../api/types'
import { renderMarkdown } from '@/utils/markdown'

const posts = ref<PostResponse[]>([])
const totalElements = ref(0)
const currentPage = ref(0)
const showDialog = ref(false)
const showRejectDialog = ref(false)
const viewPost = ref<PostResponse | null>(null)
const rejectReason = ref('')
const detailHtml = computed(() => renderMarkdown(viewPost.value?.content ?? '').html)

const loadPosts = async (page = 0) => {
  try {
    const response = await getPendingPosts(page)
    if (response.code === 200) {
      posts.value = response.data.content
      totalElements.value = response.data.totalElements
    }
  } catch (error) {
    console.error('加载待审核文章失败', error)
  }
}

const handleView = (post: PostResponse) => {
  viewPost.value = post
  showDialog.value = true
}

const handleApprove = async (id: number) => {
  try {
    const response = await approvePost(id)
    if (response.code === 200) {
      ElMessage.success('审核通过')
      loadPosts(currentPage.value)
    }
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

const handleReject = async () => {
  if (!viewPost.value) return
  try {
    const response = await rejectPost(viewPost.value.id, rejectReason.value || '内容不符合规范')
    if (response.code === 200) {
      ElMessage.success('已拒绝')
      showRejectDialog.value = false
      showDialog.value = false
      loadPosts(currentPage.value)
    }
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

const openRejectDialog = (post: PostResponse) => {
  viewPost.value = post
  rejectReason.value = ''
  showDialog.value = false
  showRejectDialog.value = true
}

const handlePageChange = (page: number) => {
  currentPage.value = page - 1
  loadPosts(page - 1)
}

const formatDate = (timestamp: number) => {
  return new Date(timestamp).toLocaleString('zh-CN')
}

onMounted(() => {
  loadPosts()
})
</script>

<template>
  <div class="pending-posts">
    <div class="page-header">
      <h2 class="page-title">待审核文章</h2>
      <p class="page-subtitle">管理待审核的文章内容</p>
    </div>

    <el-card class="content-card">
      <div class="card-header">
        <div class="pending-count">
          <el-icon class="warning-icon"><Warning /></el-icon>
          <span>共 <strong>{{ totalElements }}</strong> 篇文章待审核</span>
        </div>
      </div>

      <el-table :data="posts" border="false" class="data-table">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="title" label="标题" min-width="200">
          <template #default="scope">
            <span class="title-cell">{{ scope.row.title }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="author.nickname" label="作者" width="120">
          <template #default="scope">
            <el-tag size="small">{{ scope.row.author.nickname }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="isAiGenerated" label="AI生成" width="100">
          <template #default="scope">
            <el-tag v-if="scope.row.isAiGenerated" type="warning" size="small">是</el-tag>
            <span v-else class="no-tag">否</span>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="提交时间" width="180">
          <template #default="scope">
            {{ formatDate(scope.row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="scope">
            <el-button size="small" @click="handleView(scope.row as unknown as PostResponse)">
              <View /> 查看
            </el-button>
            <el-button size="small" type="success" @click="handleApprove((scope.row as unknown as PostResponse).id)">
              <Check /> 通过
            </el-button>
            <el-button size="small" type="danger" @click="openRejectDialog(scope.row as unknown as PostResponse)">
              <Close /> 拒绝
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          :current-page="currentPage + 1"
          :page-size="10"
          :total="totalElements"
          layout="prev, pager, next"
          @current-change="handlePageChange"
          class="pagination"
        />
      </div>
    </el-card>

    <el-dialog v-model="showDialog" title="文章详情" width="800px">
      <div v-if="viewPost" class="post-detail">
        <h3>{{ viewPost.title }}</h3>
        <div class="post-meta">
          <el-tag size="small">作者：{{ viewPost.author.nickname }}</el-tag>
          <el-tag v-if="viewPost.isAiGenerated" type="warning" size="small">AI生成</el-tag>
        </div>
        <div class="post-tags">
          <el-tag v-for="tag in viewPost.tags" :key="tag.id" type="info">{{ tag.name }}</el-tag>
        </div>
        <div class="post-summary">{{ viewPost.summary }}</div>
        <div class="post-content" v-html="detailHtml"></div>
      </div>
      <template #footer>
        <el-button @click="showDialog = false">关闭</el-button>
        <el-button type="success" @click="handleApprove(viewPost!.id)">
          <Check /> 通过审核
        </el-button>
        <el-button type="danger" @click="openRejectDialog(viewPost!)">
          <Close /> 拒绝
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showRejectDialog" title="拒绝文章" width="500px">
      <el-form label-width="80px" class="form">
        <el-form-item label="拒绝原因">
          <el-input 
            v-model="rejectReason" 
            type="textarea" 
            :rows="4" 
            placeholder="请输入拒绝原因，将通知作者"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showRejectDialog = false">取消</el-button>
        <el-button type="danger" @click="handleReject">
          <Close /> 确认拒绝
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.pending-posts {
  padding: 0;
}

.page-header {
  margin-bottom: 24px;
}

.page-title {
  font-size: 24px;
  font-weight: bold;
  color: var(--color-text);
  margin: 0 0 8px 0;
}

.page-subtitle {
  font-size: 14px;
  color: var(--color-text-secondary);
  margin: 0;
}

.content-card {
  border-radius: 12px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--color-border);
  margin-bottom: 16px;
}

.pending-count {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--color-warning);
  font-size: 14px;
}

.warning-icon {
  font-size: 18px;
}

.data-table {
  width: 100%;
}

.title-cell {
  color: var(--color-text);
  font-weight: 500;
}

.title-cell:hover {
  color: var(--color-brand);
}

.no-tag {
  color: var(--color-text-tertiary);
  font-size: 13px;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  padding-top: 16px;
  border-top: 1px solid var(--color-border);
}

.post-detail {
  padding: 16px 0;
}

.post-detail h3 {
  font-size: 20px;
  font-weight: bold;
  color: var(--color-text);
  margin: 0 0 16px 0;
}

.post-meta {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.post-tags {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

.post-summary {
  padding: 16px;
  background-color: var(--color-surface-subtle);
  border-radius: 8px;
  color: var(--color-text-secondary);
  margin-bottom: 16px;
  line-height: 1.6;
}

.post-content {
  line-height: 1.8;
  color: var(--color-text);
  max-height: 400px;
  overflow-y: auto;
}

.form {
  padding: 16px 0;
}

:deep(.el-table) {
  border: none;
}

:deep(.el-table th) {
  background-color: var(--color-surface-subtle);
  color: var(--color-text-secondary);
  font-weight: 500;
  padding: 12px 16px;
}

:deep(.el-table td) {
  padding: 12px 16px;
}

:deep(.el-table tr:hover) {
  background-color: var(--color-surface-subtle);
}

:deep(.el-table::before) {
  display: none;
}
</style>
