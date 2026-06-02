import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

/**
 * 通用 CRUD composable，封装表单对话框、提交、删除的重复逻辑
 * 参考 eladmin 的 crud mixin 设计，适配 Vue 3 Composition API
 *
 * @param {Object} options
 * @param {Function} options.onSubmit - 提交函数 (form) => Promise
 * @param {Function} options.onDelete - 删除函数 (id) => Promise
 * @param {Function} options.onRefresh - 提交/删除成功后的刷新函数
 * @param {Function} [options.defaultForm] - 表单默认值工厂函数
 * @param {string} [options.entityName='记录'] - 实体名称，用于提示文案
 */
export function useCrud({ onSubmit, onDelete, onRefresh, defaultForm, entityName = '记录' }) {
  const dialogVisible = ref(false)
  const submitting = ref(false)
  const form = ref(defaultForm ? defaultForm() : {})

  function resetForm() {
    form.value = defaultForm ? defaultForm() : {}
  }

  function openDialog(initialData) {
    resetForm()
    if (initialData) {
      Object.assign(form.value, initialData)
    }
    dialogVisible.value = true
  }

  function closeDialog() {
    dialogVisible.value = false
  }

  async function handleSubmit() {
    if (submitting.value) return
    submitting.value = true
    try {
      await onSubmit(form.value)
      ElMessage.success(`${entityName}保存成功`)
      closeDialog()
      onRefresh?.()
    } catch (error) {
      ElMessage.error(error?.message || `${entityName}保存失败`)
    } finally {
      submitting.value = false
    }
  }

  async function handleDelete(id) {
    try {
      await ElMessageBox.confirm(`确定删除这条${entityName}？`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
      await onDelete(id)
      ElMessage.success('删除成功')
      onRefresh?.()
    } catch (error) {
      if (error !== 'cancel') {
        ElMessage.error(error?.message || '删除失败')
      }
    }
  }

  return {
    dialogVisible,
    submitting,
    form,
    resetForm,
    openDialog,
    closeDialog,
    handleSubmit,
    handleDelete
  }
}
