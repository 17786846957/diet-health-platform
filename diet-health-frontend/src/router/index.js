import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../stores/user'

const routes = [
  { path: '/login', name: 'Login', component: () => import('../views/LoginView.vue'), meta: { guest: true } },
  { path: '/register', name: 'Register', component: () => import('../views/RegisterView.vue'), meta: { guest: true } },
  {
    path: '/',
    component: () => import('../views/LayoutView.vue'),
    children: [
      { path: '', name: 'Home', component: () => import('../views/HomeView.vue'), meta: { title: '首页' } },
      { path: 'diet', name: 'Diet', component: () => import('../views/DietRecordView.vue'), meta: { title: '饮食记录' } },
      { path: 'food', name: 'Food', component: () => import('../views/FoodLibraryView.vue'), meta: { title: '食物库' } },
      { path: 'stats', name: 'Stats', component: () => import('../views/NutritionStatsView.vue'), meta: { title: '营养统计' } },
      { path: 'diet-advice', name: 'DietAdvice', component: () => import('../views/DietAdviceView.vue'), meta: { title: '智能饮食建议' } },
      { path: 'water', name: 'Water', component: () => import('../views/WaterRecordView.vue'), meta: { title: '饮水记录' } },
      { path: 'exercise', name: 'Exercise', component: () => import('../views/ExerciseRecordView.vue'), meta: { title: '运动记录' } },
      { path: 'weight', name: 'Weight', component: () => import('../views/WeightRecordView.vue'), meta: { title: '体重记录' } },
      { path: 'health-goal', name: 'HealthGoal', component: () => import('../views/HealthGoalView.vue'), meta: { title: '健康目标' } },
      { path: 'symptom', name: 'Symptom', component: () => import('../views/SymptomRecordView.vue'), meta: { title: '身体症状' } },
      { path: 'profile', name: 'Profile', component: () => import('../views/ProfileView.vue'), meta: { title: '个人中心' } },
      { path: 'admin', name: 'Admin', component: () => import('../views/AdminView.vue'), meta: { title: '后台管理', requiresAdmin: true } }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('../views/NotFoundView.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()

  if (to.meta.guest) {
    if (userStore.isLoggedIn) {
      next('/')
    } else {
      next()
    }
    return
  }

  if (!userStore.isLoggedIn) {
    next({ path: '/login', query: { redirect: to.fullPath } })
    return
  }

  if (to.meta.requiresAdmin && !userStore.isAdmin) {
    next('/')
    return
  }

  if (to.meta.title) {
    document.title = `${to.meta.title} - 智能饮食健康管理平台`
  }

  next()
})

export default router
