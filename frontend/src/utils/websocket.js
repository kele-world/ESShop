/**
 * WebSocket 客户端封装
 *
 * 职责：
 *   - 建立连接（Token 通过 URL 参数传递）
 *   - 心跳保活，避免被服务端/代理断开
 *   - 断线自动重连（指数退避）
 *   - 提供订阅机制，多个组件可同时监听消息
 *
 * 用法：
 *   wsClient.connect()
 *   const off = wsClient.on('CHAT', msg => { ... })
 *   wsClient.sendChat(conversationId, '你好')
 */

const RECONNECT_BASE_DELAY = 1000   // 重连基础延迟
const RECONNECT_MAX_DELAY = 30000   // 重连最大延迟
const HEARTBEAT_INTERVAL = 25000    // 心跳间隔

class ChatWebSocketClient {
  constructor() {
    this.ws = null
    this.listeners = new Map()   // type -> Set<callback>
    this.reconnectAttempts = 0
    this.reconnectTimer = null
    this.heartbeatTimer = null
    this.manualClose = false     // 主动断开时不重连
    this.connected = false
  }

  /** 建立连接（已连接则忽略） */
  connect() {
    const token = localStorage.getItem('token')
    if (!token) return

    // 已连接或正在连接中，不重复建连
    if (this.ws && (this.ws.readyState === WebSocket.OPEN || this.ws.readyState === WebSocket.CONNECTING)) {
      return
    }

    this.manualClose = false

    // 根据当前页面协议决定 ws/wss
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const url = `${protocol}//${window.location.host}/ws/chat?token=${encodeURIComponent(token)}`

    try {
      this.ws = new WebSocket(url)
    } catch (e) {
      this.scheduleReconnect()
      return
    }

    this.ws.onopen = () => {
      this.connected = true
      this.reconnectAttempts = 0
      this.startHeartbeat()
      this.emit('_status', { connected: true })
    }

    this.ws.onmessage = (event) => {
      try {
        const payload = JSON.parse(event.data)
        if (payload.type === 'PONG') return
        this.emit(payload.type, payload)
      } catch (e) {
        console.warn('解析 WebSocket 消息失败', e)
      }
    }

    this.ws.onclose = (event) => {
      this.connected = false
      this.stopHeartbeat()
      this.emit('_status', { connected: false })
      // 1006 = 异常关闭（握手被拒/网络中断），排查时很有用
      if (!this.manualClose) {
        console.warn(`[WebSocket] 连接关闭 code=${event.code}，${this.reconnectAttempts + 1}s 后重连`)
        this.scheduleReconnect()
      }
    }

    this.ws.onerror = () => {
      // onerror 后浏览器会紧接着触发 onclose，重连逻辑统一放在 onclose
      console.warn('[WebSocket] 连接出错，请确认后端已启动且 Vite 代理已配置 /ws')
    }
  }

  /** 主动断开（退出登录时调用） */
  disconnect() {
    this.manualClose = true
    this.stopHeartbeat()
    clearTimeout(this.reconnectTimer)
    if (this.ws) {
      this.ws.close()
      this.ws = null
    }
    this.connected = false
  }

  /** 指数退避重连 */
  scheduleReconnect() {
    clearTimeout(this.reconnectTimer)
    const delay = Math.min(
      RECONNECT_BASE_DELAY * Math.pow(2, this.reconnectAttempts),
      RECONNECT_MAX_DELAY
    )
    this.reconnectAttempts++
    this.reconnectTimer = setTimeout(() => this.connect(), delay)
  }

  startHeartbeat() {
    this.stopHeartbeat()
    this.heartbeatTimer = setInterval(() => {
      this.send({ type: 'PING' })
    }, HEARTBEAT_INTERVAL)
  }

  stopHeartbeat() {
    clearInterval(this.heartbeatTimer)
    this.heartbeatTimer = null
  }

  /** 底层发送 */
  send(payload) {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify(payload))
      return true
    }
    return false
  }

  /** 发送聊天消息 */
  sendChat(conversationId, content) {
    return this.send({ type: 'CHAT', conversationId, content })
  }

  /** 发送已读回执 */
  sendRead(conversationId) {
    return this.send({ type: 'READ', conversationId })
  }

  /**
   * 订阅消息
   * @returns 取消订阅的函数
   */
  on(type, callback) {
    if (!this.listeners.has(type)) {
      this.listeners.set(type, new Set())
    }
    this.listeners.get(type).add(callback)
    return () => {
      const set = this.listeners.get(type)
      if (set) set.delete(callback)
    }
  }

  emit(type, payload) {
    const set = this.listeners.get(type)
    if (set) {
      set.forEach(cb => {
        try { cb(payload) } catch (e) { console.warn('WebSocket 监听器执行出错', e) }
      })
    }
  }
}

export const wsClient = new ChatWebSocketClient()
