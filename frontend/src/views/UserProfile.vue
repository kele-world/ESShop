<template>
  <div class="profile-page" v-loading="loading">
    <!-- 顶部资料卡 -->
    <div class="profile-header" v-if="profile.userId">
      <div class="header-main">
        <el-avatar :size="88" :src="profile.avatar" class="profile-avatar" />
        <div class="profile-info">
          <div class="name-row">
            <h2 class="nickname">{{ profile.nickname || profile.username }}</h2>
            <el-tag v-if="profile.avgRating > 0" type="warning" size="small" effect="dark">
              ★ {{ profile.avgRating }}
            </el-tag>
            <el-tag v-else type="info" size="small" effect="plain">暂无评价</el-tag>
          </div>
          <div class="stats-row">
            <span class="stat"><b>{{ profile.productCount || 0 }}</b> 件闲置</span>
            <span class="stat"><b>{{ profile.postCount || 0 }}</b> 篇帖子</span>
            <span class="stat"><b>{{ profile.reviewCount || 0 }}</b> 条评价</span>
            <span class="stat clickable" @click="showFollowList('followers')">
              <b>{{ profile.followerCount || 0 }}</b> 粉丝
            </span>
            <span class="stat clickable" @click="showFollowList('followings')">
              <b>{{ profile.followingCount || 0 }}</b> 关注
            </span>
          </div>
        </div>

        <div class="header-actions">
          <template v-if="!profile.self">
            <el-button :type="profile.following ? 'default' : 'primary'"
                       @click="toggleFollow" :loading="followLoading">
              <el-icon v-if="!profile.following"><Plus /></el-icon>
              {{ profile.following ? '已关注' : '关注' }}
            </el-button>
            <el-button @click="contactHim">
              <el-icon><ChatDotRound /></el-icon> 私信
            </el-button>
          </template>
          <el-button v-else type="primary" plain @click="$router.push('/user-center')">
            编辑资料
          </el-button>
        </div>
      </div>
    </div>

    <!-- 内容 Tab -->
    <div class="profile-body" v-if="profile.userId">
      <el-tabs v-model="activeTab" @tab-change="loadTab">
        <!-- 商品 -->
        <el-tab-pane label="他的闲置" name="products">
          <div class="product-grid" v-loading="tabLoading">
            <div v-for="p in products" :key="p.id" class="product-card"
                 @click="$router.push('/product/' + p.id)">
              <div class="product-img">
                <img :src="getFirstImage(p.images)" />
                <span class="price">¥{{ p.price }}</span>
              </div>
              <div class="product-title">{{ p.title }}</div>
            </div>
            <el-empty v-if="!tabLoading && !products.length" description="还没有发布闲置" />
          </div>
          <div class="pagination" v-if="productTotal > pageSize">
            <el-pagination background layout="prev, pager, next" :total="productTotal"
                           :page-size="pageSize" :current-page="productPage"
                           @current-change="p => { productPage = p; loadProducts() }" />
          </div>
        </el-tab-pane>

        <!-- 社区 -->
        <el-tab-pane label="他的社区" name="posts">
          <div class="post-grid" v-loading="tabLoading">
            <div v-for="post in posts" :key="post.id" class="post-card">
              <div class="post-img" v-if="post.images">
                <img :src="getFirstImage(post.images)" />
              </div>
              <div class="post-img placeholder" v-else>
                <el-icon :size="32"><Document /></el-icon>
              </div>
              <div class="post-body">
                <div class="post-title">{{ post.title }}</div>
                <div class="post-stats">
                  <span><el-icon><Star /></el-icon> {{ post.likeCount }}</span>
                  <span><el-icon><ChatDotRound /></el-icon> {{ post.commentCount }}</span>
                </div>
              </div>
            </div>
            <el-empty v-if="!tabLoading && !posts.length" description="还没有发布帖子" />
          </div>
          <div class="pagination" v-if="postTotal > pageSize">
            <el-pagination background layout="prev, pager, next" :total="postTotal"
                           :page-size="pageSize" :current-page="postPage"
                           @current-change="p => { postPage = p; loadPosts() }" />
          </div>
        </el-tab-pane>

        <!-- 评价 -->
        <el-tab-pane :label="`收到的评价 (${profile.reviewCount || 0})`" name="reviews">
          <div class="review-list" v-loading="tabLoading">
            <div class="review-summary" v-if="reviewTotal > 0">
              <div class="summary-score">{{ avgRating }}</div>
              <div class="summary-detail">
                <el-rate :model-value="avgRating" disabled />
                <div class="summary-text">共 {{ reviewTotal }} 条评价</div>
              </div>
            </div>

            <div v-for="r in reviews" :key="r.id" class="review-item">
              <el-avatar :size="38" :src="r.userAvatar" class="clickable-avatar"
                         @click="goProfile(r.userId)" />
              <div class="review-body">
                <div class="review-top">
                  <span class="review-user clickable-name" @click="goProfile(r.userId)">{{ r.userName }}</span>
                  <el-rate :model-value="r.rating" disabled size="small" />
                  <span class="review-time">{{ r.createTime?.substring(0, 10) }}</span>
                </div>
                <div class="review-text">{{ r.content || '该用户没有填写评价内容' }}</div>
                <div class="review-goods" v-if="r.productTitle">交易商品：{{ r.productTitle }}</div>
              </div>
            </div>

            <el-empty v-if="!tabLoading && !reviews.length" description="还没有收到评价" />
          </div>
          <div class="pagination" v-if="reviewTotal > pageSize">
            <el-pagination background layout="prev, pager, next" :total="reviewTotal"
                           :page-size="pageSize" :current-page="reviewPage"
                           @current-change="p => { reviewPage = p; loadReviews() }" />
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 粉丝/关注列表 -->
    <el-dialog v-model="followDialog" :title="followMode === 'followers' ? '粉丝' : '关注'" width="420px">
      <div class="follow-list" v-loading="followListLoading">
        <div v-for="u in followList" :key="u.id" class="follow-item" @click="goProfile(u.id)">
          <el-avatar :size="42" :src="u.avatar" />
          <span class="follow-name">{{ u.nickname || u.username }}</span>
        </div>
        <el-empty v-if="!followListLoading && !followList.length" description="暂无" :image-size="70" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { profileApi } from '../api/profile'
import { chatMessageApi } from '../api/chatMessage'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const tabLoading = ref(false)
const profile = reactive({})
const activeTab = ref('products')

const products = ref([]); const productPage = ref(1); const productTotal = ref(0)
const posts = ref([]); const postPage = ref(1); const postTotal = ref(0)
const reviews = ref([]); const reviewPage = ref(1); const reviewTotal = ref(0); const avgRating = ref(0)
const pageSize = ref(8)

const followLoading = ref(false)
const followDialog = ref(false)
const followMode = ref('followers')
const followList = ref([])
const followListLoading = ref(false)

const getFirstImage = (images) => images ? images.split(',')[0] : ''

const loadProfile = async () => {
  loading.value = true
  try {
    const res = await profileApi.get(route.params.userId)
    Object.assign(profile, res.data)
  } catch (e) {
    ElMessage.error('用户不存在')
    router.push('/')
  } finally { loading.value = false }
}

const loadProducts = async () => {
  tabLoading.value = true
  try {
    const res = await profileApi.products(route.params.userId, { page: productPage.value, size: pageSize.value })
    products.value = res.data.records
    productTotal.value = res.data.total
  } catch (e) { /* */ } finally { tabLoading.value = false }
}

const loadPosts = async () => {
  tabLoading.value = true
  try {
    const res = await profileApi.posts(route.params.userId, { page: postPage.value, size: pageSize.value })
    posts.value = res.data.records
    postTotal.value = res.data.total
  } catch (e) { /* */ } finally { tabLoading.value = false }
}

const loadReviews = async () => {
  tabLoading.value = true
  try {
    const res = await profileApi.reviews(route.params.userId, { page: reviewPage.value, size: 10 })
    reviews.value = res.data.reviews
    reviewTotal.value = res.data.total
    avgRating.value = res.data.avgRating
  } catch (e) { /* */ } finally { tabLoading.value = false }
}

/** Tab 切换时按需加载，避免一次性请求三个接口 */
const loadTab = () => {
  if (activeTab.value === 'products' && !products.value.length) loadProducts()
  else if (activeTab.value === 'posts' && !posts.value.length) loadPosts()
  else if (activeTab.value === 'reviews' && !reviews.value.length) loadReviews()
}

const toggleFollow = async () => {
  const token = localStorage.getItem('token')
  if (!token) { router.push('/login'); return }
  followLoading.value = true
  try {
    if (profile.following) {
      await profileApi.unfollow(profile.userId)
      profile.following = false
      profile.followerCount = Math.max(0, profile.followerCount - 1)
      ElMessage.success('已取消关注')
    } else {
      await profileApi.follow(profile.userId)
      profile.following = true
      profile.followerCount++
      ElMessage.success('已关注')
    }
  } catch (e) { /* */ } finally { followLoading.value = false }
}

const contactHim = async () => {
  const token = localStorage.getItem('token')
  if (!token) { router.push('/login'); return }
  try {
    const res = await chatMessageApi.createConversation(profile.userId, null)
    router.push({ path: '/messages', query: { conversationId: res.data.id } })
  } catch (e) { /* */ }
}

const showFollowList = async (mode) => {
  followMode.value = mode
  followDialog.value = true
  followListLoading.value = true
  try {
    const api = mode === 'followers' ? profileApi.followers : profileApi.followings
    const res = await api(route.params.userId, { page: 1, size: 50 })
    followList.value = res.data.records
  } catch (e) { /* */ } finally { followListLoading.value = false }
}

/** 进入别人的主页 */
const goProfile = (userId) => {
  followDialog.value = false
  if (userId === profile.userId) return
  router.push('/user/' + userId)
}

/** 支持 URL 中的 tab 参数，比如从社区点头像直达「社区」Tab */
const initFromQuery = () => {
  const tab = route.query.tab
  if (tab && ['products', 'posts', 'reviews'].includes(tab)) {
    activeTab.value = tab
  }
}

const loadAll = async () => {
  initFromQuery()
  products.value = []; posts.value = []; reviews.value = []
  productPage.value = 1; postPage.value = 1; reviewPage.value = 1
  await loadProfile()
  // 只加载当前 Tab 的数据
  loadTab()
}

onMounted(loadAll)
// 在主页之间跳转时（如从 A 的粉丝列表点进 B），需要重新加载
watch(() => route.params.userId, (id) => { if (id) loadAll() })
</script>

<style scoped>
.profile-page { max-width: 1100px; margin: 0 auto; }

/* 资料卡 */
.profile-header { background: #fff; border-radius: 14px; padding: 28px 32px; margin-bottom: 16px; }
.header-main { display: flex; align-items: center; gap: 24px; }
.profile-avatar { flex-shrink: 0; }
.profile-info { flex: 1; min-width: 0; }
.name-row { display: flex; align-items: center; gap: 10px; margin-bottom: 14px; }
.nickname { font-size: 22px; font-weight: 700; }
.stats-row { display: flex; gap: 24px; flex-wrap: wrap; }
.stat { font-size: 13px; color: #999; }
.stat b { color: #333; font-size: 15px; margin-right: 2px; }
.stat.clickable { cursor: pointer; }
.stat.clickable:hover { color: #409eff; }
.stat.clickable:hover b { color: #409eff; }
.header-actions { display: flex; gap: 10px; flex-shrink: 0; }

/* 商品网格 */
.product-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; }
.product-card { background: #fff; border-radius: 10px; overflow: hidden; cursor: pointer; border: 1px solid #eee; transition: all .2s; }
.product-card:hover { transform: translateY(-3px); box-shadow: 0 6px 18px rgba(0,0,0,.08); }
.product-img { position: relative; height: 150px; background: #f5f5f5; }
.product-img img { width: 100%; height: 100%; object-fit: cover; }
.price { position: absolute; bottom: 6px; right: 6px; background: rgba(245,34,45,.9); color: #fff; padding: 2px 8px; border-radius: 5px; font-size: 13px; font-weight: 600; }
.product-title { padding: 10px 12px; font-size: 13px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

/* 帖子网格 */
.post-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; }
.post-card { background: #fff; border-radius: 10px; overflow: hidden; border: 1px solid #eee; }
.post-img { height: 130px; background: #f5f5f5; }
.post-img img { width: 100%; height: 100%; object-fit: cover; }
.post-img.placeholder { display: flex; align-items: center; justify-content: center; color: #ddd; }
.post-body { padding: 10px 12px; }
.post-title { font-size: 13px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; margin-bottom: 6px; }
.post-stats { display: flex; gap: 12px; font-size: 12px; color: #bbb; }
.post-stats span { display: flex; align-items: center; gap: 3px; }

/* 评价 */
.review-summary { display: flex; align-items: center; gap: 20px; padding: 18px 20px; background: #fff; border-radius: 10px; margin-bottom: 12px; }
.summary-score { font-size: 40px; font-weight: 700; color: #f59e0b; line-height: 1; }
.summary-text { font-size: 13px; color: #999; margin-top: 4px; }
.review-item { display: flex; gap: 12px; padding: 16px 20px; background: #fff; border-radius: 10px; margin-bottom: 10px; }
.review-body { flex: 1; min-width: 0; }
.review-top { display: flex; align-items: center; gap: 12px; margin-bottom: 8px; }
.review-user { font-size: 14px; font-weight: 600; }
.review-time { font-size: 12px; color: #bbb; margin-left: auto; }
.review-text { font-size: 14px; color: #555; line-height: 1.7; }
.review-goods { font-size: 12px; color: #bbb; margin-top: 8px; }
.clickable-avatar { cursor: pointer; }
.clickable-name { cursor: pointer; }
.clickable-name:hover { color: #409eff; }

/* 粉丝列表 */
.follow-list { max-height: 380px; overflow-y: auto; }
.follow-item { display: flex; align-items: center; gap: 12px; padding: 10px 8px; border-radius: 8px; cursor: pointer; }
.follow-item:hover { background: #f7f9fc; }
.follow-name { font-size: 14px; }

.pagination { display: flex; justify-content: center; margin-top: 20px; }
@media (max-width: 768px) {
  .product-grid, .post-grid { grid-template-columns: repeat(2, 1fr); }
  .header-main { flex-direction: column; text-align: center; }
}
</style>
