<template>
  <el-card v-loading="loading">
    <template #header>
      <div class="card-header">
        <span>今日饮食建议</span>
        <el-button text @click="fetchData">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
    </template>

    <div v-if="dailyAdvice" class="advice-content">
      <div class="status-badge" :class="getStatusClass(dailyAdvice.status)">
        {{ dailyAdvice.status }}
      </div>

      <div class="advice-section">
        <h4>建议</h4>
        <ul class="advice-list">
          <li v-for="(item, index) in dailyAdvice.advice" :key="index">{{ item }}</li>
        </ul>
      </div>

      <div class="nutrition-gap-section">
        <h4>营养摄入情况</h4>
        <el-row :gutter="16">
          <el-col :span="6" v-for="(item, key) in dailyAdvice.nutritionGap" :key="key">
            <div class="nutrient-card">
              <div class="nutrient-name">{{ getNutrientName(key) }}</div>
              <div class="nutrient-progress">
                <el-progress
                  :percentage="getProgress(item)"
                  :color="getProgressColor(item)"
                  :stroke-width="8"
                />
              </div>
              <div class="nutrient-detail">
                {{ item.actual }} / {{ item.target }}{{ getNutrientUnit(key) }}
              </div>
            </div>
          </el-col>
        </el-row>
      </div>

      <slot />
    </div>
    <el-empty v-else description="暂无数据" />
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { getDailyAdvice } from '../api/dietAdvice'

const props = defineProps({
  memberId: { type: Number, default: null }
})

const loading = ref(false)
const dailyAdvice = ref(null)

onMounted(() => fetchData())

async function fetchData() {
  loading.value = true
  try {
    const res = await getDailyAdvice(props.memberId)
    if (res.code === 200) dailyAdvice.value = res.data
  } catch {
    ElMessage.error('获取每日建议失败')
  } finally {
    loading.value = false
  }
}

function getStatusClass(status) {
  const map = { '已达标': 'status-success', '接近达标': 'status-warning', '未达标': 'status-danger', '未开始': 'status-info' }
  return map[status] || 'status-info'
}

function getProgress(item) {
  if (!item.target) return 0
  return Math.min(Math.round(item.actual / item.target * 100), 100)
}

function getProgressColor(item) {
  const percent = item.actual / item.target
  if (percent >= 0.9 && percent <= 1.1) return '#67c23a'
  if (percent < 0.9) return '#e6a23c'
  return '#f56c6c'
}

function getNutrientName(key) {
  const map = { calories: '热量', protein: '蛋白质', fat: '脂肪', carbs: '碳水' }
  return map[key] || key
}

function getNutrientUnit(key) {
  return key === 'calories' ? '千卡' : 'g'
}
</script>

<style scoped>
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.advice-content {
  line-height: 1.8;
}

.status-badge {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 4px;
  font-weight: 600;
  margin-bottom: 16px;
}

.status-success { background: #f0f9eb; color: #67c23a; }
.status-warning { background: #fdf6ec; color: #e6a23c; }
.status-danger { background: #fef0f0; color: #f56c6c; }
.status-info { background: #f4f4f5; color: #909399; }

.advice-section,
.nutrition-gap-section {
  margin-top: 20px;
}

.advice-section h4,
.nutrition-gap-section h4 {
  margin-bottom: 12px;
  color: #303133;
}

.advice-list {
  padding-left: 20px;
}

.advice-list li {
  margin-bottom: 8px;
  color: #606266;
}

.nutrient-card {
  text-align: center;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 8px;
}

.nutrient-name {
  font-size: 14px;
  color: #606266;
  margin-bottom: 8px;
}

.nutrient-detail {
  font-size: 12px;
  color: #909399;
  margin-top: 8px;
}
</style>
