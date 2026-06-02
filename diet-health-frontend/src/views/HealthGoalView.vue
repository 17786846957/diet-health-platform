<template>
  <div class="health-goal">
    <el-row :gutter="20">
      <el-col :xs="24" :sm="24" :md="16">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>健康目标</span>
              <el-button type="primary" @click="showAddDialog">
                <el-icon><Plus /></el-icon>
                新建目标
              </el-button>
            </div>
          </template>

          <div v-if="activeGoal" class="active-goal">
            <el-descriptions :column="2" border>
              <el-descriptions-item label="目标类型">{{ getGoalTypeName(activeGoal.goalType) }}</el-descriptions-item>
              <el-descriptions-item label="状态">
                <el-tag :type="getStatusType(activeGoal.status)">{{ getStatusName(activeGoal.status) }}</el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="目标体重" v-if="activeGoal.targetWeight">{{ activeGoal.targetWeight }} kg</el-descriptions-item>
              <el-descriptions-item label="目标热量" v-if="activeGoal.targetCalories">{{ activeGoal.targetCalories }} 千卡</el-descriptions-item>
              <el-descriptions-item label="目标蛋白质" v-if="activeGoal.targetProtein">{{ activeGoal.targetProtein }} g</el-descriptions-item>
              <el-descriptions-item label="目标脂肪" v-if="activeGoal.targetFat">{{ activeGoal.targetFat }} g</el-descriptions-item>
              <el-descriptions-item label="目标碳水" v-if="activeGoal.targetCarbs">{{ activeGoal.targetCarbs }} g</el-descriptions-item>
              <el-descriptions-item label="目标饮水" v-if="activeGoal.targetWater">{{ activeGoal.targetWater }} ml</el-descriptions-item>
            </el-descriptions>
            <div class="goal-actions">
              <el-button type="success" @click="handleComplete(activeGoal.id)">完成目标</el-button>
              <el-button type="warning" @click="handleCancel(activeGoal.id)">取消目标</el-button>
            </div>
          </div>

          <el-empty v-else description="暂无活跃目标" />

          <el-divider>历史目标</el-divider>

          <el-table :data="goals" style="width: 100%">
            <el-table-column prop="goalType" label="目标类型" width="120">
              <template #default="{ row }">
                {{ getGoalTypeName(row.goalType) }}
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row.status)" size="small">{{ getStatusName(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="targetWeight" label="目标体重" width="100" />
            <el-table-column prop="targetCalories" label="目标热量" width="100" />
            <el-table-column prop="startDate" label="开始日期" width="120" />
            <el-table-column prop="endDate" label="结束日期" width="120" />
            <el-table-column label="操作" width="120">
              <template #default="{ row }">
                <el-button v-if="row.status === 'active'" type="primary" text @click="handleEdit(row)">编辑</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="24" :md="8">
        <el-card>
          <template #header>
            <span>目标类型说明</span>
          </template>
          <div class="goal-types">
            <div class="goal-type-item">
              <strong>减重</strong>
              <p>设定目标体重，系统会根据您的饮食和运动提供减重建议</p>
            </div>
            <div class="goal-type-item">
              <strong>增肌</strong>
              <p>设定蛋白质和热量目标，帮助您增加肌肉质量</p>
            </div>
            <div class="goal-type-item">
              <strong>维持</strong>
              <p>保持当前体重，均衡摄入各类营养素</p>
            </div>
            <div class="goal-type-item">
              <strong>健康饮食</strong>
              <p>改善饮食习惯，均衡营养摄入</p>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑目标' : '新建健康目标'" width="500px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="目标类型" prop="goalType">
          <el-select v-model="form.goalType" placeholder="请选择目标类型">
            <el-option value="lose_weight" label="减重" />
            <el-option value="gain_muscle" label="增肌" />
            <el-option value="maintain" label="维持" />
            <el-option value="healthy_diet" label="健康饮食" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标体重" v-if="form.goalType === 'lose_weight' || form.goalType === 'gain_muscle'">
          <el-input-number v-model="form.targetWeight" :min="20" :max="300" :step="0.5" :precision="1" />
          <span style="margin-left: 8px">kg</span>
        </el-form-item>
        <el-form-item label="目标热量">
          <el-input-number v-model="form.targetCalories" :min="500" :max="5000" :step="100" />
          <span style="margin-left: 8px">千卡/天</span>
        </el-form-item>
        <el-form-item label="目标蛋白质">
          <el-input-number v-model="form.targetProtein" :min="20" :max="300" :step="10" />
          <span style="margin-left: 8px">g/天</span>
        </el-form-item>
        <el-form-item label="目标脂肪">
          <el-input-number v-model="form.targetFat" :min="10" :max="200" :step="5" />
          <span style="margin-left: 8px">g/天</span>
        </el-form-item>
        <el-form-item label="目标碳水">
          <el-input-number v-model="form.targetCarbs" :min="50" :max="500" :step="10" />
          <span style="margin-left: 8px">g/天</span>
        </el-form-item>
        <el-form-item label="目标饮水">
          <el-input-number v-model="form.targetWater" :min="500" :max="5000" :step="100" />
          <span style="margin-left: 8px">ml/天</span>
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
import { ref, onMounted, computed } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { createGoal, updateGoal, completeGoal, cancelGoal, getActiveGoal, listGoals } from '../api/healthGoal'
import { useUserStore } from '../stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'

const userStore = useUserStore()
const goals = ref([])
const activeGoal = ref(null)
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)

const form = ref({
  id: null,
  goalType: '',
  targetWeight: null,
  targetCalories: null,
  targetProtein: null,
  targetFat: null,
  targetCarbs: null,
  targetWater: null,
  memberId: null
})

const rules = {
  goalType: [{ required: true, message: '请选择目标类型', trigger: 'change' }]
}

onMounted(() => {
  fetchData()
})

async function fetchData() {
  try {
    const memberId = userStore.activeMemberId
    const [activeRes, listRes] = await Promise.all([
      getActiveGoal(memberId),
      listGoals(null, memberId)
    ])
    if (activeRes.code === 200) activeGoal.value = activeRes.data
    if (listRes.code === 200) goals.value = listRes.data
  } catch (error) {
    ElMessage.error('获取目标数据失败')
  }
}

function showAddDialog() {
  isEdit.value = false
  form.value = {
    id: null,
    goalType: '',
    targetWeight: null,
    targetCalories: 2000,
    targetProtein: 60,
    targetFat: 60,
    targetCarbs: 300,
    targetWater: 2000,
    memberId: userStore.activeMemberId
  }
  dialogVisible.value = true
}

function handleEdit(row) {
  isEdit.value = true
  form.value = { ...row, memberId: userStore.activeMemberId }
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    try {
      if (isEdit.value) {
        await updateGoal(form.value)
        ElMessage.success('更新成功')
      } else {
        await createGoal(form.value)
        ElMessage.success('创建成功')
      }
      dialogVisible.value = false
      fetchData()
    } catch (error) {
      ElMessage.error(isEdit.value ? '更新失败' : '创建失败')
    }
  })
}

async function handleComplete(id) {
  try {
    await ElMessageBox.confirm('确定完成此目标？', '提示')
    await completeGoal(id)
    ElMessage.success('目标已完成')
    fetchData()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('操作失败')
  }
}

async function handleCancel(id) {
  try {
    await ElMessageBox.confirm('确定取消此目标？', '提示')
    await cancelGoal(id)
    ElMessage.success('目标已取消')
    fetchData()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('操作失败')
  }
}

function getGoalTypeName(type) {
  const map = { lose_weight: '减重', gain_muscle: '增肌', maintain: '维持', healthy_diet: '健康饮食' }
  return map[type] || type
}

function getStatusName(status) {
  const map = { active: '进行中', completed: '已完成', cancelled: '已取消' }
  return map[status] || status
}

function getStatusType(status) {
  const map = { active: 'primary', completed: 'success', cancelled: 'info' }
  return map[status] || ''
}
</script>

<style scoped>
.health-goal { padding: 20px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.active-goal { margin-bottom: 20px; }
.goal-actions { margin-top: 16px; display: flex; gap: 12px; }
.goal-types { display: flex; flex-direction: column; gap: 16px; }
.goal-type-item strong { color: #409eff; }
.goal-type-item p { margin: 4px 0 0; font-size: 13px; color: #909399; }
</style>
