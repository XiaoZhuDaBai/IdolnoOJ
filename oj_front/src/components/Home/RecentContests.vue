<template>
  <div class="section contest-container">
    <div class="contest-header">
      <h2>近期比赛</h2>
      <router-link to="/contests" class="view-all">查看全部</router-link>
    </div>

    <div v-if="contestStore.loading" class="loading">
      <div class="loading-spinner"></div>
      <span>加载中...</span>
    </div>

    <div v-else-if="contestStore.error" class="error">
      <div class="error-icon">!</div>
      <div class="error-message">{{ contestStore.error }}</div>
    </div>

    <div v-else class="contest-list">
      <div
        v-for="contest in recentContests"
        :key="contest.id"
        class="contest-item"
        :class="{
          'active': contest.status === 'running',
          'upcoming': contest.status === 'upcoming',
          'ended': contest.status === 'ended'
        }"
      >
        <div class="contest-time">
          <div class="contest-date">{{ contestStore.formatDate(contest.startTime) }}</div>
          <div class="contest-duration">{{ contest.duration.toFixed(1) }}小时</div>
          <div v-if="contest.status === 'upcoming'" class="countdown">
            {{ contestStore.formatCountdown(contest.startTime) }}
          </div>
        </div>
        <div class="contest-info">
          <a
            :href="contest.link"
            target="_blank"
            class="contest-name"
          >
            <span>{{ contest.name }}</span>
            <span class="contest-oj-tag">{{ contest.oj }}</span>
          </a>
          <div class="contest-meta">
            <span class="contest-status" :class="contest.status">
              {{ contestStatusText[contest.status] }}
            </span>
            <span class="contest-phase" v-if="contest.phase">{{ contest.phase }}</span>
          </div>
        </div>
        <div class="contest-action">
          <a
            v-if="contest.status === 'upcoming'"
            :href="contest.link"
            target="_blank"
            class="btn register"
          >
            立即查看
          </a>
          <a
            v-else-if="contest.status === 'running'"
            :href="contest.link"
            target="_blank"
            class="btn enter"
          >
            进入比赛
          </a>
          <div v-else class="contest-ended">已结束</div>
        </div>
      </div>
    </div>

    <div v-if="!contestStore.loading && !contestStore.error && recentContests.length === 0" class="no-contests">
      <div class="no-contests-icon">📅</div>
      <div class="no-contests-text">当前没有近期比赛</div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useContestStore } from '@/stores/contestStore'

const contestStore = useContestStore()

const contestStatusText = {
  upcoming: '未开始',
  running: '进行中',
  ended: '已结束'
}

// 获取最近的5条比赛数据
const recentContests = computed(() => {
  return contestStore.contests.slice(0, 5)
})

onMounted(async () => {
  if (contestStore.contests.length === 0) {
    await contestStore.fetchContests()
  }
})
</script>

<style scoped>
.section {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.contest-container {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.contest-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-lg);
}

.view-all {
  font-size: 13px;
  color: var(--color-primary);
  text-decoration: none;
  font-weight: 500;
  transition: opacity 0.2s;
}

.view-all:hover {
  opacity: 0.7;
}

.loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--spacing-md);
  padding: var(--spacing-xl);
  color: var(--color-text-secondary);
  font-size: 13px;
}

.loading-spinner {
  width: 24px;
  height: 24px;
  border: 2px solid var(--color-border);
  border-top: 2px solid var(--color-primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.error {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  padding: var(--spacing-md);
  background: rgba(239, 68, 68, 0.05);
  border: 1px solid rgba(239, 68, 68, 0.2);
  border-radius: var(--radius-md);
  color: var(--color-error);
  font-size: 13px;
}

.error-icon {
  width: 18px;
  height: 18px;
  background: var(--color-error);
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  font-size: 12px;
  flex-shrink: 0;
}

.contest-list {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.contest-item {
  display: flex;
  align-items: center;
  padding: var(--spacing-md);
  border-radius: var(--radius-md);
  background-color: var(--color-bg);
  margin-bottom: 0;
  transition: all 0.2s;
  border: 1px solid transparent;
  flex-shrink: 0;
}

.contest-item:hover {
  background-color: var(--color-surface);
  border-color: var(--color-border);
}

.contest-time {
  min-width: 80px;
  text-align: center;
  margin-right: var(--spacing-md);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--spacing-xs);
}

.contest-date {
  font-weight: 600;
  color: var(--color-text);
  font-size: 13px;
}

.contest-duration {
  font-size: 11px;
  color: var(--color-text-tertiary);
}

.countdown {
  font-size: 11px;
  color: var(--color-error);
  font-weight: 500;
}

.contest-info {
  flex: 1;
  min-width: 0;
}

.contest-name {
  font-weight: 500;
  margin-bottom: var(--spacing-xs);
  color: var(--color-text);
  text-decoration: none;
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  transition: color 0.2s;
  font-size: 14px;
  min-width: 0;
}

.contest-name > span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
  min-width: 0;
}

.contest-name:hover {
  color: var(--color-primary);
}

.contest-oj-tag {
  font-size: 10px;
  padding: 2px 6px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  color: var(--color-text-secondary);
  font-weight: 500;
  flex-shrink: 0;
}

.contest-meta {
  display: flex;
  align-items: center;
  font-size: 11px;
  color: var(--color-text-tertiary);
  gap: var(--spacing-sm);
}

.contest-status {
  padding: 2px 8px;
  border-radius: var(--radius-sm);
  font-weight: 500;
}

.contest-status.upcoming {
  background-color: rgba(245, 158, 11, 0.1);
  color: var(--color-warning);
}

.contest-status.running {
  background-color: rgba(16, 185, 129, 0.1);
  color: var(--color-success);
}

.contest-status.ended {
  background-color: var(--color-bg);
  color: var(--color-text-tertiary);
}

.contest-phase {
  font-size: 10px;
  color: var(--color-text-tertiary);
  background: var(--color-bg);
  padding: 2px 6px;
  border-radius: var(--radius-sm);
}

.contest-action {
  flex-shrink: 0;
  width: 80px;
  text-align: right;
}

.btn {
  border: none;
  padding: 6px 12px;
  border-radius: var(--radius-sm);
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  text-decoration: none;
  display: inline-block;
}

.btn.register {
  background-color: var(--color-primary);
  color: white;
}

.btn.register:hover {
  opacity: 0.9;
}

.btn.enter {
  background-color: var(--color-success);
  color: white;
}

.btn.enter:hover {
  opacity: 0.9;
}

.contest-ended {
  color: var(--color-text-tertiary);
  font-size: 12px;
}

.no-contests {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--spacing-sm);
  padding: var(--spacing-xl);
  background: var(--color-bg);
  border-radius: var(--radius-md);
  color: var(--color-text-tertiary);
}

.no-contests-icon {
  font-size: 32px;
  opacity: 0.5;
}

.no-contests-text {
  font-size: 13px;
}
</style>
