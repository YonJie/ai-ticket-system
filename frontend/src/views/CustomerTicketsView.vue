<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { isHandledApiError } from '@/types/api'
import { useTicketStore } from '@/stores/ticket'
import AppHeader from '@/components/AppHeader.vue'
import { ticketStatusLabels, ticketStatusTagType } from '@/utils/ticketLabels'
import type { Ticket } from '@/types/ticket'

const ticketStore = useTicketStore()
const router = useRouter()

onMounted(async () => {
  try {
    await ticketStore.fetchTickets()
  } catch (e) {
    if (!isHandledApiError(e)) {
      // 本地校验类错误才提示
      console.error(e)
    }
  }
})

/**
 * 进入工单详情。
 *
 * @param row 表格行
 */
function goDetail(row: Ticket) {
  void router.push(`/tickets/${row.id}`)
}
</script>

<template>
  <div class="page">
    <AppHeader />
    <main class="main">
      <div class="toolbar">
        <h1>我的工单</h1>
        <el-button type="primary" @click="router.push('/tickets/new')">新建工单</el-button>
      </div>
      <el-table
        v-loading="ticketStore.loading"
        :data="ticketStore.tickets"
        stripe
        empty-text="暂无工单，点击右上角新建"
        style="width: 100%"
        @row-click="goDetail"
      >
        <el-table-column prop="title" label="标题" min-width="180" />
        <el-table-column prop="category" label="分类" width="100" />
        <el-table-column label="状态" width="120">
          <template #default="{ row }: { row: Ticket }">
            <el-tag :type="ticketStatusTagType[row.status]" size="small">
              {{ ticketStatusLabels[row.status] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" min-width="180" />
        <el-table-column label="操作" width="100">
          <template #default="{ row }: { row: Ticket }">
            <el-button link type="primary" @click.stop="goDetail(row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>
    </main>
  </div>
</template>

<style scoped>
.page {
  min-height: 100vh;
  background: linear-gradient(180deg, #f8fafc 0%, #eef2ff 100%);
}

.main {
  max-width: 1100px;
  margin: 0 auto;
  padding: 24px 16px 48px;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

h1 {
  margin: 0;
  font-size: 1.4rem;
}

:deep(.el-table__row) {
  cursor: pointer;
}
</style>
