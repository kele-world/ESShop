import request from './request'

export const orderApi = {
  create: (data) => request.post('/order/create', data),
  myOrders: (params) => request.get('/order/my', { params }),
  mySold: (params) => request.get('/order/my-sold', { params }),
  detail: (id) => request.get(`/order/detail/${id}`),
  pay: (id) => request.put(`/order/${id}/pay`),
  complete: (id) => request.put(`/order/${id}/complete`),
  cancel: (id) => request.put(`/order/${id}/cancel`),
  deleteOrder: (id) => request.delete(`/order/${id}`)
}