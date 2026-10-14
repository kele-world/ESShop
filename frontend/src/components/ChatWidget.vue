<template>
  <div class="chat-widget">
    <!-- 悬浮按钮 -->
    <div class="chat-fab" @click="toggle" v-show="!visible">
      <el-icon :size="24"><ChatDotRound /></el-icon>
      <span class="fab-text">客服</span>
    </div>

    <!-- 聊天窗口 -->
    <transition name="slide-up">
      <div class="chat-panel" v-show="visible">
        <div class="chat-header">
          <div class="header-left">
            <el-avatar :size="32" style="background:#409eff">小E</el-avatar>
            <div>
              <div class="header-title">智能客服</div>
              <div class="header-status">
                <span class="dot" :class="online ? 'on' : 'off'"></span>
                {{ online ? '在线' : '规则库模式' }}
              </div>
            </div>
          </div>
          <div class="header-right">
            <el-icon class="header-btn" @click="clearChat" title="清空对话"><Delete /></el-icon>
            <el-icon class="header-btn" @click="toggle" title="关闭"><Close /></el-icon>
          </div>
        </div>

        <div class="chat-body" ref="bodyRef">
          <div v-for="(msg, i) in messages" :key="i" class="msg-row" :class="msg.role">
            <el-avatar v-if="msg.role === 'ASSISTANT'" :size="28" style="background:#409eff;flex-shrink:0">E</el-avatar>
            <div class="bubble">{{ msg.content }}</div>
            <el-avatar v-if="msg.role === 'USER'" :size="28" :src="userAvatar" style="flex-shrink:0" />
          </div>

          <div v-if="loading" class="msg-row assistant">
            <el-avatar :size="28" style="background:#409eff;flex-shrink:0">E</el-avatar>
            <div class="bubble typing"><span></span><span></span><span></span></div>
          </div>

          <!-- 常见问题引导 -->
          <div v-if="messages.length <= 1 && hotQuestions.length" class="hot-questions">
            <div class="hot-label">你可以这样问：</div>
            <div v-for="q in hotQuestions" :key="q.id" class="hot-item" @click="send(q.question)">
              {{ q.question }}
            </div>
          </div>
        </div>

        <div class="chat-footer">
          <el-input v-model="input" placeholder="输入你的问题..." size="default"
                    @keyup.enter="send()" :disabled="loading">
            <template #suffix>
              <el-icon class="send-btn" @click="send()"><Promotion /></el-icon>
            </template>
          </el-input>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted } from 'vue'
import { chatApi } from '../api/chat'
import { ElMessage } from 'element-plus'

const visible = ref(false)
const input = ref('')
const messages = ref([])
const loading = ref(false)
const online = ref(false)
const hotQuestions = ref([])
const bodyRef = ref(null)

const user = JSON.parse(localStorage.getItem('user') || 'null')
const userAvatar = user?.avatar || ''

// 会话ID：登录用户按 userId，游客用随机串
const sessionId = (() => {
  const key = 'chat_session_id'
  let id = localStorage.getItem(key)
  if (!id) {
    id = (user?.userId ? `u${user.userId}_` : 'guest_') + Date.now()
    localStorage.setItem(key, id)
  }
  return id
})()

const scrollToBottom = async () => {
  await nextTick()
  if (bodyRef.value) bodyRef.value.scrollTop = bodyRef.value.scrollHeight
}

const toggle = () => {
  visible.value = !visible.value
  if (visible.value) {
    scrollToBottom()
    if (messages.value.length === 0) initChat()
  }
}

const initChat = async () => {
  messages.value = [{
    role: 'ASSISTANT',
    content: '你好呀，我是 ESshop 的智能客服小E～\n有什么关于二手交易的问题都可以问我哦！'
  }]
  try {
    const res = await chatApi.hotQuestions()
    hotQuestions.value = res.data
  } catch (e) { /* 忽略 */ }
}

const send = async (text) => {
  const content = (text || input.value).trim()
  if (!content || loading.value) return

  messages.value.push({ role: 'USER', content })
  input.value = ''
  loading.value = true
  scrollToBottom()

  // 探测是否走智能客服服务（超时 1.5s 则视为离线）
  try {
    const res = await chatApi.send(sessionId, content)
    online.value = res.data.source === 'AI'
    messages.value.push({ role: 'ASSISTANT', content: res.data.content })
  } catch (e) {
    messages.value.push({
      role: 'ASSISTANT',
      content: '抱歉，服务暂时不可用，请稍后再试～'
    })
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

const clearChat = async () => {
  try {
    await chatApi.clear(sessionId)
    online.value = false
    initChat()
    ElMessage.success('已清空对话')
  } catch (e) { /* 忽略 */ }
}

onMounted(initChat)
</script>

<style scoped>
.chat-fab {
  position: fixed; right: 28px; bottom: 28px; z-index: 999;
  width: 56px; height: 56px; border-radius: 50%;
  background: linear-gradient(135deg, #409eff, #667eea);
  color: #fff; cursor: pointer;
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  box-shadow: 0 4px 16px rgba(64,158,255,.4);
  transition: transform .2s;
}
.chat-fab:hover { transform: scale(1.08); }
.fab-text { font-size: 10px; margin-top: 1px; }

.chat-panel {
  position: fixed; right: 28px; bottom: 28px; z-index: 1000;
  width: 360px; height: 520px;
  background: #fff; border-radius: 14px;
  box-shadow: 0 8px 32px rgba(0,0,0,.16);
  display: flex; flex-direction: column; overflow: hidden;
}
.slide-up-enter-active, .slide-up-leave-active { transition: all .25s ease; }
.slide-up-enter-from, .slide-up-leave-to { opacity: 0; transform: translateY(20px); }

.chat-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 12px 16px; background: linear-gradient(135deg, #409eff, #667eea); color: #fff;
}
.header-left { display: flex; align-items: center; gap: 10px; }
.header-title { font-size: 15px; font-weight: 600; }
.header-status { font-size: 11px; opacity: .9; display: flex; align-items: center; gap: 4px; }
.dot { width: 6px; height: 6px; border-radius: 50%; }
.dot.on { background: #67c23a; }
.dot.off { background: #e6a23c; }
.header-right { display: flex; gap: 12px; }
.header-btn { cursor: pointer; font-size: 16px; opacity: .85; }
.header-btn:hover { opacity: 1; }

.chat-body { flex: 1; overflow-y: auto; padding: 16px; background: #f7f8fa; }
.msg-row { display: flex; gap: 8px; margin-bottom: 14px; align-items: flex-start; }
.msg-row.user { justify-content: flex-end; }
.bubble {
  max-width: 230px; padding: 9px 13px; border-radius: 10px;
  font-size: 14px; line-height: 1.6; white-space: pre-wrap; word-break: break-word;
}
.msg-row.assistant .bubble { background: #fff; color: #333; border-top-left-radius: 2px; }
.msg-row.user .bubble { background: #409eff; color: #fff; border-top-right-radius: 2px; }

.typing span {
  display: inline-block; width: 6px; height: 6px; margin: 0 2px;
  background: #bbb; border-radius: 50%; animation: blink 1.4s infinite both;
}
.typing span:nth-child(2) { animation-delay: .2s; }
.typing span:nth-child(3) { animation-delay: .4s; }
@keyframes blink { 0%, 80%, 100% { opacity: .3; } 40% { opacity: 1; } }

.hot-questions { margin-top: 8px; }
.hot-label { font-size: 12px; color: #999; margin-bottom: 8px; }
.hot-item {
  background: #fff; border: 1px solid #e8e8e8; border-radius: 8px;
  padding: 8px 12px; font-size: 13px; color: #409eff;
  margin-bottom: 6px; cursor: pointer; transition: all .2s;
}
.hot-item:hover { background: #ecf5ff; border-color: #409eff; }

.chat-footer { padding: 10px 12px; border-top: 1px solid #f0f0f0; background: #fff; }
.send-btn { cursor: pointer; color: #409eff; font-size: 18px; }
.send-btn:hover { color: #66b1ff; }
</style>