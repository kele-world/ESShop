import request from './request'

export const profileApi = {
  /** 用户主页信息 */
  get: (userId) => request.get(`/user-profile/${userId}`),
  /** 他发布的商品 */
  products: (userId, params) => request.get(`/user-profile/${userId}/products`, { params }),
  /** 他发布的帖子 */
  posts: (userId, params) => request.get(`/user-profile/${userId}/posts`, { params }),
  /** 他收到的评价 */
  reviews: (userId, params) => request.get(`/user-profile/${userId}/reviews`, { params }),
  /** 关注 / 取关 */
  follow: (userId) => request.post(`/user-profile/${userId}/follow`),
  unfollow: (userId) => request.delete(`/user-profile/${userId}/follow`),
  /** 粉丝 / 关注列表 */
  followers: (userId, params) => request.get(`/user-profile/${userId}/followers`, { params }),
  followings: (userId, params) => request.get(`/user-profile/${userId}/followings`, { params })
}
