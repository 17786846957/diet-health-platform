<template>
  <div>
    <el-row :gutter="16">
      <el-col :xs="24" :sm="14">
        <el-card v-loading="profileLoading" style="margin-bottom: 16px">
          <template #header>
            <span style="font-weight: 600; color: var(--text-primary)">
              {{ isMemberView ? (form.avatar || '') + ' ' + form.name + ' 的信息' : '个人信息' }}
            </span>
          </template>
          <el-form :model="form" :rules="rules" ref="formRef" label-width="100px" style="max-width: 500px">
            <el-form-item :label="isMemberView ? '姓名' : '用户名'">
              <el-input v-if="isMemberView" v-model="form.name" />
              <el-input v-else :value="form.username" disabled />
            </el-form-item>
            <el-form-item v-if="!isMemberView" label="邮箱">
              <el-input v-model="form.email" />
            </el-form-item>
            <el-form-item label="性别">
              <el-radio-group v-model="form.gender">
                <el-radio value="male">男</el-radio>
                <el-radio value="female">女</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="年龄">
              <el-input-number v-model="form.age" :min="1" :max="120" />
            </el-form-item>
            <el-form-item label="身高(cm)">
              <el-input-number v-model="form.height" :min="50" :max="250" :step="0.1" />
            </el-form-item>
            <el-form-item label="体重(kg)">
              <el-input-number v-model="form.weight" :min="20" :max="300" :step="0.1" />
            </el-form-item>
            <el-form-item label="活动水平">
              <el-select v-model="form.activityLevel">
                <el-option label="久坐不动" value="sedentary" />
                <el-option label="轻度活动" value="light" />
                <el-option label="中度活动" value="moderate" />
                <el-option label="高度活动" value="active" />
                <el-option label="极高活动" value="very_active" />
              </el-select>
            </el-form-item>
            <el-form-item label="健康目标">
              <el-select v-model="form.goal">
                <el-option label="减脂" value="lose" />
                <el-option label="维持体重" value="maintain" />
                <el-option label="增肌" value="gain" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="saving" @click="handleSave">保存修改</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="10">
        <el-card>
          <template #header><span style="font-weight: 600; color: var(--text-primary)">每日热量推荐</span></template>
          <div class="calorie-recommend">
            <div class="calorie-value">{{ targetCalories }}</div>
            <div class="calorie-unit">kcal / 天</div>
            <div class="calorie-detail">
              <p>计算公式: Harris-Benedict</p>
              <p>基础代谢率(BMR): {{ bmr }} kcal</p>
              <p>活动系数: {{ activityLabel }}</p>
              <p>目标系数: {{ goalLabel }}</p>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 家庭成员管理 -->
    <FamilyMemberCard />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useUserStore } from '../stores/user'
import { ElMessage } from 'element-plus'
import { updateProfile } from '../api/user'
import { getFamilyMember, updateFamilyMember } from '../api/familyMember'
import { calculateTargetCalories, calculateBMR, activityMap, goalMap } from '../utils/nutrition'
import { FALLBACK_ERROR } from '../utils/constants'
import FamilyMemberCard from '../components/FamilyMemberCard.vue'

const userStore = useUserStore()
const saving = ref(false)
const profileLoading = ref(false)
const formRef = ref(null)
const isMemberView = computed(() => !!userStore.activeMemberId)
const form = ref({
  username: '', name: '', email: '', gender: 'male', age: 25,
  height: 170, weight: 65, activityLevel: 'moderate', goal: 'maintain'
})

const rules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  email: [{ type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }],
  age: [{ type: 'number', min: 1, max: 150, message: '年龄范围1-150', trigger: 'blur' }],
  height: [{ type: 'number', min: 50, max: 250, message: '身高范围50-250cm', trigger: 'blur' }],
  weight: [{ type: 'number', min: 20, max: 300, message: '体重范围20-300kg', trigger: 'blur' }]
}

const activityLabel = computed(() => {
  const item = activityMap[form.value.activityLevel]
  return item ? `${item.label}(×${item.multiplier})` : ''
})
const goalLabel = computed(() => {
  const item = goalMap[form.value.goal]
  return item ? `${item.label}(${item.adjustment >= 0 ? '+' : ''}${item.adjustment}kcal)` : ''
})

const bmr = computed(() => {
  const { gender, weight, height, age } = form.value
  if (!weight || !height || !age) return 0
  return Math.round(calculateBMR(gender, weight, height, age))
})

const targetCalories = computed(() => calculateTargetCalories(form.value))

async function handleSave() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  saving.value = true
  try {
    if (isMemberView.value) {
      await updateFamilyMember(userStore.activeMemberId, form.value)
      // 更新 store 中的活跃成员数据
      const updated = { ...userStore.activeMember, ...form.value }
      userStore.setActiveMember(updated)
      await userStore.fetchFamilyMembers()
    } else {
      await updateProfile(form.value)
      await userStore.fetchProfile()
    }
    ElMessage.success('保存成功')
  } catch (e) {
    ElMessage.error(e.message || FALLBACK_ERROR)
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  profileLoading.value = true
  try {
    if (isMemberView.value) {
      // 加载家庭成员信息
      const member = await getFamilyMember(userStore.activeMemberId)
      Object.assign(form.value, member.data)
    } else {
      const profile = await userStore.fetchProfile()
      Object.assign(form.value, profile)
    }
  } catch (e) {
    ElMessage.error(e.message || FALLBACK_ERROR)
  } finally {
    profileLoading.value = false
  }
})
</script>

<style scoped>
.calorie-recommend {
  text-align: center;
  padding: 20px 0;
}
.calorie-value {
  font-size: 48px;
  font-weight: 700;
  color: var(--color-primary);
}
.calorie-unit {
  color: var(--text-muted);
  margin-top: 8px;
  font-size: 14px;
}
.calorie-detail {
  margin-top: 20px;
  color: var(--text-secondary);
  font-size: 14px;
  text-align: left;
  line-height: 1.8;
  padding: 0 10px;
}
</style>
