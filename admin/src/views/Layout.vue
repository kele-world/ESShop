<template>
  <el-container class="admin-layout">
    <el-aside width="220px">
      <div class="logo">ESshop 管理</div>
      <el-menu :default-active="route.path" router background-color="#304156" text-color="#bfcbd9" active-text-color="#409eff">
        <el-menu-item index="/">
          <el-icon><DataAnalysis /></el-icon><span>仪表盘</span>
        </el-menu-item>
        <el-menu-item index="/users">
          <el-icon><User /></el-icon><span>用户管理</span>
        </el-menu-item>
        <el-menu-item index="/products">
          <el-icon><Goods /></el-icon><span>商品管理</span>
        </el-menu-item>
        <el-menu-item index="/orders">
          <el-icon><Document /></el-icon><span>订单管理</span>
        </el-menu-item>
        <el-menu-item index="/categories">
          <el-icon><Menu /></el-icon><span>分类管理</span>
        </el-menu-item>
        <el-menu-item index="/community">
          <el-icon><ChatLineSquare /></el-icon><span>社区管理</span>
        </el-menu-item>
        <el-menu-item index="/banners">
          <el-icon><Picture /></el-icon><span>运营位管理</span>
        </el-menu-item>
      </el-menu>
      <div class="logout-btn" @click="logout">
        <el-icon><SwitchButton /></el-icon><span>退出登录</span>
      </div>
    </el-aside>
    <el-main>
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup>
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const logout = () => {
  localStorage.removeItem('admin_token')
  localStorage.removeItem('admin_user')
  router.push('/login')
}
</script>

<style scoped>
.admin-layout { min-height: 100vh; }
.el-aside { background: #304156; display: flex; flex-direction: column; }
.logo { height: 60px; display: flex; align-items: center; justify-content: center; color: #fff; font-size: 18px; font-weight: 700; border-bottom: 1px solid rgba(255,255,255,.1); }
.logout-btn { margin-top: auto; padding: 16px; color: #bfcbd9; cursor: pointer; display: flex; align-items: center; gap: 8px; border-top: 1px solid rgba(255,255,255,.1); }
.logout-btn:hover { color: #409eff; }
.el-main { background: #f0f2f5; padding: 24px; }
</style>