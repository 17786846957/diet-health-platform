import request from './request'

export const getDashboard = () => request.get('/admin/dashboard')
export const getUsers = (params) => request.get('/admin/users', { params })
export const deleteUser = (id) => request.delete(`/admin/users/${id}`)
