<template>
  <div class="exercise-record">
    <el-row :gutter="20">
      <el-col :xs="24" :sm="24" :md="16">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>运动记录</span>
              <el-button type="primary" @click="showAddDialog">
                <el-icon><Plus /></el-icon>
                记录运动
              </el-button>
            </div>
          </template>

          <div class="date-picker">
            <el-date-picker v-model="selectedDate" type="date" format="YYYY-MM-DD"
              value-format="YYYY-MM-DD" @change="fetchRecords" :clearable="false" />
          </div>

          <div class="exercise-summary">
            <div class="summary-item">
              <div class="summary-value">{{ dailyStats.totalDuration || 0 }}</div>
              <div class="summary-label">运动时长(分钟)</div>
            </div>
            <div class="summary-item">
              <div class="summary-value">{{ dailyStats.totalCalories || 0 }}</div>
              <div class="summary-label">消耗热量(千卡)</div>
            </div>
            <div class="summary-item">
              <div class="summary-value">{{ dailyStats.recordCount || 0 }}</div>
              <div class="summary-label">运动次数</div>
            </div>
          </div>

          <el-table :data="dailyStats.records || []" style="margin-top: 20px">
            <el-table-column prop="exerciseType" label="运动类型" width="120" />
            <el-table-column prop="duration" label="时长(分钟)" width="100" />
            <el-table-column prop="caloriesBurned" label="消耗(千卡)" width="100" />
            <el-table-column prop="intensity" label="强度" width="80">
              <template #default="{ row }">
                <el-tag :type="getIntensityType(row.intensity)" size="small">
                  {{ getIntensityName(row.intensity) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="notes" label="备注" />
            <el-table-column label="操作" width="80">
              <template #default="{ row }">
                <el-button type="danger" text @click="handleDelete(row.id)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="24" :md="8">
        <el-card>
          <template #header>
            <span>本周趋势</span>
          </template>
          <div ref="weeklyChartRef" style="height: 250px"></div>
        </el-card>

        <el-card style="margin-top: 20px">
          <template #header>
            <span>常见运动</span>
          </template>
          <div class="quick-exercises">
            <el-button v-for="ex in commonExercises" :key="ex" @click="quickAdd(ex)">
              {{ ex }}
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="dialogVisible" title="记录运动" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="运动类型">
          <el-input v-model="form.exerciseType" placeholder="如：跑步、游泳、瑜伽" />
        </el-form-item>
        <el-form-item label="时长">
          <el-input-number v-model="form.duration" :min="5" :max="600" :step="5" />
          <span style="margin-left: 8px">分钟</span>
        </el-form-item>
        <el-form-item label="强度">
          <el-radio-group v-model="form.intensity">
            <el-radio value="low">低</el-radio>
            <el-radio value="moderate">中</el-radio>
            <el-radio value="high">高</el-radio>
          </el-radio-group>
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
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { addExerciseRecord, deleteExerciseRecord, getExerciseDailyStats, getExerciseWeeklyStats } from '../api/exercise'
import { useUserStore } from '../stores/user'
import { useCrud } from '../composables/useCrud'
import { getToday } from '../utils/date'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts/core'
import { BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

echarts.use([BarChart, GridComponent, TooltipComponent, LegendComponent, CanvasRenderer])

const userStore = useUserStore()
const selectedDate = ref(getToday())
const dailyStats = ref({})
const weeklyChartRef = ref(null)
let weeklyChart = null
const commonExercises = ['跑步', '快走', '游泳', '骑行', '瑜伽', '健身', '跳绳', '球类']

const { dialogVisible, form, openDialog, handleSubmit, handleDelete } = useCrud({
  entityName: '运动记录',
  defaultForm: () => ({ exerciseType: '', duration: 30, intensity: 'moderate', notes: '' }),
  onSubmit: async (data) => {
    if (!data.exerciseType) throw new Error('请输入运动类型')
    await addExerciseRecord({ ...data, recordDate: selectedDate.value, memberId: userStore.activeMemberId })
  },
  onDelete: deleteExerciseRecord,
  onRefresh: fetchRecords
})

onMounted(() => {
  fetchRecords()
})

onBeforeUnmount(() => {
  if (weeklyChart) {
    weeklyChart.dispose()
    weeklyChart = null
  }
})

async function fetchRecords() {
  try {
    const memberId = userStore.activeMemberId
    const res = await getExerciseDailyStats(selectedDate.value, memberId)
    if (res.code === 200) {
      dailyStats.value = res.data
    }
    fetchWeeklyStats()
  } catch (error) {
    ElMessage.error('获取运动记录失败')
  }
}

async function fetchWeeklyStats() {
  try {
    const memberId = userStore.activeMemberId
    const res = await getExerciseWeeklyStats(null, memberId)
    if (res.code === 200) {
      await nextTick()
      renderWeeklyChart(res.data)
    }
  } catch (error) {
    ElMessage.error('获取周统计失败')
  }
}

function renderWeeklyChart(data) {
  if (!weeklyChartRef.value) return
  if (weeklyChart) weeklyChart.dispose()
  weeklyChart = echarts.init(weeklyChartRef.value)
  weeklyChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['时长(分钟)', '消耗(千卡)'] },
    xAxis: { type: 'category', data: data.dates.map(d => d.slice(5)) },
    yAxis: { type: 'value' },
    series: [
      { name: '时长(分钟)', type: 'bar', data: data.durations, itemStyle: { color: '#409eff' } },
      { name: '消耗(千卡)', type: 'bar', data: data.calories, itemStyle: { color: '#67c23a' } }
    ]
  })
}

function showAddDialog() {
  openDialog()
}

async function quickAdd(type) {
  openDialog({ exerciseType: type, duration: 30, intensity: 'moderate', notes: '' })
}

function getIntensityType(intensity) {
  return { low: 'info', moderate: 'warning', high: 'danger' }[intensity] || 'info'
}

function getIntensityName(intensity) {
  return { low: '低', moderate: '中', high: '高' }[intensity] || intensity
}
</script>

<style scoped>
.exercise-record { padding: 20px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.date-picker { margin-bottom: 20px; }
.exercise-summary { display: flex; justify-content: space-around; margin-bottom: 20px; }
.summary-item { text-align: center; }
.summary-value { font-size: 28px; font-weight: 700; color: #67c23a; }
.summary-label { font-size: 12px; color: #909399; }
.quick-exercises { display: flex; flex-wrap: wrap; gap: 8px; }
</style>