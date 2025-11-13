<template>
  <div class="wa-container">
    <div class="header">
      <h2>近期提交未过的题目</h2>
      <span class="badge">{{ waList.length }}</span>
    </div>

    <div class="scroll-container">
      <div v-if="loading" class="loading-state">
        <div class="loading-spinner"></div>
        <span>加载中...</span>
      </div>
      <ul v-else class="wa-list">
        <li
          v-for="(item, index) in waList"
          :key="index"
          class="wa-item"
          @click="viewProblem(item.id)"
        >
          <div class="problem-id">{{ item.id }}</div>
          <div class="problem-title">{{ item.title }}</div>
          <div class="attempt-count">尝试: {{ item.attempts }}次</div>
        </li>
      </ul>
    </div>

    <div class="footer" v-if="!loading && waList.length === 0">
      暂时没有WA记录，继续保持！
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { WrongCommit } from '@/api/UserApi.js'
import { useUserStore } from '@/stores/userStore.js'

const waList = ref([])
const loading = ref(true)
const userStore = useUserStore()

// 获取错误提交数据
async function fetchWrongCommits() {
  try {
    loading.value = true
    const uuid = userStore.user?.uuid
    if (!uuid) {
      console.warn('用户未登录，无法获取错误提交数据')
      return
    }

    const response = await WrongCommit(uuid)

    // 检查响应数据结构
    let apiData = response.data || response
    if (!Array.isArray(apiData)) {
      apiData = []
    }

    // 转换API数据格式
    waList.value = apiData.map(item => ({
      id: item.problemId,
      title: item.problemName,
      attempts: item.tryCount
    }))

  } catch (error) {
    console.error('获取错误提交数据失败:', error)
    // 使用默认数据作为后备
    waList.value = []
  } finally {
    loading.value = false
  }
}

function viewProblem(id) {
  // 实际应跳转到问题详情页
  console.log('查看问题:', id);
}

onMounted(() => {
  fetchWrongCommits()
})
</script>

<style scoped>
.wa-container {
  background: transparent;
  overflow: hidden;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 0 16px 0;
  border-bottom: 1px solid var(--color-border);
  flex-shrink: 0;
}

.header h2 {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text);
  letter-spacing: -0.2px;
}

.badge {
  background: var(--color-error);
  color: white;
  padding: 3px 8px;
  border-radius: var(--radius-full);
  font-size: 11px;
  font-weight: 600;
  min-width: 20px;
  text-align: center;
}

.scroll-container {
  flex: 1;
  overflow-y: auto;
  min-height: 0;
}

.scroll-container::-webkit-scrollbar {
  width: 4px;
}

.scroll-container::-webkit-scrollbar-track {
  background: transparent;
}

.scroll-container::-webkit-scrollbar-thumb {
  background: var(--color-border);
  border-radius: 2px;
}

.scroll-container::-webkit-scrollbar-thumb:hover {
  background: var(--color-text-tertiary);
}

.wa-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.wa-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 12px 0;
  cursor: pointer;
  transition: all 0.2s ease;
  border-bottom: 1px solid var(--color-border);
}

.wa-item:hover {
  background: rgba(51, 65, 85, 0.04);
  padding: 12px 0 12px 8px;
  margin: 0 -8px 0 0;
}

.wa-item:last-child {
  border-bottom: none;
}

.problem-id {
  font-weight: 600;
  color: var(--color-primary);
  font-size: 13px;
  font-family: 'Courier New', monospace;
}

.problem-title {
  color: var(--color-text);
  font-size: 13px;
  font-weight: 500;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
}

.attempt-count {
  color: var(--color-error);
  font-size: 11px;
  font-weight: 500;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.attempt-count::before {
  content: '🔄';
  font-size: 10px;
}

.footer {
  padding: 24px 0;
  text-align: center;
  color: var(--color-text-tertiary);
  font-size: 13px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.footer::before {
  content: '🎉';
  font-size: 32px;
  opacity: 0.6;
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 0;
  color: var(--color-text-tertiary);
  gap: 12px;
}

.loading-spinner {
  width: 28px;
  height: 28px;
  border: 3px solid var(--color-border);
  border-top-color: var(--color-error);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
