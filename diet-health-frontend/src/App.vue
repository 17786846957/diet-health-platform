<template>
  <div v-loading.fullscreen="appStore.loading" :element-loading-text="appStore.loadingText">
    <router-view v-slot="{ Component }">
      <transition name="fade" mode="out-in">
        <component :is="Component" />
      </transition>
    </router-view>
  </div>
</template>

<script setup>
import './styles/variables.css'
import { useAppStore } from './stores/app'

const appStore = useAppStore()
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}
body {
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB',
    'Microsoft YaHei', '微软雅黑', Arial, sans-serif;
  background-color: var(--bg-page);
  color: var(--text-primary);
}

/* 全局卡片样式 */
.el-card {
  border: 1px solid var(--border-color);
  border-radius: var(--radius-card);
  box-shadow: var(--shadow-card);
}
.el-card:hover {
  box-shadow: var(--shadow-card-hover);
}

/* 全局按钮圆角 */
.el-button {
  border-radius: var(--radius-btn);
}
.el-button--primary {
  --el-button-bg-color: var(--color-primary);
  --el-button-border-color: var(--color-primary);
  --el-button-hover-bg-color: var(--color-primary-hover);
  --el-button-hover-border-color: var(--color-primary-hover);
}

/* 全局输入框圆角 */
.el-input__wrapper {
  border-radius: var(--radius-input);
}

/* 全局表格样式 */
.el-table th.el-table__cell {
  background-color: #f8fafc;
}
.el-table--enable-row-hover .el-table__body tr:hover > td {
  background-color: var(--color-primary-lighter);
}

/* 全局对话框圆角 */
.el-dialog {
  border-radius: var(--radius-card);
}

/* 过渡动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
