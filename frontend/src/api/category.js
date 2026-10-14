import request from './request'

export const categoryApi = {
  list: () => request.get('/category/list')
}