<template>
  <div class="diet-advice">
    <el-tabs v-model="activeTab">
      <!-- 每日建议 -->
      <el-tab-pane label="每日建议" name="daily">
        <DailyAdviceTab :member-id="memberId">
          <div class="recommend-section">
            <div class="recommend-banner">
              <div class="recommend-banner-bg"></div>
              <div class="recommend-banner-content">
                <div class="recommend-banner-left">
                  <div class="recommend-icon">🍽️</div>
                  <div>
                    <h3 class="recommend-title">智能推荐</h3>
                    <p class="recommend-subtitle">{{ getMealName(recommendMeal) }} · 基于你的营养缺口和饮食偏好</p>
                  </div>
                </div>
                <div class="recommend-banner-right">
                  <el-radio-group v-model="recommendMeal" size="small" @change="loadRecommendations">
                    <el-radio-button value="breakfast">🌅 早餐</el-radio-button>
                    <el-radio-button value="lunch">☀️ 午餐</el-radio-button>
                    <el-radio-button value="dinner">🌙 晚餐</el-radio-button>
                    <el-radio-button value="snack">🍪 加餐</el-radio-button>
                  </el-radio-group>
                  <el-button circle :loading="recommendLoading" @click="loadRecommendations" class="refresh-btn">
                    <el-icon><Refresh /></el-icon>
                  </el-button>
                </div>
              </div>
            </div>

            <div v-loading="recommendLoading" class="recommend-body">
              <div v-if="recommendations.length">
                <div class="recommend-hero" v-if="recommendations[0]">
                  <div class="recommend-hero-rank">TOP 1</div>
                  <div class="recommend-hero-content">
                    <div class="recommend-hero-left">
                      <div class="recommend-hero-name">{{ recommendations[0].foodName }}</div>
                      <div class="recommend-hero-cal">{{ recommendations[0].calories }} <span>千卡/100g</span></div>
                      <div class="recommend-hero-reason">
                        <span class="reason-icon">💡</span>
                        {{ recommendations[0].reason }}
                      </div>
                    </div>
                    <div class="recommend-hero-right">
                      <div class="score-ring" :style="{ '--pct': Math.round(recommendations[0].score * 100) }">
                        <svg viewBox="0 0 100 100">
                          <circle class="score-ring-bg" cx="50" cy="50" r="42" />
                          <circle class="score-ring-fill" cx="50" cy="50" r="42"
                            :style="{ stroke: getScoreColor(recommendations[0].score) }" />
                        </svg>
                        <div class="score-ring-text">
                          <span class="score-num">{{ Math.round(recommendations[0].score * 100) }}</span>
                          <span class="score-label">推荐分</span>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>

                <div class="recommend-grid">
                  <div
                    v-for="(item, index) in recommendations.slice(1)"
                    :key="item.foodId"
                    class="recommend-card"
                    :style="{ animationDelay: (index * 0.06) + 's' }"
                  >
                    <div class="card-top">
                      <span class="card-rank" :class="getRankClass(index + 1)">#{{ index + 2 }}</span>
                      <span class="card-score-bar">
                        <span class="card-score-fill" :style="{ width: (item.score * 100) + '%', background: getScoreColor(item.score) }"></span>
                      </span>
                    </div>
                    <div class="card-name">{{ item.foodName }}</div>
                    <div class="card-cal">
                      <span class="cal-num">{{ item.calories }}</span>
                      <span class="cal-unit">千卡/100g</span>
                    </div>
                    <div class="card-reason">{{ item.reason }}</div>
                  </div>
                </div>
              </div>
              <div v-else class="recommend-empty">
                <div class="empty-icon">🥗</div>
                <div class="empty-text">暂无推荐，请先记录今日饮食</div>
              </div>
            </div>
          </div>
        </DailyAdviceTab>
      </el-tab-pane>

      <!-- 饮食分析 -->
      <el-tab-pane label="饮食分析" name="analysis">
        <DietAnalysisTab :member-id="memberId" :active="activeTab === 'analysis'" />
      </el-tab-pane>

      <!-- 健康建议 -->
      <el-tab-pane label="健康建议" name="health">
        <HealthAdviceTab :member-id="memberId" :active="activeTab === 'health'" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { getRecommendations } from '../api/diet'
import { useUserStore } from '../stores/user'
import { mealMap } from '../utils/constants'
import DailyAdviceTab from '../components/DailyAdviceTab.vue'
import DietAnalysisTab from '../components/DietAnalysisTab.vue'
import HealthAdviceTab from '../components/HealthAdviceTab.vue'
import '../styles/diet-recommend.css'

const userStore = useUserStore()
const memberId = computed(() => userStore.activeMemberId)
const activeTab = ref('daily')

const recommendMeal = ref('lunch')
const recommendLoading = ref(false)
const recommendations = ref([])

onMounted(() => {
  const hour = new Date().getHours()
  if (hour < 10) recommendMeal.value = 'breakfast'
  else if (hour < 14) recommendMeal.value = 'lunch'
  else if (hour < 17) recommendMeal.value = 'snack'
  else recommendMeal.value = 'dinner'
  loadRecommendations()
})

async function loadRecommendations() {
  recommendLoading.value = true
  try {
    const params = { mealType: recommendMeal.value }
    if (userStore.activeMemberId) params.memberId = userStore.activeMemberId
    const res = await getRecommendations(params)
    recommendations.value = res.data || []
  } catch (e) {
    console.warn('加载推荐失败:', e)
  } finally {
    recommendLoading.value = false
  }
}

function getMealName(type) {
  return mealMap[type] || type
}

function getRankClass(index) {
  if (index === 1) return 'rank-silver'
  if (index === 2) return 'rank-bronze'
  return ''
}

function getScoreColor(score) {
  if (score > 0.7) return '#22c55e'
  if (score > 0.5) return '#f59e0b'
  if (score > 0.3) return '#3b82f6'
  return '#94a3b8'
}
</script>

<style scoped>
.diet-advice {
  padding: 20px;
}
</style>
