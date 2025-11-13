<template>
  <div class="tags-container">
    <button class="tags-toggle" @click="toggleTagsPanel">{{ tagsButtonText }}</button>
    <div class="tags-panel" :class="{ active: showTagsPanel }">
      <div class="tags-header">
        <span class="tags-title">标签</span>
        <button class="reset-btn" @click="resetFilters">重置</button>
      </div>
      <div class="tags-content">
        <div class="filter-section">
        <div class="platform-list">
          <h3>题库</h3>
          <div class="tag-item">
            <input
              type="checkbox"
              id="all-platforms"
              v-model="selectAllPlatforms"
              @change="toggleAllPlatforms"
            >
            <label for="all-platforms">全部</label>
          </div>
          <div class="tag-item" v-for="platform in platforms" :key="platform.id">
            <input
              type="checkbox"
              :id="platform.id"
              v-model="selectedPlatforms"
              :value="platform.id"
              @change="handlePlatformChange"
            >
            <label :for="platform.id">{{ platform.name }}</label>
          </div>
        </div>

        <div class="difficulty-list">
          <h3>难度</h3>
          <div class="tag-item" v-for="difficulty in difficulties" :key="difficulty.id" :class="`difficulty-${difficulty.id}`">
            <div
              class="difficulty-option"
              :class="{ 'selected': selectedDifficulties.includes(difficulty.id) }"
              @click="handleDifficultyClick(difficulty.id)"
            >
              {{ difficulty.name }}
            </div>
          </div>
        </div>
      </div>

        <div class="algorithm-list">
          <h3>算法标签</h3>
          <div class="tag-item" v-for="tag in algorithmTags" :key="tag.id">
            <input
              type="checkbox"
              :id="tag.id"
              v-model="selectedAlgorithmTags"
              :value="tag.id"
            >
            <label :for="tag.id">{{ tag.name }}</label>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const emit = defineEmits(['filter-change'])

const showTagsPanel = ref(false)
const selectAllPlatforms = ref(true)
const selectedPlatforms = ref([])
const selectedDifficulties = ref([])
const selectedAlgorithmTags = ref([])

const platforms = ref([
  { id: 'leetcode', name: 'LeetCode' },
  { id: 'atcoder', name: 'AtCoder' },
  { id: 'codeforces', name: 'Codeforces' },
  { id: 'nowcoder', name: 'NowCoder' }
])

const difficulties = ref([
  { id: 0, name: '简单' },
  { id: 1, name: '中等' },
  { id: 2, name: '困难' }
])

const algorithmTags = ref([
  // 基础算法
  { id: 'simulation', name: '模拟' },
  { id: 'greedy', name: '贪心' },
  { id: 'dynamic-programming', name: '动态规划' },
  { id: 'search', name: '搜索' },
  { id: 'graph', name: '图论' },
  { id: 'math', name: '数学' },
  { id: 'string', name: '字符串' },
  { id: 'data-structures', name: '数据结构' },
  { id: 'geometry', name: '几何' },
  // 搜索相关
  { id: 'depth-first-search', name: '深度优先搜索' },
  { id: 'breadth-first-search', name: '广度优先搜索' },
  { id: 'binary-search', name: '二分查找' },
  { id: 'backtracking', name: '回溯' },
  // 数据结构
  { id: 'array', name: '数组' },
  { id: 'linked-list', name: '链表' },
  { id: 'stack', name: '栈' },
  { id: 'queue', name: '队列' },
  { id: 'tree', name: '树' },
  { id: 'heap-priority-queue', name: '堆' },
  { id: 'hash-table', name: '哈希表' },
  { id: 'union-find', name: '并查集' },
  { id: 'trie', name: '字典树' },
  // 图论
  { id: 'shortest-path', name: '最短路' },
  { id: 'minimum-spanning-tree', name: '最小生成树' },
  { id: 'topological-sort', name: '拓扑排序' },
  { id: 'strongly-connected-components', name: '强连通分量' },
  { id: 'eulerian-circuit', name: '欧拉回路' },
  // 动态规划
  { id: 'linear-dynamic-programming', name: '线性DP' },
  { id: 'interval-dynamic-programming', name: '区间DP' },
  { id: 'tree-dynamic-programming', name: '树形DP' },
  { id: 'state-compression-dynamic-programming', name: '状态压缩DP' },
  { id: 'digit-dynamic-programming', name: '数位DP' },
  // 数学
  { id: 'number-theory', name: '数论' },
  { id: 'combinatorics', name: '组合数学' },
  { id: 'probability', name: '概率论' },
  { id: 'matrix', name: '矩阵快速幂' },
  { id: 'game-theory', name: '博弈论' },
  // 字符串
  { id: 'string-matching', name: 'KMP' },
  { id: 'suffix-array', name: '后缀数组' },
  { id: 'manacher', name: 'Manacher' },
  { id: 'aho-corasick', name: 'AC自动机' },
  // 其他
  { id: 'divide-and-conquer', name: '分治' },
  { id: 'two-pointers', name: '双指针' },
  { id: 'sliding-window', name: '滑动窗口' },
  { id: 'prefix-sum', name: '前缀和' },
  { id: 'difference-array', name: '差分' },
  { id: 'bit-manipulation', name: '位运算' },
  { id: 'sorting', name: '排序' },
  { id: 'counting', name: '计数' },
  { id: 'bucket-sort', name: '桶排序' },
  { id: 'merge-sort', name: '归并排序' },
  { id: 'quick-sort', name: '快速排序' }
])

const tagsButtonText = computed(() => {
  const count = selectedPlatforms.value.length + selectedDifficulties.value.length + selectedAlgorithmTags.value.length
  return count > 0 ? `标签 (${count})` : '标签'
})

function toggleTagsPanel() {
  showTagsPanel.value = !showTagsPanel.value
}

function resetFilters() {
  selectAllPlatforms.value = true
  selectedPlatforms.value = []
  selectedDifficulties.value = []
  selectedAlgorithmTags.value = []
}

function toggleAllPlatforms() {
  if (selectAllPlatforms.value) {
    selectedPlatforms.value = []
  } else {
    // 如果取消全选，则选择所有平台
    selectedPlatforms.value = platforms.value.map(p => p.id)
  }
}

function handlePlatformChange() {
  selectAllPlatforms.value = selectedPlatforms.value.length === 0
  // 如果选择了所有平台，则设置为全选
  if (selectedPlatforms.value.length === platforms.value.length) {
    selectAllPlatforms.value = true
  }
}

function handleDifficultyClick(difficultyId) {
  if (selectedDifficulties.value.includes(difficultyId)) {
    // 如果点击的是已选中的难度，则取消选择
    selectedDifficulties.value = []
  } else {
    // 如果点击的是未选中的难度，则只选择该难度
    selectedDifficulties.value = [difficultyId]
  }
  emit('filter-change', {
    platforms: selectedPlatforms.value,
    difficulties: selectedDifficulties.value,
    tags: selectedAlgorithmTags.value
  })
}

// 点击页面其他地方关闭面板
document.addEventListener('click', (e) => {
  if (!e.target.closest('.tags-container')) {
    showTagsPanel.value = false
  }
})

// 暴露获取当前选择的方法
defineExpose({
  getSelectedFilters: () => ({
    platforms: selectedPlatforms.value,
    difficulties: selectedDifficulties.value,
    tags: selectedAlgorithmTags.value
  })
})
</script>

<style scoped>
.tags-container {
  position: relative;
  width: 100%;
}

@media (min-width: 768px) {
  .tags-container {
    width: auto;
    min-width: 200px;
  }
}

.tags-toggle {
  padding: 12px 16px;
  background: var(--color-surface);
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-md);
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  transition: all 0.2s ease;
  color: var(--color-text);
}

.tags-toggle:hover {
  border-color: var(--color-primary);
  background: rgba(51, 65, 85, 0.02);
}

.tags-toggle::after {
  content: "▼";
  font-size: 10px;
  margin-left: 8px;
  transition: transform 0.3s ease;
  color: var(--color-text-secondary);
}

.tags-toggle.collapsed::after {
  transform: rotate(-90deg);
}

.tags-panel {
  position: absolute;
  top: calc(100% + 8px);
  left: 0;
  width: 100%;
  min-width: 320px;
  max-width: 100vw;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 20px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12), 0 4px 8px rgba(0, 0, 0, 0.08);
  z-index: 100;
  display: none;
  animation: slideDown 0.2s ease;
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (min-width: 576px) {
  .tags-panel {
    width: 600px;
  }
}

.tags-panel.active {
  display: block;
}

.tags-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--color-border);
}

.tags-title {
  font-weight: 600;
  font-size: 15px;
  color: var(--color-text);
}

.reset-btn {
  background: none;
  border: none;
  color: var(--color-primary);
  cursor: pointer;
  font-size: 13px;
  font-weight: 500;
  padding: 6px 12px;
  border-radius: var(--radius-sm);
  transition: all 0.2s ease;
}

.reset-btn:hover {
  background: rgba(51, 65, 85, 0.08);
}

.tags-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
  max-height: 60vh;
  overflow-y: auto;
  padding-right: 8px;
}

.tags-content::-webkit-scrollbar {
  width: 6px;
}

.tags-content::-webkit-scrollbar-track {
  background: transparent;
}

.tags-content::-webkit-scrollbar-thumb {
  background: var(--color-border);
  border-radius: 3px;
}

.tags-content::-webkit-scrollbar-thumb:hover {
  background: var(--color-text-tertiary);
}

.filter-section {
  display: grid;
  grid-template-columns: 1fr;
  gap: 20px;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--color-border);
}

@media (min-width: 576px) {
  .filter-section {
    grid-template-columns: repeat(2, 1fr);
  }
}

.platform-list, .difficulty-list {
  min-width: 0;
}

.platform-list h3, .difficulty-list h3 {
  font-size: 13px;
  margin: 0 0 12px 0;
  color: var(--color-text-secondary);
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.tag-item {
  display: inline-block;
  margin-right: 8px;
  margin-bottom: 8px;
}

.tag-item label {
  display: inline-block;
  padding: 6px 14px;
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  cursor: pointer;
  font-size: 13px;
  font-weight: 500;
  transition: all 0.2s ease;
  white-space: nowrap;
  color: var(--color-text-secondary);
}

.tag-item label:hover {
  background: rgba(51, 65, 85, 0.06);
  border-color: var(--color-primary);
  color: var(--color-text);
}

.tag-item input[type="checkbox"] {
  display: none;
}

.tag-item input[type="checkbox"]:disabled + label {
  opacity: 0.5;
  cursor: not-allowed;
}

.tag-item input[type="checkbox"]:checked + label {
  background: var(--color-primary);
  color: white;
  border-color: var(--color-primary);
}

.difficulty-list .tag-item {
  margin: 4px;
}

.difficulty-option {
  display: inline-block;
  padding: 6px 14px;
  background: var(--color-bg);
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-md);
  cursor: pointer;
  font-size: 13px;
  font-weight: 600;
  transition: all 0.2s ease;
  white-space: nowrap;
}

.difficulty-option:hover {
  background: rgba(51, 65, 85, 0.06);
  transform: translateY(-1px);
}

.difficulty-option.selected {
  background: var(--color-primary);
  color: white;
  border-color: var(--color-primary);
  box-shadow: 0 2px 8px rgba(51, 65, 85, 0.2);
}

.difficulty-0 .difficulty-option {
  color: var(--color-success);
  border-color: rgba(34, 197, 94, 0.3);
}

.difficulty-1 .difficulty-option {
  color: var(--color-warning);
  border-color: rgba(251, 191, 36, 0.3);
}

.difficulty-2 .difficulty-option {
  color: var(--color-error);
  border-color: rgba(239, 68, 68, 0.3);
}

.difficulty-0 .difficulty-option.selected,
.difficulty-1 .difficulty-option.selected,
.difficulty-2 .difficulty-option.selected {
  color: white;
  border-color: var(--color-primary);
}

.algorithm-list h3 {
  font-size: 13px;
  margin: 0 0 12px 0;
  color: var(--color-text-secondary);
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.tag-item label.disabled {
  opacity: 0.5;
  cursor: not-allowed;
  pointer-events: none;
}
</style>
