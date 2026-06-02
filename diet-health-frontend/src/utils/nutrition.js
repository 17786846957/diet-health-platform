/**
 * 计算BMR（基础代谢率）- Harris-Benedict公式
 */
export function calculateBMR(gender, weight, height, age) {
  if (gender === 'male') {
    return 88.362 + 13.397 * weight + 4.799 * height - 5.677 * age
  } else {
    return 447.593 + 9.247 * weight + 3.098 * height - 4.330 * age
  }
}

/**
 * 活动水平系数
 */
export const activityMap = {
  sedentary: { label: '久坐不动', multiplier: 1.2 },
  light: { label: '轻度活动', multiplier: 1.375 },
  moderate: { label: '中度活动', multiplier: 1.55 },
  active: { label: '高度活动', multiplier: 1.725 },
  very_active: { label: '极高活动', multiplier: 1.9 }
}

/**
 * 健康目标调整
 */
export const goalMap = {
  lose: { label: '减脂', adjustment: -500 },
  maintain: { label: '维持体重', adjustment: 0 },
  gain: { label: '增肌', adjustment: 300 }
}

/**
 * 计算每日目标热量
 */
export function calculateTargetCalories(profile) {
  if (!profile || !profile.weight || !profile.height || !profile.age) {
    return 2000
  }
  const gender = profile.gender || 'male'
  const bmr = calculateBMR(gender, profile.weight, profile.height, profile.age)
  const activity = activityMap[profile.activityLevel] || activityMap.moderate
  const goal = goalMap[profile.goal] || goalMap.maintain
  return Math.round(bmr * activity.multiplier + goal.adjustment)
}
