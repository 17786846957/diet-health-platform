<template>
  <div class="not-found">
    <h1>404</h1>
    <p>页面不存在</p>
    <el-button type="primary" @click="$router.push('/')">返回首页</el-button>
  </div>
</template>

<style scoped>
.not-found {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 80vh;
  background: var(--bg-page);
}
.not-found h1 {
  font-size: 72px;
  color: var(--color-primary);
  margin-bottom: 16px;
  font-weight: 700;
}
.not-found p {
  font-size: 18px;
  color: var(--text-muted);
  margin-bottom: 24px;
}
</style>
