import { defineStore } from 'pinia'
import { ref } from 'vue'

/**
 * 应用全局状态。
 */
export const useAppStore = defineStore('app', () => {
  const title = ref('AI 智能客服工单系统')

  return {
    title,
  }
})
