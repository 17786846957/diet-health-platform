<template>
  <el-card v-loading="loading">
    <template #header>
      <div class="card-header">
        <span>饮食分析报告</span>
        <div>
          <el-select v-model="analysisDays" size="small" style="width: 100px; margin-right: 8px">
            <el-option :value="7" label="近7天" />
            <el-option :value="14" label="近14天" />
            <el-option :value="30" label="近30天" />
          </el-select>
          <el-button text @click="fetchData">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
        </div>
      </div>
    </template>

    <div v-if="dietAnalysis" class="analysis-content">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="统计周期">{{ dietAnalysis.period }}</el-descriptions-item>
        <el-descriptions-item label="记录天数">{{ dietAnalysis.recordDays }}天</el-descriptions-item>
        <el-descriptions-item label="总记录数">{{ dietAnalysis.totalRecords }}条</el-descriptions-item>
        <el-descriptions-item label="食物种类">{{ dietAnalysis.uniqueFoods }}种</el-descriptions-item>
      </el-descriptions>

      <div class="analysis-section">
        <h4>分析结果</h4>
        <ul class="analysis-list">
          <li v-for="(item, index) in dietAnalysis.analysis" :key="index">{{ item }}</li>
        </ul>
      </div>

      <div class="avg-nutrition-section">
        <h4>平均每日营养摄入</h4>
        <el-row :gutter="16">
          <el-col :span="6">
            <div class="stat-card">
              <div class="stat-value">{{ dietAnalysis.avgNutrition.calories }}</div>
              <div class="stat-label">热量(千卡)</div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="stat-card">
              <div class="stat-value">{{ dietAnalysis.avgNutrition.protein }}</div>
              <div class="stat-label">蛋白质(g)</div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="stat-card">
              <div class="stat-value">{{ dietAnalysis.avgNutrition.fat }}</div>
              <div class="stat-label">脂肪(g)</div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="stat-card">
              <div class="stat-value">{{ dietAnalysis.avgNutrition.carbs }}</div>
              <div class="stat-label">碳水(g)</div>
            </div>
          </el-col>
        </el-row>
      </div>

      <div class="meal-distribution-section">
        <h4>餐次分布</h4>
        <div ref="mealChartRef" style="height: 200px"></div>
      </div>
    </div>
    <el-empty v-else description="暂无数据" />
  </el-card>
</template>

<script setup>
import { ref, watch, nextTick, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { getDietAnalysis } from '../api/dietAdvice'
import { mealMap } from '../utils/constants'
import * as echarts from 'echarts/core'
import { PieChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

echarts.use([PieChart, TitleComponent, TooltipComponent, LegendComponent, CanvasRenderer])

const props = defineProps({
  memberId: { type: Number, default: null },
  active: { type: Boolean, default: false }
})

const loading = ref(false)
const analysisDays = ref(7)
const dietAnalysis = ref(null)
const mealChartRef = ref(null)
let mealChart = null
const loaded = ref(false)

watch(() => props.active, (val) => {
  if (val && !loaded.value) {
    loaded.value = true
    fetchData()
  }
}, { immediate: true })

watch(analysisDays, () => {
  if (loaded.value) fetchData()
})

onBeforeUnmount(() => {
  if (mealChart) {
    mealChart.dispose()
    mealChart = null
  }
})

async function fetchData() {
  loading.value = true
  try {
    dietAnalysis.value = await getDietAnalysis(analysisDays.value, props.memberId)
    await nextTick()
    renderMealChart()
  } catch {
    ElMessage.error('获取饮食分析失败')
  } finally {
    loading.value = false
  }
}

function renderMealChart() {
  if (!mealChartRef.value || !dietAnalysis.value?.mealDistribution) return
  if (mealChart) mealChart.dispose()

  mealChart = echarts.init(mealChartRef.value)
  const data = Object.entries(dietAnalysis.value.mealDistribution).map(([name, value]) => ({
    name: getMealName(name),
    value
  }))

  mealChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c}次 ({d}%)' },
    series: [{
      type: 'pie',
      radius: '60%',
      data,
      emphasis: {
        itemStyle: { shadowBlur: 10, shadowOffsetX: 0, shadowColor: 'rgba(0, 0, 0, 0.5)' }
      }
    }]
  })
}

function getMealName(type) {
  return mealMap[type] || type
}
</script>

<style scoped>
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.analysis-section,
.avg-nutrition-section,
.meal-distribution-section {
  margin-top: 20px;
}

.analysis-section h4,
.avg-nutrition-section h4,
.meal-distribution-section h4 {
  margin-bottom: 12px;
  color: #303133;
}

.analysis-list {
  padding-left: 20px;
}

.analysis-list li {
  margin-bottom: 8px;
  color: #606266;
}

.stat-card {
  text-align: center;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #409eff;
}

.stat-label {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}
</style>
