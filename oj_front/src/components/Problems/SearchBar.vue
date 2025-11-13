<template>
  <div class="search-container">
    <TagFilter ref="tagFilterRef" />

    <div class="search-bar">
      <input
        type="text"
        v-model="searchQuery"
        placeholder="搜索题目..."
        @keyup.enter="doSearch"
      >
      <button @click="doSearch" type="button">搜索</button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import TagFilter from './TagFilter.vue'
import { useProblemStore } from '@/stores/problemStore'

const problemStore = useProblemStore()
const searchQuery = ref('')
const tagFilterRef = ref(null)

function doSearch() {
  // 获取当前选中的过滤器
  const selectedFilters = tagFilterRef.value.getSelectedFilters()

  // 构建搜索参数，完全匹配 SearchQuestionVo 结构
  const searchParams = {
    platform: selectedFilters.platforms && selectedFilters.platforms.length > 0
      ? selectedFilters.platforms[0]  // 使用第一个选中的平台
      : '',
    difficulty: selectedFilters.difficulties && selectedFilters.difficulties.length > 0
      ? selectedFilters.difficulties[0]
      : '',
    resource: searchQuery.value || '',
    tags: selectedFilters.tags || []
  }

  console.log('发送搜索参数:', searchParams)
  problemStore.searchProblems(searchParams)
}
</script>

<style scoped>
.search-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
  width: 100%;
}

@media (min-width: 768px) {
  .search-container {
    flex-direction: row;
    align-items: stretch;
  }
}

.search-bar {
  flex: 1;
  display: flex;
  position: relative;
  min-width: 0;
}

.search-bar input {
  flex: 1;
  padding: 12px 16px;
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 14px;
  outline: none;
  transition: all 0.2s ease;
  background: var(--color-surface);
  color: var(--color-text);
  border-right: none;
  border-radius: var(--radius-md) 0 0 var(--radius-md);
}

.search-bar input:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(51, 65, 85, 0.1);
}

.search-bar input::placeholder {
  color: var(--color-text-tertiary);
}

.search-bar button {
  padding: 12px 24px;
  background: var(--color-primary);
  color: white;
  border: none;
  border-radius: 0 var(--radius-md) var(--radius-md) 0;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s ease;
  white-space: nowrap;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.search-bar button:hover {
  background: #2d3f5f;
  transform: translateY(-1px);
  box-shadow: 0 4px 8px rgba(51, 65, 85, 0.2);
}

.search-bar button:active {
  transform: translateY(0);
}

.search-bar button::before {
  content: '🔍';
  font-size: 14px;
}
</style>
