<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useAppStore } from '../stores/app'
import http from '../api/http'

const appStore = useAppStore()
const healthStatus = ref('检查中...')

/**
 * 调用后端健康检查接口。
 */
async function checkHealth() {
  try {
    const { data } = await http.get<{ status: string }>('/health')
    healthStatus.value = data.status
  } catch {
    healthStatus.value = '后端未连接'
    ElMessage.warning('后端服务暂不可用，请先启动 backend')
  }
}

onMounted(() => {
  void checkHealth()
})
</script>

<template>
  <main class="home">
    <el-card class="card">
      <h1>{{ appStore.title }}</h1>
      <p>Vue3 + Vite + Element Plus 前端已初始化。</p>
      <p>后端健康状态：{{ healthStatus }}</p>
      <el-button type="primary" @click="checkHealth">重新检查</el-button>
    </el-card>
  </main>
</template>

<style scoped>
.home {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px;
  background:
    radial-gradient(circle at top left, #dbeafe 0%, transparent 40%),
    linear-gradient(180deg, #f8fafc 0%, #eef2ff 100%);
}

.card {
  width: min(560px, 100%);
}

h1 {
  margin: 0 0 12px;
  font-size: 1.75rem;
}
</style>
