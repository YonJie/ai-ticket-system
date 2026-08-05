import { onBeforeUnmount, watch, type Ref } from 'vue'
import { onBeforeRouteLeave } from 'vue-router'
import { ElMessageBox } from 'element-plus'

/**
 * 有未保存内容时：拦截刷新/关页（beforeunload）与站内路由离开。
 *
 * @param isDirty 是否存在未保存草稿
 * @param message 确认框文案
 */
export function useUnsavedGuard(
  isDirty: Ref<boolean>,
  message = '有未保存内容，确定离开？',
) {
  /**
   * 浏览器原生离开提示。
   *
   * @param event beforeunload 事件
   */
  function onBeforeUnload(event: BeforeUnloadEvent) {
    if (!isDirty.value) return
    event.preventDefault()
    event.returnValue = ''
  }

  watch(
    isDirty,
    (dirty) => {
      if (dirty) {
        window.addEventListener('beforeunload', onBeforeUnload)
      } else {
        window.removeEventListener('beforeunload', onBeforeUnload)
      }
    },
    { immediate: true },
  )

  onBeforeUnmount(() => {
    window.removeEventListener('beforeunload', onBeforeUnload)
  })

  onBeforeRouteLeave(async () => {
    if (!isDirty.value) return true
    try {
      await ElMessageBox.confirm(message, '提示', {
        type: 'warning',
        confirmButtonText: '离开',
        cancelButtonText: '取消',
      })
      return true
    } catch {
      return false
    }
  })
}
