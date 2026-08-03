<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { isHandledApiError } from '@/types/api'
import type { UserRole } from '@/types/user'
import { useUserStore } from '@/stores/user'
import AppHeader from '@/components/AppHeader.vue'

const userStore = useUserStore()
const router = useRouter()
const formRef = ref<FormInstance>()
const submitting = ref(false)

const form = reactive({
  username: '',
  password: '',
  role: 'CUSTOMER' as UserRole,
})

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, message: '用户名至少 2 个字符', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' },
  ],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
}

/**
 * 提交注册（成功后自动登录）。
 */
async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await userStore.register(form)
    ElMessage.success('注册成功')
    if (userStore.isAgent) {
      void router.replace('/agent')
    } else {
      void router.replace('/tickets')
    }
  } catch (e) {
    if (!isHandledApiError(e)) {
      ElMessage.error(e instanceof Error ? e.message : '注册失败')
    }
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="page">
    <AppHeader />
    <main class="main">
      <el-card class="card" shadow="never">
        <h1>注册</h1>
        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent>
          <el-form-item label="用户名" prop="username">
            <el-input v-model="form.username" autocomplete="username" />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input v-model="form.password" type="password" show-password autocomplete="new-password" />
          </el-form-item>
          <el-form-item label="角色" prop="role">
            <el-select v-model="form.role" style="width: 100%">
              <el-option label="客户" value="CUSTOMER" />
              <el-option label="客服" value="AGENT" />
            </el-select>
          </el-form-item>
          <el-button type="primary" :loading="submitting" style="width: 100%" @click="handleSubmit">
            注册
          </el-button>
        </el-form>
        <p class="foot">
          已有账号？
          <router-link to="/login">去登录</router-link>
        </p>
      </el-card>
    </main>
  </div>
</template>

<style scoped>
.page {
  min-height: 100vh;
  background:
    radial-gradient(circle at 90% 0%, #dbeafe 0%, transparent 45%),
    linear-gradient(180deg, #f8fafc 0%, #eef2ff 100%);
}

.main {
  display: grid;
  place-items: center;
  padding: 48px 16px;
}

.card {
  width: min(420px, 100%);
  border: 1px solid #e2e8f0;
}

h1 {
  margin: 0 0 20px;
  font-size: 1.5rem;
}

.foot {
  margin: 16px 0 0;
  text-align: center;
  color: #64748b;
  font-size: 0.9rem;
}
</style>
