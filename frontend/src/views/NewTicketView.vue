<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useTicketStore } from '@/stores/ticket'
import AppHeader from '@/components/AppHeader.vue'

const ticketStore = useTicketStore()
const router = useRouter()
const formRef = ref<FormInstance>()
const submitting = ref(false)

const form = reactive({
  title: '',
  description: '',
})

const rules: FormRules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  description: [{ required: true, message: '请输入描述', trigger: 'blur' }],
}

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
    void router.replace(`/tickets/${ticket.id}`)
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '创建失败')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="page">
    <AppHeader />
    <main class="main">
      <el-card shadow="never" class="card">
        <div class="toolbar">
          <h1>新建工单</h1>
          <el-button @click="router.back()">返回</el-button>
        </div>
        <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
          <el-form-item label="标题" prop="title">
            <el-input v-model="form.title" maxlength="200" show-word-limit placeholder="简要说明问题" />
          </el-form-item>
          <el-form-item label="描述" prop="description">
            <el-input
              v-model="form.description"
              type="textarea"
              :rows="6"
              placeholder="请详细描述问题，便于客服与 AI 辅助处理"
            />
          </el-form-item>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">提交</el-button>
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
}

.card {
  border: 1px solid #e2e8f0;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

h1 {
  margin: 0;
  font-size: 1.35rem;
}
</style>
