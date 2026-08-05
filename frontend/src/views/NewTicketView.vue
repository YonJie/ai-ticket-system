<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { isHandledApiError } from '@/types/api'
import { useTicketStore } from '@/stores/ticket'
import { useUnsavedGuard } from '@/composables/useUnsavedGuard'
import AppHeader from '@/components/AppHeader.vue'

const ticketStore = useTicketStore()
const router = useRouter()
const formRef = ref<FormInstance>()
const submitting = ref(false)
/** 提交成功后放行路由守卫 */
const allowLeave = ref(false)

const form = reactive({
  title: '',
  description: '',
})

const rules: FormRules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  description: [{ required: true, message: '请输入描述', trigger: 'blur' }],
}

const isDirty = computed(
  () => !allowLeave.value && Boolean(form.title.trim() || form.description.trim()),
)
useUnsavedGuard(isDirty)

/**
 * 提交新建工单。
 */
async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    const ticket = await ticketStore.createTicket(form)
    ElMessage.success('工单已创建')
    allowLeave.value = true
    void router.replace(`/tickets/${ticket.id}`)
  } catch (e) {
    if (!isHandledApiError(e)) {
      ElMessage.error(e instanceof Error ? e.message : '创建失败')
    }
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="page">
    <AppHeader />
    <main id="main-content" class="main" tabindex="-1">
      <el-card shadow="never" class="card">
        <div class="toolbar">
          <h1>新建工单</h1>
          <el-button :tag="RouterLink" to="/tickets">返回</el-button>
        </div>
        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="handleSubmit">
          <el-form-item label="标题" prop="title">
            <el-input
              v-model="form.title"
              name="title"
              autocomplete="off"
              maxlength="200"
              show-word-limit
              placeholder="例如：无法登录账号…"
            />
          </el-form-item>
          <el-form-item label="描述" prop="description">
            <el-input
              v-model="form.description"
              name="description"
              autocomplete="off"
              type="textarea"
              :rows="6"
              placeholder="请详细描述问题，便于客服与 AI 辅助处理…"
            />
          </el-form-item>
          <el-button type="primary" native-type="submit" :loading="submitting">提交</el-button>
        </el-form>
      </el-card>
    </main>
  </div>
</template>

<style scoped>
.page {
  min-height: 100vh;
  background: linear-gradient(180deg, #f8fafc 0%, #eef2ff 100%);
}

.main {
  max-width: 720px;
  margin: 0 auto;
  padding: 24px 16px 48px;
  scroll-margin-top: 72px;
}

.card {
  border: 1px solid #e2e8f0;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
  gap: 12px;
}

h1 {
  margin: 0;
  font-size: 1.35rem;
  min-width: 0;
}
</style>
