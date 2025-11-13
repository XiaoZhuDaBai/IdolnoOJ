<template>
  <div class="section submission-analysis">
    <div class="analysis-header">
      <h2>一周提交数据</h2>
      <div class="time-range">
        {{ formatDate(timeRange.start) }} ~ {{ formatDate(timeRange.end) }}
      </div>
    </div>

    <div class="chart-container">
      <div v-if="loading" class="loading-state">
        <div class="loading-spinner"></div>
        <span>加载中...</span>
      </div>
      <canvas v-else ref="chartCanvas"></canvas>
    </div>

    <div class="chart-legend">
      <div class="legend-item">
        <div class="legend-color total"></div>
        <span>提交总数</span>
      </div>
      <div class="legend-item">
        <div class="legend-color accepted"></div>
        <span>通过数</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import Chart from 'chart.js/auto'
import { MyCommitsData } from '@/api/UserApi.js'
import { useUserStore } from '@/stores/userStore.js'

const chartCanvas = ref(null)
let chart = null
const userStore = useUserStore()

// 获取最近一周的日期范围
const today = new Date()
const timeRange = ref({
  start: new Date(today.getTime() - 6 * 24 * 60 * 60 * 1000), // 6天前
  end: today
})

// 一周的提交数据
const submissionData = ref([])
const loading = ref(true)

// 获取一周提交数据
async function fetchSubmissionData() {
  try {
    loading.value = true
    const uuid = userStore.user?.uuid
    if (!uuid) {
      console.warn('用户未登录，无法获取提交数据')
      return
    }

    const response = await MyCommitsData(uuid)

    // 检查响应数据结构
    let apiData = response.data || response
    if (!Array.isArray(apiData)) {
      apiData = []
    }

    // 转换API数据格式
    submissionData.value = apiData.map(item => ({
      date: new Date(item.time),
      total: item.commitCount,
      accepted: item.acCount
    })).sort((a, b) => a.date - b.date) // 按日期排序

    // 更新时间范围
    if (submissionData.value.length > 0) {
      timeRange.value.start = submissionData.value[0].date
      timeRange.value.end = submissionData.value[submissionData.value.length - 1].date
    }

  } catch (error) {
    console.error('获取提交数据失败:', error)
    // 使用默认数据作为后备
    submissionData.value = [
      { date: new Date(today.getTime() - 6 * 24 * 60 * 60 * 1000), total: 0, accepted: 0 },
      { date: new Date(today.getTime() - 5 * 24 * 60 * 60 * 1000), total: 0, accepted: 0 },
      { date: new Date(today.getTime() - 4 * 24 * 60 * 60 * 1000), total: 0, accepted: 0 },
      { date: new Date(today.getTime() - 3 * 24 * 60 * 60 * 1000), total: 0, accepted: 0 },
      { date: new Date(today.getTime() - 2 * 24 * 60 * 60 * 1000), total: 0, accepted: 0 },
      { date: new Date(today.getTime() - 1 * 24 * 60 * 60 * 1000), total: 0, accepted: 0 },
      { date: today, total: 0, accepted: 0 }
    ]
  } finally {
    loading.value = false
  }
}

function formatDate(date) {
  return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}

function initChart() {
  if (!chartCanvas.value || submissionData.value.length === 0) return

  const ctx = chartCanvas.value.getContext('2d')

  // 准备标签数据
  const labels = submissionData.value.map(item =>
    formatDate(item.date)
  )

  // 准备数据集
  const totalData = submissionData.value.map(item => item.total)
  const acceptedData = submissionData.value.map(item => item.accepted)

  // 如果图表已存在，先销毁
  if (chart) {
    chart.destroy()
  }

  chart = new Chart(ctx, {
    type: 'bar',
    data: {
      labels: labels,
      datasets: [
        {
          label: '提交总数',
          data: totalData,
          backgroundColor: '#94a3b8',
          borderRadius: 6,
          borderWidth: 0
        },
        {
          label: '通过数',
          data: acceptedData,
          backgroundColor: '#10b981',
          borderRadius: 6,
          borderWidth: 0
        }
      ]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      scales: {
        x: {
          grid: {
            display: false
          }
        },
        y: {
          beginAtZero: true,
          ticks: {
            stepSize: 1
          }
        }
      },
      plugins: {
        legend: {
          display: false
        },
        tooltip: {
          callbacks: {
            title: function(context) {
              return submissionData.value[context[0].dataIndex].date.toLocaleDateString('zh-CN')
            },
            afterBody: function(context) {
              const data = submissionData.value[context[0].dataIndex]
              if (data.total === 0) {
                return ['通过率: 0%']
              }
              const passRate = ((data.accepted / data.total) * 100).toFixed(1)
              return [`通过率: ${passRate}%`]
            }
          }
        }
      },
      // 设置柱状图堆叠
      interaction: {
        intersect: false,
        mode: 'index'
      }
    }
  })
}

// 监听数据变化，重新初始化图表
watch(submissionData, async (newData) => {
  if (!loading.value && newData.length > 0) {
    await nextTick()
    initChart()
  }
}, { deep: true })

// 监听loading状态变化
watch(loading, async (newLoading) => {
  if (!newLoading && submissionData.value.length > 0) {
    await nextTick()
    initChart()
  }
})

onMounted(async () => {
  await fetchSubmissionData()
  // 确保DOM更新后再初始化图表
  await nextTick()
  initChart()
})

onBeforeUnmount(() => {
  if (chart) {
    chart.destroy()
  }
})
</script>

<style scoped>
.submission-analysis {
  margin-bottom: 0;
}

.analysis-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-lg);
}

.time-range {
  color: var(--color-text-secondary);
  font-size: 13px;
  font-weight: 500;
}

.chart-container {
  position: relative;
  height: 280px;
  margin: var(--spacing-lg) 0;
}

.chart-legend {
  display: flex;
  justify-content: center;
  gap: var(--spacing-lg);
  margin-top: var(--spacing-md);
}

.legend-item {
  display: flex;
  align-items: center;
  font-size: 13px;
  color: var(--color-text-secondary);
}

.legend-color {
  width: 12px;
  height: 12px;
  margin-right: var(--spacing-sm);
  border-radius: 2px;
}

.legend-color.total {
  background-color: #94a3b8;
}

.legend-color.accepted {
  background-color: var(--color-success);
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: var(--color-text-tertiary);
  gap: var(--spacing-md);
}

.loading-spinner {
  width: 28px;
  height: 28px;
  border: 2px solid var(--color-border);
  border-top: 2px solid var(--color-primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}
</style>
