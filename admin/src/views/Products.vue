<template>
  <div class="products-page">
    <h2>商品管理</h2>
    <div class="search-bar">
      <el-input v-model="keyword" placeholder="搜索商品标题" clearable style="width:260px" @keyup.enter="load" />
      <el-button type="primary" @click="load">搜索</el-button>
    </div>
    <el-table :data="products" v-loading="loading" stripe>
      <el-table-column label="商品" min-width="220">
        <template #default="{row}">
          <div class="product-cell">
            <img :src="getFirstImage(row.images)" class="thumb" />
            <span>{{ row.title }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="price" label="价格" width="100">
        <template #default="{row}">¥{{ row.price }}</template>
      </el-table-column>
      <el-table-column prop="categoryName" label="分类" width="100" />
      <el-table-column prop="sellerName" label="发布者" width="120" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{row}">
          <el-tag :type="statusTag(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="发布时间" width="160">
        <template #default="{row}">{{ row.createTime?.substring(0, 16) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{row}">
          <el-button size="small" type="warning" @click="forceOffShelf(row.id)" v-if="row.status === 'ON_SALE'">下架</el-button>
          <el-popconfirm title="确认删除？" @confirm="del(row.id)">
            <template #reference><el-button size="small" type="danger">删除</el-button></template>
          </el-popconfirm>
        </template>
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
import { ElMessage } from 'element-plus'

const products = ref([])
const loading = ref(true)
const keyword = ref('')
const page = ref(1)
const size = ref(10)
const total = ref(0)

const statusTag = (s) => ({ ON_SALE: 'success', SOLD: 'info', OFF_SHELF: 'warning' }[s] || 'info')
const statusLabel = (s) => ({ ON_SALE: '在售', SOLD: '已售出', OFF_SHELF: '已下架' }[s] || s)

const getFirstImage = (images) => {
  if (!images) return ''
  return images.split(',')[0]
}

const load = async () => {
  loading.value = true
  try {
    const res = await adminApi.productList({ page: page.value, size: size.value, keyword: keyword.value || undefined })
    products.value = res.data.records
    total.value = res.data.total
  } catch (e) { /* handled */ }
  finally { loading.value = false }
}

const forceOffShelf = async (id) => {
  try { await adminApi.forceOffShelf(id); ElMessage.success('已下架'); load() } catch (e) { /* handled */ }
}
const del = async (id) => {
  try { await adminApi.deleteProduct(id); ElMessage.success('已删除'); load() } catch (e) { /* handled */ }
}

onMounted(load)
</script>

<style scoped>
.products-page h2 { margin-bottom: 16px; }
.search-bar { display: flex; gap: 12px; margin-bottom: 16px; }
.product-cell { display: flex; align-items: center; gap: 10px; }
.thumb { width: 40px; height: 40px; object-fit: cover; border-radius: 6px; background: #f5f5f5; }
.pagination { display: flex; justify-content: center; margin-top: 24px; }
</style>