<template>
  <div class="symptom-record">
    <el-row :gutter="20">
      <el-col :xs="24" :sm="24" :md="16">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>身体症状记录</span>
              <el-button type="primary" @click="showAddDialog">
                <el-icon><Plus /></el-icon>
                记录症状
              </el-button>
            </div>
          </template>

          <div class="date-range">
            <el-date-picker v-model="dateRange" type="daterange" range-separator="至"
              start-placeholder="开始日期" end-placeholder="结束日期" format="YYYY-MM-DD"
              value-format="YYYY-MM-DD" @change="fetchRecords" />
          </div>

          <el-table :data="symptoms" style="width: 100%">
            <el-table-column prop="recordDate" label="日期" width="120" />
            <el-table-column prop="symptomType" label="症状类型" width="120">
              <template #default="{ row }">
                {{ getSymptomTypeName(row.symptomType) }}
              </template>
            </el-table-column>
            <el-table-column prop="severity" label="严重程度" width="120">
              <template #default="{ row }">
                <el-rate v-model="row.severity" disabled :max="10" />
              </template>
            </el-table-column>
            <el-table-column prop="description" label="描述" />
            <el-table-column prop="possibleCause" label="可能原因" width="150" />
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
            <span>症状分析</span>
          </template>
          <div v-if="analysis.totalRecords > 0">
            <el-descriptions :column="1" border>
              <el-descriptions-item label="记录总数">{{ analysis.totalRecords }}</el-descriptions-item>
              <el-descriptions-item label="平均严重程度">{{ analysis.avgSeverity?.toFixed(1) }}</el-descriptions-item>
            </el-descriptions>
            <div class="top-symptoms" style="margin-top: 16px">
              <h4>常见症状</h4>
              <div v-for="(item, index) in analysis.topSymptoms" :key="index" class="symptom-item">
                <span>{{ getSymptomTypeName(item.type) }}</span>
                <span class="count">{{ item.count }} 次</span>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无分析数据" />
        </el-card>

        <el-card style="margin-top: 20px">
          <template #header>
            <span>常见症状类型</span>
          </template>
          <div class="symptom-types">
            <el-tag v-for="type in commonSymptomTypes" :key="type.value"
              :type="type.color" style="margin: 4px" @click="quickAdd(type.value)">
              {{ type.label }}
            </el-tag>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="dialogVisible" title="记录身体症状" width="500px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="记录日期" prop="recordDate">
          <el-date-picker v-model="form.recordDate" type="date" format="YYYY-MM-DD"
            value-format="YYYY-MM-DD" :clearable="false" />
        </el-form-item>
        <el-form-item label="症状类型" prop="symptomType">
          <el-select v-model="form.symptomType" placeholder="请选择症状类型" filterable>
            <el-option v-for="type in commonSymptomTypes" :key="type.value"
              :value="type.value" :label="type.label" />
          </el-select>
        </el-form-item>
        <el-form-item label="严重程度" prop="severity">
          <el-slider v-model="form.severity" :min="1" :max="10" :step="1" show-stops />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="描述症状的具体表现" />
        </el-form-item>
        <el-form-item label="可能原因">
          <el-input v-model="form.possibleCause" placeholder="您认为可能的原因" />
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
import { ref, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { addSymptom, deleteSymptom, listSymptoms, getSymptomAnalysis } from '../api/symptom'
import { useUserStore } from '../stores/user'
import { getToday, formatDate } from '../utils/date'
import { ElMessage, ElMessageBox } from 'element-plus'

const userStore = useUserStore()
const symptoms = ref([])
const analysis = ref({})
const dialogVisible = ref(false)
const formRef = ref(null)

const today = getToday()
const dateRange = ref([formatDate(new Date(Date.now() - 30 * 24 * 60 * 60 * 1000)), today])

const form = ref({
  recordDate: today,
  symptomType: '',
  severity: 5,
  description: '',
  possibleCause: '',
  memberId: null
})

const rules = {
  recordDate: [{ required: true, message: '请选择记录日期', trigger: 'change' }],
  symptomType: [{ required: true, message: '请选择症状类型', trigger: 'change' }],
  severity: [{ required: true, message: '请设置严重程度', trigger: 'change' }]
}

const commonSymptomTypes = [
  { value: 'headache', label: '头痛', color: '' },
  { value: 'stomachache', label: '胃痛', color: 'warning' },
  { value: 'fatigue', label: '疲劳', color: 'info' },
  { value: 'dizziness', label: '头晕', color: 'danger' },
  { value: 'nausea', label: '恶心', color: 'warning' },
  { value: 'insomnia', label: '失眠', color: '' },
  { value: 'backache', label: '腰痛', color: 'info' },
  { value: 'allergy', label: '过敏', color: 'danger' },
  { value: 'cold', label: '感冒', color: '' },
  { value: 'constipation', label: '便秘', color: 'warning' }
]

onMounted(() => {
  fetchRecords()
  fetchAnalysis()
})

async function fetchRecords() {
  try {
    const memberId = userStore.activeMemberId
    const [start, end] = dateRange.value || []
    symptoms.value = await listSymptoms(start, end, memberId)
  } catch (error) {
    ElMessage.error('获取症状记录失败')
  }
}

async function fetchAnalysis() {
  try {
    const memberId = userStore.activeMemberId
    analysis.value = await getSymptomAnalysis(30, memberId)
  } catch (error) {
    console.error('获取分析数据失败', error)
  }
}

function showAddDialog() {
  form.value = {
    recordDate: today,
    symptomType: '',
    severity: 5,
    description: '',
    possibleCause: '',
    memberId: userStore.activeMemberId
  }
  dialogVisible.value = true
}

function quickAdd(type) {
  form.value = {
    recordDate: today,
    symptomType: type,
    severity: 5,
    description: '',
    possibleCause: '',
    memberId: userStore.activeMemberId
  }
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    try {
      await addSymptom(form.value)
      ElMessage.success('添加成功')
      dialogVisible.value = false
      fetchRecords()
      fetchAnalysis()
    } catch (error) {
      ElMessage.error('添加失败')
    }
  })
}

async function handleDelete(id) {
  try {
    await ElMessageBox.confirm('确定删除这条记录？', '提示')
    await deleteSymptom(id)
    ElMessage.success('删除成功')
    fetchRecords()
    fetchAnalysis()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('删除失败')
  }
}

function getSymptomTypeName(type) {
  const found = commonSymptomTypes.find(t => t.value === type)
  return found ? found.label : type
}
</script>

<style scoped>
.symptom-record { padding: 20px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.date-range { margin-bottom: 20px; }
.symptom-types { display: flex; flex-wrap: wrap; gap: 8px; }
.symptom-item { display: flex; justify-content: space-between; padding: 8px 0; border-bottom: 1px solid #eee; }
.symptom-item:last-child { border-bottom: none; }
.count { color: #909399; font-size: 13px; }
.top-symptoms h4 { margin: 0 0 12px; color: #303133; }
</style>
