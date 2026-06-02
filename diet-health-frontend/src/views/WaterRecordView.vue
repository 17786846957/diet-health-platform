<template>
  <div class="water-record">
    <el-row :gutter="20">
      <el-col :xs="24" :sm="24" :md="16">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>饮水记录</span>
              <el-button type="primary" @click="showAddDialog">
                <el-icon><Plus /></el-icon>
                记录饮水
              </el-button>
            </div>
          </template>

          <div class="date-picker">
            <el-date-picker v-model="selectedDate" type="date" format="YYYY-MM-DD"
              value-format="YYYY-MM-DD" @change="fetchRecords" :clearable="false" />
          </div>

          <div class="water-summary">
            <div class="summary-item">
              <div class="summary-value">{{ dailyStats.totalAmount || 0 }}</div>
              <div class="summary-label">今日饮水(ml)</div>
            </div>
            <div class="summary-item">
              <div class="summary-value">{{ dailyStats.recordCount || 0 }}</div>
              <div class="summary-label">记录次数</div>
            </div>
            <div class="summary-item">
              <div class="summary-value">{{ targetAmount }}</div>
              <div class="summary-label">目标(ml)</div>
            </div>
          </div>

          <el-progress :percentage="progressPercent" :color="progressColor" :stroke-width="12" />

          <el-table :data="dailyStats.records || []" style="margin-top: 20px">
            <el-table-column prop="drinkType" label="类型" width="100">
              <template #default="{ row }">
                {{ getDrinkTypeName(row.drinkType) }}
              </template>
            </el-table-column>
            <el-table-column prop="amount" label="饮水量(ml)" width="120" />
            <el-table-column prop="recordTime" label="时间" width="100" />
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
            <span>快捷添加</span>
          </template>
          <div class="quick-add">
            <el-button v-for="amount in quickAmounts" :key="amount" @click="quickAdd(amount)">
              {{ amount }}ml
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="dialogVisible" title="记录饮水" width="400px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="饮水量">
          <el-input-number v-model="form.amount" :min="50" :max="2000" :step="50" />
          <span style="margin-left: 8px">ml</span>
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.drinkType">
            <el-option value="water" label="水" />
            <el-option value="tea" label="茶" />
            <el-option value="coffee" label="咖啡" />
            <el-option value="juice" label="果汁" />
            <el-option value="milk" label="牛奶" />
            <el-option value="other" label="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间">
          <el-time-picker v-model="form.recordTime" format="HH:mm" value-format="HH:mm" />
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
import { addWaterRecord, deleteWaterRecord, getWaterDailyStats, getWaterWeeklyStats } from '../api/water'
import { useUserStore } from '../stores/user'
import { useCrud } from '../composables/useCrud'
import { getToday } from '../utils/date'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts/core'
import { BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

echarts.use([BarChart, GridComponent, TooltipComponent, CanvasRenderer])

const userStore = useUserStore()
const selectedDate = ref(getToday())
const dailyStats = ref({})
const weeklyChartRef = ref(null)
let weeklyChart = null
const targetAmount = ref(2000)
const quickAmounts = [150, 200, 250, 300, 500]

const { dialogVisible, form, openDialog, handleSubmit, handleDelete } = useCrud({
  entityName: '饮水记录',
  defaultForm: () => ({
    amount: 200,
    drinkType: 'water',
    recordTime: new Date().toTimeString().slice(0, 5)
  }),
  onSubmit: async (data) => {
    await addWaterRecord({ ...data, recordDate: selectedDate.value, memberId: userStore.activeMemberId })
  },
  onDelete: deleteWaterRecord,
  onRefresh: fetchRecords
})

const progressPercent = computed(() => {
  const total = dailyStats.value.totalAmount || 0
  return Math.min(Math.round(total / targetAmount.value * 100), 100)
})

const progressColor = computed(() => {
  if (progressPercent.value >= 100) return '#67c23a'
  if (progressPercent.value >= 60) return '#409eff'
  return '#e6a23c'
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
    const data = await getWaterDailyStats(selectedDate.value, memberId)
    dailyStats.value = data
    fetchWeeklyStats()
  } catch (error) {
    ElMessage.error('获取饮水记录失败')
  }
}

async function fetchWeeklyStats() {
  try {
    const memberId = userStore.activeMemberId
    const data = await getWaterWeeklyStats(null, memberId)
    await nextTick()
    renderWeeklyChart(data)
  } catch (error) {
    console.error('获取周统计失败', error)
  }
}

function renderWeeklyChart(data) {
  if (!weeklyChartRef.value) return
  if (weeklyChart) weeklyChart.dispose()
  weeklyChart = echarts.init(weeklyChartRef.value)
  weeklyChart.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: data.dates.map(d => d.slice(5)) },
    yAxis: { type: 'value', name: 'ml' },
    series: [{
      type: 'bar',
      data: data.amounts,
      itemStyle: { color: '#409eff' }
    }]
  })
}

function showAddDialog() {
  openDialog()
}

async function quickAdd(amount) {
  try {
    const memberId = userStore.activeMemberId
    await addWaterRecord({
      amount,
      drinkType: 'water',
      recordDate: selectedDate.value,
      recordTime: new Date().toTimeString().slice(0, 5),
      memberId
    })
    ElMessage.success(`已添加 ${amount}ml`)
    fetchRecords()
  } catch (error) {
    ElMessage.error('添加失败')
  }
}

function getDrinkTypeName(type) {
  const map = { water: '水', tea: '茶', coffee: '咖啡', juice: '果汁', milk: '牛奶', other: '其他' }
  return map[type] || type
}
</script>

<style scoped>
.water-record { padding: 20px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.date-picker { margin-bottom: 20px; }
.water-summary { display: flex; justify-content: space-around; margin-bottom: 20px; }
.summary-item { text-align: center; }
.summary-value { font-size: 28px; font-weight: 700; color: #409eff; }
.summary-label { font-size: 12px; color: #909399; }
.quick-add { display: flex; flex-wrap: wrap; gap: 8px; }
</style>