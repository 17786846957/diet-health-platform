<template>
  <div v-loading="loading">
    <el-alert
      v-if="calorieWarning.show"
      :title="calorieWarning.title"
      :type="calorieWarning.type"
      :closable="false"
      show-icon
      style="margin-bottom: 20px; border-radius: 8px"
    />

    <el-row :gutter="16" style="margin-bottom: 20px">
      <el-col :xs="12" :sm="6">
        <div class="stat-card">
          <div class="stat-label">今日热量</div>
          <div class="stat-value" :style="{ color: 'var(--color-calories)' }">
            {{ stats.totalCalories || 0 }} <span class="stat-unit">kcal</span>
          </div>
          <div class="stat-target">目标: {{ targetCalories }} kcal</div>
          <el-progress
            :percentage="Math.min(Math.round((stats.totalCalories || 0) / targetCalories * 100), 100)"
            :color="'var(--color-calories)'"
            :stroke-width="6"
            :show-text="false"
          />
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-card">
          <div class="stat-label">蛋白质</div>
          <div class="stat-value" style="color: var(--color-protein)">
            {{ stats.totalProtein || 0 }} <span class="stat-unit">g</span>
          </div>
          <div class="stat-target">目标: {{ proteinTarget }}g</div>
          <el-progress
            :percentage="Math.min(Math.round((stats.totalProtein || 0) / proteinTarget * 100), 100)"
            :color="'var(--color-protein)'"
            :stroke-width="6"
            :show-text="false"
          />
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-card">
          <div class="stat-label">脂肪</div>
          <div class="stat-value" style="color: var(--color-fat)">
            {{ stats.totalFat || 0 }} <span class="stat-unit">g</span>
          </div>
          <div class="stat-target">目标: {{ fatTarget }}g</div>
          <el-progress
            :percentage="Math.min(Math.round((stats.totalFat || 0) / fatTarget * 100), 100)"
            :color="'var(--color-fat)'"
            :stroke-width="6"
            :show-text="false"
          />
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-card">
          <div class="stat-label">碳水化合物</div>
          <div class="stat-value" style="color: var(--color-carbs)">
            {{ stats.totalCarbs || 0 }} <span class="stat-unit">g</span>
          </div>
          <div class="stat-target">目标: {{ carbsTarget }}g</div>
          <el-progress
            :percentage="Math.min(Math.round((stats.totalCarbs || 0) / carbsTarget * 100), 100)"
            :color="'var(--color-carbs)'"
            :stroke-width="6"
            :show-text="false"
          />
        </div>
      </el-col>
    </el-row>

    <el-card style="margin-bottom: 20px">
      <template #header><span style="font-weight: 600; color: var(--text-primary)">今日营养摄入进度</span></template>
      <div v-for="item in nutritionGaps" :key="item.nutrient" class="gap-item">
        <div class="gap-header">
          <span>{{ item.nutrient }}</span>
          <span :style="{ color: getStatusColor(item.percentage) }">
            {{ item.actual }}/{{ item.target }} {{ item.unit }} ({{ item.percentage }}%)
          </span>
        </div>
        <el-progress
          :percentage="Math.min(item.percentage, 100)"
          :color="getProgressColor(item.percentage)"
          :stroke-width="8"
        />
        <div v-if="item.gap > 0" class="gap-hint">还需摄入 {{ item.gap }} {{ item.unit }}</div>
        <div v-else-if="item.gap < 0" class="gap-hint gap-over">已超标 {{ Math.abs(item.gap) }} {{ item.unit }}</div>
      </div>
    </el-card>

    <el-row :gutter="16">
      <el-col :xs="24" :sm="16">
        <el-card style="margin-bottom: 20px">
          <template #header>
            <div style="display: flex; justify-content: space-between; align-items: center">
              <span style="font-weight: 600; color: var(--text-primary)">热量趋势</span>
              <el-radio-group v-model="trendDays" size="small" @change="loadTrend">
                <el-radio-button :value="7">7天</el-radio-button>
                <el-radio-button :value="14">14天</el-radio-button>
                <el-radio-button :value="30">30天</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div ref="chartRef" style="height: 300px"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="8">
        <el-card style="margin-bottom: 20px">
          <template #header><span style="font-weight: 600; color: var(--text-primary)">快捷操作</span></template>
          <el-space direction="vertical" :size="10" style="width: 100%">
            <el-button type="primary" style="width: 100%" @click="$router.push('/diet')">记录饮食</el-button>
            <el-button style="width: 100%" @click="$router.push('/food')">浏览食物库</el-button>
            <el-button style="width: 100%" @click="$router.push('/stats')">查看统计</el-button>
            <el-button style="width: 100%" @click="$router.push('/profile')">更新个人信息</el-button>
          </el-space>
        </el-card>

        <el-card>
          <template #header><span style="font-weight: 600; color: var(--text-primary)">今日饮食建议</span></template>
          <div class="advice-content">
            <p>推荐蛋白质: <strong style="color: var(--color-protein)">{{ proteinTarget }}g</strong></p>
            <p>推荐脂肪: <strong style="color: var(--color-fat)">{{ fatTarget }}g</strong></p>
            <p>推荐碳水: <strong style="color: var(--color-carbs)">{{ carbsTarget }}g</strong></p>
            <p v-if="advice" style="margin-top: 8px; color: var(--color-primary); font-weight: 500">{{ advice }}</p>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, shallowRef, onMounted, computed, onBeforeUnmount, nextTick } from 'vue'
import * as echarts from 'echarts/core'
import { LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, MarkLineComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

echarts.use([LineChart, GridComponent, TooltipComponent, MarkLineComponent, CanvasRenderer])
import { useDietStore } from '../stores/diet'
import { useLoading } from '../composables/useLoading'
import { useCalorieWarning } from '../composables/useCalorieWarning'
import { getToday, formatDate } from '../utils/date'

const dietStore = useDietStore()
const { loading, withLoading } = useLoading()
const chartRef = ref()
const chart = shallowRef(null)
const stats = ref({})
const nutritionGaps = ref([])
const trendDays = ref(7)
let resizeHandler = null

const totalCalories = computed(() => stats.value.totalCalories || 0)
const { calorieWarning, targetCalories } = useCalorieWarning(totalCalories)
const proteinTarget = computed(() => Math.round(targetCalories.value * 0.15 / 4))
const fatTarget = computed(() => Math.round(targetCalories.value * 0.25 / 9))
const carbsTarget = computed(() => Math.round(targetCalories.value * 0.60 / 4))

const advice = computed(() => {
  const total = stats.value.totalCalories || 0
  const target = targetCalories.value
  if (target <= 0) return '请先完善个人信息以获取个性化建议'
  const pct = total / target * 100
  if (pct >= 100) return '今日热量已达标，建议控制后续摄入'
  if (pct >= 80) return '今日热量接近目标，晚餐建议清淡饮食'
  if (pct < 30) return '今日摄入较少，记得按时吃饭哦'
  return ''
})

function getProgressColor(pct) {
  if (pct >= 100) return '#ef4444'
  if (pct >= 80) return '#f59e0b'
  return '#10b981'
}

function getStatusColor(pct) {
  if (pct >= 100) return '#ef4444'
  if (pct >= 80) return '#f59e0b'
  return '#10b981'
}

async function loadTrend() {
  try {
    const today = new Date()
    const start = new Date(today)
    start.setDate(start.getDate() - trendDays.value + 1)
    const startStr = formatDate(start)
    const endStr = formatDate(today)
    let weeklyData
    try {
      weeklyData = await dietStore.fetchWeeklyStats(startStr, endStr)
    } catch (e) {
      weeklyData = { dates: [], calories: [] }
    }

    if (!chart.value && chartRef.value) {
      chart.value = echarts.init(chartRef.value)
    }
    if (!chart.value) return
    chart.value.setOption({
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: weeklyData.dates, axisLine: { lineStyle: { color: '#e2e8f0' } }, axisLabel: { color: '#94a3b8' } },
      yAxis: { type: 'value', name: 'kcal', axisLine: { show: false }, splitLine: { lineStyle: { color: '#f1f5f9' } } },
      series: [{
        data: weeklyData.calories,
        type: 'line',
        smooth: true,
        areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: 'rgba(16,185,129,0.15)' }, { offset: 1, color: 'rgba(16,185,129,0)' }] } },
        lineStyle: { color: '#10b981', width: 2.5 },
        itemStyle: { color: '#10b981' },
        markLine: {
          silent: true,
          data: [{
            yAxis: targetCalories.value,
            label: { formatter: '目标', position: 'end', color: '#94a3b8' },
            lineStyle: { color: '#94a3b8', type: 'dashed' }
          }]
        }
      }]
    })
  } catch (e) {
    console.warn('加载热量趋势失败:', e)
  }
}

onMounted(async () => {
  await withLoading(async () => {
    const today = getToday()
    const results = await Promise.allSettled([
      dietStore.fetchDailyStats(today),
      dietStore.fetchNutritionGap(today)
    ])
    if (results[0].status === 'fulfilled') {
      stats.value = results[0].value
    }
    nutritionGaps.value = dietStore.nutritionGaps
  })

  await nextTick()
  await loadTrend()

  resizeHandler = () => chart.value?.resize()
  window.addEventListener('resize', resizeHandler)
})

onBeforeUnmount(() => {
  if (chart.value) {
    chart.value.dispose()
    chart.value = null
  }
  if (resizeHandler) {
    window.removeEventListener('resize', resizeHandler)
  }
})
</script>

<style scoped>
.stat-card {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-card);
  padding: 16px;
  margin-bottom: 16px;
}
.stat-label {
  font-size: 13px;
  color: var(--text-muted);
  margin-bottom: 4px;
}
.stat-value {
  font-size: 28px;
  font-weight: 700;
  line-height: 1.2;
}
.stat-unit {
  font-size: 14px;
  color: var(--text-muted);
  font-weight: 400;
}
.stat-target {
  font-size: 12px;
  color: var(--text-muted);
  margin: 4px 0 10px;
}
.gap-item {
  margin-bottom: 16px;
}
.gap-item:last-child {
  margin-bottom: 0;
}
.gap-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 6px;
  font-size: 14px;
  color: var(--text-secondary);
}
.gap-hint {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 4px;
}
.gap-hint.gap-over {
  color: #ef4444;
}
.advice-content {
  font-size: 13px;
  line-height: 1.8;
  color: var(--text-secondary);
}
</style>
