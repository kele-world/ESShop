<template>
  <div class="product-detail-page" v-loading="loading">
    <div class="detail-container" v-if="product">
      <!-- 图片区 -->
      <div class="image-section">
        <div class="main-image">
          <img :src="currentImage" alt="" />
        </div>
        <div class="thumb-list" v-if="imageList.length > 1">
          <img v-for="(img, i) in imageList" :key="i" :src="img"
               :class="{ active: currentImage === img }" @click="currentImage = img" />
        </div>
      </div>
      <!-- 信息区 -->
      <div class="info-section">
        <h1 class="title">{{ product.title }}</h1>
        <div class="price-row">
          <span class="price">¥{{ product.price }}</span>
          <span class="original" v-if="product.originalPrice">原价 ¥{{ product.originalPrice }}</span>
        </div>
        <div class="meta-row">
          <el-tag type="info">{{ product.categoryName }}</el-tag>
          <span>{{ product.viewCount }} 次浏览</span>
          <span>{{ product.createTime?.substring(0, 10) }}</span>
        </div>
        <div class="seller-row">
          <!-- 点击卖家进入其主页（定位到「闲置」Tab） -->
          <el-avatar :size="40" :src="product.sellerAvatar" class="seller-link"
                     @click="goSellerProfile('products')" />
          <span class="seller-name seller-link" @click="goSellerProfile('products')">
            {{ product.sellerName }}
          </span>
          <span class="seller-credit" @click="goSellerProfile('reviews')" v-if="sellerRating > 0">
            ★ {{ sellerRating }} · {{ sellerReviewCount }} 条评价
          </span>
          <span class="seller-credit muted" @click="goSellerProfile('reviews')" v-else>
            暂无评价
          </span>
        </div>
        <div class="actions">
          <el-button type="primary" size="large" @click="buyNow" :disabled="product.status !== 'ON_SALE'">
            {{ product.status === 'ON_SALE' ? '立即购买' : '已售出/已下架' }}
          </el-button>
          <el-button v-if="!isOwner" size="large" @click="contactSeller">
            <el-icon><ChatDotRound /></el-icon> 联系卖家
          </el-button>
          <el-button size="large" @click="toggleFav" :type="favorited ? 'warning' : 'default'">
            <el-icon><StarFilled v-if="favorited" /><Star v-else /></el-icon>
            {{ favorited ? '已收藏' : '收藏' }} {{ product.favoriteCount || 0 }}
          </el-button>
          <el-button v-if="isOwner" size="large" @click="$router.push('/my-products')">管理商品</el-button>
        </div>
      </div>
    </div>
    <!-- 描述区 -->
    <div class="desc-section" v-if="product">
      <h3>商品描述</h3>
      <p>{{ product.description || '暂无描述' }}</p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { productApi } from '../api/product'
import { orderApi } from '../api/order'
import { chatMessageApi } from '../api/chatMessage'
import { profileApi } from '../api/profile'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()
const product = ref(null)
const loading = ref(true)
const currentImage = ref('')
const favorited = ref(false)

const imageList = computed(() => {
  if (!product.value?.images) return []
  return product.value.images.split(',').filter(Boolean)
})

const isOwner = computed(() => {
  const user = JSON.parse(localStorage.getItem('user') || '{}')
  return user.userId === product.value?.userId
})

const buyNow = async () => {
  const token = localStorage.getItem('token')
  if (!token) { router.push('/login'); return }
  try {
    await ElMessageBox.confirm(`确认购买「${product.value.title}」，价格 ¥${product.value.price}？`, '确认下单', { type: 'info' })
    const res = await orderApi.create({ productId: product.value.id })
    ElMessage.success('下单成功，正在跳转支付...')
    // 直接跳转收银台，省去用户再去订单中心的一步
    router.push({ path: '/payment', query: { orderId: res.data.id } })
  } catch (e) { /* */ }
}

/**
 * 联系卖家：创建会话后跳到消息中心
 *
 * 首次咨询会自动发一条开场消息，带上商品链接，
 * 让卖家一眼知道买家问的是哪件商品。已有对话则不会重复发送。
 */
const contactSeller = async () => {
  const token = localStorage.getItem('token')
  if (!token) { router.push('/login'); return }
  try {
    const p = product.value
    const link = `${window.location.origin}/product/${p.id}`
    const opening = `你好，我想咨询这件商品：\n【${p.title}】¥${p.price}\n${link}`

    const res = await chatMessageApi.createConversation(p.userId, p.id, opening)
    router.push({ path: '/messages', query: { conversationId: res.data.id } })
  } catch (e) { /* 拦截器已提示 */ }
}

const toggleFav = async () => {
  const token = localStorage.getItem('token')
  if (!token) { router.push('/login'); return }
  try {
    if (favorited.value) {
      await productApi.unfavorite(product.value.id)
      favorited.value = false
      // 本地同步更新计数，避免额外请求
      product.value.favoriteCount = Math.max(0, (product.value.favoriteCount || 0) - 1)
      ElMessage.success('已取消收藏')
    } else {
      await productApi.favorite(product.value.id)
      favorited.value = true
      product.value.favoriteCount = (product.value.favoriteCount || 0) + 1
      ElMessage.success('已收藏')
    }
  } catch (e) { /* */ }
}

// ==================== 卖家信誉 ====================
// 商品详情页只在卖家信息行显示累计评分，
// 详细的评价列表在卖家主页的「评价」Tab，这里不重复展示。
const sellerReviewCount = ref(0)
const sellerRating = ref(0)

const loadSellerReviews = async (sellerId) => {
  try {
    const res = await profileApi.reviews(sellerId, { page: 1, size: 1 })
    sellerReviewCount.value = res.data.total
    sellerRating.value = res.data.avgRating
  } catch (e) { /* 静默失败，不影响商品展示 */ }
}

/** 进入卖家主页，可指定落到哪个 Tab */
const goSellerProfile = (tab) => {
  if (!product.value?.userId) return
  router.push({ path: '/user/' + product.value.userId, query: { tab } })
}

onMounted(async () => {
  try {
    const res = await productApi.detail(route.params.id)
    product.value = res.data
    // 后端返回当前用户的收藏状态，未登录时为 null
    favorited.value = res.data.favorited === true
    // 只取卖家的评分汇总，用于卖家信息行展示
    if (res.data.userId) loadSellerReviews(res.data.userId)
    currentImage.value = imageList.value[0] || 'data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 400 400"><rect fill="%23f0f0f0" width="400" height="400"/><text x="50%" y="50%" text-anchor="middle" dy=".3em" fill="%23ccc" font-size="20">暂无图片</text></svg>'
  } catch (e) { /* handled */ }
  finally { loading.value = false }
})
</script>

<style scoped>
.detail-container { display: flex; gap: 32px; background: #fff; padding: 24px; border-radius: 12px; }
.image-section { flex: 0 0 400px; }
.main-image { width: 400px; height: 400px; border-radius: 8px; overflow: hidden; background: #f5f5f5; }
.main-image img { width: 100%; height: 100%; object-fit: cover; }
.thumb-list { display: flex; gap: 8px; margin-top: 12px; }
.thumb-list img { width: 60px; height: 60px; object-fit: cover; border-radius: 6px; cursor: pointer; border: 2px solid transparent; }
.thumb-list img.active { border-color: #409eff; }
.info-section { flex: 1; }
.title { font-size: 22px; font-weight: 600; margin-bottom: 16px; }
.price-row { margin-bottom: 16px; }
.price { font-size: 28px; color: #f5222d; font-weight: 700; }
.original { font-size: 14px; color: #999; text-decoration: line-through; margin-left: 12px; }
.meta-row { display: flex; gap: 16px; align-items: center; font-size: 13px; color: #999; margin-bottom: 20px; }
.seller-row { display: flex; align-items: center; gap: 12px; padding: 16px 0; border-top: 1px solid #eee; border-bottom: 1px solid #eee; margin-bottom: 20px; }
.seller-name { font-size: 15px; font-weight: 500; }
/* 卖家信息可点击进入主页 */
.seller-link { cursor: pointer; }
.seller-link:hover { color: #409eff; }
.seller-credit { font-size: 13px; color: #f59e0b; cursor: pointer; margin-left: auto; }
.seller-credit.muted { color: #bbb; }
.seller-credit:hover { text-decoration: underline; }
.actions { display: flex; gap: 12px; }
.desc-section { background: #fff; padding: 24px; border-radius: 12px; margin-top: 16px; }
.desc-section h3 { font-size: 16px; margin-bottom: 12px; }
.desc-section p { font-size: 14px; color: #666; line-height: 1.8; white-space: pre-wrap; }
@media (max-width: 768px) { .detail-container { flex-direction: column; } .image-section { flex: none; } .main-image { width: 100%; height: auto; } }
</style>