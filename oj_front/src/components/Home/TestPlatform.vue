<template>
  <div class="section platforms-container">
    <h2>题目来源</h2>
    <div class="platform-intro">
      本平台支持以下主流在线测评平台题目导入和评测
    </div>
    <div class="platform-list">
      <div
        v-for="platform in platforms"
        :key="platform.id"
        class="platform-item"
        @click="visitPlatform(platform.url)"
      >
        <div class="platform-logo">
          <img :src="platform.logo" :alt="platform.name + 'logo'">
        </div>
        <div class="platform-info">
          <div class="platform-name">{{ platform.name }}</div>
          <div class="platform-desc">{{ platform.description }}</div>
          <div class="platform-stats">
            <span class="stat">
              <i class="stat-icon">📊</i>
              {{ platform.problemCount }}题
            </span>
            <span class="stat">
              <i class="stat-icon">👥</i>
              {{ platform.userCount }}
            </span>
          </div>
        </div>
        <div class="platform-link">
          <i class="external-icon">↗</i>
        </div>
      </div>
    </div>
    <div class="sync-tip">
      <i class="tip-icon">🔄</i>
      题目模式均为<h3>ACM</h3>模式
    </div>
  </div>
</template>

<script setup>
import {ref} from "vue";

const platforms = ref([
  {
    id: 1,
    name: 'LeetCode',
    description: '全球知名的算法题库平台',
    logo: 'https://assets.leetcode.com/static_assets/public/webpack_bundles/images/logo-dark.e99485d9b.svg',
    url: 'https://leetcode.com',
    problemCount: '2000+',
    userCount: '1000万+'
  },
  {
    id: 2,
    name: 'Codeforces',
    description: '竞技编程和算法竞赛平台',
    logo: 'https://codeforces.org/s/0/favicon-96x96.png',
    url: 'https://codeforces.com',
    problemCount: '8000+',
    userCount: '300万+'
  },
  {
    id: 3,
    name: 'AtCoder',
    description: '日本著名编程竞赛平台',
    logo: 'https://img.atcoder.jp/assets/favicon.png',
    url: 'https://atcoder.jp',
    problemCount: '5000+',
    userCount: '500万+'
  },
  {
    id: 4,
    name: 'NowCoder',
    description: '国内知名IT笔试面试平台',
    logo: 'https://www.nowcoder.com/favicon.ico',
    url: 'https://www.nowcoder.com',
    problemCount: '3000+',
    userCount: '600万+'
  }
])

function visitPlatform(url) {
  window.open(url, '_blank')
}
</script>

<style scoped>
.platforms-container {
  padding: 0;
}

.platforms-container h2 {
  margin: 0 0 12px 0;
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text);
  letter-spacing: -0.2px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--color-border);
}

.platform-intro {
  color: var(--color-text-secondary);
  font-size: 13px;
  line-height: 1.5;
  margin: 16px 0;
}

.platform-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.platform-item {
  display: flex;
  align-items: center;
  padding: 12px;
  background: var(--color-bg);
  border-radius: var(--radius-md);
  transition: all 0.2s ease;
  cursor: pointer;
  border: 1px solid var(--color-border);
}

.platform-item:hover {
  border-color: var(--color-primary);
  background: rgba(51, 65, 85, 0.04);
  transform: translateX(2px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.platform-logo {
  width: 36px;
  height: 36px;
  margin-right: 12px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: white;
  border-radius: var(--radius-sm);
  padding: 4px;
  border: 1px solid var(--color-border);
}

.platform-logo img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.platform-info {
  flex: 1;
  min-width: 0;
}

.platform-name {
  font-weight: 600;
  font-size: 13px;
  margin-bottom: 3px;
  color: var(--color-text);
}

.platform-desc {
  font-size: 11px;
  color: var(--color-text-tertiary);
  margin-bottom: 4px;
  line-height: 1.3;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.platform-stats {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.stat {
  font-size: 11px;
  color: var(--color-text-secondary);
  font-weight: 500;
  display: inline-flex;
  align-items: center;
  gap: 3px;
}

.stat-icon {
  font-size: 12px;
}

.platform-link {
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-primary);
  opacity: 0;
  transition: all 0.2s ease;
  font-size: 14px;
  font-weight: bold;
}

.platform-item:hover .platform-link {
  opacity: 1;
  transform: translateX(2px) translateY(-2px);
}

.sync-tip {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid var(--color-border);
  font-size: 11px;
  color: var(--color-text-tertiary);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.sync-tip h3 {
  display: inline;
  margin: 0;
  font-size: 11px;
  font-weight: 600;
  color: var(--color-text-secondary);
}

.tip-icon {
  font-size: 13px;
}
</style>
