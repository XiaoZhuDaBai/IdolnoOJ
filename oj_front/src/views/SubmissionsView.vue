<template>
  <div class="submissions-view">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-content">
        <h1 class="page-title">提交记录</h1>
        <p class="page-subtitle">查看所有代码提交历史和运行结果</p>
      </div>
    </div>

    <!-- 主要内容区域 -->
    <div class="content-wrapper">
      <!-- 主要内容区 -->
      <div class="main-section">
        <!-- 筛选器卡片 -->
        <div class="filter-card">
          <div class="filter-row">
            <div class="filter-item">
              <label class="filter-label">题目名称</label>
        <input
          type="text"
          v-model="filters.problemId"
                placeholder="搜索题目..."
          class="filter-input"
        >
            </div>

            <div class="filter-item">
              <label class="filter-label">编程语言</label>
        <select v-model="filters.language" class="filter-select">
                <option value="">全部语言</option>
          <option value="cpp">C++</option>
          <option value="java">Java</option>
          <option value="python">Python</option>
          <option value="javascript">JavaScript</option>
        </select>
            </div>

            <div class="filter-item">
              <label class="filter-label">提交状态</label>
        <select v-model="filters.status" class="filter-select">
                <option value="">全部状态</option>
                <option value="通过">✓ 通过</option>
                <option value="答案错误">✗ 答案错误</option>
                <option value="提交超时">⏱ 提交超时</option>
                <option value="内存超限">💾 内存超限</option>
                <option value="运行错误">⚠ 运行错误</option>
                <option value="编译错误">🔧 编译错误</option>
                <option value="非零异常">⚡ 非零异常</option>
        </select>
            </div>

            <div class="filter-item">
              <label class="filter-label">提交用户</label>
        <select v-model="filters.userType" class="filter-select">
          <option value="all">所有用户</option>
          <option value="me">我的提交</option>
          <option value="others">其他用户</option>
        </select>
            </div>
          </div>

          <div class="filter-actions">
            <button class="search-btn primary" @click="searchSubmissions">
              <span class="btn-icon">🔍</span>
              搜索
            </button>
            <button class="search-btn secondary" @click="resetFilters">
              <span class="btn-icon">🔄</span>
              重置
            </button>
      </div>
    </div>

        <!-- 提交列表卡片 -->
        <div class="submissions-card">
          <!-- 列表头部 -->
      <div class="list-header">
            <div class="header-cell problem-col">题目</div>
            <div class="header-cell user-col">用户</div>
            <div class="header-cell status-col">状态</div>
            <div class="header-cell lang-col">语言</div>
            <div class="header-cell time-col">执行时间</div>
            <div class="header-cell memory-col">内存</div>
            <div class="header-cell date-col">提交时间</div>
      </div>

          <!-- 加载状态 -->
          <div v-if="loading && submissions.length === 0" class="loading-state">
        <div class="loading-spinner"></div>
            <span class="loading-text">加载中...</span>
      </div>

          <!-- 空状态 -->
          <div v-else-if="submissions.length === 0" class="empty-state">
            <div class="empty-icon">📝</div>
            <div class="empty-text">暂无提交记录</div>
            <div class="empty-hint">尝试调整筛选条件或开始提交代码</div>
      </div>

          <!-- 提交列表 -->
          <div v-else class="submission-items" :class="{ 'loading-overlay': loading }">
        <div
          v-for="submission in submissions"
          :key="submission.id"
          class="submission-item"
          @click="viewSubmission(submission.id)"
        >
              <div class="item-cell problem-col">
                <router-link
                  :to="`/problem/${submission.problemId}`"
                  class="problem-link"
                  @click.stop
                >
                  <span class="problem-icon">📋</span>
              {{ submission.problemName }}
            </router-link>
          </div>

              <div class="item-cell user-col">
                <div class="user-info">
            <img :src="submission.userAvatar" :alt="submission.username" class="user-avatar">
                  <span class="username">{{ submission.username }}</span>
                </div>
          </div>

              <div class="item-cell status-col">
            <span :class="['status-badge', submission.status]">
                  <span class="status-icon">{{ getStatusIcon(submission.status) }}</span>
              {{ getStatusText(submission.status) }}
            </span>
          </div>

              <div class="item-cell lang-col">
                <span class="language-tag">{{ submission.language }}</span>
              </div>

              <div class="item-cell time-col">
                <span class="metric-value">{{ submission.executionTime }}ms</span>
              </div>

              <div class="item-cell memory-col">
                <span class="metric-value">{{ formatMemory(submission.memory) }}</span>
              </div>

              <div class="item-cell date-col">
                <span class="date-text">{{ formatDate(submission.submitTime) }}</span>
              </div>
        </div>
      </div>

      <!-- 分页 -->
      <div class="pagination-container">
        <div class="pagination">
          <button
            @click="prevPage"
            :disabled="currentPage === 1 || loading"
                class="page-btn nav-btn"
                title="上一页"
          >
                ‹
          </button>

          <button
            v-if="showFirstPage"
            @click="goToPage(1)"
                :class="['page-btn', { active: currentPage === 1 }]"
            :disabled="loading"
          >
            1
          </button>

          <span v-if="showFirstEllipsis" class="ellipsis">...</span>

          <button
            v-for="page in displayedPages"
            :key="page"
            @click="goToPage(page)"
                :class="['page-btn', { active: currentPage === page }]"
            :disabled="loading"
          >
            {{ page }}
          </button>

          <span v-if="showLastEllipsis" class="ellipsis">...</span>

          <button
            v-if="showLastPage"
            @click="goToPage(totalPages)"
                :class="['page-btn', { active: currentPage === totalPages }]"
            :disabled="loading"
          >
            {{ totalPages }}
          </button>

          <button
            @click="nextPage"
            :disabled="currentPage === totalPages || loading"
                class="page-btn nav-btn"
                title="下一页"
          >
                ›
          </button>
        </div>

        <div class="page-info">
              <span class="info-text">
                第 <strong>{{ currentPage }}</strong> / <strong>{{ totalPages }}</strong> 页
              </span>
              <span class="info-divider">·</span>
              <span class="info-text">
                共 <strong>{{ totalSubmissions }}</strong> 条记录
              </span>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧边栏 -->
      <aside class="sidebar-section">
        <!-- 快速统计卡片 -->
        <div class="sidebar-card stats-card">
          <h3 class="card-title">提交统计</h3>
          <div class="stats-list">
            <div class="stat-row">
              <span class="stat-label">总提交</span>
              <span class="stat-value">{{ totalSubmissions }}</span>
            </div>
            <div class="stat-row">
              <span class="stat-label">当前页</span>
              <span class="stat-value">{{ submissions.length }}</span>
            </div>
            <div class="stat-row">
              <span class="stat-label">每页数量</span>
              <span class="stat-value">{{ itemsPerPage }}</span>
            </div>
          </div>
        </div>

        <!-- 状态说明卡片 -->
        <div class="sidebar-card legend-card">
          <h3 class="card-title">状态说明</h3>
          <div class="legend-list">
            <div class="legend-item">
              <span class="legend-badge accepted">✓</span>
              <span class="legend-text">通过</span>
            </div>
            <div class="legend-item">
              <span class="legend-badge wrong_answer">✗</span>
              <span class="legend-text">答案错误</span>
            </div>
            <div class="legend-item">
              <span class="legend-badge time_limit">⏱</span>
              <span class="legend-text">超时</span>
            </div>
            <div class="legend-item">
              <span class="legend-badge memory_limit">💾</span>
              <span class="legend-text">内存超限</span>
            </div>
            <div class="legend-item">
              <span class="legend-badge runtime_error">⚠</span>
              <span class="legend-text">运行错误</span>
            </div>
            <div class="legend-item">
              <span class="legend-badge compile_error">🔧</span>
              <span class="legend-text">编译错误</span>
            </div>
          </div>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/userStore'
import { useSubmissionStore } from '@/stores/submissionStroe'

const router = useRouter()
const userStore = useUserStore()
const submissionStore = useSubmissionStore()

// 从 store 中获取状态，使用 computed 保持响应性
const submissions = computed(() => submissionStore.submissions)
const loading = computed(() => submissionStore.loading)
const currentPage = computed({
  get: () => submissionStore.currentPage,
  set: (value) => {
    submissionStore.currentPage = value
  }
})
const totalPages = computed(() => submissionStore.totalPages)
const totalSubmissions = computed(() => submissionStore.totalSubmissions)
const itemsPerPage = computed(() => submissionStore.itemsPerPage)
const filters = computed({
  get: () => submissionStore.filters,
  set: (value) => {
    Object.assign(submissionStore.filters, value)
  }
})

// 获取方法（方法不需要保持响应性）
const { getStatusText, formatMemory, formatDate } = submissionStore

// 分页逻辑 - 参考ProblemTable.vue
const maxDisplayedPages = 5 // 显示的页码数量

// 计算显示的页码范围
const displayedPages = computed(() => {
  const pages = []
  const halfDisplayed = Math.floor(maxDisplayedPages / 2)

  let start = Math.max(currentPage.value - halfDisplayed, 1)
  let end = Math.min(start + maxDisplayedPages - 1, totalPages.value)

  // 调整起始页，确保显示足够的页码
  if (end - start + 1 < maxDisplayedPages) {
    start = Math.max(end - maxDisplayedPages + 1, 1)
  }

  for (let i = start; i <= end; i++) {
    pages.push(i)
  }

  return pages
})

// 是否显示第一页
const showFirstPage = computed(() => {
  return displayedPages.value[0] > 1
})

// 是否显示第一页后的省略号
const showFirstEllipsis = computed(() => {
  return displayedPages.value[0] > 2
})

// 是否显示最后一页
const showLastPage = computed(() => {
  return displayedPages.value[displayedPages.value.length - 1] < totalPages.value
})

// 是否显示最后一页前的省略号
const showLastEllipsis = computed(() => {
  return displayedPages.value[displayedPages.value.length - 1] < totalPages.value - 1
})

// 搜索提交记录
const searchSubmissions = () => {
  const uuid = userStore.user?.uuid
  submissionStore.searchSubmissions(uuid)
}

// 分页方法
const prevPage = () => {
  if (currentPage.value > 1 && !loading.value) {
    changePage(currentPage.value - 1)
  }
}

const nextPage = () => {
  if (currentPage.value < totalPages.value && !loading.value) {
    changePage(currentPage.value + 1)
  }
}

const goToPage = (page) => {
  if (!loading.value) {
    changePage(page)
  }
}

// 切换页码
const changePage = (page) => {
  const uuid = userStore.user?.uuid
  submissionStore.changePage(page, uuid)
}

// 查看提交详情
const viewSubmission = (id) => {
  // 找到对应的提交记录
  const submission = submissions.value.find(s => s.id === id)
  if (submission) {
    // 将完整的提交数据存储到 store 中，供详情页面使用
    submissionStore.setCurrentSubmission(submission)

    // 跳转到详情页面
    router.push({
      path: `/submission/${id}`
    })
  }
}

// 重置筛选器
const resetFilters = () => {
  filters.value = {
    problemId: '',
    language: '',
    status: '',
    userType: 'all'
  }
  searchSubmissions()
}

// 获取状态图标
const getStatusIcon = (status) => {
  const iconMap = {
    accepted: '✓',
    wrong_answer: '✗',
    time_limit: '⏱',
    memory_limit: '💾',
    runtime_error: '⚠',
    compile_error: '🔧',
    non_zero_exit: '⚡',
    pending: '⏳',
    judging: '🔄'
  }
  return iconMap[status] || '•'
}

// 组件挂载时加载数据
onMounted(() => {
  // 重置页码到第一页
  currentPage.value = 1
  searchSubmissions()
})
</script>

<style scoped>
.submissions-view {
  min-height: calc(100vh - var(--header-height, 64px));
  background: var(--color-bg);
}

/* 页面头部 */
.page-header {
  background: linear-gradient(135deg, var(--color-primary) 0%, #5b7fc9 100%);
  padding: 32px 24px;
  margin-bottom: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.header-content {
  max-width: 1400px;
  margin: 0 auto;
}

.page-title {
  font-size: 28px;
  font-weight: 600;
  color: white;
  margin: 0 0 8px 0;
  letter-spacing: -0.5px;
}

.page-subtitle {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.9);
  margin: 0;
  font-weight: 400;
}

/* 内容包装器 */
.content-wrapper {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 24px 24px;
  display: grid;
  grid-template-columns: 1fr;
  gap: 24px;
}

@media (min-width: 1024px) {
  .content-wrapper {
    grid-template-columns: 1fr 320px;
  }
}

/* 主要内容区域 */
.main-section {
  display: flex;
  flex-direction: column;
  gap: 20px;
  min-width: 0;
}

/* 筛选器卡片 */
.filter-card {
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  border: 1px solid var(--color-border);
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.filter-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
}

.filter-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.filter-label {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-secondary);
  margin-bottom: 2px;
}

.filter-input,
.filter-select {
  padding: 10px 14px;
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 14px;
  color: var(--color-text);
  background: var(--color-surface);
  transition: all 0.2s ease;
}

.filter-input:focus,
.filter-select:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(79, 109, 188, 0.1);
}

.filter-input::placeholder {
  color: var(--color-text-tertiary);
}

.filter-actions {
  display: flex;
  gap: 12px;
  padding-top: 8px;
}

.search-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  border: none;
  border-radius: var(--radius-md);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.search-btn.primary {
  background: var(--color-primary);
  color: white;
}

.search-btn.primary:hover {
  background: #5b7fc9;
  transform: translateY(-1px);
  box-shadow: 0 4px 8px rgba(79, 109, 188, 0.3);
}

.search-btn.secondary {
  background: var(--color-bg);
  color: var(--color-text-secondary);
  border: 1.5px solid var(--color-border);
}

.search-btn.secondary:hover {
  background: var(--color-surface);
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.btn-icon {
  font-size: 14px;
}

/* 提交列表卡片 */
.submissions-card {
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  border: 1px solid var(--color-border);
  overflow: hidden;
}

/* 列表头部 */
.list-header {
  display: grid;
  grid-template-columns: 2.5fr 1.5fr 1.2fr 1fr 1fr 1fr 1.5fr;
  padding: 16px 20px;
  background: var(--color-bg);
  border-bottom: 2px solid var(--color-border);
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.header-cell {
  display: flex;
  align-items: center;
}

/* 状态样式 */
.loading-state,
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  gap: 16px;
}

.loading-spinner {
  width: 32px;
  height: 32px;
  border: 3px solid var(--color-border);
  border-top: 3px solid var(--color-primary);
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.loading-text {
  color: var(--color-text-secondary);
  font-size: 14px;
}

.empty-icon {
  font-size: 64px;
  opacity: 0.5;
}

.empty-text {
  font-size: 16px;
  font-weight: 500;
  color: var(--color-text);
}

.empty-hint {
  font-size: 13px;
  color: var(--color-text-tertiary);
}

/* 提交列表 */
.submission-items {
  max-height: calc(100vh - 400px);
  min-height: 300px;
  overflow-y: auto;
}

.submission-items.loading-overlay {
  opacity: 0.6;
  pointer-events: none;
}

.submission-item {
  display: grid;
  grid-template-columns: 2.5fr 1.5fr 1.2fr 1fr 1fr 1fr 1.5fr;
  padding: 16px 20px;
  border-bottom: 1px solid var(--color-border);
  cursor: pointer;
  transition: all 0.2s ease;
  background: var(--color-surface);
}

.submission-item:hover {
  background: var(--color-bg);
  transform: translateX(4px);
}

.submission-item:last-child {
  border-bottom: none;
}

.item-cell {
  display: flex;
  align-items: center;
  font-size: 14px;
  color: var(--color-text);
}

/* 题目列 */
.problem-link {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--color-primary);
  text-decoration: none;
  font-weight: 500;
  transition: color 0.2s ease;
}

.problem-link:hover {
  color: #5b7fc9;
}

.problem-icon {
  font-size: 16px;
}

/* 用户列 */
.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  border: 2px solid var(--color-border);
}

.username {
  font-weight: 500;
  color: var(--color-text);
}

/* 状态徽章 */
.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: var(--radius-sm);
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
}

.status-icon {
  font-size: 14px;
}

.status-badge.accepted {
  background: rgba(16, 185, 129, 0.1);
  color: #10b981;
}

.status-badge.wrong_answer {
  background: rgba(239, 68, 68, 0.1);
  color: #ef4444;
}

.status-badge.time_limit {
  background: rgba(245, 158, 11, 0.1);
  color: #f59e0b;
}

.status-badge.memory_limit {
  background: rgba(168, 85, 247, 0.1);
  color: #a855f7;
}

.status-badge.runtime_error {
  background: rgba(239, 68, 68, 0.1);
  color: #ef4444;
}

.status-badge.compile_error {
  background: rgba(249, 115, 22, 0.1);
  color: #f97316;
}

.status-badge.non_zero_exit {
  background: rgba(234, 88, 12, 0.1);
  color: #ea580c;
}

.status-badge.pending {
  background: rgba(107, 114, 128, 0.1);
  color: #6b7280;
}

.status-badge.judging {
  background: rgba(59, 130, 246, 0.1);
  color: #3b82f6;
}

/* 语言标签 */
.language-tag {
  padding: 4px 10px;
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  font-size: 12px;
  font-weight: 500;
  color: var(--color-text-secondary);
}

/* 指标值 */
.metric-value {
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 13px;
  color: var(--color-text-secondary);
  font-weight: 500;
}

.date-text {
  font-size: 13px;
  color: var(--color-text-tertiary);
}

/* 分页 */
.pagination-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: var(--color-bg);
  border-top: 1px solid var(--color-border);
}

.pagination {
  display: flex;
  gap: 6px;
  align-items: center;
}

.page-btn {
  min-width: 36px;
  height: 36px;
  padding: 0 12px;
  border: 1.5px solid var(--color-border);
  background: var(--color-surface);
  color: var(--color-text);
  border-radius: var(--radius-md);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
}

.page-btn:hover:not(:disabled) {
  border-color: var(--color-primary);
  color: var(--color-primary);
  background: rgba(79, 109, 188, 0.05);
}

.page-btn.active {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: white;
}

.page-btn.nav-btn {
  font-size: 20px;
  font-weight: 400;
}

.page-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.ellipsis {
  padding: 0 8px;
  color: var(--color-text-tertiary);
  font-weight: 500;
}

.page-info {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--color-text-secondary);
}

.info-text strong {
  color: var(--color-text);
  font-weight: 600;
}

.info-divider {
  color: var(--color-border);
}

/* 侧边栏 */
.sidebar-section {
  display: flex;
  flex-direction: column;
  gap: 20px;
  align-self: flex-start;
  position: sticky;
  top: 24px;
}

.sidebar-card {
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  border: 1px solid var(--color-border);
}

.card-title {
  margin: 0 0 16px 0;
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text);
  letter-spacing: -0.2px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--color-border);
}

/* 统计列表 */
.stats-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.stat-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  background: var(--color-bg);
  border-radius: var(--radius-sm);
}

.stat-label {
  font-size: 13px;
  color: var(--color-text-secondary);
}

.stat-value {
  font-size: 18px;
  font-weight: 700;
  color: var(--color-primary);
}

/* 图例列表 */
.legend-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px;
  border-radius: var(--radius-sm);
  transition: background 0.2s ease;
}

.legend-item:hover {
  background: var(--color-bg);
}

.legend-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 28px;
  height: 28px;
  padding: 4px 8px;
  border-radius: var(--radius-sm);
  font-size: 12px;
  font-weight: 600;
}

.legend-badge.accepted {
  background: rgba(16, 185, 129, 0.1);
  color: #10b981;
}

.legend-badge.wrong_answer {
  background: rgba(239, 68, 68, 0.1);
  color: #ef4444;
}

.legend-badge.time_limit {
  background: rgba(245, 158, 11, 0.1);
  color: #f59e0b;
}

.legend-badge.memory_limit {
  background: rgba(168, 85, 247, 0.1);
  color: #a855f7;
}

.legend-badge.runtime_error {
  background: rgba(239, 68, 68, 0.1);
  color: #ef4444;
}

.legend-badge.compile_error {
  background: rgba(249, 115, 22, 0.1);
  color: #f97316;
}

.legend-text {
  font-size: 13px;
  color: var(--color-text-secondary);
}

/* 移动端优化 */
@media (max-width: 1023px) {
  .list-header,
  .submission-item {
    grid-template-columns: 2fr 1fr 1fr;
  }

  .user-col,
  .lang-col,
  .time-col,
  .memory-col {
    display: none;
  }
}

@media (max-width: 768px) {
  .page-header {
    padding: 24px 16px;
    margin-bottom: 16px;
  }

  .page-title {
    font-size: 24px;
  }

  .page-subtitle {
    font-size: 13px;
  }

  .content-wrapper {
    padding: 0 16px 16px;
    gap: 16px;
  }

  .filter-card,
  .submissions-card,
  .sidebar-card {
    padding: 16px;
  }

  .filter-row {
    grid-template-columns: 1fr;
  }

  .filter-actions {
    flex-direction: column;
  }

  .search-btn {
    width: 100%;
    justify-content: center;
  }

  .submission-items {
    max-height: calc(100vh - 500px);
  }

  .list-header,
  .submission-item {
    padding: 12px 16px;
  }
}
</style>
