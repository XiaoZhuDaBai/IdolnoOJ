<template>
  <div class="problems-table-container">
    <!-- 添加加载状态显示 -->
    <div v-if="loading" class="loading-state">
      加载中...
    </div>

    <!-- 添加空状态显示 -->
    <div v-else-if="problems.length === 0" class="empty-state">
      暂无题目
    </div>

    <table class="problems-table">
      <thead>
      <tr>
        <th>题目</th>
        <th>难度</th>
        <th>总数</th>
        <th>提交通过率</th>
      </tr>
      </thead>
      <tbody>
      <tr
        v-for="problem in paginatedProblems"
        :key="problem.id"
        @click="navigateToProblem(problem)"
        class="problem-row"
      >
        <td class="problem-title-cell">
          <span class="problem-title">{{ problem.title }}</span>
        </td>
        <td :class="['difficulty', `difficulty-${problem.difficulty}`]">
          {{ difficultyText[problem.difficulty] }}
        </td>
        <td>{{ problem.total }}</td>
        <td>
          <div class="progress-container">
            <div class="progress-bar">
              <div
                class="progress-value"
                :style="{ width: `${problem.acRate}%` }"
              ></div>
            </div>
            <span>{{ problem.acRate }}%</span>
          </div>
        </td>
      </tr>
      </tbody>
    </table>

    <div class="pagination-container">
      <div class="pagination">
        <button
          @click="prevPage"
          :disabled="currentPage === 1"
          class="page-btn"
        >
          &laquo;
        </button>

        <!-- 第一页 -->
        <button
          v-if="showFirstPage"
          @click="goToPage(1)"
          :class="{ active: currentPage === 1 }"
          class="page-btn"
        >
          1
        </button>

        <!-- 第一页后的省略号 -->
        <span v-if="showFirstEllipsis" class="ellipsis">...</span>

        <!-- 当前页附近的页码 -->
        <button
          v-for="page in displayedPages"
          :key="page"
          @click="goToPage(page)"
          :class="{ active: currentPage === page }"
          class="page-btn"
        >
          {{ page }}
        </button>

        <!-- 最后一页前的省略号 -->
        <span v-if="showLastEllipsis" class="ellipsis">...</span>

        <!-- 最后一页 -->
        <button
          v-if="showLastPage"
          @click="goToPage(totalPages)"
          :class="{ active: currentPage === totalPages }"
          class="page-btn"
        >
          {{ totalPages }}
        </button>

        <button
          @click="nextPage"
          :disabled="currentPage === totalPages"
          class="page-btn"
        >
          &raquo;
        </button>
      </div>
      <div class="page-info">
        每页 {{ itemsPerPage }} 条，共 {{ totalProblems }} 题
      </div>
    </div>
  </div>
</template>

<script setup>
import {computed, watch} from 'vue'
import { useRouter } from 'vue-router'
import { useProblemStore } from "@/stores/problemStore.js";

const router = useRouter()
const problemStore = useProblemStore()

const difficultyText = {
  easy: '简单',
  medium: '中等',
  hard: '困难'
}

// 修改后的导航方法
const navigateToProblem = async (problem) => {
  try {
    // 先获取题目详情
    await problemStore.fetchProblemDetail(problem.id)

    // 使用resolve方法获取完整路径
    const resolved = router.resolve({
      path: `/problemPage/${problem.id}`
    })

    // 构造完整URL
    const fullUrl = new URL(
      resolved.href,
      window.location.origin
    ).href
    // 安全打开新窗口
    window.open(
      fullUrl,
      '_blank',
      'noopener,noreferrer'
    )
  } catch (error) {
    console.error('获取题目详情失败:', error)
  }
}

// 使用 store 中的状态
const loading = computed(() => problemStore.loading)
const problems = computed(() => problemStore.problems)
const totalProblems = computed(() => problemStore.totalProblems)
const currentPage = computed({
  get: () => problemStore.currentPage,
  set: (value) => {
    problemStore.currentPage = value
  }
})

// 分页逻辑
const itemsPerPage = 15
const maxDisplayedPages = 5 // 显示的页码数量

// 计算总页数
const totalPages = computed(() => Math.ceil(totalProblems.value / itemsPerPage))

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

// 直接使用 store 中的问题列表
const paginatedProblems = computed(() => problems.value)

// 监听页码变化
watch(currentPage, (newPage) => {
  console.log('Page changed to:', newPage)
  if (problemStore.searchParams) {
    // 如果有搜索参数，使用搜索
    problemStore.searchProblems(problemStore.searchParams)
  } else {
    // 否则使用普通获取
    problemStore.fetchProblems(newPage)
  }
}, { immediate: true })

function prevPage() {
  if (currentPage.value > 1) currentPage.value--
}

function nextPage() {
  if (currentPage.value < totalPages.value) currentPage.value++
}

function goToPage(page) {
  currentPage.value = page
}
</script>

<style scoped>
.problems-table-container {
  width: 100%;
  overflow-x: auto;
}

.problems-table {
  width: 100%;
  border-collapse: separate;
  border-spacing: 0;
  min-width: 600px;
}

.problems-table th {
  background: var(--color-bg);
  padding: 14px 16px;
  text-align: left;
  font-weight: 600;
  font-size: 13px;
  color: var(--color-text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  border-bottom: 2px solid var(--color-border);
  position: sticky;
  top: 0;
  z-index: 10;
}

.problems-table td {
  padding: 16px;
  border-bottom: 1px solid var(--color-border);
  font-size: 14px;
}

/* 整行点击效果 */
.problem-row {
  cursor: pointer;
  transition: all 0.2s ease;
  background: var(--color-surface);
}

.problem-row:hover {
  background: rgba(51, 65, 85, 0.04);
  transform: translateX(2px);
}

.problem-row:active {
  transform: translateX(0);
}

.problem-title-cell {
  color: var(--color-primary);
  font-weight: 500;
  transition: color 0.2s;
}

.problem-row:hover .problem-title-cell {
  color: #2d3f5f;
}

.problem-title {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
  line-height: 1.5;
}

/* 难度颜色 */
.difficulty {
  display: inline-flex;
  align-items: center;
  padding: 4px 12px;
  border-radius: var(--radius-full);
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.3px;
}

.difficulty-easy {
  background: rgba(34, 197, 94, 0.1);
  color: var(--color-success);
  border: 1px solid rgba(34, 197, 94, 0.2);
}

.difficulty-medium {
  background: rgba(251, 191, 36, 0.1);
  color: var(--color-warning);
  border: 1px solid rgba(251, 191, 36, 0.2);
}

.difficulty-hard {
  background: rgba(239, 68, 68, 0.1);
  color: var(--color-error);
  border: 1px solid rgba(239, 68, 68, 0.2);
}

/* 进度条样式 */
.progress-container {
  display: flex;
  align-items: center;
  gap: 12px;
}

.progress-bar {
  height: 6px;
  background: var(--color-bg);
  border-radius: var(--radius-full);
  overflow: hidden;
  position: relative;
  min-width: 100px;
  flex: 1;
  max-width: 120px;
}

.progress-value {
  height: 100%;
  background: linear-gradient(90deg, var(--color-primary) 0%, #5b7fc9 100%);
  transition: width 0.3s ease;
  border-radius: var(--radius-full);
}

.progress-container > span {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-secondary);
  min-width: 45px;
  text-align: right;
}

/* 分页样式 */
.pagination-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  margin-top: 32px;
  padding-top: 24px;
  border-top: 1px solid var(--color-border);
}

.pagination {
  display: flex;
  gap: 6px;
  align-items: center;
  flex-wrap: wrap;
  justify-content: center;
}

.ellipsis {
  padding: 8px;
  color: var(--color-text-tertiary);
  font-weight: 500;
}

.page-btn {
  padding: 8px 14px;
  border: 1.5px solid var(--color-border);
  background: var(--color-surface);
  color: var(--color-text-secondary);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all 0.2s ease;
  min-width: 40px;
  text-align: center;
  font-weight: 500;
  font-size: 14px;
}

.page-btn:hover:not(:disabled):not(.active) {
  background: rgba(51, 65, 85, 0.06);
  border-color: var(--color-primary);
  color: var(--color-text);
  transform: translateY(-1px);
}

.page-btn.active {
  background: var(--color-primary);
  color: white;
  border-color: var(--color-primary);
  box-shadow: 0 2px 8px rgba(51, 65, 85, 0.2);
}

.page-btn:disabled {
  color: var(--color-text-tertiary);
  cursor: not-allowed;
  opacity: 0.5;
  border-color: var(--color-border);
}

.page-btn:disabled:hover {
  transform: none;
}

.page-info {
  color: var(--color-text-secondary);
  font-size: 13px;
  font-weight: 500;
}

/* 状态样式 */
.loading-state,
.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: var(--color-text-secondary);
  font-size: 14px;
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.loading-state::before {
  content: '';
  width: 32px;
  height: 32px;
  border: 3px solid var(--color-border);
  border-top-color: var(--color-primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.empty-state::before {
  content: '📝';
  font-size: 48px;
  opacity: 0.5;
}

/* 响应式优化 */
@media (max-width: 768px) {
  .problems-table th,
  .problems-table td {
    padding: 12px;
    font-size: 13px;
  }

  .page-btn {
    padding: 6px 10px;
    min-width: 36px;
    font-size: 13px;
  }

  .progress-bar {
    min-width: 60px;
    max-width: 80px;
  }
}
</style>
