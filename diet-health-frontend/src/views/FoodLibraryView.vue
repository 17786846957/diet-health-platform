<template>
  <div>
    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span>食物库</span>
          <div style="display: flex; gap: 12px">
            <el-input v-model="keyword" placeholder="搜索食物（支持拼音，如 jdz 找鸡蛋）" :prefix-icon="Search" clearable style="width: 280px" @clear="loadFoods" @input="onSearchInput" />
            <el-select v-model="category" placeholder="分类" clearable style="width: 120px" @change="loadFoods">
              <el-option v-for="cat in allCategories" :key="cat" :label="cat" :value="cat" />
            </el-select>
            <el-button type="primary" @click="loadFoods">搜索</el-button>
          </div>
        </div>
      </template>

      <el-table :data="displayFoods" stripe v-loading="loading">
        <el-table-column prop="name" label="食物名称" width="150">
          <template #default="{ row }">
            <span>{{ row.name }}</span>
            <el-button text size="small" :type="isFavorite(row.id) ? 'warning' : 'default'" @click="toggleFavorite(row.id, row)" style="margin-left: 4px; padding: 0">
              {{ isFavorite(row.id) ? '★' : '☆' }}
            </el-button>
          </template>
        </el-table-column>
        <el-table-column prop="category" label="分类" width="80" />
        <el-table-column prop="calories" label="热量(kcal)" width="100" />
        <el-table-column prop="protein" label="蛋白质(g)" width="100" />
        <el-table-column prop="fat" label="脂肪(g)" width="80" />
        <el-table-column prop="carbs" label="碳水(g)" width="80" />
        <el-table-column prop="fiber" label="膳食纤维(g)" width="100" />
      </el-table>

      <el-pagination
        v-if="total > 0"
        style="margin-top: 16px; justify-content: flex-end"
        v-model:current-page="page"
        v-model:page-size="size"
        :total="total"
        :page-sizes="[20, 50, 100]"
        layout="total, sizes, prev, pager, next"
        @current-change="loadFoods"
        @size-change="loadFoods"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import { getFoods } from '../api/food'
import { getFavoriteIds, getFavorites, addFavorite, removeFavorite } from '../api/favorite'
import { useLoading } from '../composables/useLoading'
import { foodCategories, FALLBACK_ERROR } from '../utils/constants'
import { Search } from '@element-plus/icons-vue'
import { pinyinMatch } from '../utils/pinyin'

const allCategories = ['收藏', ...foodCategories]
const keyword = ref('')
const category = ref('')
const page = ref(1)
const size = ref(20)
const total = ref(0)
const foods = ref([])
const favoriteIds = ref(new Set())
const { loading, withLoading } = useLoading()
let searchTimer = null

const displayFoods = computed(() => {
  if (!keyword.value) return foods.value
  const kw = keyword.value.toLowerCase()
  const serverMatched = foods.value.filter(f => f.name.toLowerCase().includes(kw))
  if (serverMatched.length > 0) return serverMatched
  return foods.value.filter(f => pinyinMatch(f.name, kw))
})

function isFavorite(foodId) {
  return favoriteIds.value.has(foodId)
}

async function toggleFavorite(foodId, row) {
  try {
    if (isFavorite(foodId)) {
      await removeFavorite(foodId)
      favoriteIds.value.delete(foodId)
      ElMessage.success('已取消收藏')
    } else {
      await addFavorite(foodId)
      favoriteIds.value.add(foodId)
      ElMessage.success('已收藏')
    }
    favoriteIds.value = new Set(favoriteIds.value)
  } catch (e) {
    ElMessage.error(e.message || FALLBACK_ERROR)
  }
}

function onSearchInput() {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    page.value = 1
    loadFoods()
  }, 300)
}

async function loadFoods() {
  await withLoading(async () => {
    try {
      if (category.value === '收藏') {
        const res = await getFavorites()
        foods.value = (res.data || []).map(f => ({
          id: f.foodId,
          name: f.foodName,
          category: f.category,
          calories: f.calories,
          protein: f.protein,
          fat: f.fat,
          carbs: f.carbs,
          fiber: f.fiber
        }))
        total.value = foods.value.length
      } else {
        const res = await getFoods({ page: page.value, size: size.value, keyword: keyword.value, category: category.value })
        foods.value = res.data.records
        total.value = res.data.total
      }
    } catch (e) {
      ElMessage.error(e.message || FALLBACK_ERROR)
    }
  })
}

async function loadFavoriteIds() {
  try {
    const res = await getFavoriteIds()
    favoriteIds.value = new Set(res.data || [])
  } catch (e) {
    // silent
  }
}

onMounted(() => {
  loadFoods()
  loadFavoriteIds()
})

onBeforeUnmount(() => {
  if (searchTimer) clearTimeout(searchTimer)
})
</script>
