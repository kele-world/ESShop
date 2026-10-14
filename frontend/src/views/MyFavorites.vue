<template>
  <div class="favorites-page">
    <h2>我的收藏</h2>
    <el-tabs v-model="tab" @tab-change="load">
      <el-tab-pane label="收藏的帖子" name="POST" />
      <el-tab-pane label="收藏的商品" name="PRODUCT" />
    </el-tabs>

    <div class="fav-list" v-loading="loading">
      <div v-for="item in items" :key="item.id" class="fav-card">
        <div class="fav-main" @click="tab === 'POST' ? openPost(item) : openProduct(item)">
          <img :src="getFirstImage(item.targetImage)" class="fav-img" />
          <div class="fav-info">
            <div class="fav-title">{{ item.targetTitle }}</div>
            <div class="fav-time">{{ item.createTime?.substring(0, 16) }}</div>
          </div>
        </div>
        <el-popconfirm title="确认取消收藏？" @confirm="removeFav(item)">
          <template #reference>
            <el-button size="small" text type="danger" class="fav-btn">取消收藏</el-button>
          </template>
        </el-popconfirm>
      </div>
      <el-empty v-if="!loading && items.length === 0" description="暂无收藏" />
      <div class="pagination" v-if="total > size">
        <el-pagination background layout="prev, pager, next" :total="total" :page-size="size"
                       :current-page="page" @current-change="p => { page = p; load() }" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { communityApi } from '../api/community'
import { productApi } from '../api/product'
import { ElMessage } from 'element-plus'

const router = useRouter()
const tab = ref('POST')
const items = ref([])
const loading = ref(true)
const page = ref(1)
const size = ref(10)
const total = ref(0)

const getFirstImage = (images) => {
  if (!images) return ''
  return images.split(',')[0]
}

const load = async () => {
  loading.value = true
  try {
    const api = tab.value === 'POST' ? communityApi.myFavorites : productApi.myFavorites
    const res = await api({ page: page.value, size: size.value })
    items.value = res.data.records
    total.value = res.data.total
  } catch (e) { /* */ }
  finally { loading.value = false }
}

const openPost = (item) => { router.push('/community') }
const openProduct = (item) => { router.push('/product/' + item.targetId) }

const removeFav = async (item) => {
  try {
    if (tab.value === 'POST') {
      await communityApi.unfavorite(item.targetId)
    } else {
      await productApi.unfavorite(item.targetId)
    }
    ElMessage.success('已取消收藏')
    load()
  } catch (e) { /* 拦截器已提示 */ }
}

onMounted(load)
</script>

<style scoped>
.favorites-page { max-width: 700px; margin: 0 auto; }
.favorites-page h2 { font-size: 20px; margin-bottom: 16px; }
.fav-card { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 12px; background: #fff; border-radius: 8px; margin-bottom: 10px; transition: all .2s; }
.fav-card:hover { box-shadow: 0 2px 8px rgba(0,0,0,.08); }
.fav-main { display: flex; gap: 12px; flex: 1; min-width: 0; cursor: pointer; }
.fav-btn { flex-shrink: 0; }
.fav-img { width: 60px; height: 60px; object-fit: cover; border-radius: 6px; background: #f5f5f5; }
.fav-title { font-size: 15px; font-weight: 500; margin-bottom: 4px; }
.fav-time { font-size: 12px; color: #999; }
.pagination { display: flex; justify-content: center; margin-top: 24px; }
</style>