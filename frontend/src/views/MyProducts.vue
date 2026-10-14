<template>
  <div class="my-products-page">
    <div class="page-header">
      <h2>我的发布</h2>
      <router-link to="/publish"><el-button type="primary">发布新商品</el-button></router-link>
    </div>
    <el-table :data="products" v-loading="loading" stripe style="width:100%">
      <el-table-column prop="title" label="商品" min-width="200">
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
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{row}">
          <el-tag :type="statusTag(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="viewCount" label="浏览" width="80" />
      <el-table-column prop="createTime" label="发布时间" width="160">
        <template #default="{row}">{{ row.createTime?.substring(0, 16) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="260" fixed="right">
        <template #default="{row}">
          <el-button size="small" text @click="$router.push('/product/'+row.id)">查看</el-button>
          <el-button size="small" text type="warning" @click="showChangePrice(row)" v-if="row.status === 'ON_SALE'">改价</el-button>
          <el-button size="small" text type="primary" @click="$router.push({path:'/publish', query:{id:row.id}})" v-if="row.status === 'ON_SALE'">编辑</el-button>
          <el-button size="small" text type="danger" @click="offShelf(row.id)" v-if="row.status === 'ON_SALE'">下架</el-button>
          <el-button size="small" text type="success" @click="reShelf(row.id)" v-if="row.status === 'OFF_SHELF'">重新上架</el-button>
          <el-popconfirm title="确认删除？" @confirm="del(row.id)">
            <template #reference><el-button size="small" text type="danger">删除</el-button></template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <!-- 改价对话框 -->
    <el-dialog v-model="priceDialog" title="快捷改价" width="360px">
      <div class="price-dialog-body">
        <div class="current-price">当前价格：<span>¥{{ currentProduct?.price }}</span></div>
        <el-input-number v-model="newPrice" :min="0" :precision="2" :step="1" style="width:100%" />
      </div>
      <template #footer>
        <el-button @click="priceDialog = false">取消</el-button>
        <el-button type="primary" :loading="priceSaving" @click="submitPrice">确认改价</el-button>
      </template>
    </el-dialog>
    <div class="pagination" v-if="total > size">
      <el-pagination background layout="prev, pager, next" :total="total" :page-size="size"
                     :current-page="page" @current-change="p => { page = p; load() }" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { productApi } from '../api/product'
import { ElMessage, ElMessageBox } from 'element-plus'

const products = ref([])
const loading = ref(true)
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
    const res = await productApi.myProducts({ page: page.value, size: size.value })
    products.value = res.data.records
    total.value = res.data.total
  } catch (e) { /* handled */ }
  finally { loading.value = false }
}

const offShelf = async (id) => {
  try {
    await ElMessageBox.confirm('确认下架该商品？', '提示', { type: 'warning' })
    await productApi.offShelf(id)
    ElMessage.success('已下架')
    load()
  } catch (e) { /* cancelled */ }
}

const reShelf = async (id) => {
  try {
    await ElMessageBox.confirm('确认重新上架该商品？', '提示', { type: 'info' })
    await productApi.reShelf(id)
    ElMessage.success('已重新上架')
    load()
  } catch (e) { /* cancelled */ }
}

const del = async (id) => {
  try {
    await productApi.deleteProduct(id)
    ElMessage.success('已删除')
    load()
  } catch (e) { /* handled */ }
}

// 快捷改价
const priceDialog = ref(false)
const currentProduct = ref(null)
const newPrice = ref(0)
const priceSaving = ref(false)

const showChangePrice = (row) => {
  currentProduct.value = row
  newPrice.value = row.price
  priceDialog.value = true
}

const submitPrice = async () => {
  priceSaving.value = true
  try {
    await productApi.changePrice(currentProduct.value.id, newPrice.value)
    ElMessage.success('改价成功')
    priceDialog.value = false
    load()
  } catch (e) { /* handled */ }
  finally { priceSaving.value = false }
}

onMounted(load)
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.page-header h2 { font-size: 20px; }
.product-cell { display: flex; align-items: center; gap: 10px; }
.thumb { width: 50px; height: 50px; object-fit: cover; border-radius: 6px; background: #f5f5f5; }
.pagination { display: flex; justify-content: center; margin-top: 24px; }
.price-dialog-body { padding: 8px 0; }
.current-price { font-size: 14px; color: #666; margin-bottom: 12px; }
.current-price span { color: #f5222d; font-weight: 700; font-size: 18px; }
</style>