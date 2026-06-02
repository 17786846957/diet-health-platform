<template>
  <el-card v-loading="loading">
    <template #header>
      <div class="card-header">
        <span>个性化健康建议</span>
        <el-button text @click="fetchData">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
    </template>

    <div v-if="healthAdvice" class="health-content">
      <div class="goal-badge">
        当前目标：<el-tag>{{ getGoalName(healthAdvice.goal) }}</el-tag>
      </div>

      <div class="health-advice-section">
        <ul class="health-list">
          <li v-for="(item, index) in healthAdvice.advice" :key="index"
              :class="{ 'is-header': !item.startsWith('1') && !item.startsWith('2') && !item.startsWith('3') && !item.startsWith('4') && !item.startsWith('-') }">
            {{ item }}
          </li>
        </ul>
      </div>
    </div>
    <el-empty v-else description="暂无数据" />
  </el-card>
</template>

<script setup>
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { getHealthAdvice } from '../api/dietAdvice'

const props = defineProps({
  memberId: { type: Number, default: null },
  active: { type: Boolean, default: false }
})

const loading = ref(false)
const healthAdvice = ref(null)
const loaded = ref(false)

watch(() => props.active, (val) => {
  if (val && !loaded.value) {
    loaded.value = true
    fetchData()
  }
}, { immediate: true })

async function fetchData() {
  loading.value = true
  try {
    healthAdvice.value = await getHealthAdvice(props.memberId)
  } catch {
    ElMessage.error('获取健康建议失败')
  } finally {
    loading.value = false
  }
}

function getGoalName(goal) {
  const map = {
    lose_weight: '减脂',
    gain_weight: '增重',
    muscle: '增肌',
    health: '保持健康',
    maintain: '保持健康'
  }
  return map[goal] || '未设置'
}
</script>

<style scoped>
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.goal-badge {
  margin-bottom: 16px;
}

.health-advice-section {
  margin-top: 20px;
}

.health-advice-section h4 {
  margin-bottom: 12px;
  color: #303133;
}

.health-list {
  padding-left: 20px;
}

.health-list li {
  margin-bottom: 8px;
  color: #606266;
}

.health-list li.is-header {
  font-weight: 600;
  color: #303133;
  margin-top: 12px;
}
</style>
