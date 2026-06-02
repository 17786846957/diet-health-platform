import request from './request'

export const getFoods = (params) => request.get('/food/list', { params })
export const addFood = (data) => request.post('/food', data)
export const updateFood = (data) => request.put('/food', data)
export const deleteFood = (id) => request.delete(`/food/${id}`)
export const removeFoodImage = (id) => request.delete(`/food/${id}/image`)
