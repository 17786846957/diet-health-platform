<template>
  <el-container class="layout-container">
    <el-aside v-if="!isMobile" :width="isCollapsed ? '64px' : '220px'" class="layout-aside">
      <div class="aside-title" v-show="!isCollapsed">
        <span class="aside-icon">🥬</span>
        <span>饮食健康管理</span>
      </div>
      <div v-if="userStore.activeMember && !isCollapsed" class="aside-member-badge">
        <el-tag size="small" type="success">{{ userStore.activeMember.name }}</el-tag>
      </div>
      <div class="aside-toggle" @click="toggleSidebar">
        <el-icon :size="18">
          <Fold v-if="!isCollapsed" />
          <Expand v-else />
        </el-icon>
      </div>
      <side-menu :collapsed="isCollapsed" />
    </el-aside>
    <el-container>
      <el-header class="layout-header">
        <div style="display: flex; align-items: center; gap: 12px">
          <el-button v-if="isMobile" :icon="Menu" text @click="drawerVisible = true" />
          <span class="header-title">{{ currentTitle }}</span>
        </div>
        <div style="display: flex; align-items: center; gap: 16px">
          <el-dropdown @command="handleMemberSwitch" trigger="click">
            <span class="member-selector">
              <span>{{ activeLabel }}</span>
              <el-icon style="margin-left: 4px"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item :command="null" :class="{ 'is-active': !userStore.activeMember }">
                  {{ userStore.user?.username }} (本人)
                </el-dropdown-item>
                <el-dropdown-item
                  v-for="member in userStore.familyMembers"
                  :key="member.id"
                  :command="member"
                  :class="{ 'is-active': userStore.activeMember?.id === member.id }"
                >
                  {{ member.avatar || '' }} {{ member.name }}
                </el-dropdown-item>
                <el-dropdown-item divided command="manage">
                  管理家庭成员
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <span class="header-user">{{ userStore.user?.username }}</span>
          <el-button type="danger" text @click="handleLogout">退出登录</el-button>
        </div>
      </el-header>
      <el-main class="layout-main">
        <router-view :key="`${route.path}-${userStore.activeMemberId}`" />
      </el-main>
    </el-container>

    <el-drawer v-model="drawerVisible" direction="ltr" size="220px" :show-close="false">
      <side-menu :collapsed="false" @select="drawerVisible = false" />
    </el-drawer>
  </el-container>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { Menu, ArrowDown } from '@element-plus/icons-vue'
import SideMenu from '../components/SideMenu.vue'

const isCollapsed = ref(false)
const isMobile = ref(false)
const drawerVisible = ref(false)

const toggleSidebar = () => {
  isCollapsed.value = !isCollapsed.value
}

const checkMobile = () => {
  isMobile.value = window.innerWidth < 768
  if (isMobile.value) {
    isCollapsed.value = true
  }
}

onMounted(() => {
  checkMobile()
  window.addEventListener('resize', checkMobile)
  if (userStore.isLoggedIn) {
    userStore.fetchFamilyMembers()
  }
})

onUnmounted(() => {
  window.removeEventListener('resize', checkMobile)
})

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const currentTitle = computed(() => route.meta.title || '智能饮食健康管理')

const activeLabel = computed(() => {
  if (userStore.activeMember) {
    return (userStore.activeMember.avatar || '') + ' ' + userStore.activeMember.name
  }
  return userStore.user?.username || '未登录'
})

function handleMemberSwitch(command) {
  if (command === null) {
    userStore.clearActiveMember()
  } else if (command === 'manage') {
    router.push('/profile')
  } else {
    userStore.setActiveMember(command)
  }
}

async function handleLogout() {
  await userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
}
.layout-aside {
  background: var(--bg-sidebar);
  border-right: 1px solid var(--border-color);
  overflow: hidden;
}
.aside-title {
  padding: 20px 16px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 700;
  color: var(--color-primary);
}
.aside-member-badge {
  padding: 0 16px 8px;
  text-align: center;
}
.aside-icon {
  width: 28px;
  height: 28px;
  background: var(--color-primary-light);
  border-radius: 8px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
}
.aside-toggle {
  padding: 8px;
  text-align: center;
  cursor: pointer;
  color: var(--text-muted);
}
.aside-toggle:hover {
  color: var(--text-secondary);
}
.layout-header {
  background: var(--bg-header);
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid var(--border-color);
  box-shadow: none;
}
.header-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary);
}
.header-user {
  font-size: 14px;
  color: var(--text-muted);
}
.member-selector {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 14px;
  color: var(--color-primary);
  background: var(--color-primary-lighter);
  transition: background 0.2s;
}
.member-selector:hover {
  background: var(--color-primary-light);
}
.layout-main {
  background: var(--bg-page);
}
</style>
