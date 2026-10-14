import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/', name: 'Home', component: () => import('../views/Home.vue') },
  { path: '/community', name: 'Community', component: () => import('../views/Community.vue') },
  { path: '/login', name: 'Login', component: () => import('../views/Login.vue') },
  { path: '/register', name: 'Register', component: () => import('../views/Register.vue') },
  { path: '/product/list', name: 'ProductList', component: () => import('../views/ProductList.vue') },
  { path: '/product/:id', name: 'ProductDetail', component: () => import('../views/ProductDetail.vue') },
  { path: '/user/:userId', name: 'UserProfile', component: () => import('../views/UserProfile.vue') },
  { path: '/publish', name: 'PublishProduct', component: () => import('../views/PublishProduct.vue'), meta: { requiresAuth: true } },
  { path: '/my-products', name: 'MyProducts', component: () => import('../views/MyProducts.vue'), meta: { requiresAuth: true } },
  { path: '/orders', name: 'OrderList', component: () => import('../views/OrderList.vue'), meta: { requiresAuth: true } },
  { path: '/payment', name: 'Payment', component: () => import('../views/Payment.vue'), meta: { requiresAuth: true } },
  { path: '/my-favorites', name: 'MyFavorites', component: () => import('../views/MyFavorites.vue'), meta: { requiresAuth: true } },
  { path: '/messages', name: 'Messages', component: () => import('../views/Messages.vue'), meta: { requiresAuth: true } },
  { path: '/user-center', name: 'UserCenter', component: () => import('../views/UserCenter.vue'), meta: { requiresAuth: true } }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.meta.requiresAuth && !token) {
    next('/login')
  } else {
    next()
  }
})

export default router