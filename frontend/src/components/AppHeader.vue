<template>
  <header class="app-header">
    <div class="header-inner">
      <!-- 左侧 Logo + 导航 -->
      <div class="header-left">
        <router-link to="/" class="logo">
          <el-icon :size="22"><ShoppingCart /></el-icon>
          <span class="logo-text">ESshop</span>
        </router-link>
        <nav class="nav-tabs">
          <router-link to="/" class="nav-item" :class="{ active: route.path === '/' }">首页</router-link>
          <router-link to="/community" class="nav-item" :class="{ active: route.path === '/community' }">社区</router-link>
          <router-link to="/product/list" class="nav-item" :class="{ active: route.path.startsWith('/product') }">商品市场</router-link>
        </nav>
      </div>

      <!-- 中间搜索框 -->
      <div class="header-search">
        <el-input v-model="keyword" size="default" placeholder="搜索商品..." clearable
                  @keyup.enter="search" class="search-input">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
      </div>

      <!-- 右侧用户区 -->
      <div class="header-right">
        <template v-if="user">
          <!-- 消息入口 -->
          <el-badge :value="unreadCount" :hidden="unreadCount === 0" class="msg-badge">
            <router-link to="/messages" class="msg-entry">
              <el-icon :size="19"><ChatDotRound /></el-icon>
            </router-link>
          </el-badge>
          <router-link to="/publish">
            <el-button type="primary" size="small">发布</el-button>
          </router-link>
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-avatar :size="30" :src="user.avatar" />
              <span class="nickname">{{ user.nickname || user.username }}</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">我的主页</el-dropdown-item>
                <el-dropdown-item command="center">账号设置</el-dropdown-item>
                <el-dropdown-item command="myProducts">我的发布</el-dropdown-item>
                <el-dropdown-item command="orders">我的订单</el-dropdown-item>
                <el-dropdown-item command="favorites">我的收藏</el-dropdown-item>
                <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
        <template v-else>
          <router-link to="/login"><el-button text>登录</el-button></router-link>
          <router-link to="/register"><el-button type="primary" size="small">注册</el-button></router-link>
        </template>
      </div>
    </div>
  </header>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { userApi } from '../api/user'
import { chatMessageApi } from '../api/chatMessage'
import { wsClient } from '../utils/websocket'

const router = useRouter()
const route = useRoute()
const user = ref(null)
const keyword = ref('')
const unreadCount = ref(0)

let offChat = null

const loadUser = () => {
  const stored = localStorage.getItem('user')
  if (stored) {
    try { user.value = JSON.parse(stored) } catch (e) { /* ignore */ }
  }
}

/** 拉取未读数（登录状态下） */
const loadUnread = async () => {
  if (!localStorage.getItem('token')) {
    unreadCount.value = 0
    return
  }
  try {
    const res = await chatMessageApi.unread()
    unreadCount.value = res.data || 0
  } catch (e) { /* 静默失败即可 */ }
}

const search = () => {
  if (keyword.value.trim()) {
    router.push({ path: '/product/list', query: { keyword: keyword.value } })
  }
}

/** 根据登录状态同步 WebSocket 连接 */
const syncWebSocket = () => {
  if (localStorage.getItem('token')) {
    wsClient.connect()
  } else {
    wsClient.disconnect()
  }
}

watch(() => route.path, () => {
  loadUser()
  loadUnread()
  // 登录/退出后路由会变化，借此机会同步连接
  syncWebSocket()
})

const handleCommand = (cmd) => {
  switch (cmd) {
    case 'profile': router.push('/user/' + user.value?.userId); break
    case 'center': router.push('/user-center'); break
    case 'myProducts': router.push('/my-products'); break
    case 'orders': router.push('/orders'); break
    case 'favorites': router.push('/my-favorites'); break
    case 'logout':
      userApi.logout().finally(() => {
        wsClient.disconnect()
        localStorage.removeItem('token')
        localStorage.removeItem('user')
        user.value = null
        unreadCount.value = 0
        ElMessage.success('已退出登录')
        router.push('/')
      })
      break
  }
}

onMounted(() => {
  loadUser()
  loadUnread()
  syncWebSocket()
  window.addEventListener('user-updated', loadUser)

  // 收到新消息时更新未读红点
  offChat = wsClient.on('CHAT', (payload) => {
    // 自己发的消息不计入未读
    if (payload.self) return
    // 打开着消息中心时不显示红点
    if (route.path === '/messages') return
    unreadCount.value++
  })
})

onUnmounted(() => {
  if (offChat) offChat()
  window.removeEventListener('user-updated', loadUser)
})
</script>

<style scoped>
.app-header {
  background: #fff;
  box-shadow: 0 1px 4px rgba(0,0,0,.08);
  position: sticky;
  top: 0;
  z-index: 100;
}
.header-inner {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 0 20px;
  height: 56px;
}
/* 左侧 */
.header-left {
  display: flex;
  align-items: center;
  gap: 32px;
  flex-shrink: 0;
}
.logo {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #409eff;
  font-size: 20px;
  font-weight: 700;
}
.nav-tabs {
  display: flex;
  gap: 4px;
}
.nav-item {
  padding: 6px 14px;
  font-size: 14px;
  color: #555;
  border-radius: 6px;
  transition: all .2s;
}
.nav-item:hover {
  color: #409eff;
  background: #ecf5ff;
}
.nav-item.active {
  color: #409eff;
  font-weight: 600;
}
/* 中间搜索 */
.header-search {
  flex: 1;
  max-width: 400px;
}
.search-input :deep(.el-input__wrapper) {
  border-radius: 20px;
  background: #f5f6f8;
  box-shadow: none;
}
.search-input :deep(.el-input__wrapper:hover) {
  background: #eee;
}
/* 右侧 */
.header-right {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-shrink: 0;
}
.user-info {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  font-size: 14px;
  color: #333;
}
.nickname {
  max-width: 80px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.msg-entry {
  display: flex;
  align-items: center;
  color: #666;
  padding: 4px;
  border-radius: 6px;
  transition: all .2s;
}
.msg-entry:hover { color: #409eff; background: #ecf5ff; }
.msg-badge :deep(.el-badge__content) { top: 4px; right: 8px; }
</style>