import { computed } from 'vue'
import { useUserStore } from '../stores/user'
import { calculateTargetCalories } from '../utils/nutrition'

/**
 * 热量预警 composable
 * @param {Ref|ComputedRef} totalCalories - 当日已摄入总热量
 * @returns {{ calorieWarning: ComputedRef<{show: boolean, type?: string, title?: string}> }}
 */
export function useCalorieWarning(totalCalories) {
  const userStore = useUserStore()
  const targetCalories = computed(() => calculateTargetCalories(userStore.activeProfile))

  const calorieWarning = computed(() => {
    const total = totalCalories.value
    const target = targetCalories.value
    if (target <= 0) return { show: false }
    const pct = total / target * 100
    if (pct >= 100) {
      return {
        show: true,
        type: 'error',
        title: `热量超标！已摄入 ${total.toFixed(1)} kcal，超出目标 ${(total - target).toFixed(1)} kcal`
      }
    }
    if (pct >= 80) {
      return {
        show: true,
        type: 'warning',
        title: `热量预警：已摄入 ${total.toFixed(1)} kcal，已达目标的 ${pct.toFixed(0)}%`
      }
    }
    return { show: false }
  })

  return { calorieWarning, targetCalories }
}
