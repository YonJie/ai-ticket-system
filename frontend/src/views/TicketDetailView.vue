<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { isHandledApiError } from '@/types/api'
import { useTicketStore } from '@/stores/ticket'
import AppHeader from '@/components/AppHeader.vue'
import { ticketStatusLabels, ticketStatusTagType } from '@/utils/ticketLabels'

const route = useRoute()
const router = useRouter()
const ticketStore = useTicketStore()

const messageContent = ref('')
const feedbackVisible = ref(false)
const feedbackForm = reactive({
  rating: 5,
  comment: '',
})
const submitting = ref(false)

const ticket = computed(() => ticketStore.currentTicket)
const canFeedback = computed(
  () => ticket.value?.status === 'RESOLVED' && !ticket.value?.feedback,
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
    void router.replace('/tickets')
  }
})

onUnmounted(() => {
  ticketStore.clearCurrentTicket()
})

/**
 * 客户追加留言。
 */
async function handleAddMessage() {
  if (!messageContent.value.trim()) {
    ElMessage.warning('请输入留言内容')
    return
  }
  submitting.value = true
  try {
    await ticketStore.addMessage(String(route.params.id), {
      content: messageContent.value,
    })
    messageContent.value = ''
    ElMessage.success('留言已发送')
  } catch (e) {
    if (!isHandledApiError(e)) {
      ElMessage.error(e instanceof Error ? e.message : '发送失败')
    }
  } finally {
    submitting.value = false
  }
}

/**
 * 提交评价。
 */
async function handleSubmitFeedback() {
  submitting.value = true
  try {
    await ticketStore.submitFeedback(String(route.params.id), {
      rating: feedbackForm.rating,
      comment: feedbackForm.comment,
    })
    feedbackVisible.value = false
    ElMessage.success('评价已提交')
  } catch (e) {
    if (!isHandledApiError(e)) {
      ElMessage.error(e instanceof Error ? e.message : '评价失败')
    }
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="page">
    <AppHeader />
    <main v-loading="ticketStore.loading && !ticket" class="main">
      <template v-if="ticket">
        <div class="toolbar">
          <h1>工单详情</h1>
          <div class="actions">
            <el-button v-if="canFeedback" type="warning" @click="feedbackVisible = true">
              评价
            </el-button>
            <el-button @click="router.push('/tickets')">返回列表</el-button>
          </div>
        </div>

        <el-card shadow="never" class="section">
          <div class="title-row">
            <h2>{{ ticket.title }}</h2>
            <el-tag :type="ticketStatusTagType[ticket.status]">
              {{ ticketStatusLabels[ticket.status] }}
            </el-tag>
          </div>
          <p class="desc">{{ ticket.description }}</p>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="分类">{{ ticket.category || '-' }}</el-descriptions-item>
            <el-descriptions-item label="客户">{{ ticket.customerUsername }}</el-descriptions-item>
            <el-descriptions-item label="处理人">
              {{ ticket.assignedTo ? '已指派' : '未指派' }}
            </el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ ticket.createdAt }}</el-descriptions-item>
          </el-descriptions>
        </el-card>

        <el-card shadow="never" class="section">
          <h3>AI 建议回复</h3>
          <p class="ai">{{ ticket.aiSuggestedReply || '暂无建议回复' }}</p>
        </el-card>

        <el-card v-if="ticket.feedback" shadow="never" class="section">
          <h3>我的评价</h3>
          <el-rate :model-value="ticket.feedback.rating" disabled />
          <p v-if="ticket.feedback.comment" class="feedback-comment">{{ ticket.feedback.comment }}</p>
        </el-card>

        <el-card shadow="never" class="section">
          <h3>留言</h3>
          <div v-if="ticket.messages?.length" class="messages">
            <div v-for="msg in ticket.messages" :key="msg.id" class="msg">
              <div class="msg-head">
                <strong>{{ msg.username }}</strong>
                <span class="role">{{ isCustomerMessage(msg.userId) ? '客户' : '客服' }}</span>
                <span class="time">{{ msg.createdAt }}</span>
              </div>
              <p>{{ msg.content }}</p>
            </div>
          </div>
          <el-empty v-else description="暂无留言" :image-size="60" />
          <div class="composer">
            <el-input
              v-model="messageContent"
              type="textarea"
              :rows="3"
              placeholder="追加留言…"
            />
            <el-button type="primary" :loading="submitting" @click="handleAddMessage">
              发送留言
            </el-button>
          </div>
        </el-card>
      </template>
      <el-empty v-else-if="!ticketStore.loading" description="工单不存在或无权查看" />
    </main>

    <el-dialog v-model="feedbackVisible" title="服务评价" width="420px">
      <el-form label-position="top">
        <el-form-item label="评分">
          <el-rate v-model="feedbackForm.rating" />
        </el-form-item>
        <el-form-item label="评论（可选）">
          <el-input v-model="feedbackForm.comment" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="feedbackVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmitFeedback">
          提交
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page {
  min-height: 100vh;
  background: linear-gradient(180deg, #f8fafc 0%, #eef2ff 100%);
}

.main {
  max-width: 860px;
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

.actions {
  display: flex;
  gap: 8px;
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
}

.desc {
  color: #334155;
  margin: 0 0 16px;
  white-space: pre-wrap;
}

.ai {
  margin: 0;
  padding: 12px;
  background: #f1f5f9;
  border-radius: 8px;
  color: #0f172a;
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
