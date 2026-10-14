import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 是否通过 HTTPS 隧道（cpolar / ngrok 等）对外暴露
// 用法：set TUNNEL=1 && npm run dev
const isTunnel = process.env.TUNNEL === '1'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 3000,
    host: true, // 监听 0.0.0.0，局域网内其他人也能直接访问

    // 允许通过内网穿透域名访问（Vite 默认只信任 localhost，防 DNS 重绑定）
    // 以 . 开头表示匹配该域名及其所有子域名
    allowedHosts: ['.cpolar.top', '.cpolar.cn', '.ngrok-free.app', '.ngrok.io', '.trycloudflare.com'],

    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/uploads': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      // WebSocket 聊天：必须显式开启 ws，否则握手会被 Vite 拒绝
      '/ws': {
        target: 'ws://localhost:8080',
        changeOrigin: true,
        ws: true
      }
    },

    // 走隧道时，HMR 必须连 443（隧道只转发 HTTPS 端口）；
    // 本地开发时不设置，保持默认的 3000，避免热更新失效
    ...(isTunnel ? { hmr: { clientPort: 443 } } : {})
  }
})
