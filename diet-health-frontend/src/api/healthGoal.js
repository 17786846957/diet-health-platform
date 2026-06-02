import request from './request'

export function createGoal(data) {
  return request({ url: '/health-goal', method: 'post', data })
}

export function updateGoal(data) {
  return request({ url: '/health-goal', method: 'put', data })
}

export function completeGoal(id) {
  return request({ url: `/health-goal/${id}/complete`, method: 'post' })
}

export function cancelGoal(id) {
  return request({ url: `/health-goal/${id}/cancel`, method: 'post' })
}

export function getActiveGoal(memberId) {
  return request({ url: '/health-goal/active', method: 'get', params: { memberId } })
}

export function listGoals(status, memberId) {
  return request({ url: '/health-goal/list', method: 'get', params: { status, memberId } })
}