import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, register as registerApi, logout as logoutApi } from '../api/auth'
import { getProfile } from '../api/user'
import { getFamilyMembers } from '../api/familyMember'

export const useUserStore = defineStore('user', () => {
  // Token 由 httpOnly Cookie 管理，不再存 localStorage
  // 只缓存用户基本信息用于 UI 显示
  let parsedUser = null
  try { parsedUser = JSON.parse(localStorage.getItem('user')) } catch {}
  const user = ref(parsedUser)

  let parsedMember = null
  try {
    const stored = localStorage.getItem('activeMember')
    if (stored) parsedMember = JSON.parse(stored)
  } catch {}
  const activeMember = ref(parsedMember)
  const familyMembers = ref([])

  // 基于 user 对象判断登录状态（Cookie 由浏览器自动管理）
  const isLoggedIn = computed(() => !!user.value)
  const isAdmin = computed(() => user.value?.role === 'admin')
  const activeMemberId = computed(() => activeMember.value?.id || null)
  const activeProfile = computed(() => activeMember.value || user.value)

  async function login(form) {
    const res = await loginApi(form)
    // Token 已通过 httpOnly Cookie 设置，不需要手动存储
    user.value = res.data.user
    localStorage.setItem('user', JSON.stringify(res.data.user))
    return res
  }

  async function register(form) {
    return await registerApi(form)
  }

  async function fetchProfile() {
    const res = await getProfile()
    user.value = res.data
    localStorage.setItem('user', JSON.stringify(res.data))
    return res.data
  }

  async function fetchFamilyMembers() {
    const res = await getFamilyMembers()
    familyMembers.value = res.data || []
    return familyMembers.value
  }

  function setActiveMember(member) {
    activeMember.value = member
    if (member) {
      localStorage.setItem('activeMember', JSON.stringify(member))
    } else {
      localStorage.removeItem('activeMember')
    }
  }

  function clearActiveMember() {
    activeMember.value = null
    localStorage.removeItem('activeMember')
  }

  async function logout() {
    try {
      await logoutApi()
    } catch (e) {
      // 即使后端 logout 失败，也要清除本地状态
      console.warn('Logout API failed:', e)
    } finally {
      user.value = null
      activeMember.value = null
      familyMembers.value = []
      localStorage.removeItem('user')
      localStorage.removeItem('activeMember')
    }
  }

  return {
    user, isLoggedIn, isAdmin,
    activeMember, familyMembers, activeMemberId, activeProfile,
    login, register, fetchProfile, logout,
    fetchFamilyMembers, setActiveMember, clearActiveMember
  }
})
