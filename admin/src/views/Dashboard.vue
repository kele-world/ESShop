<template>
  <div class="dashboard">
    <h2>仪表盘</h2>
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background:#e6f7ff"><el-icon :size="32" color="#1890ff"><User /></el-icon></div>
          <div class="stat-info">
            <div class="stat-value">{{ data.userCount }}</div>
            <div class="stat-label">用户总数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background:#f6ffed"><el-icon :size="32" color="#52c41a"><Goods /></el-icon></div>
          <div class="stat-info">
            <div class="stat-value">{{ data.productCount }}</div>
            <div class="stat-label">在售商品</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background:#fff7e6"><el-icon :size="32" color="#fa8c16"><Document /></el-icon></div>
          <div class="stat-info">
            <div class="stat-value">{{ data.orderCount }}</div>
            <div class="stat-label">订单总数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background:#f9f0ff"><el-icon :size="32" color="#722ed1"><CircleCheck /></el-icon></div>
          <div class="stat-info">
            <div class="stat-value">{{ data.completedOrderCount }}</div>
            <div class="stat-label">已完成订单</div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { adminApi } from '../api/admin'

const data = ref({ userCount: 0, productCount: 0, orderCount: 0, completedOrderCount: 0 })

onMounted(async () => {
  try {
    const res = await adminApi.dashboard()
    data.value = res.data
  } catch (e) { /* handled */ }
})
</script>

<style scoped>
.dashboard h2 { margin-bottom: 24px; }
.stat-card { display: flex; align-items: center; }
.stat-card :deep(.el-card__body) { display: flex; align-items: center; gap: 16px; width: 100%; }
.stat-icon { width: 64px; height: 64px; display: flex; align-items: center; justify-content: center; border-radius: 12px; }
.stat-value { font-size: 28px; font-weight: 700; color: #333; }
.stat-label { font-size: 14px; color: #999; margin-top: 4px; }
</style>