<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { isHandledApiError } from '@/types/api'
import type { Ticket, TicketStatus } from '@/types/ticket'
import { useTicketStore } from '@/stores/ticket'
import AppHeader from '@/components/AppHeader.vue'
import { ticketStatusLabels, ticketStatusTagType } from '@/utils/ticketLabels'

const ticketStore = useTicketStore()
const router = useRouter()
const statusFilter = ref<TicketStatus | undefined>(undefined)

/**
 * 按筛选加载列表。
 */
async function loadList() {
  try {
    await ticketStore.fetchTickets(statusFilter.value)
  } catch (e) {
    if (!isHandledApiError(e)) {
      console.error(e)
    }
  }
}

onMounted(() => {
  void loadList()
})

watch(statusFilter, () => {
  void loadList()
})

/**
 * 进入客服处理页。
 *
 * @param row 表格行
 */
function goDetail(row: Ticket) {
  void router.push(`/agent/tickets/${row.id}`)
}
</script>

<template>
  <div class="page">
    <AppHeader />
    <main class="main">
      <div class="toolbar">
        <h1>客服后台</h1>
        <el-select v-model="statusFilter" clearable placeholder="全部状态" style="width: 180px">
          <el-option label="待处理" value="PENDING" />
          <el-option label="处理中" value="PROCESSING" />
          <el-option label="已解决" value="RESOLVED" />
          <el-option label="已关闭" value="CLOSED" />
        </el-select>
      </div>
      <el-table
        v-loading="ticketStore.loading"
        :data="ticketStore.tickets"
        stripe
        empty-text="暂无工单"
        style="width: 100%"
        @row-click="goDetail"
      >
        <el-table-column prop="title" label="标题" min-width="180" />
        <el-table-column prop="customerUsername" label="客户" width="120" />
        <el-table-column prop="category" label="分类" width="100" />
        <el-table-column label="状态" width="120">
          <template #default="{ row }: { row: Ticket }">
            <el-tag :type="ticketStatusTagType[row.status]" size="small">
              {{ ticketStatusLabels[row.status] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="处理人" width="140">
          <template #default="{ row }: { row: Ticket }">
            {{ row.assignedTo ? '已指派' : '未指派' }}
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" min-width="180" />
        <el-table-column label="操作" width="100">
          <template #default="{ row }: { row: Ticket }">
            <el-button link type="primary" @click.stop="goDetail(row)">处理</el-button>
          </template>
        </el-table-column>
      </el-table>
    </main>
  </div>
</template>

<style scoped>
.page {
  min-height: 100vh;
  background: linear-gradient(180deg, #f8fafc 0%, #e8eefc 100%);
}

.main {
  max-width: 1180px;
  margin: 0 auto;
  padding: 24px 16px 48px;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  gap: 12px;
}

h1 {
  margin: 0;
  font-size: 1.4rem;
}

:deep(.el-table__row) {
  cursor: pointer;
}
</style>
