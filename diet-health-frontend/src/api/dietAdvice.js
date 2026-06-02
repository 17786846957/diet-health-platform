import request from './request'

export function getDailyAdvice(memberId) {
  return request({
    url: '/diet-advice/daily',
    method: 'get',
    params: { memberId }
  })
}

export function getDietAnalysis(days, memberId) {
  return request({
    url: '/diet-advice/analysis',
    method: 'get',
    params: { days, memberId }
  })
}

export function getHealthAdvice(memberId) {
  return request({
    url: '/diet-advice/health',
    method: 'get',
    params: { memberId }
  })
}