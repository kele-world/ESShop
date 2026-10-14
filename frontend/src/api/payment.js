import request from './request'

export const paymentApi = {
  /** 创建支付单（打开收银台） */
  create: (orderId, payMethod) => request.post('/payment/create', { orderId, payMethod }),
  /** 确认支付（模拟支付成功） */
  confirm: (payNo) => request.post('/payment/confirm', { payNo }),
  /** 查询订单支付信息 */
  getByOrderId: (orderId) => request.get(`/payment/order/${orderId}`)
}
