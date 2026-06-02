<template>
  <el-menu
    :default-active="route.path"
    class="side-menu"
    router
    :collapse="collapsed"
    :collapse-transition="false"
    @select="$emit('select')"
  >
    <el-menu-item index="/">
      <el-icon><HomeFilled /></el-icon>
      <span>首页概览</span>
    </el-menu-item>
    <el-menu-item index="/diet">
      <el-icon><Notebook /></el-icon>
      <span>饮食记录</span>
    </el-menu-item>
    <el-menu-item index="/food">
      <el-icon><Apple /></el-icon>
      <span>食物库</span>
    </el-menu-item>
    <el-menu-item index="/stats">
      <el-icon><DataAnalysis /></el-icon>
      <span>营养统计</span>
    </el-menu-item>
    <el-menu-item index="/diet-advice">
      <el-icon><MagicStick /></el-icon>
      <span>智能饮食建议</span>
    </el-menu-item>
    <el-menu-item index="/water">
      <el-icon><CoffeeCup /></el-icon>
      <span>饮水记录</span>
    </el-menu-item>
    <el-menu-item index="/exercise">
      <el-icon><Timer /></el-icon>
      <span>运动记录</span>
    </el-menu-item>
    <el-menu-item index="/weight">
      <el-icon><ScaleToOriginal /></el-icon>
      <span>体重记录</span>
    </el-menu-item>
    <el-menu-item index="/health-goal">
      <el-icon><Aim /></el-icon>
      <span>健康目标</span>
    </el-menu-item>
    <el-menu-item index="/symptom">
      <el-icon><FirstAidKit /></el-icon>
      <span>身体症状</span>
    </el-menu-item>
    <el-menu-item index="/profile">
      <el-icon><User /></el-icon>
      <span>个人信息</span>
    </el-menu-item>
    <el-menu-item v-if="userStore.isAdmin" index="/admin">
      <el-icon><Setting /></el-icon>
      <span>管理后台</span>
    </el-menu-item>
  </el-menu>
</template>

<script setup>
import { useRoute } from 'vue-router'
import { useUserStore } from '../stores/user'
import { HomeFilled, Notebook, Apple, DataAnalysis, User, Setting, MagicStick, CoffeeCup, Timer, ScaleToOriginal, Aim, FirstAidKit } from '@element-plus/icons-vue'

defineProps({
  collapsed: { type: Boolean, default: false }
})

defineEmits(['select'])

const route = useRoute()
const userStore = useUserStore()
</script>

<style scoped>
.side-menu {
  border-right: none;
  background: transparent;
}
.side-menu:not(.el-menu--collapse) {
  width: 100%;
}
.side-menu .el-menu-item {
  color: var(--text-secondary);
  border-radius: 0;
  margin: 2px 8px;
  height: 40px;
  line-height: 40px;
}
.side-menu .el-menu-item:hover {
  background: var(--color-primary-lighter);
  color: var(--text-secondary);
}
.side-menu .el-menu-item.is-active {
  background: var(--color-primary-lighter);
  color: var(--color-primary);
  font-weight: 600;
  position: relative;
}
.side-menu .el-menu-item.is-active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 6px;
  bottom: 6px;
  width: 3px;
  background: var(--color-primary);
  border-radius: 0 2px 2px 0;
}
</style>
