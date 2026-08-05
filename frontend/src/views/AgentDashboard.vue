<script setup lang="ts">
import { computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { isHandledApiError } from '@/types/api'
import type { Ticket, TicketStatus } from '@/types/ticket'
import { useTicketStore } from '@/stores/ticket'
import AppHeader from '@/components/AppHeader.vue'
import { formatDateTime } from '@/utils/datetime'
import { ticketStatusLabels, ticketStatusTagType } from '@/utils/ticketLabels'

const ticketStore = useTicketStore()
const route = useRoute()
const router = useRouter()

const STATUS_VALUES: TicketStatus[] = ['PENDING', 'PROCESSING', 'RESOLVED', 'CLOSED']

/**
 * 解析 URL 中的 status；非法值返回 undefined。
 *
 * @param raw query 原始值
 */
function parseStatus(raw: unknown): TicketStatus | undefined {
  if (typeof raw !== 'string') return undefined
  return STATUS_VALUES.includes(raw as TicketStatus) ? (raw as TicketStatus) : undefined
}

const statusFilter = computed<TicketStatus | undefined>({
  get() {
    return parseStatus(route.query.status)
  },
  set(val) {
    const query = { ...route.query }
    if (val) {
      query.status = val
    } else {
      delete query.status
    }
    void router.replace({ path: '/agent', query })
  },
})

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
  if (route.query.status != null && statusFilter.value === undefined) {
    const query = { ...route.query }
    delete query.status
    void router.replace({ path: '/agent', query })
  }
})

watch(
  () => route.query.status,
  () => {
    void loadList()
  },
  { immediate: true },
)
</script>

<template>
  <div class="page">
    <AppHeader />
    <main id="main-content" class="main" tabindex="-1">
      <div class="toolbar">
        <h1>客服后台</h1>
        <div class="filter">
          <label class="filter-label" for="agent-status-filter">状态</label>
          <el-select
            id="agent-status-filter"
            v-model="statusFilter"
            clearable
            placeholder="全部状态"
            aria-label="按状态筛选工单"
            style="width: 180px"
          >
            <el-option label="待处理" value="PENDING" />
            <el-option label="处理中" value="PROCESSING" />
            <el-option label="已解决" value="RESOLVED" />
            <el-option label="已关闭" value="CLOSED" />
          </el-select>
        </div>
      </div>
      <el-table
        v-loading="ticketStore.loading"
        :data="ticketStore.tickets"
        stripe
        empty-text="暂无工单"
        style="width: 100%"
      >
        <el-table-column label="标题" min-width="180">
          <template #default="{ row }: { row: Ticket }">
            <router-link class="title-link" :to="`/agent/tickets/${row.id}`">
              {{ row.title }}
            </router-link>
          </template>
        </el-table-column>
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
        <el-table-column label="创建时间" min-width="180">
          <template #default="{ row }: { row: Ticket }">
            <span class="tabular-nums">{{ formatDateTime(row.createdAt) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ row }: { row: Ticket }">
            <router-link class="title-link" :to="`/agent/tickets/${row.id}`">处理</router-link>
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
  scroll-margin-top: 72px;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  gap: 12px;
}

.filter {
  display: flex;
  align-items: center;
  gap: 8px;
}

.filter-label {
  color: #475569;
  font-size: 0.9rem;
  white-space: nowrap;
}

h1 {
  margin: 0;
  font-size: 1.4rem;
}
</style>
