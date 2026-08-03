import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'

/**
 * 应用路由：登录/注册、客户工单、客服后台。
 */
const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'root',
    redirect: () => {
      const userStore = useUserStore()
      if (!userStore.isLoggedIn) return '/login'
      return userStore.isAgent ? '/agent' : '/tickets'
    },
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/LoginView.vue'),
    meta: { guest: true },
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('@/views/RegisterView.vue'),
    meta: { guest: true },
  },
  {
    path: '/tickets',
    name: 'customer-tickets',
    component: () => import('@/views/CustomerTicketsView.vue'),
    meta: { requiresAuth: true, roles: ['CUSTOMER'] },
  },
  {
    path: '/tickets/new',
    name: 'new-ticket',
    component: () => import('@/views/NewTicketView.vue'),
    meta: { requiresAuth: true, roles: ['CUSTOMER'] },
  },
  {
    path: '/tickets/:id',
    name: 'ticket-detail',
    component: () => import('@/views/TicketDetailView.vue'),
    meta: { requiresAuth: true, roles: ['CUSTOMER'] },
  },
  {
    path: '/agent',
    name: 'agent-dashboard',
    component: () => import('@/views/AgentDashboard.vue'),
    meta: { requiresAuth: true, roles: ['AGENT', 'ADMIN'] },
  },
  {
    path: '/agent/tickets/:id',
    name: 'agent-ticket-detail',
    component: () => import('@/views/AgentTicketDetail.vue'),
    meta: { requiresAuth: true, roles: ['AGENT', 'ADMIN'] },
  },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

router.beforeEach((to) => {
  const userStore = useUserStore()

  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  if (to.meta.guest && userStore.isLoggedIn) {
    return userStore.isAgent ? '/agent' : '/tickets'
  }

  const roles = to.meta.roles as string[] | undefined
  if (roles && userStore.role && !roles.includes(userStore.role)) {
    return userStore.isAgent ? '/agent' : '/tickets'
  }

  return true
})

export default router
