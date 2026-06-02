import request from './request'

export function addWaterRecord(data) {
  return request({ url: '/water', method: 'post', data })
}

export function deleteWaterRecord(id) {
  return request({ url: `/water/${id}`, method: 'delete' })
}

export function getWaterList(date, memberId) {
  return request({ url: '/water/list', method: 'get', params: { date, memberId } })
}

export function getWaterDailyStats(date, memberId) {
  return request({ url: '/water/daily-stats', method: 'get', params: { date, memberId } })
}

export function getWaterWeeklyStats(start, memberId) {
  return request({ url: '/water/weekly-stats', method: 'get', params: { start, memberId } })
}