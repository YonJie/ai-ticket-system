<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter, RouterLink } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { isHandledApiError } from '@/types/api'
import type { TicketStatus } from '@/types/ticket'
import { useTicketStore } from '@/stores/ticket'
import { useUserStore } from '@/stores/user'
import { useUnsavedGuard } from '@/composables/useUnsavedGuard'
import AppHeader from '@/components/AppHeader.vue'
import { formatDateTime } from '@/utils/datetime'
import { ticketStatusLabels, ticketStatusTagType } from '@/utils/ticketLabels'

const route = useRoute()
const router = useRouter()
const ticketStore = useTicketStore()
const userStore = useUserStore()

const replyContent = ref('')
const statusValue = ref<TicketStatus>('PENDING')
const submitting = ref(false)
const allowLeave = ref(false)

const ticket = computed(() => ticketStore.currentTicket)
const isDirty = computed(() => !allowLeave.value && Boolean(replyContent.value.trim()))
useUnsavedGuard(isDirty)

watch(
  ticket,
  (val) => {
    if (val) statusValue.value = val.status
  },
  { immediate: true },
)

/**
 * 判断留言是否来自工单客户。
 *
 * @param userId 发送者 ID
 */
function isCustomerMessage(userId: string): boolean {
  return Boolean(ticket.value && ticket.value.customerId === userId)
}

onMounted(async () => {
  try {
    await ticketStore.fetchTicketDetail(String(route.params.id))
  } catch (e) {
    if (!isHandledApiError(e)) {
      ElMessage.error(e instanceof Error ? e.message : '加载失败')
    }
    allowLeave.value = true
    void router.replace('/agent')
  }
})

onUnmounted(() => {
  ticketStore.clearCurrentTicket()
})

/**
 * 修改工单状态（需携带 updatedAt）；关闭需确认。
 *
 * @param status 目标状态
 */
async function handleStatusChange(status: TicketStatus) {
  if (!ticket.value) return

  if (status === 'CLOSED') {
    try {
      await ElMessageBox.confirm('确定关闭该工单？关闭后将结束处理流程。', '确认关闭', {
        type: 'warning',
        confirmButtonText: '关闭工单',
        cancelButtonText: '取消',
      })
    } catch {
      statusValue.value = ticket.value.status
      return
    }
  }

  submitting.value = true
  try {
    await ticketStore.updateTicket(String(route.params.id), {
      status,
      updatedAt: ticket.value.updatedAt,
      assignedTo: ticket.value.assignedTo || userStore.userInfo?.id,
    })
    ElMessage.success('状态已更新')
  } catch (e) {
    if (ticket.value) statusValue.value = ticket.value.status
    if (!isHandledApiError(e)) {
      ElMessage.error(e instanceof Error ? e.message : '更新失败')
    }
  } finally {
    submitting.value = false
  }
}

/**
 * 发送客服回复留言。
 */
async function handleReply() {
  if (!replyContent.value.trim()) {
    ElMessage.warning('请输入回复内容')
    return
  }
  submitting.value = true
  try {
    await ticketStore.addMessage(String(route.params.id), {
      content: replyContent.value,
    })
    replyContent.value = ''
    ElMessage.success('回复已发送')
  } catch (e) {
    if (!isHandledApiError(e)) {
      ElMessage.error(e instanceof Error ? e.message : '发送失败')
    }
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="page">
    <AppHeader />
    <main
      id="main-content"
      v-loading="ticketStore.loading && !ticket"
      class="main"
      tabindex="-1"
    >
      <template v-if="ticket">
        <div class="toolbar">
          <h1>工单处理</h1>
          <el-button :tag="RouterLink" to="/agent">返回后台</el-button>
        </div>

        <el-card shadow="never" class="section">
          <div class="title-row">
            <h2 class="truncate-title" :title="ticket.title">{{ ticket.title }}</h2>
            <el-tag :type="ticketStatusTagType[ticket.status]">
              {{ ticketStatusLabels[ticket.status] }}
            </el-tag>
          </div>
          <p class="desc break-words">{{ ticket.description }}</p>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="分类">{{ ticket.category || '-' }}</el-descriptions-item>
            <el-descriptions-item label="客户">{{ ticket.customerUsername }}</el-descriptions-item>
            <el-descriptions-item label="处理人">
              {{ ticket.assignedTo ? '已指派' : '未指派' }}
            </el-descriptions-item>
            <el-descriptions-item label="创建时间">
              <span class="tabular-nums">{{ formatDateTime(ticket.createdAt) }}</span>
            </el-descriptions-item>
          </el-descriptions>

          <div class="status-row">
            <label class="status-label" for="agent-ticket-status">修改状态</label>
            <el-select
              id="agent-ticket-status"
              v-model="statusValue"
              style="width: 180px"
              :disabled="submitting"
              aria-label="修改工单状态"
              @change="handleStatusChange"
            >
              <el-option label="待处理" value="PENDING" />
              <el-option label="处理中" value="PROCESSING" />
              <el-option label="已解决" value="RESOLVED" />
              <el-option label="已关闭" value="CLOSED" />
            </el-select>
          </div>
        </el-card>

        <el-card shadow="never" class="section">
          <h3>AI 建议回复</h3>
          <p class="ai break-words">{{ ticket.aiSuggestedReply || '暂无建议回复' }}</p>
          <el-button
            size="small"
            :disabled="!ticket.aiSuggestedReply"
            @click="replyContent = ticket.aiSuggestedReply || ''"
          >
            填入回复框
          </el-button>
        </el-card>

        <el-card v-if="ticket.feedback" shadow="never" class="section">
          <h3>客户评价</h3>
          <el-rate :model-value="ticket.feedback.rating" disabled />
          <p v-if="ticket.feedback.comment" class="feedback-comment break-words">
            {{ ticket.feedback.comment }}
          </p>
        </el-card>

        <el-card shadow="never" class="section">
          <h3>留言</h3>
          <div v-if="ticket.messages?.length" class="messages">
            <div v-for="msg in ticket.messages" :key="msg.id" class="msg">
              <div class="msg-head">
                <strong>{{ msg.username }}</strong>
                <span class="role">{{ isCustomerMessage(msg.userId) ? '客户' : '客服' }}</span>
                <span class="time tabular-nums">{{ formatDateTime(msg.createdAt) }}</span>
              </div>
              <p class="break-words">{{ msg.content }}</p>
            </div>
          </div>
          <el-empty v-else description="暂无留言" :image-size="60" />
          <div class="composer">
            <el-input
              v-model="replyContent"
              name="reply"
              autocomplete="off"
              type="textarea"
              :rows="3"
              placeholder="输入客服回复…"
            />
            <el-button type="primary" :loading="submitting" @click="handleReply">
              发送回复
            </el-button>
          </div>
        </el-card>
      </template>
      <el-empty v-else-if="!ticketStore.loading" description="工单不存在或无权查看" />
    </main>
  </div>
</template>

<style scoped>
.page {
  min-height: 100vh;
  background: linear-gradient(180deg, #f8fafc 0%, #e8eefc 100%);
}

.main {
  max-width: 860px;
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

h1 {
  margin: 0;
  font-size: 1.35rem;
}

h2 {
  margin: 0;
  font-size: 1.2rem;
}

h3 {
  margin: 0 0 12px;
  font-size: 1rem;
}

.section {
  margin-bottom: 16px;
  border: 1px solid #e2e8f0;
}

.title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
  min-width: 0;
}

.desc {
  color: #334155;
  margin: 0 0 16px;
  white-space: pre-wrap;
}

.status-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 16px;
}

.status-label {
  color: #475569;
  font-size: 0.9rem;
  white-space: nowrap;
}

.ai {
  margin: 0 0 10px;
  padding: 12px;
  background: #f1f5f9;
  border-radius: 8px;
  white-space: pre-wrap;
}

.messages {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 16px;
}

.msg {
  padding: 12px;
  background: #f8fafc;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
}

.msg p {
  margin: 8px 0 0;
  white-space: pre-wrap;
}

.msg-head {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.85rem;
  min-width: 0;
}

.role {
  color: #64748b;
}

.time {
  margin-left: auto;
  color: #94a3b8;
}

.composer {
  display: flex;
  flex-direction: column;
  gap: 10px;
  align-items: flex-end;
}

.feedback-comment {
  margin: 8px 0 0;
  color: #475569;
}
</style>
