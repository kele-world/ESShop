import request from './request'

export const chatApi = {
  send: (sessionId, content) => request.post('/chat/send', { sessionId, content }),
  history: (sessionId) => request.get('/chat/history', { params: { sessionId } }),
  clear: (sessionId) => request.delete('/chat/history', { params: { sessionId } }),
  hotQuestions: () => request.get('/chat/hot-questions')
}