import { defineStore } from 'pinia'
import { ref } from 'vue'
import { useUserStore } from './user'
import { getRecordsByDate, addRecord, updateRecord, deleteRecord, getDailyStats, getWeeklyStats, getNutritionGap } from '../api/diet'
import { getToday } from '../utils/date'

export const useDietStore = defineStore('diet', () => {
  const records = ref([])
  const dailyStats = ref({})
  const weeklyStats = ref({})
  const nutritionGaps = ref([])

  function memberParams(extra = {}) {
    const userStore = useUserStore()
    const params = { ...extra }
    if (userStore.activeMemberId) params.memberId = userStore.activeMemberId
    return params
  }

  async function fetchRecords(date) {
    const res = await getRecordsByDate(memberParams({ date: date || getToday() }))
    records.value = res.data
    return res.data
  }

  async function fetchDailyStats(date) {
    const res = await getDailyStats(memberParams({ date }))
    dailyStats.value = res.data
    return res.data
  }

  async function fetchWeeklyStats(start, end) {
    const res = await getWeeklyStats(memberParams({ start, end }))
    weeklyStats.value = res.data
    return res.data
  }

  async function fetchNutritionGap(date) {
    const res = await getNutritionGap(memberParams({ date }))
    nutritionGaps.value = res.data.gaps || []
    return res.data
  }

  async function createRecord(data) {
    const res = await addRecord(data)
    return res
  }

  async function editRecord(data) {
    const res = await updateRecord(data)
    return res
  }

  async function removeRecord(id) {
    const res = await deleteRecord(id)
    return res
  }

  return {
    records, dailyStats, weeklyStats, nutritionGaps,
    fetchRecords, fetchDailyStats, fetchWeeklyStats, fetchNutritionGap,
    createRecord, editRecord, removeRecord
  }
})
