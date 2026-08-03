<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { roleLabels } from '@/utils/ticketLabels'

const userStore = useUserStore()
const router = useRouter()

const roleText = computed(() =>
  userStore.role ? roleLabels[userStore.role] : '',
)

/**
 * 退出并跳转登录页。
 */
function handleLogout() {
  userStore.logout()
  void router.push('/login')
}
</script>

<template>
  <header class="app-header">
    <div class="brand" @click="router.push('/')">AI 智能客服工单系统</div>
    <div v-if="userStore.isLoggedIn" class="user-area">
      <span class="meta">{{ userStore.userInfo?.username }}（{{ roleText }}）</span>
      <el-button size="small" @click="handleLogout">退出</el-button>
    </div>
  </header>
</template>

<style scoped>
.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 24px;
  background: rgba(255, 255, 255, 0.92);
  border-bottom: 1px solid #e5e7eb;
  backdrop-filter: blur(8px);
  position: sticky;
  top: 0;
  z-index: 10;
}

.brand {
  font-weight: 700;
  font-size: 1.05rem;
  color: #0f172a;
  cursor: pointer;
}

.user-area {
  display: flex;
  align-items: center;
  gap: 12px;
}

.meta {
  color: #475569;
  font-size: 0.9rem;
}
</style>
