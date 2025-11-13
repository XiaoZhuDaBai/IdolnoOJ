<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useSubmissionStore } from '@/stores/submissionStroe'
import Prism from 'prismjs'
import 'prismjs/themes/prism-tomorrow.css'
// 导入需要的语言支持
import 'prismjs/components/prism-python'
import 'prismjs/components/prism-java'
import 'prismjs/components/prism-cpp'
import 'prismjs/components/prism-javascript'

const route = useRoute()
const router = useRouter()
const submissionStore = useSubmissionStore()
const loading = ref(true)

// 从 store 获取当前提交详情
const submission = computed(() => submissionStore.currentSubmission)

// 获取提交详情
const fetchSubmission = async () => {
  try {
    loading.value = true
    // 如果 store 中没有数据，尝试从 URL 参数获取
    if (!submission.value) {
      console.warn('未找到提交详情数据，请从提交列表页面进入')
    }
  } catch (error) {
    console.error('获取提交详情失败:', error)
  } finally {
    loading.value = false
  }
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

// 获取语言对应的 Prism 类名
const getLanguageClass = (language) => {
  const languageMap = {
    'cpp': 'cpp',
    'c++': 'cpp',
    'java': 'java',
    'python': 'python',
    'py': 'python',
    'javascript': 'javascript',
    'js': 'javascript'
  }
  return languageMap[language?.toLowerCase()] || 'javascript'
}

// 获取语言显示名称
const getLanguageName = (language) => {
  const nameMap = {
    'cpp': 'C++',
    'java': 'Java',
    'python': 'Python',
    'javascript': 'JavaScript'
  }
  return nameMap[language?.toLowerCase()] || language
}

// 高亮代码
const highlightCode = () => {
  if (submission.value?.code) {
    setTimeout(() => {
      Prism.highlightAll()
    }, 100)
  }
}

// 返回列表
const goBack = () => {
  router.push('/submissions')
}

// 复制代码
const copyCode = async () => {
  try {
    await navigator.clipboard.writeText(submission.value.code)
    alert('代码已复制到剪贴板')
  } catch (err) {
    console.error('复制失败:', err)
  }
}

onMounted(() => {
  fetchSubmission().then(() => {
    highlightCode()
  })
})
</script>

<template>
  <div class="submission-detail-view">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-content">
        <button class="back-btn" @click="goBack">
          <span class="back-icon">←</span>
          返回列表
        </button>
        <h1 class="page-title">提交详情</h1>
        <p class="page-subtitle">查看代码提交的详细信息和运行结果</p>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-state">
      <div class="loading-spinner"></div>
      <span class="loading-text">加载中...</span>
    </div>

    <!-- 空状态 -->
    <div v-else-if="!submission" class="empty-state">
      <div class="empty-icon">📝</div>
      <div class="empty-text">未找到提交记录</div>
      <div class="empty-hint">请从提交列表页面进入查看详情</div>
      <button class="go-back-btn" @click="goBack">返回提交列表</button>
    </div>

    <!-- 主要内容 -->
    <div v-else class="content-wrapper">
      <!-- 主内容区 -->
      <div class="main-section">
        <!-- 提交信息卡片 -->
        <div class="info-card">
          <div class="card-header">
            <h2 class="card-title">
              <span class="title-icon">📋</span>
          {{ submission.problemName }}
        </h2>
            <div class="header-actions">
              <router-link
                :to="`/problemPage/${submission.problemId}`"
                class="view-problem-btn"
              >
                查看题目
              </router-link>
            </div>
          </div>

          <div class="info-grid">
            <div class="info-item">
              <div class="info-label">提交用户</div>
              <div class="info-value user-value">
                <img
                  :src="submission.userAvatar"
                  :alt="submission.username"
                  class="user-avatar"
                >
                <span class="username">{{ submission.username }}</span>
              </div>
            </div>

            <div class="info-item">
              <div class="info-label">提交状态</div>
              <div class="info-value">
                <span :class="['status-badge', submission.status]">
                  <span class="status-icon">{{ getStatusIcon(submission.status) }}</span>
                  {{ submissionStore.getStatusText(submission.status) }}
                </span>
        </div>
      </div>

            <div class="info-item">
              <div class="info-label">编程语言</div>
              <div class="info-value">
                <span class="language-badge">{{ getLanguageName(submission.language) }}</span>
              </div>
            </div>

            <div class="info-item">
              <div class="info-label">执行时间</div>
              <div class="info-value metric-value">
                {{ submission.executionTime }}ms
              </div>
            </div>

            <div class="info-item">
              <div class="info-label">内存消耗</div>
              <div class="info-value metric-value">
                {{ submissionStore.formatMemory(submission.memory) }}
              </div>
            </div>

            <div class="info-item">
              <div class="info-label">提交时间</div>
              <div class="info-value">
                {{ submissionStore.formatDate(submission.submitTime) }}
              </div>
            </div>
          </div>
        </div>

        <!-- 代码卡片 -->
        <div class="code-card">
          <div class="code-header">
            <div class="code-header-left">
              <h3 class="code-title">
                <span class="code-icon">💻</span>
                提交的代码
              </h3>
              <span class="language-tag">{{ getLanguageName(submission.language) }}</span>
            </div>
            <div class="code-header-right">
              <button class="copy-btn" @click="copyCode" title="复制代码">
                <span class="copy-icon">📋</span>
                复制
              </button>
            </div>
          </div>

          <div class="code-wrapper">
            <pre class="code-pre"><code :class="`language-${getLanguageClass(submission.language)}`">{{ submission.code }}</code></pre>
          </div>
        </div>
      </div>

      <!-- 侧边栏 -->
      <aside class="sidebar-section">
        <!-- 执行结果卡片 -->
        <div class="sidebar-card result-card">
          <h3 class="card-title">执行结果</h3>
          <div class="result-content">
            <div :class="['result-status', submission.status]">
              <span class="result-icon">{{ getStatusIcon(submission.status) }}</span>
              <span class="result-text">{{ submissionStore.getStatusText(submission.status) }}</span>
            </div>
          </div>
        </div>

        <!-- 性能指标卡片 -->
        <div class="sidebar-card metrics-card">
          <h3 class="card-title">性能指标</h3>
          <div class="metrics-list">
            <div class="metric-item">
              <div class="metric-icon time-icon">⏱</div>
              <div class="metric-content">
                <div class="metric-label">执行时间</div>
                <div class="metric-value-large">{{ submission.executionTime }}ms</div>
              </div>
            </div>
            <div class="metric-item">
              <div class="metric-icon memory-icon">💾</div>
              <div class="metric-content">
                <div class="metric-label">内存消耗</div>
                <div class="metric-value-large">{{ submissionStore.formatMemory(submission.memory) }}</div>
              </div>
            </div>
          </div>
        </div>

        <!-- 快捷操作卡片 -->
        <div class="sidebar-card actions-card">
          <h3 class="card-title">快捷操作</h3>
          <div class="actions-list">
            <router-link
              :to="`/problemPage/${submission.problemId}`"
              class="action-item"
            >
              <span class="action-icon">🎯</span>
              <span class="action-text">查看题目</span>
            </router-link>
            <button class="action-item" @click="copyCode">
              <span class="action-icon">📋</span>
              <span class="action-text">复制代码</span>
            </button>
            <button class="action-item" @click="goBack">
              <span class="action-icon">📝</span>
              <span class="action-text">返回列表</span>
            </button>
          </div>
        </div>
      </aside>
    </div>
  </div>
</template>

<style scoped>
.submission-detail-view {
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
  position: relative;
}

.back-btn {
  position: absolute;
  left: 0;
  top: 0;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: rgba(255, 255, 255, 0.2);
  color: white;
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: var(--radius-md);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.back-btn:hover {
  background: rgba(255, 255, 255, 0.3);
  transform: translateX(-4px);
}

.back-icon {
  font-size: 18px;
  font-weight: 700;
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

/* 状态样式 */
.loading-state,
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  gap: 16px;
  max-width: 600px;
  margin: 0 auto;
}

.loading-spinner {
  width: 48px;
  height: 48px;
  border: 4px solid var(--color-border);
  border-top: 4px solid var(--color-primary);
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.loading-text {
  color: var(--color-text-secondary);
  font-size: 16px;
}

.empty-icon {
  font-size: 80px;
  opacity: 0.5;
}

.empty-text {
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text);
}

.empty-hint {
  font-size: 14px;
  color: var(--color-text-tertiary);
  text-align: center;
}

.go-back-btn {
  margin-top: 16px;
  padding: 12px 24px;
  background: var(--color-primary);
  color: white;
  border: none;
  border-radius: var(--radius-md);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.go-back-btn:hover {
  background: #5b7fc9;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(79, 109, 188, 0.3);
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
    grid-template-columns: 1fr 340px;
  }
}

/* 主要内容区域 */
.main-section {
  display: flex;
  flex-direction: column;
  gap: 20px;
  min-width: 0;
}

/* 信息卡片 */
.info-card {
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  padding: 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  border: 1px solid var(--color-border);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 2px solid var(--color-border);
}

.card-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 20px;
  font-weight: 600;
  color: var(--color-text);
  margin: 0;
}

.title-icon {
  font-size: 24px;
}

.view-problem-btn {
  padding: 8px 16px;
  background: var(--color-primary);
  color: white;
  text-decoration: none;
  border-radius: var(--radius-md);
  font-size: 13px;
  font-weight: 500;
  transition: all 0.2s ease;
}

.view-problem-btn:hover {
  background: #5b7fc9;
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(79, 109, 188, 0.3);
}

/* 信息网格 */
.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 20px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.info-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--color-text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.info-value {
  font-size: 15px;
  color: var(--color-text);
  font-weight: 500;
}

.user-value {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 2px solid var(--color-border);
}

.username {
  font-weight: 600;
  color: var(--color-text);
}

/* 状态徽章 */
.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  border-radius: var(--radius-md);
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
}

.status-icon {
  font-size: 16px;
}

.status-badge.accepted {
  background: rgba(16, 185, 129, 0.15);
  color: #10b981;
}

.status-badge.wrong_answer {
  background: rgba(239, 68, 68, 0.15);
  color: #ef4444;
}

.status-badge.time_limit {
  background: rgba(245, 158, 11, 0.15);
  color: #f59e0b;
}

.status-badge.memory_limit {
  background: rgba(168, 85, 247, 0.15);
  color: #a855f7;
}

.status-badge.runtime_error {
  background: rgba(239, 68, 68, 0.15);
  color: #ef4444;
}

.status-badge.compile_error {
  background: rgba(249, 115, 22, 0.15);
  color: #f97316;
}

.status-badge.non_zero_exit {
  background: rgba(234, 88, 12, 0.15);
  color: #ea580c;
}

.status-badge.pending {
  background: rgba(107, 114, 128, 0.15);
  color: #6b7280;
}

.status-badge.judging {
  background: rgba(59, 130, 246, 0.15);
  color: #3b82f6;
}

/* 语言徽章 */
.language-badge {
  display: inline-flex;
  padding: 6px 12px;
  background: var(--color-bg);
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-sm);
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text);
}

/* 指标值 */
.metric-value {
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 15px;
  color: var(--color-primary);
  font-weight: 600;
}

/* 代码卡片 */
.code-card {
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  border: 1px solid var(--color-border);
  overflow: hidden;
}

.code-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: var(--color-bg);
  border-bottom: 2px solid var(--color-border);
}

.code-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.code-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text);
  margin: 0;
}

.code-icon {
  font-size: 20px;
}

.language-tag {
  padding: 4px 10px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  font-size: 12px;
  font-weight: 600;
  color: var(--color-text-secondary);
}

.copy-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  background: var(--color-primary);
  color: white;
  border: none;
  border-radius: var(--radius-md);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.copy-btn:hover {
  background: #5b7fc9;
  transform: translateY(-1px);
  box-shadow: 0 2px 6px rgba(79, 109, 188, 0.3);
}

.copy-icon {
  font-size: 14px;
}

/* 代码容器 */
.code-wrapper {
  background: #2d2d2d;
  max-height: calc(100vh - 400px);
  overflow: auto;
}

.code-pre {
  margin: 0 !important;
  padding: 20px !important;
  background: #2d2d2d !important;
  border-radius: 0 !important;
  font-size: 14px;
  line-height: 1.6;
}

.code-pre code {
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  background: transparent !important;
}

/* 侧边栏 */
.sidebar-section {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.sidebar-card {
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  border: 1px solid var(--color-border);
}

.sidebar-card .card-title {
  margin: 0 0 16px 0;
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text);
  padding-bottom: 12px;
  border-bottom: 1px solid var(--color-border);
}

/* 执行结果卡片 */
.result-content {
  padding: 8px 0;
}

.result-status {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 16px;
  border-radius: var(--radius-md);
  font-size: 16px;
  font-weight: 700;
}

.result-icon {
  font-size: 24px;
}

.result-status.accepted {
  background: rgba(16, 185, 129, 0.15);
  color: #10b981;
}

.result-status.wrong_answer {
  background: rgba(239, 68, 68, 0.15);
  color: #ef4444;
}

.result-status.time_limit {
  background: rgba(245, 158, 11, 0.15);
  color: #f59e0b;
}

.result-status.memory_limit {
  background: rgba(168, 85, 247, 0.15);
  color: #a855f7;
}

.result-status.runtime_error {
  background: rgba(239, 68, 68, 0.15);
  color: #ef4444;
}

.result-status.compile_error {
  background: rgba(249, 115, 22, 0.15);
  color: #f97316;
}

.result-status.non_zero_exit {
  background: rgba(234, 88, 12, 0.15);
  color: #ea580c;
}

.result-status.pending {
  background: rgba(107, 114, 128, 0.15);
  color: #6b7280;
}

.result-status.judging {
  background: rgba(59, 130, 246, 0.15);
  color: #3b82f6;
}

/* 性能指标卡片 */
.metrics-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.metric-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: var(--color-bg);
  border-radius: var(--radius-md);
}

.metric-icon {
  font-size: 32px;
  flex-shrink: 0;
}

.metric-content {
  flex: 1;
}

.metric-label {
  font-size: 12px;
  color: var(--color-text-secondary);
  margin-bottom: 4px;
}

.metric-value-large {
  font-size: 20px;
  font-weight: 700;
  color: var(--color-primary);
  font-family: 'Consolas', 'Monaco', monospace;
}

/* 快捷操作卡片 */
.actions-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.action-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 14px;
  color: var(--color-text);
  text-decoration: none;
  cursor: pointer;
  transition: all 0.2s ease;
}

.action-item:hover {
  background: var(--color-surface);
  border-color: var(--color-primary);
  color: var(--color-primary);
  transform: translateX(4px);
}

.action-icon {
  font-size: 18px;
}

.action-text {
  font-weight: 500;
}

/* 移动端优化 */
@media (max-width: 768px) {
  .page-header {
    padding: 24px 16px;
    margin-bottom: 16px;
  }

  .back-btn {
    position: relative;
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

  .info-card,
  .code-card,
  .sidebar-card {
    padding: 16px;
  }

  .card-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .view-problem-btn {
    width: 100%;
    text-align: center;
  }

  .info-grid {
    grid-template-columns: 1fr;
    gap: 16px;
  }

  .code-header {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
  }

  .code-header-left {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }

  .copy-btn {
    width: 100%;
    justify-content: center;
  }

  .code-wrapper {
    max-height: 400px;
  }
}
</style>
