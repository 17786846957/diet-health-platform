<template>
  <div class="stats-page">
    <!-- 顶部趋势区 -->
    <el-row :gutter="16" style="margin-bottom: 16px">
      <el-col :xs="24" :sm="12">
        <div class="chart-card">
          <div class="chart-card__header">
            <span class="chart-card__title">热量趋势</span>
            <el-radio-group v-model="trendRange" size="small" @change="loadTrend" class="trend-switch">
              <el-radio-button :value="7">7天</el-radio-button>
              <el-radio-button :value="14">14天</el-radio-button>
              <el-radio-button :value="30">30天</el-radio-button>
            </el-radio-group>
          </div>
          <div ref="lineChartRef" style="height: 300px"></div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="12">
        <div class="chart-card">
          <div class="chart-card__header">
            <span class="chart-card__title">今日营养素占比</span>
            <el-date-picker v-model="pieDate" type="date" value-format="YYYY-MM-DD" @change="loadPie" style="width: 140px" size="small" />
          </div>
          <div ref="pieChartRef" style="height: 300px"></div>
        </div>
      </el-col>
    </el-row>

    <!-- 营养目标完成度 -->
    <div class="chart-card" style="margin-top: 16px">
      <div class="chart-card__header">
        <span class="chart-card__title">营养目标完成度</span>
        <el-date-picker v-model="gapDate" type="date" value-format="YYYY-MM-DD" @change="loadGap" style="width: 140px" size="small" />
      </div>
      <div ref="gapChartRef" style="height: 280px"></div>
    </div>

    <!-- 每日营养明细 -->
    <div class="chart-card" style="margin-top: 16px">
      <div class="chart-card__header">
        <span class="chart-card__title">每日营养明细</span>
      </div>
      <el-table :data="dailyDetails" stripe class="detail-table">
        <el-table-column prop="mealType" label="餐次" width="80">
          <template #default="{ row }">{{ mealMap[row.mealType] }}</template>
        </el-table-column>
        <el-table-column prop="totalCalories" label="热量(kcal)" width="120">
          <template #default="{ row }">
            <span class="cal-badge">{{ row.totalCalories }}</span>
          </template>
        </el-table-column>
        <el-table-column label="食物">
          <template #default="{ row }">
            <span v-for="(d, i) in row.details" :key="i" class="food-tag">
              {{ d.foodName }}<span class="food-tag__amount">{{ d.amount }}g</span>{{ i < row.details.length - 1 ? '' : '' }}
            </span>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, shallowRef, computed, onMounted, onBeforeUnmount } from 'vue'
import * as echarts from 'echarts/core'
import { LineChart, PieChart, BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent, TitleComponent, DataZoomComponent, MarkLineComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { ElMessage } from 'element-plus'
import { useDietStore } from '../stores/diet'
import { useUserStore } from '../stores/user'
import { useLoading } from '../composables/useLoading'
import { getToday, formatDate } from '../utils/date'
import { calculateTargetCalories } from '../utils/nutrition'
import { mealMap, FALLBACK_ERROR } from '../utils/constants'
import { getNutritionGap } from '../api/diet'

echarts.use([LineChart, PieChart, BarChart, GridComponent, TooltipComponent, LegendComponent, TitleComponent, DataZoomComponent, MarkLineComponent, CanvasRenderer])

const dietStore = useDietStore()
const userStore = useUserStore()
const { loading: lineLoading, withLoading: withLineLoading } = useLoading()
const { loading: pieLoading, withLoading: withPieLoading } = useLoading()

const today = getToday()

const lineChartRef = ref()
const pieChartRef = ref()
const gapChartRef = ref()
const pieDate = ref(today)
const gapDate = ref(today)
const trendRange = ref(7)
const dailyDetails = ref([])

const lineChart = shallowRef(null)
const pieChart = shallowRef(null)
const gapChart = shallowRef(null)

const targetCalories = computed(() => {
  const val = calculateTargetCalories(userStore.activeProfile)
  return val > 0 ? val : 2000
})

function makeGradient(ec, color1, color2) {
  return new echarts.graphic.LinearGradient(0, 0, 0, 1, [
    { offset: 0, color: color1 },
    { offset: 1, color: color2 }
  ])
}

async function loadTrend() {
  await withLineLoading(async () => {
    try {
      const days = Number(trendRange.value)
      const end = new Date()
      const start = new Date(end)
      start.setDate(start.getDate() - days + 1)
      const data = await dietStore.fetchWeeklyStats(formatDate(start), formatDate(end))
      if (!lineChart.value) lineChart.value = echarts.init(lineChartRef.value)

      lineChart.value.setOption({
        tooltip: {
          trigger: 'axis',
          backgroundColor: 'rgba(255,255,255,0.96)',
          borderColor: '#e8e8e8',
          borderWidth: 1,
          textStyle: { color: '#333', fontSize: 13 },
          axisPointer: { type: 'cross', crossStyle: { color: '#999' } }
        },
        grid: { top: 30, right: 20, bottom: 30, left: 50 },
        xAxis: {
          type: 'category',
          data: data.dates,
          axisLabel: { color: '#999', fontSize: 11, rotate: days > 14 ? 45 : 0 },
          axisLine: { lineStyle: { color: '#eee' } },
          axisTick: { show: false }
        },
        yAxis: {
          type: 'value',
          name: 'kcal',
          nameTextStyle: { color: '#bbb', fontSize: 11 },
          axisLabel: { color: '#999', fontSize: 11 },
          splitLine: { lineStyle: { color: '#f5f5f5', type: 'dashed' } },
          axisLine: { show: false }
        },
        series: [{
          data: data.calories,
          type: 'line',
          smooth: 0.4,
          symbol: 'circle',
          symbolSize: 6,
          showSymbol: data.dates.length <= 14,
          lineStyle: { width: 2.5, color: '#10b981' },
          itemStyle: { color: '#10b981', borderWidth: 2, borderColor: '#fff' },
          areaStyle: {
            color: makeGradient(echarts, 'rgba(16,185,129,0.25)', 'rgba(16,185,129,0.02)')
          },
          markLine: {
            silent: true,
            symbol: 'none',
            data: [{
              yAxis: targetCalories.value,
              label: { formatter: '目标 ' + targetCalories.value, position: 'insideEndTop', color: '#10b981', fontSize: 11, backgroundColor: 'rgba(255,255,255,0.8)', padding: [2, 6], borderRadius: 3 },
              lineStyle: { color: '#10b981', type: [6, 4], width: 1.5 }
            }]
          }
        }],
        dataZoom: days > 14 ? [{ type: 'inside', start: 0, end: 100 }] : []
      }, true)
    } catch (e) {
      ElMessage.error(e.message || FALLBACK_ERROR)
    }
  })
}

async function loadPie() {
  await withPieLoading(async () => {
    try {
      const data = await dietStore.fetchDailyStats(pieDate.value)
      if (!pieChart.value) pieChart.value = echarts.init(pieChartRef.value)

      pieChart.value.setOption({
        tooltip: {
          trigger: 'item',
          backgroundColor: 'rgba(255,255,255,0.96)',
          borderColor: '#e8e8e8',
          textStyle: { color: '#333' },
          formatter: '{b}: {c}g ({d}%)'
        },
        legend: {
          bottom: 10,
          itemWidth: 12,
          itemHeight: 12,
          textStyle: { color: '#666', fontSize: 12 }
        },
        series: [{
          type: 'pie',
          radius: ['42%', '70%'],
          center: ['50%', '45%'],
          avoidLabelOverlap: true,
          itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
          label: { show: false },
          emphasis: {
            label: { show: true, fontSize: 14, fontWeight: 'bold' },
            itemStyle: { shadowBlur: 10, shadowOffsetX: 0, shadowColor: 'rgba(0,0,0,0.15)' }
          },
          data: [
            { name: '蛋白质', value: data.totalProtein || 0, itemStyle: { color: makeGradient(echarts, '#f59e0b', '#fbbf24') } },
            { name: '脂肪', value: data.totalFat || 0, itemStyle: { color: makeGradient(echarts, '#ef4444', '#f87171') } },
            { name: '碳水化合物', value: data.totalCarbs || 0, itemStyle: { color: makeGradient(echarts, '#6366f1', '#818cf8') } }
          ]
        }]
      }, true)
      dailyDetails.value = data.records || []
    } catch (e) {
      ElMessage.error(e.message || FALLBACK_ERROR)
    }
  })
}

async function loadGap() {
  try {
    const params = { date: gapDate.value }
    if (userStore.activeMemberId) params.memberId = userStore.activeMemberId
    const res = await getNutritionGap(params)
    const gaps = res.data.gaps || []
    if (!gapChart.value) gapChart.value = echarts.init(gapChartRef.value)

    gapChart.value.setOption({
      tooltip: {
        trigger: 'axis',
        backgroundColor: 'rgba(255,255,255,0.96)',
        borderColor: '#e8e8e8',
        textStyle: { color: '#333' },
        axisPointer: { type: 'shadow' }
      },
      grid: { top: 30, right: 30, bottom: 30, left: 80 },
      xAxis: {
        type: 'value',
        name: '完成度(%)',
        nameTextStyle: { color: '#94a3b8', fontSize: 11 },
        axisLabel: { color: '#94a3b8', fontSize: 11 },
        splitLine: { lineStyle: { color: '#f1f5f9', type: 'dashed' } },
        max: function (val) { return Math.max(120, val.max + 10) }
      },
      yAxis: {
        type: 'category',
        data: gaps.map(g => g.nutrient),
        axisLabel: { color: '#475569', fontSize: 12, fontWeight: 500 },
        axisLine: { show: false },
        axisTick: { show: false }
      },
      series: [{
        type: 'bar',
        barWidth: 28,
        data: gaps.map(g => ({
          value: g.percentage,
          itemStyle: {
            borderRadius: [0, 6, 6, 0],
            color: g.percentage >= 100
              ? makeGradient(echarts, '#10b981', '#34d399')
              : g.percentage >= 80
                ? makeGradient(echarts, '#f59e0b', '#fbbf24')
                : makeGradient(echarts, '#ef4444', '#f87171')
          }
        })),
        label: {
          show: true,
          position: 'right',
          formatter: function (p) {
            const icon = p.value >= 100 ? '✓' : p.value >= 80 ? '!' : '✗'
            return `${icon} ${p.value}%`
          },
          color: '#475569',
          fontSize: 12,
          fontWeight: 600
        },
        markLine: {
          silent: true,
          symbol: 'none',
          data: [{
            xAxis: 100,
            label: { formatter: '达标线', position: 'insideEndTop', color: '#10b981', fontSize: 11 },
            lineStyle: { color: '#10b981', type: [6, 4], width: 1.5 }
          }]
        }
      }],
      animationDuration: 600,
      animationEasing: 'cubicOut'
    }, true)
  } catch (e) {
    console.warn('加载营养目标完成度失败:', e)
  }
}

function handleResize() {
  lineChart.value?.resize()
  pieChart.value?.resize()
  gapChart.value?.resize()
}

onMounted(() => {
  window.addEventListener('resize', handleResize)
  loadTrend()
  loadPie()
  loadGap()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  lineChart.value?.dispose()
  pieChart.value?.dispose()
  gapChart.value?.dispose()
  lineChart.value = null
  pieChart.value = null
  gapChart.value = null
})
</script>

<style scoped>
.stats-page {
  padding: 4px;
}

.chart-card {
  background: var(--bg-card);
  border-radius: var(--radius-card);
  padding: 20px;
  box-shadow: var(--shadow-card);
  border: 1px solid var(--border-color);
}
.chart-card__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.chart-card__title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}

.trend-switch :deep(.el-radio-button__inner) {
  border-radius: 6px !important;
  border: none !important;
  box-shadow: none !important;
  font-size: 12px;
  padding: 5px 12px;
}

.detail-table :deep(.el-table__header th) {
  background: #f8fafc;
  color: var(--text-secondary);
  font-weight: 600;
  font-size: 13px;
}
.cal-badge {
  display: inline-block;
  background: var(--color-primary-lighter);
  color: var(--color-primary);
  padding: 2px 8px;
  border-radius: 4px;
  font-weight: 600;
  font-size: 13px;
}
.food-tag {
  display: inline-block;
  margin-right: 8px;
  margin-bottom: 2px;
  font-size: 13px;
  color: var(--text-primary);
}
.food-tag__amount {
  color: var(--text-muted);
  font-size: 12px;
  margin-left: 2px;
}
</style>
