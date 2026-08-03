<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { isHandledApiError } from '@/types/api'
import { useUserStore } from '@/stores/user'
import AppHeader from '@/components/AppHeader.vue'

const userStore = useUserStore()
const router = useRouter()
const route = useRoute()
const formRef = ref<FormInstance>()
const submitting = ref(false)

const form = reactive({
  username: 'customer',
  password: '123456',
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

/**
 * 按角色跳转首页。
 */
function redirectByRole() {
  const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : ''
  if (redirect) {
    void router.replace(redirect)
    return
  }
  if (userStore.isAgent) {
    void router.replace('/agent')
  } else {
    void router.replace('/tickets')
  }
}

/**
 * 提交登录。
 */
async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await userStore.login(form)
    ElMessage.success('登录成功')
    redirectByRole()
  } catch (e) {
    if (!isHandledApiError(e)) {
      ElMessage.error(e instanceof Error ? e.message : '登录失败')
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
        <h1>登录</h1>
        <p class="hint">演示账号：customer / agent / admin，密码均为 123456</p>
        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent>
          <el-form-item label="用户名" prop="username">
            <el-input v-model="form.username" autocomplete="username" />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password"
              type="password"
              show-password
              autocomplete="current-password"
              @keyup.enter="handleSubmit"
            />
          </el-form-item>
          <el-button type="primary" :loading="submitting" style="width: 100%" @click="handleSubmit">
            登录
          </el-button>
        </el-form>
        <p class="foot">
          还没有账号？
          <router-link to="/register">去注册</router-link>
        </p>
      </el-card>
    </main>
  </div>
</template>

<style scoped>
.page {
  min-height: 100vh;
  background:
    radial-gradient(circle at 10% 0%, #dbeafe 0%, transparent 45%),
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
  margin: 0 0 8px;
  font-size: 1.5rem;
}

.hint {
  margin: 0 0 20px;
  color: #64748b;
  font-size: 0.85rem;
}

.foot {
  margin: 16px 0 0;
  text-align: center;
  color: #64748b;
  font-size: 0.9rem;
}
</style>
