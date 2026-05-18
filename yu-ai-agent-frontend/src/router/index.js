import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { title: '登录 - 天天AI超级智能体' }
  },
  {
    path: '/',
    name: 'Home',
    component: () => import('../views/Home.vue'),
    meta: {
      title: '首页 - 天天AI超级智能体应用平台',
      description: '天天AI超级智能体应用平台提供知识库问答和AI超级智能体服务，满足您的各种AI问答需求'
    }
  },
  {
    path: '/love-master',
    name: 'LoveMaster',
    component: () => import('../views/LoveMaster.vue'),
    meta: {
      title: '知识库问答 - 天天AI超级智能体应用平台',
      description: '知识库问答是天天AI超级智能体应用平台的智能问答助手，基于知识库快速解答你的问题'
    }
  },
  {
    path: '/super-agent',
    name: 'SuperAgent',
    component: () => import('../views/SuperAgent.vue'),
    meta: {
      title: 'AI超级智能体 - 天天AI超级智能体应用平台',
      description: 'AI超级智能体是天天AI超级智能体应用平台的全能助手，能解答各类专业问题，提供精准建议和解决方案'
    }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

const publicPages = ['/login']

router.beforeEach((to, from, next) => {
  if (to.meta.title) {
    document.title = to.meta.title
  }
  const token = localStorage.getItem('access_token')
  if (!publicPages.includes(to.path) && !token) {
    return next('/login')
  }
  next()
})

export default router
