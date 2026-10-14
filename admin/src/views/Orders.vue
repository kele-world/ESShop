<template>
  <div class="orders-page">
    <h2>订单管理</h2>
    <div class="status-filter">
      <el-radio-group v-model="statusFilter" size="small" @change="load">
        <el-radio-button value="">全部</el-radio-button>
        <el-radio-button value="PENDING_PAYMENT">待付款</el-radio-button>
        <el-radio-button value="PAID">已付款</el-radio-button>
        <el-radio-button value="COMPLETED">已完成</el-radio-button>
        <el-radio-button value="CANCELLED">已取消</el-radio-button>
      </el-radio-group>
    </div>
    <el-table :data="orders" v-loading="loading" stripe>
      <el-table-column prop="orderNo" label="订单编号" width="180" />
      <el-table-column prop="amount" label="金额" width="100">
        <template #default="{row}">¥{{ row.amount }}</template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{row}">
          <el-tag :type="statusTag(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="productId" label="商品ID" width="80" />
      <el-table-column prop="buyerId" label="买家ID" width="80" />
      <el-table-column prop="sellerId" label="卖家ID" width="80" />
      <el-table-column prop="createTime" label="创建时间" min-width="160">
        <template #default="{row}">{{ row.createTime?.substring(0, 16) }}</template>
      </el-table-column>
    </el-table>
    <div class="pagination" v-if="total > size">
      <el-pagination background layout="prev, pager, next" :total="total" :page-size="size"
                     :current-page="page" @current-change="p => { page = p; load() }" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { adminApi } from '../api/admin'

const orders = ref([])
const loading = ref(true)
const statusFilter = ref('')
const page = ref(1)
const size = ref(10)
const total = ref(0)

const statusTag = (s) => ({ PENDING_PAYMENT: 'warning', PAID: 'primary', COMPLETED: 'success', CANCELLED: 'info' }[s] || 'info')
const statusLabel = (s) => ({ PENDING_PAYMENT: '待付款', PAID: '已付款', COMPLETED: '已完成', CANCELLED: '已取消' }[s] || s)

const load = async () => {
  loading.value = true
  try {
    const res = await adminApi.orderList({ page: page.value, size: size.value, status: statusFilter.value || undefined })
    orders.value = res.data.records
    total.value = res.data.total
  } catch (e) { /* handled */ }
  finally { loading.value = false }
}

onMounted(load)
</script>

<style scoped>
.orders-page h2 { margin-bottom: 16px; }
.status-filter { margin-bottom: 16px; }
.pagination { display: flex; justify-content: center; margin-top: 24px; }
</style>