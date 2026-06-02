import request from './request'

export function addExerciseRecord(data) {
  return request({ url: '/exercise', method: 'post', data })
}

export function updateExerciseRecord(data) {
  return request({ url: '/exercise', method: 'put', data })
}

export function deleteExerciseRecord(id) {
  return request({ url: `/exercise/${id}`, method: 'delete' })
}

export function getExerciseList(date, memberId) {
  return request({ url: '/exercise/list', method: 'get', params: { date, memberId } })
}

export function getExerciseDailyStats(date, memberId) {
  return request({ url: '/exercise/daily-stats', method: 'get', params: { date, memberId } })
}

export function getExerciseWeeklyStats(start, memberId) {
  return request({ url: '/exercise/weekly-stats', method: 'get', params: { start, memberId } })
}