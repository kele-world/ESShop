import request from './request'

export const adminApi = {
  login: (data) => request.post('/user/login', data),
  dashboard: () => request.get('/admin/dashboard'),
  userList: (params) => request.get('/admin/user/list', { params }),
  updateUserStatus: (id, status) => request.put(`/admin/user/${id}/status`, { status }),
  productList: (params) => request.get('/admin/product/list', { params }),
  deleteProduct: (id) => request.delete(`/admin/product/${id}`),
  forceOffShelf: (id) => request.put(`/admin/product/${id}/off-shelf`),
  orderList: (params) => request.get('/admin/order/list', { params }),
  addCategory: (data) => request.post('/admin/category', data),
  updateCategory: (id, data) => request.put(`/admin/category/${id}`, data),
  deleteCategory: (id) => request.delete(`/admin/category/${id}`),
  categoryList: () => request.get('/category/list'),

  // 社区管理
  communityPending: (params) => request.get('/admin/community/pending', { params }),
  communityAll: (params) => request.get('/admin/community/all', { params }),
  communityOffShelf: (params) => request.get('/admin/community/off-shelf', { params }),
  communityApprove: (id) => request.put(`/admin/community/${id}/approve`),
  communityReject: (id) => request.put(`/admin/community/${id}/reject`),
  communityOffShelfPost: (id) => request.put(`/admin/community/${id}/off-shelf`),
  communityReList: (id) => request.put(`/admin/community/${id}/re-list`),
  communityDelete: (id) => request.delete(`/admin/community/${id}`),

  // 运营位管理
  bannerList: () => request.get('/admin/banner/list'),
  addBanner: (data) => request.post('/admin/banner', data),
  updateBanner: (id, data) => request.put(`/admin/banner/${id}`, data),
  deleteBanner: (id) => request.delete(`/admin/banner/${id}`)
}