import request from './request'

export const bannerApi = {
  /** 获取当前生效的运营位 */
  list: () => request.get('/banner/list')
}
