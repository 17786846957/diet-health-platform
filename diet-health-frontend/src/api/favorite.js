import request from './request'

export const getFavorites = () => request.get('/favorites')
export const getFavoriteIds = () => request.get('/favorites/ids')
export const addFavorite = (foodId) => request.post('/favorites', { foodId })
export const removeFavorite = (foodId) => request.delete(`/favorites/${foodId}`)
