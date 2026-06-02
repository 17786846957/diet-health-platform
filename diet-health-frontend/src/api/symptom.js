import request from './request'

export function addSymptom(data) {
  return request({ url: '/symptom', method: 'post', data })
}

export function deleteSymptom(id) {
  return request({ url: `/symptom/${id}`, method: 'delete' })
}

export function listSymptoms(start, end, memberId) {
  return request({ url: '/symptom/list', method: 'get', params: { start, end, memberId } })
}

export function getSymptomAnalysis(days, memberId) {
  return request({ url: '/symptom/analysis', method: 'get', params: { days, memberId } })
}