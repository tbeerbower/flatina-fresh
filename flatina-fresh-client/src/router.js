import { createRouter, createWebHistory } from 'vue-router'
import { auth } from './api'

const routes = [
  { path: '/login', component: () => import('./views/LoginView.vue'), meta: { public: true } },
  { path: '/join/:token', component: () => import('./views/JoinView.vue'), props: true, meta: { public: true } },
  { path: '/', redirect: '/plan' },
  { path: '/recipes', component: () => import('./views/RecipesView.vue') },
  { path: '/recipes/new', component: () => import('./views/RecipeEditView.vue') },
  { path: '/recipes/scan', component: () => import('./views/ScanView.vue') },
  { path: '/recipes/:id', component: () => import('./views/RecipeDetailView.vue'), props: true },
  { path: '/recipes/:id/edit', component: () => import('./views/RecipeEditView.vue'), props: true },
  { path: '/plan', component: () => import('./views/WeekView.vue'), props: (r) => ({ start: r.query.week || null }) },
  { path: '/weeks', component: () => import('./views/WeeksView.vue') },
  { path: '/weeks/:start', component: () => import('./views/WeekView.vue'), props: true },
  { path: '/weeks/:start/add', component: () => import('./views/RecipePickerView.vue'), props: true },
  { path: '/shopping', component: () => import('./views/ShoppingListView.vue') },
  { path: '/account', component: () => import('./views/AccountView.vue') },
  { path: '/family', component: () => import('./views/FamilyView.vue') },
  { path: '/admin', component: () => import('./views/AdminView.vue'), meta: { admin: true } },
  { path: '/:pathMatch(.*)*', redirect: '/plan' },
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach((to) => {
  if (!to.meta.public && !auth.token) return { path: '/login', query: { next: to.fullPath } }
  if (to.meta.admin && !(auth.user && auth.user.admin)) return '/plan'
  return true
})

export default router
