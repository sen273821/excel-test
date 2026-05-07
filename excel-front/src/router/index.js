import Vue from 'vue'
import VueRouter from 'vue-router'

Vue.use(VueRouter)

const routes = [
  { path: '/', redirect: '/import' },
  { path: '/import', name: 'Import', component: () => import('@/views/ImportView.vue') },
  { path: '/preview', name: 'Preview', component: () => import('@/views/PreviewView.vue') },
  { path: '/history', name: 'History', component: () => import('@/views/HistoryView.vue') }
]

const router = new VueRouter({
  routes
})

export default router
