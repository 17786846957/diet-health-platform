import axios from 'axios'
import { useAppStore } from '../stores/app'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000,
  withCredentials: true // 自动携带 httpOnly Cookie
})

// 并发请求计数器，解决 loading 闪烁问题
let activeRequests = 0

// 请求拦截器：显示全局 loading
request.interceptors.request.use(config => {
  if (!config.silent) {
    activeRequests++
    useAppStore().showLoading()
  }
  return config
})

// 响应拦截器
request.interceptors.response.use(
  response => {
    if (!response.config.silent) {
      activeRequests = Math.max(0, activeRequests - 1)
      if (activeRequests === 0) useAppStore().hideLoading()
    }
    const { data } = response
    if (data.code !== 200) {
      return Promise.reject(new Error(data.message || '请求失败'))
    }
    return data
  },
  async error => {
    if (!error.config?.silent) {
      activeRequests = Math.max(0, activeRequests - 1)
      if (activeRequests === 0) useAppStore().hideLoading()
    }
    if (error.response?.status === 401) {
      const { ElMessage } = await import('element-plus')
      const { useUserStore } = await import('../stores/user')
      const userStore = useUserStore()
      userStore.logout()
      ElMessage.error(error.response?.data?.message || error.response?.data?.msg || '未登录或登录已过期')
      setTimeout(() => {
        window.location.href = '/login'
      }, 1500)
      return Promise.reject(new Error('未登录或登录已过期'))
    }
    const msg = error.response?.data?.message || error.response?.data?.msg || '网络错误'
    return Promise.reject(new Error(msg))
  }
)

export default request
