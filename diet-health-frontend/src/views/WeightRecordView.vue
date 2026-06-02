<template>
  <div class="weight-record">
    <el-row :gutter="20">
      <el-col :xs="24" :sm="24" :md="16">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>体重记录</span>
              <el-button type="primary" @click="showAddDialog">
                <el-icon><Plus /></el-icon>
                记录体重
              </el-button>
            </div>
          </template>

          <div class="weight-summary">
            <div class="summary-item">
              <div class="summary-value">{{ latestWeight?.weight || '--' }}</div>
              <div class="summary-label">当前体重(kg)</div>
            </div>
            <div class="summary-item">
              <div class="summary-value">{{ latestWeight?.bodyFat || '--' }}</div>
              <div class="summary-label">体脂率(%)</div>
            </div>
            <div class="summary-item">
              <div class="summary-value" :class="trendClass">{{ trendText }}</div>
              <div class="summary-label">近期趋势</div>
            </div>
          </div>

          <div ref="trendChartRef" style="height: 300px"></div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="24" :md="8">
        <el-card>
          <template #header>
            <span>数据统计</span>
          </template>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="起始体重">{{ trendData.weights?.[0] || '--' }} kg</el-descriptions-item>
            <el-descriptions-item label="最新体重">{{ latestWeight?.weight || '--' }} kg</el-descriptions-item>
            <el-descriptions-item label="体重变化">
              <span :class="changeClass">{{ trendData.change > 0 ? '+' : '' }}{{ trendData.change || 0 }} kg</span>
            </el-descriptions-item>
            <el-descriptions-item label="记录次数">{{ trendData.recordCount || 0 }} 次</el-descriptions-item>
          </el-descriptions>
        </el-card>

        <el-card style="margin-top: 20px">
          <template #header>
            <span>BMI 参考</span>
          </template>
          <div class="bmi-info">
            <div class="bmi-value">
              BMI: {{ bmi }}
            </div>
            <div class="bmi-level" :class="bmiClass">{{ bmiLevel }}</div>
            <div class="bmi-ranges">
              <div>偏瘦: &lt;18.5</div>
              <div>正常: 18.5-24</div>
              <div>偏胖: 24-28</div>
              <div>肥胖: &gt;28</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="dialogVisible" title="记录体重" width="400px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="体重">
          <el-input-number v-model="form.weight" :min="20" :max="300" :step="0.1" :precision="1" />
          <span style="margin-left: 8px">kg</span>
        </el-form-item>
        <el-form-item label="体脂率">
          <el-input-number v-model="form.bodyFat" :min="3" :max="60" :step="0.1" :precision="1" />
          <span style="margin-left: 8px">%</span>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.notes" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, computed, nextTick } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { addOrUpdateWeight, getLatestWeight, getWeightTrend } from '../api/weight'
import { useUserStore } from '../stores/user'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts/core'
import { LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent, MarkLineComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

echarts.use([LineChart, GridComponent, TooltipComponent, LegendComponent, MarkLineComponent, CanvasRenderer])

const userStore = useUserStore()
const latestWeight = ref(null)
const trendData = ref({})
const trendChartRef = ref(null)
let trendChart = null
const dialogVisible = ref(false)

const form = ref({
  weight: 60,
  bodyFat: null,
  notes: ''
})

const bmi = computed(() => {
  if (!latestWeight.value?.weight || !userStore.user?.height) return '--'
  const height = userStore.user.height / 100
  return (latestWeight.value.weight / (height * height)).toFixed(1)
})

const bmiLevel = computed(() => {
  const val = parseFloat(bmi.value)
  if (isNaN(val)) return '--'
  if (val < 18.5) return '偏瘦'
  if (val < 24) return '正常'
  if (val < 28) return '偏胖'
  return '肥胖'
})

const bmiClass = computed(() => {
  const val = parseFloat(bmi.value)
  if (isNaN(val)) return ''
  if (val < 18.5) return 'text-warning'
  if (val < 24) return 'text-success'
  return 'text-danger'
})

const trendText = computed(() => {
  const change = trendData.value.change
  if (!change) return '稳定'
  if (change > 0) return '上升'
  if (change < 0) return '下降'
  return '稳定'
})

const trendClass = computed(() => {
  const change = trendData.value.change
  if (!change) return ''
  return change > 0 ? 'text-danger' : 'text-success'
})

const changeClass = computed(() => {
  const change = trendData.value.change
  if (!change) return ''
  return change > 0 ? 'text-danger' : 'text-success'
})

onMounted(() => {
  fetchData()
})

onBeforeUnmount(() => {
  if (trendChart) {
    trendChart.dispose()
    trendChart = null
  }
})

async function fetchData() {
  try {
    const memberId = userStore.activeMemberId
    const [latestRes, trendRes] = await Promise.all([
      getLatestWeight(memberId),
      getWeightTrend(30, memberId)
    ])
    if (latestRes.code === 200) latestWeight.value = latestRes.data
    if (trendRes.code === 200) {
      trendData.value = trendRes.data
      await nextTick()
      renderTrendChart(trendRes.data)
    }
  } catch (error) {
    ElMessage.error('获取体重数据失败')
  }
}

function renderTrendChart(data) {
  if (!trendChartRef.value || !data.dates?.length) return
  if (trendChart) trendChart.dispose()
  trendChart = echarts.init(trendChartRef.value)
  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['体重(kg)', '体脂率(%)'] },
    xAxis: { type: 'category', data: data.dates.map(d => d.slice(5)) },
    yAxis: [
      { type: 'value', name: '体重(kg)', position: 'left' },
      { type: 'value', name: '体脂率(%)', position: 'right' }
    ],
    series: [
      {
        name: '体重(kg)',
        type: 'line',
        data: data.weights,
        smooth: true,
        itemStyle: { color: '#409eff' }
      },
      {
        name: '体脂率(%)',
        type: 'line',
        data: data.bodyFats,
        smooth: true,
        yAxisIndex: 1,
        itemStyle: { color: '#e6a23c' }
      }
    ]
  })
}

function showAddDialog() {
  form.value = {
    weight: latestWeight.value?.weight || 60,
    bodyFat: latestWeight.value?.bodyFat || null,
    notes: ''
  }
  dialogVisible.value = true
}

async function handleSubmit() {
  try {
    const memberId = userStore.activeMemberId
    await addOrUpdateWeight({ ...form.value, memberId })
    ElMessage.success('保存成功')
    dialogVisible.value = false
    fetchData()
  } catch (error) {
    ElMessage.error('保存失败')
  }
}
</script>

<style scoped>
.weight-record { padding: 20px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.weight-summary { display: flex; justify-content: space-around; margin-bottom: 20px; }
.summary-item { text-align: center; }
.summary-value { font-size: 28px; font-weight: 700; color: #409eff; }
.summary-label { font-size: 12px; color: #909399; }
.text-success { color: #67c23a; }
.text-warning { color: #e6a23c; }
.text-danger { color: #f56c6c; }
.bmi-info { text-align: center; }
.bmi-value { font-size: 20px; font-weight: 600; margin-bottom: 8px; }
.bmi-level { font-size: 18px; font-weight: 700; margin-bottom: 16px; }
.bmi-ranges { font-size: 12px; color: #909399; }
.bmi-ranges div { margin: 4px 0; }
</style>