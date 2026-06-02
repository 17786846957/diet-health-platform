<template>
  <el-card style="margin-top: 16px">
    <template #header>
      <div style="display: flex; justify-content: space-between; align-items: center">
        <span style="font-weight: 600; color: var(--text-primary)">家庭成员管理</span>
        <el-button type="primary" size="small" @click="openAddMember">添加成员</el-button>
      </div>
    </template>

    <el-table :data="members" stripe v-loading="membersLoading">
      <el-table-column label="头像" width="60">
        <template #default="{ row }">
          <span style="font-size: 24px">{{ row.avatar || '👤' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="name" label="姓名" width="100" />
      <el-table-column label="性别" width="70">
        <template #default="{ row }">{{ row.gender === 'male' ? '男' : row.gender === 'female' ? '女' : '-' }}</template>
      </el-table-column>
      <el-table-column label="年龄" width="70">
        <template #default="{ row }">{{ row.age || '-' }}</template>
      </el-table-column>
      <el-table-column label="身高(cm)" width="90">
        <template #default="{ row }">{{ row.height || '-' }}</template>
      </el-table-column>
      <el-table-column label="体重(kg)" width="90">
        <template #default="{ row }">{{ row.weight || '-' }}</template>
      </el-table-column>
      <el-table-column label="活动水平" width="100">
        <template #default="{ row }">{{ activityLevelMap[row.activityLevel] || '-' }}</template>
      </el-table-column>
      <el-table-column label="健康目标" width="90">
        <template #default="{ row }">{{ goalMap[row.goal]?.label || '-' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="140">
        <template #default="{ row }">
          <el-button type="primary" text size="small" @click="openEditMember(row)">编辑</el-button>
          <el-popconfirm title="删除成员后，该成员的饮食记录将变为主用户记录，确认删除？" @confirm="handleDeleteMember(row.id)">
            <template #reference>
              <el-button type="danger" text size="small">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <div v-if="!members.length && !membersLoading" style="text-align: center; padding: 20px; color: var(--text-muted)">
      暂无家庭成员，点击"添加成员"创建
    </div>

    <el-dialog v-model="memberDialogVisible" :title="isMemberEdit ? '编辑成员' : '添加成员'" width="500px">
      <el-form :model="memberForm" :rules="memberRules" ref="memberFormRef" label-width="100px">
        <el-form-item label="姓名" prop="name">
          <el-input v-model="memberForm.name" placeholder="请输入成员姓名" />
        </el-form-item>
        <el-form-item label="头像">
          <el-select v-model="memberForm.avatar" placeholder="选择头像" clearable>
            <el-option v-for="e in avatarOptions" :key="e" :label="e" :value="e">
              <span style="font-size: 20px">{{ e }}</span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="memberForm.gender">
            <el-radio value="male">男</el-radio>
            <el-radio value="female">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="年龄">
          <el-input-number v-model="memberForm.age" :min="1" :max="150" />
        </el-form-item>
        <el-form-item label="身高(cm)">
          <el-input-number v-model="memberForm.height" :min="50" :max="250" :step="0.1" />
        </el-form-item>
        <el-form-item label="体重(kg)">
          <el-input-number v-model="memberForm.weight" :min="20" :max="300" :step="0.1" />
        </el-form-item>
        <el-form-item label="活动水平">
          <el-select v-model="memberForm.activityLevel">
            <el-option label="久坐不动" value="sedentary" />
            <el-option label="轻度活动" value="light" />
            <el-option label="中度活动" value="moderate" />
            <el-option label="高度活动" value="active" />
            <el-option label="极高活动" value="very_active" />
          </el-select>
        </el-form-item>
        <el-form-item label="健康目标">
          <el-select v-model="memberForm.goal">
            <el-option label="减脂" value="lose" />
            <el-option label="维持体重" value="maintain" />
            <el-option label="增肌" value="gain" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="memberDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="memberSaving" @click="handleMemberSubmit">确定</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useUserStore } from '../stores/user'
import { ElMessage } from 'element-plus'
import { createFamilyMember, updateFamilyMember, deleteFamilyMember } from '../api/familyMember'
import { goalMap } from '../utils/nutrition'
import { FALLBACK_ERROR } from '../utils/constants'

const userStore = useUserStore()

const members = ref([])
const membersLoading = ref(false)
const memberDialogVisible = ref(false)
const isMemberEdit = ref(false)
const memberSaving = ref(false)
const memberFormRef = ref(null)
const editingMemberId = ref(null)

const avatarOptions = ['👨', '👩', '👴', '👵', '👦', '👧', '🧒', '👶', '🧑', '👱', '🧔', '👨‍🦳', '👩‍🦳']

const activityLevelMap = {
  sedentary: '久坐不动',
  light: '轻度活动',
  moderate: '中度活动',
  active: '高度活动',
  very_active: '极高活动'
}

const memberForm = ref({
  name: '', avatar: '', gender: 'male', age: 25,
  height: 170, weight: 65, activityLevel: 'moderate', goal: 'maintain'
})

const memberRules = {
  name: [{ required: true, message: '请输入成员姓名', trigger: 'blur' }]
}

async function loadMembers() {
  membersLoading.value = true
  try {
    members.value = await userStore.fetchFamilyMembers()
  } catch (e) {
    ElMessage.error(e.message || FALLBACK_ERROR)
  } finally {
    membersLoading.value = false
  }
}

function openAddMember() {
  isMemberEdit.value = false
  editingMemberId.value = null
  memberForm.value = {
    name: '', avatar: '', gender: 'male', age: 25,
    height: 170, weight: 65, activityLevel: 'moderate', goal: 'maintain'
  }
  memberDialogVisible.value = true
}

function openEditMember(row) {
  isMemberEdit.value = true
  editingMemberId.value = row.id
  memberForm.value = {
    name: row.name || '',
    avatar: row.avatar || '',
    gender: row.gender || 'male',
    age: row.age || 25,
    height: row.height || 170,
    weight: row.weight || 65,
    activityLevel: row.activityLevel || 'moderate',
    goal: row.goal || 'maintain'
  }
  memberDialogVisible.value = true
}

async function handleMemberSubmit() {
  try {
    await memberFormRef.value.validate()
  } catch {
    return
  }
  memberSaving.value = true
  try {
    if (isMemberEdit.value) {
      await updateFamilyMember(editingMemberId.value, memberForm.value)
      if (userStore.activeMember?.id === editingMemberId.value) {
        const updated = { ...userStore.activeMember, ...memberForm.value }
        userStore.setActiveMember(updated)
      }
      ElMessage.success('更新成功')
    } else {
      await createFamilyMember(memberForm.value)
      ElMessage.success('添加成功')
    }
    memberDialogVisible.value = false
    await loadMembers()
  } catch (e) {
    ElMessage.error(e.message || FALLBACK_ERROR)
  } finally {
    memberSaving.value = false
  }
}

async function handleDeleteMember(id) {
  try {
    await deleteFamilyMember(id)
    ElMessage.success('删除成功')
    if (userStore.activeMember?.id === id) {
      userStore.clearActiveMember()
    }
    await loadMembers()
  } catch (e) {
    ElMessage.error(e.message || FALLBACK_ERROR)
  }
}

onMounted(() => {
  loadMembers()
})
</script>
