import request from './request'

export function addOrUpdateWeight(data) {
  return request({ url: '/weight', method: 'post', data })
}

export function deleteWeight(id) {
  return request({ url: `/weight/${id}`, method: 'delete' })
}

export function getLatestWeight(memberId) {
  return request({ url: '/weight/latest', method: 'get', params: { memberId } })
}

export function getWeightTrend(days, memberId) {
  return request({ url: '/weight/trend', method: 'get', params: { days, memberId } })
}