import request from './request'

export const getDashboard = () => request.get('/admin/dashboard')
export const getUsers = (params) => request.get('/admin/users', { params })
export const deleteUser = (id) => request.delete(`/admin/users/${id}`)
export const getUserTrend = (params) => request.get('/admin/stats/user-trend', { params })
export const getFoodCategories = () => request.get('/admin/stats/food-categories')
export const getRecordTrend = (params) => request.get('/admin/stats/record-trend', { params })
