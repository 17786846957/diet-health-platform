import request from './request'

export const addRecord = (data) => request.post('/diet', data)
export const updateRecord = (data) => request.put('/diet', data)
export const deleteRecord = (id) => request.delete(`/diet/${id}`)
export const getRecordsByDate = (params) => request.get('/diet/list', { params })
export const getDailyStats = (params) => request.get('/diet/daily-stats', { params })
export const getWeeklyStats = (params) => request.get('/diet/weekly-stats', { params })
export const getMonthlyStats = (params) => request.get('/diet/monthly-stats', { params })
export const getNutritionGap = (params) => request.get('/diet/nutrition-gap', { params })
export const getRecommendations = (params) => request.get('/diet/recommend', { params })
export const getRecentFoods = (params) => request.get('/diet/recent-foods', { params })
