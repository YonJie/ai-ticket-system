<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { roleLabels } from '@/utils/ticketLabels'

const userStore = useUserStore()
const router = useRouter()

const roleText = computed(() =>
  userStore.role ? roleLabels[userStore.role] : '',
)

/**
 * 退出登录（需确认）。
 */
async function handleLogout() {
  try {
    await ElMessageBox.confirm('确定退出登录？', '提示', {
      type: 'warning',
      confirmButtonText: '退出',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  userStore.logout()
  void router.push('/login')
}
</script>

<template>
  <header class="app-header">
    <router-link class="brand" to="/" translate="no">AI 智能客服工单系统</router-link>
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
  padding-top: calc(14px + env(safe-area-inset-top, 0px));
  padding-left: calc(24px + env(safe-area-inset-left, 0px));
  padding-right: calc(24px + env(safe-area-inset-right, 0px));
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
  text-decoration: none;
}

.brand:hover {
  color: #1d4ed8;
}

.brand:focus-visible {
  outline: 2px solid #2563eb;
  outline-offset: 3px;
  border-radius: 4px;
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
