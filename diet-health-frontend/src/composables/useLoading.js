import { ref } from 'vue'

export function useLoading(initial = false) {
  const loading = ref(initial)

  async function withLoading(fn) {
    loading.value = true
    try {
      return await fn()
    } finally {
      loading.value = false
    }
  }

  return { loading, withLoading }
}
