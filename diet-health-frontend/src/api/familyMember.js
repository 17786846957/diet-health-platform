import request from './request'

export const getFamilyMembers = () => request.get('/family-members')
export const getFamilyMember = (id) => request.get(`/family-members/${id}`)
export const createFamilyMember = (data) => request.post('/family-members', data)
export const updateFamilyMember = (id, data) => request.put(`/family-members/${id}`, data)
export const deleteFamilyMember = (id) => request.delete(`/family-members/${id}`)
