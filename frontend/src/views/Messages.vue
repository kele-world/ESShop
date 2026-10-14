<template>
  <div class="messages-page">
    <div class="msg-layout">
      <!-- 左侧：会话列表 -->
      <div class="conv-list">
        <div class="conv-header">
          <h3>消息</h3>
          <el-badge v-if="totalUnread > 0" :value="totalUnread" />
        </div>
        <div class="conv-items" v-loading="loading">
          <div v-for="conv in conversations" :key="conv.id" class="conv-item"
               :class="{ active: activeId === conv.id }" @click="openConversation(conv)">
            <div class="avatar-wrap">
              <el-avatar :size="42" :src="conv.peerAvatar" />
              <!-- 在线状态小绿点 -->
              <span class="online-dot" :class="{ on: onlineMap[conv.peerId] }"></span>
            </div>
            <div class="conv-content">
              <div class="conv-top">
                <span class="conv-name">{{ conv.peerName }}</span>
                <span class="conv-time">{{ formatTime(conv.lastTime) }}</span>
              </div>
              <div class="conv-bottom">
                <span class="conv-preview">{{ conv.lastMessage || '开始聊天吧' }}</span>
                <el-badge v-if="conv.unread > 0" :value="conv.unread" class="conv-badge" />
              </div>
            </div>
            <!-- 悬停出现移除按钮：只从我的列表移除，对方不受影响 -->
            <el-popconfirm title="从列表中移除？对方仍可看到对话，他再发消息时会重新出现"
                           width="230" @confirm="removeConversation(conv)">
              <template #reference>
                <el-icon class="conv-remove" title="移除会话"
                         @click.stop><Close /></el-icon>
              </template>
            </el-popconfirm>
          </div>
          <el-empty v-if="!loading && !conversations.length" description="暂无消息" :image-size="80" />
        </div>
      </div>

      <!-- 右侧：聊天区（复用聊天组件的能力） -->
      <div class="chat-area">
        <template v-if="activeConv">
          <div class="chat-head">
            <div class="chat-peer">
              <el-avatar :size="34" :src="activeConv.peerAvatar" />
              <div>
                <div class="chat-name">{{ activeConv.peerName }}</div>
                <div class="chat-status">
                  <!-- 显示的是「对方」的在线状态，不是自己的连接状态 -->
                  <span class="dot" :class="{ on: peerOnline }"></span>{{ peerOnline ? '在线' : '离线' }}
                </div>
              </div>
            </div>
            <el-popconfirm title="清空后仅你自己看不到，对方仍保留记录。确认？"
                           width="240" @confirm="clearChat">
              <template #reference>
                <el-button text size="small" class="clear-btn">
                  <el-icon><Delete /></el-icon> 清空记录
                </el-button>
              </template>
            </el-popconfirm>
          </div>

          <div v-if="activeConv.productTitle" class="chat-product" @click="goProduct(activeConv.productId)">
            <img v-if="activeConv.productImage" :src="activeConv.productImage" />
            <span class="chat-product-title">{{ activeConv.productTitle }}</span>
            <el-icon><ArrowRight /></el-icon>
          </div>

          <div class="chat-body" ref="bodyRef">
            <div v-if="loadingMore" class="load-more">加载中...</div>
            <div v-else-if="hasMore" class="load-more clickable" @click="loadMore">查看更早的消息</div>

            <div v-for="msg in messages" :key="msg.id" class="chat-msg" :class="{ self: isSelf(msg) }">
              <el-avatar :size="32" :src="msg.senderAvatar" />
              <!-- 悬停时出现删除按钮，删除仅对自己隐藏 -->
              <el-icon class="msg-del" title="删除（仅自己不可见）"
                       @click="deleteMessage(msg)"><Delete /></el-icon>
              <div class="msg-bubble">
                <!-- 把消息里的站内链接渲染成可点击的卡片，其余按纯文本显示 -->
                <div class="msg-text">
                  <template v-for="(part, i) in splitLinks(msg.content)" :key="i">
                    <router-link v-if="part.link" :to="part.link" class="msg-link">
                      <el-icon><Link /></el-icon> {{ part.text }}
                    </router-link>
                    <span v-else>{{ part.text }}</span>
                  </template>
                </div>
                <div class="msg-time">{{ formatTime(msg.createTime) }}</div>
              </div>
            </div>
            <el-empty v-if="!messages.length && !loadingMore" description="还没有消息，打个招呼吧" :image-size="70" />
          </div>

          <div class="chat-input">
            <el-input v-model="input" type="textarea" :rows="3" resize="none"
                      placeholder="输入消息，Enter 发送，Shift+Enter 换行"
                      @keydown.enter="handleEnter" maxlength="1000" show-word-limit />
            <el-button type="primary" :disabled="!input.trim()" @click="send">发送</el-button>
          </div>
        </template>

        <el-empty v-else description="选择左侧的会话开始聊天" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, nextTick, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { chatMessageApi } from '../api/chatMessage'
import { wsClient } from '../utils/websocket'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()

const conversations = ref([])
const loading = ref(true)
const activeId = ref(null)
const activeConv = reactive({})
const messages = ref([])
const input = ref('')
const bodyRef = ref(null)

/** 好友在线状态映射：{ userId: true/false } */
const onlineMap = ref({})

const loadingMore = ref(false)
const hasMore = ref(false)
const oldestId = ref(null)

let offChat = null
let offRead = null
let offPresence = null
let offStatus = null
let presenceTimer = null

const totalUnread = computed(() =>
  conversations.value.reduce((sum, c) => sum + (c.unread || 0), 0)
)

/** 当前聊天对象的在线状态 */
const peerOnline = computed(() => !!onlineMap.value[activeConv.peerId])

const currentUserId = () => JSON.parse(localStorage.getItem('user') || 'null')?.userId
const isSelf = (msg) => msg.senderId === currentUserId()

/**
 * 把消息内容按站内链接切分，供模板渲染
 *
 * 例：「你好，我想咨询：\n【自行车】¥200\nhttp://x/product/5」
 *   → [ {text:'你好，我想咨询：\n【自行车】¥200\n'}, {text:'查看商品', link:'/product/5'} ]
 *
 * 只识别本站的商品/主页链接，外部链接按纯文本处理，避免钓鱼风险。
 */
const splitLinks = (content) => {
  if (!content) return []
  const pattern = /https?:\/\/[^\s]+\/product\/(\d+)/g
  const parts = []
  let last = 0
  let m
  while ((m = pattern.exec(content)) !== null) {
    if (m.index > last) {
      parts.push({ text: content.slice(last, m.index) })
    }
    parts.push({ text: '查看商品', link: '/product/' + m[1] })
    last = m.index + m[0].length
  }
  if (last < content.length) {
    parts.push({ text: content.slice(last) })
  }
  return parts
}

const formatTime = (t) => {
  if (!t) return ''
  const date = new Date(t)
  const now = new Date()
  const hh = String(date.getHours()).padStart(2, '0')
  const mm = String(date.getMinutes()).padStart(2, '0')
  if (date.toDateString() === now.toDateString()) return `${hh}:${mm}`
  return `${date.getMonth() + 1}/${date.getDate()}`
}

const loadConversations = async () => {
  try {
    const res = await chatMessageApi.conversations()
    conversations.value = res.data
    // 拉完会话列表后，批量查询这些人的在线状态
    loadOnlineStatus()
  } catch (e) { /* 拦截器已提示 */ }
  finally { loading.value = false }
}

/** 批量查询会话对方的在线状态 */
const loadOnlineStatus = async () => {
  const peerIds = conversations.value.map(c => c.peerId).filter(Boolean)
  if (!peerIds.length) return
  try {
    const res = await chatMessageApi.onlineStatus(peerIds)
    // 保留已有状态，只覆盖本次查到的，避免 PRESENCE 推送被旧数据覆盖
    onlineMap.value = { ...onlineMap.value, ...res.data }
  } catch (e) { /* 在线状态非关键功能，静默失败 */ }
}

const scrollToBottom = async () => {
  await nextTick()
  if (bodyRef.value) bodyRef.value.scrollTop = bodyRef.value.scrollHeight
}

const openConversation = async (conv) => {
  activeId.value = conv.id
  messages.value = []
  hasMore.value = false
  oldestId.value = null
  Object.assign(activeConv, conv)

  try {
    const res = await chatMessageApi.messages(conv.id, { size: 30 })
    messages.value = res.data
    if (res.data.length) {
      oldestId.value = res.data[0].id
      hasMore.value = true
    }
    await scrollToBottom()

    // 标记已读：本地清零 + 通知后端
    if (conv.unread > 0) {
      conv.unread = 0
      await chatMessageApi.markRead(conv.id)
      wsClient.sendRead(conv.id)
    }
  } catch (e) { /* ignore */ }
}

const loadMore = async () => {
  if (loadingMore.value || !oldestId.value) return
  loadingMore.value = true
  try {
    const res = await chatMessageApi.messages(activeId.value, { beforeId: oldestId.value, size: 30 })
    if (res.data.length) {
      messages.value = [...res.data, ...messages.value]
      oldestId.value = res.data[0].id
      hasMore.value = res.data.length >= 30
    } else {
      hasMore.value = false
    }
  } catch (e) { /* ignore */ }
  finally { loadingMore.value = false }
}

const send = () => {
  const text = input.value.trim()
  if (!text) return
  const ok = wsClient.sendChat(activeId.value, text)
  if (!ok) {
    ElMessage.warning('连接已断开，正在重连')
    wsClient.connect()
    return
  }
  input.value = ''
}

const handleEnter = (e) => {
  if (e.shiftKey) return
  e.preventDefault()
  send()
}

const goProduct = (productId) => {
  if (productId) router.push('/product/' + productId)
}

/** 删除单条消息：仅自己不可见，对方仍保留 */
const deleteMessage = async (msg) => {
  try {
    await chatMessageApi.deleteMessage(msg.id)
    // 从本地列表移除，无需重新拉取
    messages.value = messages.value.filter(m => m.id !== msg.id)
    ElMessage.success('已删除，对方仍可见')
  } catch (e) { /* 拦截器已提示 */ }
}

/** 从会话列表中移除：仅自己不可见，对方发新消息会重新出现 */
const removeConversation = async (conv) => {
  try {
    await chatMessageApi.removeConversation(conv.id)
    conversations.value = conversations.value.filter(c => c.id !== conv.id)
    // 移除的正好是当前打开的会话，右侧也清掉
    if (activeId.value === conv.id) {
      activeId.value = null
      messages.value = []
      Object.keys(activeConv).forEach(k => delete activeConv[k])
    }
    ElMessage.success('已移除，对方仍可看到')
  } catch (e) { /* 拦截器已提示 */ }
}

/** 清空整个会话记录：仅自己不可见 */
const clearChat = async () => {
  try {
    await chatMessageApi.clearConversation(activeId.value)
    messages.value = []
    activeConv.lastMessage = ''
    // 同步清掉左侧列表的预览
    const conv = conversations.value.find(c => c.id === activeId.value)
    if (conv) conv.lastMessage = ''
    ElMessage.success('已清空，对方仍保留记录')
  } catch (e) { /* 拦截器已提示 */ }
}

onMounted(async () => {
  await loadConversations()

  // 如果是从别处跳转过来并指定了会话
  const convId = route.query.conversationId
  if (convId) {
    const target = conversations.value.find(c => c.id === Number(convId))
    if (target) openConversation(target)
  }

  offChat = wsClient.on('CHAT', (payload) => {
    const msg = payload.data
    if (!msg) return

    // 更新左侧列表的预览和未读
    const conv = conversations.value.find(c => c.id === msg.conversationId)
    if (conv) {
      conv.lastMessage = msg.content
      conv.lastTime = msg.createTime
      if (msg.conversationId === activeId.value) {
        if (!messages.value.some(m => m.id === msg.id)) {
          messages.value.push(msg)
          scrollToBottom()
        }
        if (!isSelf(msg)) {
          chatMessageApi.markRead(activeId.value)
          wsClient.sendRead(activeId.value)
        }
      } else if (!isSelf(msg)) {
        conv.unread = (conv.unread || 0) + 1
      }
      // 把有新消息的会话顶到最前
      conversations.value = [conv, ...conversations.value.filter(c => c.id !== conv.id)]
    } else {
      // 全新会话，重新拉列表
      loadConversations()
    }
  })

  offRead = wsClient.on('READ', (payload) => {
    if (payload.data?.conversationId === activeId.value) {
      activeConv.peerRead = true
    }
  })

  // 对方的上下线推送
  offPresence = wsClient.on('PRESENCE', (payload) => {
    const { userId, online } = payload.data || {}
    if (userId == null) return
    onlineMap.value = { ...onlineMap.value, [userId]: online }
  })

  // 连接恢复时重新拉一次在线状态（断线期间的变动会错过）
  offStatus = wsClient.on('_status', ({ connected }) => {
    if (connected) loadOnlineStatus()
  })

  // 兜底轮询：WebSocket 推送偶尔会漏，每 60 秒校准一次
  presenceTimer = setInterval(loadOnlineStatus, 60000)
})

onUnmounted(() => {
  if (offChat) offChat()
  if (offRead) offRead()
  if (offPresence) offPresence()
  if (offStatus) offStatus()
  clearInterval(presenceTimer)
})
</script>

<style scoped>
.messages-page { max-width: 1100px; margin: 0 auto; }
.msg-layout { display: flex; gap: 16px; height: calc(100vh - 180px); min-height: 500px; }

/* 左侧会话列表 */
.conv-list { width: 300px; background: #fff; border-radius: 12px; display: flex; flex-direction: column; overflow: hidden; flex-shrink: 0; }
.conv-header { display: flex; align-items: center; gap: 10px; padding: 16px; border-bottom: 1px solid #f0f0f0; }
.conv-header h3 { font-size: 16px; }
.conv-items { flex: 1; overflow-y: auto; }
.conv-item { display: flex; gap: 10px; padding: 12px 16px; cursor: pointer; transition: background .2s; align-items: center; position: relative; }
.conv-item:hover { background: #f7f9fc; }
.conv-item.active { background: #ecf5ff; }
/* 悬停时出现的移除按钮 */
.conv-remove {
  flex-shrink: 0; color: #ccc; font-size: 15px;
  opacity: 0; transition: opacity .2s, color .2s;
}
.conv-item:hover .conv-remove { opacity: 1; }
.conv-remove:hover { color: #f56c6c; }
.conv-content { flex: 1; min-width: 0; }
.conv-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 4px; }
.conv-name { font-size: 14px; font-weight: 500; }
.conv-time { font-size: 11px; color: #bbb; }
.conv-bottom { display: flex; justify-content: space-between; align-items: center; gap: 8px; }
.conv-preview { font-size: 12px; color: #999; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.conv-badge :deep(.el-badge__content) { transform: none; position: static; }
/* 头像右下角的在线小绿点 */
.avatar-wrap { position: relative; flex-shrink: 0; }
.online-dot {
  position: absolute; right: 0; bottom: 0;
  width: 10px; height: 10px; border-radius: 50%;
  background: #d9d9d9; border: 2px solid #fff;
}
.online-dot.on { background: #52c41a; }

/* 右侧聊天区 */
.chat-area { flex: 1; background: #fff; border-radius: 12px; display: flex; flex-direction: column; overflow: hidden; }
.chat-head { padding: 12px 20px; border-bottom: 1px solid #f0f0f0; display: flex; justify-content: space-between; align-items: center; }
.clear-btn { color: #999; }
.clear-btn:hover { color: #f56c6c; }
.chat-peer { display: flex; align-items: center; gap: 10px; }
.chat-name { font-size: 15px; font-weight: 600; }
.chat-status { font-size: 11px; color: #999; display: flex; align-items: center; gap: 4px; }
.dot { width: 6px; height: 6px; border-radius: 50%; background: #d9d9d9; }
.dot.on { background: #52c41a; }

.chat-product { display: flex; align-items: center; gap: 10px; padding: 10px 20px; background: #f7f8fa; cursor: pointer; border-bottom: 1px solid #eee; }
.chat-product:hover { background: #eef3fb; }
.chat-product img { width: 36px; height: 36px; object-fit: cover; border-radius: 6px; }
.chat-product-title { flex: 1; font-size: 13px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.chat-body { flex: 1; overflow-y: auto; padding: 20px; background: #f7f8fa; }
.load-more { text-align: center; font-size: 12px; color: #bbb; padding: 8px 0; }
.load-more.clickable { color: #409eff; cursor: pointer; }

.chat-msg { display: flex; gap: 10px; margin-bottom: 16px; align-items: flex-start; position: relative; }
.chat-msg.self { flex-direction: row-reverse; }
/* 悬停才显示的删除按钮 */
.msg-del {
  align-self: center; cursor: pointer; color: #ccc; font-size: 15px;
  opacity: 0; transition: opacity .2s, color .2s;
}
.chat-msg:hover .msg-del { opacity: 1; }
.msg-del:hover { color: #f56c6c; }
.msg-bubble { max-width: 420px; }
.msg-text { padding: 9px 13px; border-radius: 10px; font-size: 14px; line-height: 1.6; background: #fff; word-break: break-word; white-space: pre-wrap; }
.chat-msg.self .msg-text { background: #409eff; color: #fff; }
/* 消息里的商品链接卡片 */
.msg-link {
  display: inline-flex; align-items: center; gap: 4px;
  padding: 3px 10px; margin-top: 6px;
  background: #ecf5ff; color: #409eff;
  border-radius: 6px; font-size: 13px; text-decoration: none;
  transition: all .2s;
}
.msg-link:hover { background: #d9ecff; }
.chat-msg.self .msg-link { background: rgba(255,255,255,.22); color: #fff; }
.chat-msg.self .msg-link:hover { background: rgba(255,255,255,.32); }
.msg-time { font-size: 11px; color: #bbb; margin-top: 4px; }
.chat-msg.self .msg-time { text-align: right; }

.chat-input { display: flex; gap: 10px; padding: 12px 16px; border-top: 1px solid #f0f0f0; align-items: flex-end; }
.chat-input .el-button { height: 40px; }
</style>
