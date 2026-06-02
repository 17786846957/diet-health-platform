<template>
  <div>
    <el-row :gutter="16" style="margin-bottom: 20px" v-loading="dashboardLoading">
      <el-col :span="8">
        <div class="admin-stat">
          <div class="admin-stat__value" style="color: var(--color-primary)">{{ dashboard.totalUsers || 0 }}</div>
          <div class="admin-stat__label">注册用户数</div>
        </div>
      </el-col>
      <el-col :span="8">
        <div class="admin-stat">
          <div class="admin-stat__value" style="color: var(--color-protein)">{{ dashboard.totalFoods || 0 }}</div>
          <div class="admin-stat__label">食物种类数</div>
        </div>
      </el-col>
      <el-col :span="8">
        <div class="admin-stat">
          <div class="admin-stat__value" style="color: var(--color-carbs)">{{ dashboard.totalRecords || 0 }}</div>
          <div class="admin-stat__label">饮食记录数</div>
        </div>
      </el-col>
    </el-row>

    <el-tabs type="border-card">
      <el-tab-pane label="用户管理" name="users">
        <el-table :data="users" stripe v-loading="usersLoading">
          <el-table-column prop="id" label="ID" width="60" />
          <el-table-column prop="username" label="用户名" width="120" />
          <el-table-column prop="email" label="邮箱" width="180" />
          <el-table-column prop="role" label="角色" width="80">
            <template #default="{ row }">
              <el-tag :type="row.role === 'admin' ? 'danger' : 'info'" size="small">{{ row.role }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="注册时间" width="180" />
          <el-table-column label="操作" width="100">
            <template #default="{ row }">
              <el-popconfirm title="确认删除该用户？" @confirm="handleDeleteUser(row.id)" v-if="row.role !== 'admin'">
                <template #reference>
                  <el-button type="danger" text size="small">删除</el-button>
                </template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination
          v-if="userTotal > 0"
          style="margin-top: 16px; justify-content: flex-end"
          v-model:current-page="userPage"
          :page-size="20"
          :total="userTotal"
          layout="total, prev, pager, next"
          @current-change="loadUsers"
        />
      </el-tab-pane>

      <el-tab-pane label="食物管理" name="foods">
        <div style="margin-bottom: 16px; display: flex; gap: 12px;">
          <el-input v-model="foodKeyword" placeholder="搜索食物名称" style="width: 200px" clearable @clear="loadFoods" @keyup.enter="loadFoods" />
          <el-select v-model="foodCategory" placeholder="分类筛选" clearable @change="loadFoods" style="width: 120px">
            <el-option v-for="cat in foodCategories" :key="cat" :label="cat" :value="cat" />
          </el-select>
          <el-button type="primary" @click="loadFoods">搜索</el-button>
          <el-button type="success" @click="openFoodDialog()">新增食物</el-button>
        </div>
        <el-table :data="foods" v-loading="foodLoading" stripe>
          <el-table-column prop="name" label="名称" width="120" />
          <el-table-column prop="category" label="分类" width="80" />
          <el-table-column prop="calories" label="热量(kcal)" width="100" />
          <el-table-column prop="protein" label="蛋白质(g)" width="100" />
          <el-table-column prop="fat" label="脂肪(g)" width="80" />
          <el-table-column prop="carbs" label="碳水(g)" width="80" />
          <el-table-column prop="fiber" label="纤维(g)" width="80" />
          <el-table-column label="操作" width="160">
            <template #default="{ row }">
              <el-button size="small" @click="openFoodDialog(row)">编辑</el-button>
              <el-popconfirm title="确认删除？" @confirm="handleFoodDelete(row.id)">
                <template #reference>
                  <el-button size="small" type="danger">删除</el-button>
                </template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination
          v-model:current-page="foodPage"
          v-model:page-size="foodSize"
          :total="foodTotal"
          layout="total, prev, pager, next"
          @current-change="loadFoods"
          style="margin-top: 16px; justify-content: center;"
        />
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="foodDialogVisible" :title="foodForm.id ? '编辑食物' : '新增食物'" width="500px">
      <el-form :model="foodForm" :rules="foodRules" ref="foodFormRef" label-width="80px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="foodForm.name" />
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-select v-model="foodForm.category" placeholder="选择分类">
            <el-option v-for="cat in foodCategories" :key="cat" :label="cat" :value="cat" />
          </el-select>
        </el-form-item>
        <el-form-item label="热量(kcal)" prop="calories">
          <el-input-number v-model="foodForm.calories" :min="0" :precision="1" />
        </el-form-item>
        <el-form-item label="蛋白质(g)" prop="protein">
          <el-input-number v-model="foodForm.protein" :min="0" :precision="1" />
        </el-form-item>
        <el-form-item label="脂肪(g)" prop="fat">
          <el-input-number v-model="foodForm.fat" :min="0" :precision="1" />
        </el-form-item>
        <el-form-item label="碳水(g)" prop="carbs">
          <el-input-number v-model="foodForm.carbs" :min="0" :precision="1" />
        </el-form-item>
        <el-form-item label="纤维(g)" prop="fiber">
          <el-input-number v-model="foodForm.fiber" :min="0" :precision="1" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="foodDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleFoodSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getDashboard, getUsers, deleteUser } from '../api/admin'
import { getFoods, addFood, updateFood, deleteFood } from '../api/food'
import { ElMessage } from 'element-plus'
import { foodCategories, FALLBACK_ERROR } from '../utils/constants'

const dashboard = ref({})
const users = ref([])
const userPage = ref(1)
const userTotal = ref(0)

// 食物管理
const foods = ref([])
const foodTotal = ref(0)
const foodPage = ref(1)
const foodSize = ref(10)
const foodKeyword = ref('')
const foodCategory = ref('')
const foodDialogVisible = ref(false)
const foodForm = ref({ id: null, name: '', category: '', calories: 0, protein: 0, fat: 0, carbs: 0, fiber: 0 })
const foodLoading = ref(false)
const dashboardLoading = ref(false)
const usersLoading = ref(false)
const foodFormRef = ref(null)

const foodRules = {
  name: [{ required: true, message: '请输入食物名称', trigger: 'blur' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }],
  calories: [{ required: true, message: '请输入热量', trigger: 'blur' }]
}

async function loadDashboard() {
  dashboardLoading.value = true
  try {
    const res = await getDashboard()
    dashboard.value = res.data
  } catch (e) {
    ElMessage.error(e.message || FALLBACK_ERROR)
  } finally {
    dashboardLoading.value = false
  }
}

async function loadUsers() {
  usersLoading.value = true
  try {
    const res = await getUsers({ page: userPage.value, size: 20 })
    users.value = res.data.records
    userTotal.value = res.data.total
  } catch (e) {
    ElMessage.error(e.message || FALLBACK_ERROR)
  } finally {
    usersLoading.value = false
  }
}

async function handleDeleteUser(id) {
  try {
    await deleteUser(id)
    ElMessage.success('删除成功')
    loadUsers()
    loadDashboard()
  } catch (e) {
    ElMessage.error(e.message || FALLBACK_ERROR)
  }
}

const loadFoods = async () => {
  foodLoading.value = true
  try {
    const res = await getFoods({ page: foodPage.value, size: foodSize.value, keyword: foodKeyword.value || undefined, category: foodCategory.value || undefined })
    foods.value = res.data.records
    foodTotal.value = res.data.total
  } catch (e) {
    ElMessage.error(e.message || FALLBACK_ERROR)
  } finally {
    foodLoading.value = false
  }
}

const openFoodDialog = (food = null) => {
  if (food) {
    foodForm.value = { ...food }
  } else {
    foodForm.value = { id: null, name: '', category: '', calories: 0, protein: 0, fat: 0, carbs: 0, fiber: 0 }
  }
  foodDialogVisible.value = true
}

const handleFoodSubmit = async () => {
  try {
    await foodFormRef.value.validate()
  } catch {
    return
  }
  try {
    if (foodForm.value.id) {
      await updateFood(foodForm.value)
    } else {
      await addFood(foodForm.value)
    }
    ElMessage.success('操作成功')
    foodDialogVisible.value = false
    loadFoods()
  } catch (e) {
    ElMessage.error(e.message || FALLBACK_ERROR)
  }
}

const handleFoodDelete = async (id) => {
  try {
    await deleteFood(id)
    ElMessage.success('删除成功')
    loadFoods()
  } catch (e) {
    ElMessage.error(e.message || FALLBACK_ERROR)
  }
}

onMounted(() => {
  loadDashboard()
  loadUsers()
  loadFoods()
})
</script>

<style scoped>
.admin-stat {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-card);
  padding: 20px;
  text-align: center;
}
.admin-stat__value {
  font-size: 36px;
  font-weight: 700;
}
.admin-stat__label {
  color: var(--text-muted);
  margin-top: 8px;
  font-size: 14px;
}
</style>
