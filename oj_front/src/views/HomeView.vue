<template>
  <div class="home-container">
    <!-- 顶部统计区域 -->
    <div class="stats-section">
      <SubmissionAnalysis />
    </div>

    <!-- 内容区域 -->
    <div class="content-grid">
      <div class="main-column">
        <RecentSubmissions />
      </div>
      <div class="side-column">
        <RecentContests />
      </div>
    </div>

    <AiNamedXiaozhu />
  </div>
</template>

<script setup>
import SubmissionAnalysis from '@/components/Home/SubmissionAnalysis.vue'
import RecentSubmissions from '@/components/Home/RecentSubmissions.vue'
import RecentContests from '@/components/Home/RecentContests.vue'
import AiNamedXiaozhu from '@/components/comm/AiNamedXiaozhu.vue'
</script>

<style scoped>
.home-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 32px 24px;
  min-height: calc(100vh - 60px);
}

/* 统计区域 - 全宽 */
.stats-section {
  margin-bottom: 24px;
}

/* 内容网格 */
.content-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 24px;
}

@media (min-width: 992px) {
  .content-grid {
    grid-template-columns: 1.5fr 1fr;
  }
}

.main-column,
.side-column {
  min-width: 0; /* 防止内容溢出 */
}

/* AI助手定位 */
:deep(.ai-assistant) {
  position: fixed !important;
  bottom: 24px;
  right: 24px;
  z-index: 1000;
}

/* 响应式优化 */
@media (max-width: 768px) {
  .home-container {
    padding: 16px 12px;
  }

  .stats-section {
    margin-bottom: 16px;
  }

  .content-grid {
    gap: 16px;
  }
}
</style>

