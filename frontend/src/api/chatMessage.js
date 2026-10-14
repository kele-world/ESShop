import request from './request'

export const chatMessageApi = {
  /**
   * 获取或创建与某卖家的会话
   * @param openingMessage 可选。会话为空时自动发送的开场消息
   */
  createConversation: (sellerId, productId, openingMessage) =>
    request.post('/chat-message/conversation', { sellerId, productId, openingMessage }),
  /** 我的会话列表 */
  conversations: () => request.get('/chat-message/conversations'),
  /** 会话详情 */
  conversation: (id) => request.get(`/chat-message/conversation/${id}`),
  /** 历史消息 */
  messages: (id, params) => request.get(`/chat-message/conversation/${id}/messages`, { params }),
  /** 标记已读 */
  markRead: (id) => request.put(`/chat-message/conversation/${id}/read`),
  /** 未读总数 */
  unread: () => request.get('/chat-message/unread'),
  /** 删除单条消息（仅自己不可见） */
  deleteMessage: (messageId) => request.delete(`/chat-message/message/${messageId}`),
  /** 清空会话记录（仅自己不可见） */
  clearConversation: (conversationId) => request.delete(`/chat-message/conversation/${conversationId}/messages`),
  /** 从列表中移除会话（仅自己不可见，对方发新消息会重新出现） */
  removeConversation: (conversationId) => request.delete(`/chat-message/conversation/${conversationId}`),
  /** 批量查询用户在线状态，返回 { userId: true/false } */
  onlineStatus: (userIds) => request.post('/chat-message/online-status', { userIds })
}
