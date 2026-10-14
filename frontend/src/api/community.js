import request from './request'

export const communityApi = {
  publish: (data) => request.post('/community/publish', data),
  list: (params) => request.get('/community/list', { params }),
  detail: (id) => request.get(`/community/detail/${id}`),
  delete: (id) => request.delete(`/community/${id}`),
  // 点赞
  like: (id) => request.post(`/community/${id}/like`),
  unlike: (id) => request.delete(`/community/${id}/like`),
  // 收藏
  favorite: (id) => request.post(`/community/${id}/favorite`),
  unfavorite: (id) => request.delete(`/community/${id}/favorite`),
  myFavorites: (params) => request.get('/community/my-favorites', { params }),
  // 评论
  comments: (postId) => request.get(`/community/${postId}/comments`),
  addComment: (postId, content) => request.post(`/community/${postId}/comment`, { content }),
  deleteComment: (id) => request.delete(`/community/comment/${id}`),
  // 分类
  categoryList: () => request.get('/community/category/list')
}