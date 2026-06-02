<template>
  <div>
    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span>饮食记录</span>
          <div style="display: flex; gap: 12px; align-items: center">
            <el-date-picker :model-value="selectedDate" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" @change="handleDateChange" />
            <el-button type="primary" @click="openAddDialog">新增记录</el-button>
          </div>
        </div>
      </template>

      <!-- 热量预警提示 -->
      <el-alert
        v-if="calorieWarning.show"
        :title="calorieWarning.title"
        :type="calorieWarning.type"
        :closable="false"
        show-icon
        style="margin-bottom: 16px"
      />

      <el-table :data="records" stripe v-loading="tableLoading">
        <el-table-column prop="mealType" label="餐次" width="100">
          <template #default="{ row }">
            {{ mealMap[row.mealType] }}
          </template>
        </el-table-column>
        <el-table-column label="食物明细">
          <template #default="{ row }">
            <div v-for="d in row.details" :key="d.id" style="padding: 2px 0">
              {{ d.foodName }} — {{ d.amount }}g ({{ d.calories }}kcal)
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="totalCalories" label="总热量(kcal)" width="120" />
        <el-table-column label="操作" width="160">
          <template #default="{ row }">
            <el-button type="primary" text size="small" @click="openEditDialog(row)">编辑</el-button>
            <el-popconfirm title="确认删除？" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button type="danger" text size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="records.length" style="margin-top: 16px; text-align: right; color: var(--text-muted)">
        当日总热量: {{ dailyTotal }} kcal
        <span v-if="targetCalories > 0" :style="{ marginLeft: '12px', color: dailyTotal > targetCalories ? '#ef4444' : '#10b981' }">
          目标: {{ targetCalories }} kcal | {{ dailyTotal > targetCalories ? '已超标' + (dailyTotal - targetCalories).toFixed(1) + 'kcal' : '剩余' + (targetCalories - dailyTotal).toFixed(1) + 'kcal' }}
        </span>
      </div>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑记录' : '新增记录'" width="700px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
        <el-form-item label="日期" prop="recordDate">
          <el-date-picker v-model="form.recordDate" type="date" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="餐次" prop="mealType">
          <el-select v-model="form.mealType">
            <el-option v-for="item in mealOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="食物明细">
          <div style="width: 100%">
            <div v-for="(detail, index) in form.details" :key="index" style="display: flex; gap: 8px; margin-bottom: 8px; align-items: center">
              <el-select v-model="detail.foodId" filterable remote :remote-method="v => searchFood(v, index)" placeholder="搜索食物（支持拼音）" style="flex: 2" @change="onFoodChange($event, index)" :loading="foodSearchLoading[index]">
                <el-option-group label="最近食用" v-if="recentFoods.length && !foodSearchKeyword[index]">
                  <el-option v-for="f in recentFoods.slice(0, 5)" :key="'r'+f.foodId" :label="`${f.foodName} (${f.calories}kcal/100g)`" :value="f.foodId">
                    <span>{{ f.foodName }}</span>
                    <span style="float: right; color: var(--text-muted); font-size: 12px">{{ f.calories }}kcal</span>
                  </el-option>
                </el-option-group>
                <el-option-group label="收藏食物" v-if="favoriteFoods.length && !foodSearchKeyword[index]">
                  <el-option v-for="f in favoriteFoods" :key="'f'+f.foodId" :label="`${f.foodName} (${f.calories}kcal/100g)`" :value="f.foodId">
                    <span>{{ f.foodName }}</span>
                    <span style="float: right; color: #f59e0b; font-size: 12px">★</span>
                  </el-option>
                </el-option-group>
                <el-option-group label="搜索结果" v-if="foodOptions[index] && foodOptions[index].length">
                  <el-option v-for="f in foodOptions[index]" :key="f.id" :label="`${f.name} (${f.calories}kcal/100g)`" :value="f.id">
                    <span>{{ f.name }}</span>
                    <span style="float: right; color: var(--text-muted); font-size: 12px">{{ f.calories }}kcal</span>
                    <span style="float: right; margin-right: 8px; cursor: pointer" @click.stop="toggleFavorite(f.id, index)">
                      {{ isFavorite(f.id) ? '★' : '☆' }}
                    </span>
                  </el-option>
                </el-option-group>
              </el-select>
              <el-input-number v-model="detail.amount" :min="1" :max="5000" :step="10" placeholder="食用量(g)" style="flex: 1" />
              <span style="color: var(--text-muted); min-width: 60px">g</span>
              <el-button type="danger" text @click="form.details.splice(index, 1)">删除</el-button>
            </div>
            <el-button @click="addDetail">+ 添加食物</el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import { useDietStore } from '../stores/diet'
import { useUserStore } from '../stores/user'
import { useLoading } from '../composables/useLoading'
import { useDateSelector } from '../composables/useDateSelector'
import { useCalorieWarning } from '../composables/useCalorieWarning'
import { getFoods } from '../api/food'
import { getRecentFoods } from '../api/diet'
import { getFavorites, getFavoriteIds, addFavorite, removeFavorite } from '../api/favorite'
import { getToday } from '../utils/date'
import { mealMap, mealOptions, FALLBACK_ERROR } from '../utils/constants'

const today = getToday()
const dietStore = useDietStore()
const userStore = useUserStore()
const { loading: tableLoading, withLoading: withTableLoading } = useLoading()
const { loading: submitting, withLoading: withSubmitting } = useLoading()
const { selectedDate, handleDateChange } = useDateSelector(loadRecords)

const records = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref()
const foodOptions = ref({})
const foodSearchLoading = ref({})
const foodSearchKeyword = ref({})
const recentFoods = ref([])
const favoriteFoods = ref([])
const favoriteIds = ref(new Set())

const form = ref({
  id: null,
  recordDate: today,
  mealType: 'breakfast',
  details: [{ foodId: null, amount: 100 }]
})

const rules = {
  recordDate: [{ required: true, message: '请选择日期', trigger: 'change' }],
  mealType: [{ required: true, message: '请选择餐次', trigger: 'change' }]
}

const dailyTotal = computed(() => {
  const sum = records.value.reduce((s, r) => s + (r.totalCalories || 0), 0)
  return Math.round(sum * 10) / 10
})

const { calorieWarning, targetCalories } = useCalorieWarning(dailyTotal)

function isFavorite(foodId) {
  return favoriteIds.value.has(foodId)
}

async function toggleFavorite(foodId, index) {
  try {
    if (isFavorite(foodId)) {
      await removeFavorite(foodId)
      favoriteIds.value.delete(foodId)
      favoriteFoods.value = favoriteFoods.value.filter(f => f.foodId !== foodId)
      ElMessage.success('已取消收藏')
    } else {
      await addFavorite(foodId)
      favoriteIds.value.add(foodId)
      const food = (foodOptions.value[index] || []).find(f => f.id === foodId)
      if (food) {
        favoriteFoods.value.unshift({ foodId: food.id, foodName: food.name, calories: food.calories, category: food.category })
      }
      ElMessage.success('已收藏')
    }
    favoriteIds.value = new Set(favoriteIds.value)
  } catch (e) {
    ElMessage.error(e.message || FALLBACK_ERROR)
  }
}

async function loadRecords() {
  await withTableLoading(async () => {
    try {
      records.value = await dietStore.fetchRecords(selectedDate.value)
    } catch (e) {
      ElMessage.error(e.message || FALLBACK_ERROR)
    }
  })
}

async function loadFavoritesAndRecent() {
  try {
    const params = userStore.activeMemberId ? { memberId: userStore.activeMemberId } : {}
    const [favRes, idsRes, recentRes] = await Promise.all([
      getFavorites(),
      getFavoriteIds(),
      getRecentFoods({ days: 7, ...params })
    ])
    favoriteFoods.value = favRes.data || []
    favoriteIds.value = new Set(idsRes.data || [])
    recentFoods.value = recentRes.data || []
  } catch (e) {
    console.warn('加载收藏/最近食物失败:', e)
  }
}

function openAddDialog() {
  isEdit.value = false
  form.value = { id: null, recordDate: selectedDate.value, mealType: 'breakfast', details: [{ foodId: null, amount: 100 }] }
  foodOptions.value = {}
  foodSearchKeyword.value = {}
  dialogVisible.value = true
}

function openEditDialog(row) {
  isEdit.value = true
  form.value = {
    id: row.id,
    recordDate: row.recordDate,
    mealType: row.mealType,
    details: row.details.map(d => ({ foodId: d.foodId, amount: d.amount }))
  }
  foodOptions.value = {}
  foodSearchKeyword.value = {}
  dialogVisible.value = true
}

let searchTimer = null

async function searchFood(keyword, index) {
  foodSearchKeyword.value = { ...foodSearchKeyword.value, [index]: keyword }
  if (!keyword) {
    foodOptions.value = { ...foodOptions.value, [index]: [] }
    return
  }
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(async () => {
    foodSearchLoading.value = { ...foodSearchLoading.value, [index]: true }
    try {
      const res = await getFoods({ keyword, size: 20 })
      foodOptions.value = { ...foodOptions.value, [index]: res.data.records }
    } catch (e) {
      ElMessage.error(e.message || FALLBACK_ERROR)
    } finally {
      foodSearchLoading.value = { ...foodSearchLoading.value, [index]: false }
    }
  }, 300)
}

function onFoodChange(foodId, index) {
  const allOptions = [
    ...(foodOptions.value[index] || []),
    ...recentFoods.value.map(f => ({ id: f.foodId, name: f.foodName })),
    ...favoriteFoods.value.map(f => ({ id: f.foodId, name: f.foodName }))
  ]
  const food = allOptions.find(f => f.id === foodId)
  if (food && form.value.details[index]) {
    form.value.details[index].foodName = food.name || food.foodName
  }
}

function addDetail() {
  form.value.details.push({ foodId: null, amount: 100 })
}

async function handleSubmit() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  const validDetails = form.value.details.filter(d => d.foodId && d.amount > 0)
  if (validDetails.length === 0) {
    ElMessage.warning('请至少添加一种食物')
    return
  }
  await withSubmitting(async () => {
    try {
      const data = { ...form.value, details: validDetails, memberId: userStore.activeMemberId || null }
      if (isEdit.value) {
        await dietStore.editRecord(data)
        ElMessage.success('更新成功')
      } else {
        await dietStore.createRecord(data)
        ElMessage.success('添加成功')
      }
      dialogVisible.value = false
      loadRecords()
    } catch (e) {
      ElMessage.error(e.message || FALLBACK_ERROR)
    }
  })
}

async function handleDelete(id) {
  try {
    await dietStore.removeRecord(id)
    ElMessage.success('删除成功')
    loadRecords()
  } catch (e) {
    ElMessage.error(e.message || FALLBACK_ERROR)
  }
}

onMounted(() => {
  loadRecords()
  loadFavoritesAndRecent()
})

onBeforeUnmount(() => {
  if (searchTimer) clearTimeout(searchTimer)
})
</script>
