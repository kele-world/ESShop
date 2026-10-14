import request from './request'

export const userApi = {
  register: (data) => request.post('/user/register', data),
  login: (data) => request.post('/user/login', data),
  logout: () => request.post('/user/logout'),
  getInfo: () => request.get('/user/info'),
  updateInfo: (data) => request.put('/user/info', data)
}