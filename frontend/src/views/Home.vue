<template>
  <div class="home-page">
    <!-- 平台运营位轮播 -->
    <section class="hero-section">
      <el-carousel v-if="banners.length" height="260px" :interval="5000"
                   arrow="hover" indicator-position="outside" class="hero-carousel">
        <el-carousel-item v-for="(b, i) in banners" :key="b.id">
          <div class="slide" :style="slideStyle(b, i)" @click="goBanner(b)">
            <div class="slide-mask"></div>
            <div class="slide-content">
              <el-tag size="small" effect="dark" :type="typeTag(b.type)" class="slide-badge">
                {{ typeLabel(b.type) }}
              </el-tag>
              <h2 class="slide-title">{{ b.title }}</h2>
              <div class="slide-bottom">
                <span class="slide-subtitle">{{ b.subtitle }}</span>
                <span v-if="b.buttonText" class="slide-btn">
                  {{ b.buttonText }} <el-icon><ArrowRight /></el-icon>
                </span>
              </div>
            </div>
          </div>
        </el-carousel-item>
      </el-carousel>

      <!-- 后台还没配置运营位时，显示品牌横幅 -->
      <div v-else class="hero-fallback">
        <h1 class="hero-title">校园二手交易平台</h1>
        <p class="hero-subtitle">让闲置物品在校园内流动起来</p>
      </div>
    </section>

    <!-- 分类导航 -->
    <section class="category-section">
      <div class="section-title">
        <h2>商品分类</h2>
        <router-link to="/product/list" class="view-all">查看全部 <el-icon><ArrowRight /></el-icon></router-link>
      </div>
      <div class="category-grid">
        <div v-for="cat in categories" :key="cat.id" class="category-card"
             @click="$router.push({path:'/product/list', query:{categoryId:cat.id}})">
          <span class="cat-name">{{ cat.name }}</span>
        </div>
      </div>
    </section>

    <!-- 最新商品 -->
    <section class="product-section">
      <div class="section-title">
        <h2>最新发布</h2>
        <router-link to="/product/list" class="view-all">更多 <el-icon><ArrowRight /></el-icon></router-link>
      </div>
      <div class="product-grid">
        <div v-for="p in products" :key="p.id" class="product-card" @click="$router.push('/product/'+p.id)">
          <div class="product-img">
            <img :src="getFirstImage(p.images)" alt="" />
            <span class="price">¥{{ p.price }}</span>
          </div>
          <div class="product-info">
            <h3 class="product-title">{{ p.title }}</h3>
            <div class="product-meta">
              <span class="seller">{{ p.sellerName }}</span>
              <span class="views">{{ p.viewCount }} 次浏览</span>
            </div>
          </div>
        </div>
      </div>
      <el-empty v-if="!loading && products.length === 0" description="暂无商品" />
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { productApi } from '../api/product'
import { categoryApi } from '../api/category'
import { bannerApi } from '../api/banner'

const router = useRouter()
const categories = ref([])
const products = ref([])
const banners = ref([])
const loading = ref(true)

/** 渐变兜底背景：运营位没配图时用，按下标轮换色系 */
const FALLBACK_GRADIENTS = [
  'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
  'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
  'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
  'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)',
  'linear-gradient(135deg, #fa709a 0%, #fee140 100%)'
]

const getFirstImage = (images) => {
  if (!images) return 'data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 200 200"><rect fill="%23f0f0f0" width="200" height="200"/><text x="50%" y="50%" text-anchor="middle" dy=".3em" fill="%23ccc" font-size="14">暂无图片</text></svg>'
  return images.split(',')[0]
}

/** 轮播背景：配了图用图，没配图用渐变 */
const slideStyle = (b, i) => {
  if (b.image) {
    return { backgroundImage: `url(${b.image})`, backgroundSize: 'cover', backgroundPosition: 'center' }
  }
  return { background: FALLBACK_GRADIENTS[i % FALLBACK_GRADIENTS.length] }
}

/** 运营位类型 → 标签样式 */
const typeLabel = (t) => ({ ACTIVITY: '活动', ANNOUNCEMENT: '公告', GUIDE: '指引' }[t] || '公告')
const typeTag = (t) => ({ ACTIVITY: 'danger', ANNOUNCEMENT: 'warning', GUIDE: 'success' }[t] || 'info')

/**
 * 点击运营位跳转
 * 站内路径用 router 跳，外部链接用新窗口打开
 */
const goBanner = (b) => {
  if (!b.link) return
  if (/^https?:\/\//.test(b.link)) {
    window.open(b.link, '_blank', 'noopener')
  } else {
    router.push(b.link)
  }
}

const loadData = async () => {
  try {
    const [catRes, prodRes, bannerRes] = await Promise.all([
      categoryApi.list(),
      productApi.list({ page: 1, size: 8, sort: 'create_time', order: 'desc' }),
      bannerApi.list()
    ])
    categories.value = catRes.data
    products.value = prodRes.data.records
    banners.value = bannerRes.data
  } catch (e) { /* handled by interceptor */ }
  finally { loading.value = false }
}

onMounted(loadData)
</script>

<style scoped>
/* ===== 轮播 ===== */
.hero-section { margin-bottom: 32px; }
.hero-carousel { border-radius: 16px; overflow: hidden; }
.slide {
  position: relative; height: 100%; width: 100%;
  cursor: pointer; display: flex; align-items: flex-end;
}
.slide-mask {
  position: absolute; inset: 0;
  background: linear-gradient(to top, rgba(0,0,0,.72) 0%, rgba(0,0,0,.25) 45%, rgba(0,0,0,.05) 100%);
}
.slide-content { position: relative; padding: 28px 36px; color: #fff; width: 100%; }
.slide-badge { margin-bottom: 12px; }
.slide-title {
  font-size: 28px; font-weight: 700; margin-bottom: 14px;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
  text-shadow: 0 2px 8px rgba(0,0,0,.3);
}
.slide-bottom { display: flex; align-items: center; gap: 16px; }
.slide-subtitle { font-size: 15px; opacity: .9; }
.slide-btn {
  margin-left: auto; font-size: 14px;
  display: flex; align-items: center; gap: 4px;
  background: rgba(255,255,255,.18); padding: 7px 16px;
  border-radius: 20px; backdrop-filter: blur(4px);
  transition: background .2s;
}
.slide:hover .slide-btn { background: rgba(255,255,255,.32); }

/* 无热门商品时的兜底横幅 */
.hero-fallback {
  text-align: center; padding: 60px 20px 40px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 16px; color: #fff;
}
.hero-title { font-size: 36px; font-weight: 700; margin-bottom: 12px; }
.hero-subtitle { font-size: 16px; opacity: .9; }

.category-section, .product-section { margin-bottom: 32px; }
.section-title { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.section-title h2 { font-size: 20px; font-weight: 600; }
.view-all { color: #409eff; font-size: 14px; display: flex; align-items: center; gap: 4px; }
.category-grid { display: grid; grid-template-columns: repeat(8, 1fr); gap: 12px; }
.category-card { background: #fff; padding: 16px 8px; text-align: center; border-radius: 8px; cursor: pointer; transition: all .3s; border: 1px solid #eee; }
.category-card:hover { color: #409eff; border-color: #409eff; transform: translateY(-2px); box-shadow: 0 4px 12px rgba(64,158,255,.15); }
.cat-name { font-size: 14px; font-weight: 500; }
.product-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.product-card { background: #fff; border-radius: 12px; overflow: hidden; cursor: pointer; transition: all .3s; border: 1px solid #eee; }
.product-card:hover { transform: translateY(-4px); box-shadow: 0 8px 24px rgba(0,0,0,.1); }
.product-img { position: relative; height: 180px; overflow: hidden; background: #f5f5f5; }
.product-img img { width: 100%; height: 100%; object-fit: cover; }
.price { position: absolute; bottom: 8px; right: 8px; background: rgba(245,34,45,.9); color: #fff; padding: 4px 10px; border-radius: 6px; font-size: 16px; font-weight: 700; }
.product-info { padding: 12px; }
.product-title { font-size: 15px; font-weight: 500; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; margin-bottom: 8px; }
.product-meta { display: flex; justify-content: space-between; font-size: 12px; color: #999; }
@media (max-width: 768px) { .category-grid { grid-template-columns: repeat(4, 1fr); } .product-grid { grid-template-columns: repeat(2, 1fr); } }
</style>