<template>
  <div class="problems-view">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-content">
        <h1 class="page-title">题库</h1>
        <p class="page-subtitle">探索丰富的算法题目，提升编程能力</p>
      </div>
    </div>

    <!-- 主要内容区域 -->
    <div class="content-wrapper">
      <!-- 左侧主要内容 -->
      <div class="main-section">
        <!-- 搜索和筛选区域 -->
        <div class="search-section">
          <SearchBar />
        </div>

        <!-- 题目表格 -->
        <div class="table-section">
          <ProblemTable />
        </div>
      </div>

      <!-- 右侧边栏 -->
      <aside class="sidebar-section">
        <!-- 未通过题目 -->
        <div class="sidebar-card">
          <RecentWA />
        </div>

        <!-- 题目来源 -->
        <div class="sidebar-card">
          <TestPlatform />
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup>
import SearchBar from '@/components/Problems/SearchBar.vue'
import ProblemTable from '@/components/Problems/ProblemTable.vue'
import RecentWA from '@/components/Problems/RecentWA.vue'
import TestPlatform from '@/components/Home/TestPlatform.vue'
</script>

<style scoped>
.problems-view {
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
  min-width: 0; /* 防止 flex 子项溢出 */
}

/* 搜索区域 */
.search-section {
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  border: 1px solid var(--color-border);
}

/* 表格区域 */
.table-section {
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  border: 1px solid var(--color-border);
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
  transition: box-shadow 0.2s ease;
}

.sidebar-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
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

  .search-section,
  .table-section,
  .sidebar-card {
    padding: 16px;
  }

  .sidebar-section {
    gap: 16px;
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
