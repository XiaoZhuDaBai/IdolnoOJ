<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useContestStore } from '@/stores/contestStore'

const contestStore = useContestStore()

const contestStatusText = {
  upcoming: '未开始',
  running: '进行中',
  ended: '已结束'
}

const statusFilter = ref('all') // all, upcoming, running, ended

const filteredContests = computed(() => {
  if (statusFilter.value === 'all') {
    return contestStore.contests
  }
  return contestStore.contests.filter(c => c.status === statusFilter.value)
})

const statusCounts = computed(() => {
  const counts = { all: 0, upcoming: 0, running: 0, ended: 0 }
  contestStore.contests.forEach(c => {
    counts.all++
    counts[c.status]++
  })
  return counts
})

let timer = null

onMounted(async () => {
  await contestStore.fetchContests()
  // 每分钟更新一次比赛状态和倒计时
  timer = setInterval(() => {
    contestStore.updateContestStatus()
  }, 60000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<template>
  <div class="contests-view">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-content">
        <h1 class="page-title">比赛列表</h1>
        <p class="page-subtitle">参与各大平台的编程竞赛，提升实战能力</p>
      </div>
    </div>

    <!-- 主要内容区域 -->
    <div class="content-wrapper">
      <!-- 主要内容区 -->
      <div class="main-section">
        <!-- 筛选器卡片 -->
        <div class="filter-card">
          <div class="filter-tabs">
            <button
              v-for="status in ['all', 'upcoming', 'running', 'ended']"
              :key="status"
              :class="['filter-tab', { active: statusFilter === status }]"
              @click="statusFilter = status"
            >
              <span class="tab-label">
                {{ status === 'all' ? '全部' : contestStatusText[status] }}
              </span>
              <span class="tab-count">{{ statusCounts[status] }}</span>
            </button>
          </div>

          <div v-if="contestStore.getLastUpdateTime()" class="update-time">
            <span class="update-icon">🔄</span>
            最后更新: {{ contestStore.getLastUpdateTime() }}
          </div>
        </div>

        <!-- 比赛列表卡片 -->
        <div class="contests-card">
          <!-- 加载状态 -->
          <div v-if="contestStore.loading" class="loading-state">
            <div class="loading-spinner"></div>
            <span class="loading-text">加载中...</span>
          </div>

          <!-- 错误状态 -->
          <div v-else-if="contestStore.error" class="error-state">
            <div class="error-icon">⚠️</div>
            <div class="error-message">{{ contestStore.error }}</div>
            <button class="retry-btn" @click="contestStore.fetchContests()">
              重试
            </button>
          </div>

          <!-- 空状态 -->
          <div v-else-if="filteredContests.length === 0" class="empty-state">
            <div class="empty-icon">📅</div>
            <div class="empty-text">{{ statusFilter === 'all' ? '当前没有比赛' : '没有符合条件的比赛' }}</div>
          </div>

          <!-- 比赛列表 -->
          <div v-else class="contests-list">
            <div
              v-for="contest in filteredContests"
              :key="contest.id"
              class="contest-item"
            >
              <div class="contest-header">
                <div class="contest-time-info">
                  <div class="contest-date">
                    <span class="date-icon">📅</span>
                    {{ contestStore.formatDate(contest.startTime) }}
                  </div>
                  <div class="contest-duration">
                    <span class="duration-icon">⏱️</span>
                    {{ contest.duration.toFixed(1) }}小时
                  </div>
                </div>
                <div class="contest-status-badge" :class="contest.status">
                  {{ contestStatusText[contest.status] }}
                </div>
              </div>

              <div class="contest-content">
                <a
                  :href="contest.link"
                  target="_blank"
                  class="contest-name"
                  rel="noopener noreferrer"
                >
                  {{ contest.name }}
                </a>

                <div class="contest-meta">
                  <span class="contest-oj-tag" :data-oj="contest.oj">
                    {{ contest.oj }}
                  </span>
                  <span
                    v-if="contest.phase"
                    class="contest-phase-tag"
                    :data-phase="contest.phase"
                  >
                    {{ contest.phase }}
                  </span>
                </div>

                <div v-if="contest.status === 'upcoming'" class="countdown-wrapper">
                  <span class="countdown-icon">⏰</span>
                  <span class="countdown-text">
                    {{ contestStore.formatCountdown(contest.startTime) }}
                  </span>
                </div>
              </div>

              <div class="contest-action">
                <a
                  v-if="contest.status === 'upcoming'"
                  :href="contest.link"
                  target="_blank"
                  class="action-btn upcoming"
                  rel="noopener noreferrer"
                >
                  <span class="btn-icon">👀</span>
                  立即查看
                </a>
                <a
                  v-else-if="contest.status === 'running'"
                  :href="contest.link"
                  target="_blank"
                  class="action-btn running"
                  rel="noopener noreferrer"
                >
                  <span class="btn-icon">🚀</span>
                  进入比赛
                </a>
                <div v-else class="action-ended">
                  <span class="ended-icon">✓</span>
                  已结束
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧边栏 -->
      <aside class="sidebar-section">
        <!-- 比赛统计卡片 -->
        <div class="sidebar-card stats-card">
          <h3 class="card-title">比赛统计</h3>
          <div class="stats-grid">
            <div class="stat-item">
              <div class="stat-value">{{ statusCounts.all }}</div>
              <div class="stat-label">总计</div>
            </div>
            <div class="stat-item upcoming">
              <div class="stat-value">{{ statusCounts.upcoming }}</div>
              <div class="stat-label">未开始</div>
            </div>
            <div class="stat-item running">
              <div class="stat-value">{{ statusCounts.running }}</div>
              <div class="stat-label">进行中</div>
            </div>
            <div class="stat-item ended">
              <div class="stat-value">{{ statusCounts.ended }}</div>
              <div class="stat-label">已结束</div>
            </div>
          </div>
        </div>

        <!-- 比赛提示卡片 -->
        <div class="sidebar-card tips-card">
          <h3 class="card-title">参赛提示</h3>
          <div class="tips-list">
            <div class="tip-item">
              <span class="tip-icon">💡</span>
              <span class="tip-text">提前熟悉比赛平台规则</span>
            </div>
            <div class="tip-item">
              <span class="tip-icon">⏰</span>
              <span class="tip-text">注意比赛开始时间</span>
            </div>
            <div class="tip-item">
              <span class="tip-icon">📝</span>
              <span class="tip-text">准备好常用代码模板</span>
            </div>
            <div class="tip-item">
              <span class="tip-icon">🎯</span>
              <span class="tip-text">先易后难，稳扎稳打</span>
            </div>
          </div>
        </div>
      </aside>
    </div>
  </div>
</template>

<style scoped>
.contests-view {
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

/* 筛选器卡片 */
.filter-card {
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  border: 1px solid var(--color-border);
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
}

.filter-tabs {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.filter-tab {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: transparent;
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: all 0.2s ease;
}

.filter-tab:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
  background: rgba(79, 109, 188, 0.05);
}

.filter-tab.active {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: white;
}

.tab-label {
  font-weight: 500;
}

.tab-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 20px;
  height: 20px;
  padding: 0 6px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 10px;
  font-size: 12px;
  font-weight: 600;
}

.filter-tab.active .tab-count {
  background: rgba(255, 255, 255, 0.25);
}

.filter-tab:not(.active) .tab-count {
  background: var(--color-border);
  color: var(--color-text-tertiary);
}

.update-time {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--color-text-tertiary);
}

.update-icon {
  font-size: 12px;
}

/* 比赛列表卡片 */
.contests-card {
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  border: 1px solid var(--color-border);
}

/* 状态样式 */
.loading-state,
.error-state,
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
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

.error-icon {
  font-size: 48px;
}

.error-message {
  color: var(--color-danger);
  font-size: 14px;
  text-align: center;
}

.retry-btn {
  margin-top: 8px;
  padding: 8px 20px;
  background: var(--color-primary);
  color: white;
  border: none;
  border-radius: var(--radius-md);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.retry-btn:hover {
  background: #5b7fc9;
  transform: translateY(-1px);
}

.empty-icon {
  font-size: 64px;
  opacity: 0.5;
}

.empty-text {
  color: var(--color-text-tertiary);
  font-size: 14px;
}

/* 比赛列表 */
.contests-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.contest-item {
  padding: 20px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-bg);
  transition: all 0.2s ease;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.contest-item:hover {
  border-color: var(--color-primary);
  box-shadow: 0 4px 12px rgba(79, 109, 188, 0.15);
  transform: translateY(-2px);
}

.contest-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--color-border);
}

.contest-time-info {
  display: flex;
  gap: 20px;
  align-items: center;
}

.contest-date,
.contest-duration {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--color-text-secondary);
}

.date-icon,
.duration-icon {
  font-size: 14px;
}

.contest-status-badge {
  padding: 6px 12px;
  border-radius: var(--radius-sm);
  font-size: 12px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.contest-status-badge.upcoming {
  background: rgba(245, 158, 11, 0.1);
  color: #f59e0b;
}

.contest-status-badge.running {
  background: rgba(16, 185, 129, 0.1);
  color: #10b981;
}

.contest-status-badge.ended {
  background: rgba(107, 114, 128, 0.1);
  color: #6b7280;
}

.contest-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
  flex: 1;
}

.contest-name {
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text);
  text-decoration: none;
  transition: color 0.2s ease;
  line-height: 1.4;
}

.contest-name:hover {
  color: var(--color-primary);
}

.contest-meta {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.contest-oj-tag,
.contest-phase-tag {
  padding: 4px 10px;
  border-radius: var(--radius-sm);
  font-size: 12px;
  font-weight: 500;
}

.contest-oj-tag {
  background: var(--color-border);
  color: var(--color-text-secondary);
}

.contest-oj-tag[data-oj="Codeforces"] {
  background: #1f8acb;
  color: white;
}

.contest-oj-tag[data-oj="AtCoder"] {
  background: #000;
  color: white;
}

.contest-oj-tag[data-oj="LeetCode"] {
  background: #ffa116;
  color: white;
}

.contest-phase-tag {
  background: var(--color-surface);
  color: var(--color-text-secondary);
  border: 1px solid var(--color-border);
}

.contest-phase-tag[data-phase="初级"] {
  background: rgba(59, 130, 246, 0.1);
  color: #3b82f6;
  border-color: #3b82f6;
}

.contest-phase-tag[data-phase="常规"] {
  background: rgba(239, 68, 68, 0.1);
  color: #ef4444;
  border-color: #ef4444;
}

.contest-phase-tag[data-phase="高级"] {
  background: rgba(16, 185, 129, 0.1);
  color: #10b981;
  border-color: #10b981;
}

.contest-phase-tag[data-phase="启发式算法"] {
  background: rgba(168, 85, 247, 0.1);
  color: #a855f7;
  border-color: #a855f7;
}

.countdown-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: rgba(239, 68, 68, 0.05);
  border-radius: var(--radius-sm);
  border: 1px solid rgba(239, 68, 68, 0.2);
}

.countdown-icon {
  font-size: 16px;
}

.countdown-text {
  font-size: 13px;
  font-weight: 600;
  color: #ef4444;
}

.contest-action {
  display: flex;
  justify-content: flex-end;
}

.action-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  border-radius: var(--radius-md);
  font-size: 14px;
  font-weight: 500;
  text-decoration: none;
  transition: all 0.2s ease;
  cursor: pointer;
}

.action-btn.upcoming {
  background: var(--color-primary);
  color: white;
}

.action-btn.upcoming:hover {
  background: #5b7fc9;
  transform: translateY(-1px);
  box-shadow: 0 4px 8px rgba(79, 109, 188, 0.3);
}

.action-btn.running {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  color: white;
}

.action-btn.running:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 8px rgba(16, 185, 129, 0.3);
}

.btn-icon {
  font-size: 16px;
}

.action-ended {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 20px;
  color: var(--color-text-tertiary);
  font-size: 14px;
}

.ended-icon {
  font-size: 14px;
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

.card-title {
  margin: 0 0 16px 0;
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text);
  letter-spacing: -0.2px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--color-border);
}

/* 统计卡片 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.stat-item {
  padding: 16px;
  background: var(--color-bg);
  border-radius: var(--radius-md);
  text-align: center;
  border: 1px solid var(--color-border);
  transition: all 0.2s ease;
}

.stat-item:hover {
  border-color: var(--color-primary);
  transform: translateY(-2px);
}

.stat-item.upcoming {
  border-color: rgba(245, 158, 11, 0.3);
}

.stat-item.running {
  border-color: rgba(16, 185, 129, 0.3);
}

.stat-item.ended {
  border-color: rgba(107, 114, 128, 0.3);
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--color-text);
  margin-bottom: 4px;
}

.stat-item.upcoming .stat-value {
  color: #f59e0b;
}

.stat-item.running .stat-value {
  color: #10b981;
}

.stat-item.ended .stat-value {
  color: #6b7280;
}

.stat-label {
  font-size: 12px;
  color: var(--color-text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  font-weight: 500;
}

/* 提示卡片 */
.tips-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.tip-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px;
  background: var(--color-bg);
  border-radius: var(--radius-sm);
  transition: all 0.2s ease;
}

.tip-item:hover {
  background: rgba(79, 109, 188, 0.05);
}

.tip-icon {
  font-size: 16px;
  flex-shrink: 0;
}

.tip-text {
  font-size: 13px;
  color: var(--color-text-secondary);
  line-height: 1.5;
}

/* 移动端优化 */
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
  .contests-card,
  .sidebar-card {
    padding: 16px;
  }

  .filter-card {
    flex-direction: column;
    align-items: stretch;
  }

  .filter-tabs {
    width: 100%;
  }

  .filter-tab {
    flex: 1;
    justify-content: center;
  }

  .contest-item {
    padding: 16px;
  }

  .contest-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .contest-time-info {
    flex-wrap: wrap;
  }

  .contest-name {
    font-size: 16px;
  }

  .stats-grid {
    grid-template-columns: 1fr;
  }
}

/* 平板端优化 */
@media (min-width: 768px) and (max-width: 1023px) {
  .page-header {
    padding: 28px 20px;
  }

  .page-title {
    font-size: 26px;
  }

  .content-wrapper {
    padding: 0 20px 20px;
    gap: 20px;
  }
}
</style>
