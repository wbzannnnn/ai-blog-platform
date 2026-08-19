<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Delete, View, Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { deletePostAsAdmin } from '../../api/admin'
import { getPosts } from '../../api/post'
import type { PostResponse } from '../../api/types'
import { renderMarkdown } from '@/utils/markdown'

const posts = ref<PostResponse[]>([])
const totalElements = ref(0)
const currentPage = ref(0)
const showDialog = ref(false)
const viewPost = ref<PostResponse | null>(null)
const searchKeyword = ref('')
const filteredPosts = computed(() => {
  const keyword = searchKeyword.value.trim().toLowerCase()
  if (!keyword) return posts.value
  return posts.value.filter((post) => post.title.toLowerCase().includes(keyword))
})
const detailHtml = computed(() => renderMarkdown(viewPost.value?.content ?? '').html)

const loadPosts = async (page = 0) => {
  try {
    const response = await getPosts(page)
    if (response.code === 200) {
      posts.value = response.data.content
      totalElements.value = response.data.totalElements
    }
  } catch (error) {
    console.error('加载文章失败', error)
  }
}

const handleView = (post: PostResponse) => {
  viewPost.value = post
  showDialog.value = true
}

const handleDelete = async (id: number) => {
  if (!confirm('确定要删除该文章吗？')) return
  try {
    const response = await deletePostAsAdmin(id)
    if (response.code === 200) {
      ElMessage.success('删除成功')
      loadPosts(currentPage.value)
    }
  } catch (error) {
    ElMessage.error('删除失败')
  }
}

const handlePageChange = (page: number) => {
  currentPage.value = page - 1
  loadPosts(page - 1)
}

const formatDate = (timestamp: number) => {
  return new Date(timestamp).toLocaleString('zh-CN')
}

const getStatusText = (status: string) => {
  const map: Record<string, string> = {
    'DRAFT': '草稿',
    'PUBLISHED': '已发布',
    'ARCHIVED': '已归档'
  }
  return map[status] || status
}

const getStatusType = (status: string) => {
  const map: Record<string, string> = {
    'DRAFT': 'info',
    'PUBLISHED': 'success',
    'ARCHIVED': 'warning'
  }
  return map[status] || 'info'
}

onMounted(() => {
  loadPosts()
})
</script>

<template>
  <div class="post-management">
    <div class="page-header">
      <h2 class="page-title">文章管理</h2>
      <p class="page-subtitle">管理所有博客文章</p>
    </div>

    <el-card class="content-card">
      <div class="card-header">
        <el-input 
          v-model="searchKeyword" 
          placeholder="搜索文章标题..." 
          class="search-input"
          :prefix-icon="Search"
        />
      </div>

      <el-table :data="filteredPosts" border="false" class="data-table">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="title" label="标题" min-width="200">
          <template #default="scope">
            <span class="title-cell">{{ scope.row.title }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="author.nickname" label="作者" width="100">
          <template #default="scope">
            <el-tag size="small">{{ scope.row.author.nickname }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.status)" size="small">
              {{ getStatusText(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="tags" label="标签" min-width="200">
          <template #default="scope">
            <div class="tags-wrapper">
              <el-popover 
                :trigger="'hover'" 
                placement="top-start" 
                width="200"
              >
                <template #reference>
                  <div class="tags-container">
                    <el-tag 
                      v-for="tag in scope.row.tags" 
                      :key="tag.id" 
                      size="small" 
                      type="info"
                      class="tag-item"
                    >
                      {{ tag.name }}
                    </el-tag>
                    <span class="tags-ellipsis">...</span>
                  </div>
                </template>
                <div class="tags-popup">
                  <el-tag 
                    v-for="tag in scope.row.tags" 
                    :key="tag.id" 
                    size="small" 
                    type="info"
                    class="popup-tag"
                  >
                    {{ tag.name }}
                  </el-tag>
                </div>
              </el-popover>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="viewCount" label="阅读量" width="100" />
        <el-table-column prop="createdAt" label="创建时间" width="180">
          <template #default="scope">
            {{ formatDate(scope.row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160">
          <template #default="scope">
            <div class="action-buttons">
              <el-button size="small" @click="handleView(scope.row as unknown as PostResponse)">
                <View /> 查看
              </el-button>
              <el-button size="small" type="danger" @click="handleDelete((scope.row as unknown as PostResponse).id)">
                <Delete /> 删除
              </el-button>
            </div>
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
          <el-tag :type="getStatusType(viewPost.status)" size="small">
            {{ getStatusText(viewPost.status) }}
          </el-tag>
          <el-tag size="small">审核：{{ viewPost.moderationStatus }}</el-tag>
        </div>
        <div class="post-tags">
          <el-tag v-for="tag in viewPost.tags" :key="tag.id" type="info">{{ tag.name }}</el-tag>
        </div>
        <div class="post-summary">{{ viewPost.summary }}</div>
        <div class="post-content" v-html="detailHtml"></div>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.post-management {
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
  padding-bottom: 16px;
  border-bottom: 1px solid var(--color-border);
  margin-bottom: 16px;
}

.search-input {
  width: 300px;
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

.tags-wrapper {
  max-width: 100%;
}

.tags-container {
  display: flex;
  flex-wrap: nowrap;
  overflow: hidden;
  max-width: 100%;
  align-items: center;
}

.tag-item {
  flex-shrink: 0;
  margin-right: 4px;
}

.tags-ellipsis {
  flex-shrink: 0;
  font-size: 12px;
  color: var(--color-text-secondary);
  margin-left: 2px;
}

.tags-popup {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  padding: 8px;
}

.popup-tag {
  margin-bottom: 4px;
}

.action-buttons {
  display: flex;
  gap: 8px;
}
</style>
