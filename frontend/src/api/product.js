import request from './request'

export const productApi = {
  publish: (data) => request.post('/product/publish', data),
  list: (params) => request.get('/product/list', { params }),
  detail: (id) => request.get(`/product/detail/${id}`),
  update: (id, data) => request.put(`/product/${id}`, data),
  changePrice: (id, price) => request.put(`/product/${id}/price`, { price }),
  offShelf: (id) => request.put(`/product/${id}/off-shelf`),
  reShelf: (id) => request.put(`/product/${id}/re-shelf`),
  deleteProduct: (id) => request.delete(`/product/${id}`),
  myProducts: (params) => request.get('/product/my', { params }),
  // 收藏
  favorite: (id) => request.post(`/product/${id}/favorite`),
  unfavorite: (id) => request.delete(`/product/${id}/favorite`),
  myFavorites: (params) => request.get('/product/my-favorites', { params }),
  // 卖家评价（评价对象是卖家，查询接口在 profileApi.reviews）
  submitReview: (data) => request.post('/product/review', data),
  // 自然语言搜索：如「有没有便宜的代步工具」，由向量检索语义匹配
  semanticSearch: (query, size = 12) =>
    request.get('/product/semantic-search', { params: { query, size } })
}

export const uploadApi = {
  uploadImage: (file) => {
    const formData = new FormData()
    formData.append('file', file)
    return request.post('/upload/image', formData)
  }
}