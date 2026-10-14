<template>
  <div class="product-list-page">
    <!-- 筛选栏 -->
    <div class="filter-bar">
      <div class="categories">
        <el-button :type="!activeCategory ? 'primary' : 'default'" size="small" @click="activeCategory = null; search()">全部</el-button>
        <el-button v-for="cat in categories" :key="cat.id" size="small"
                   :type="activeCategory === cat.id ? 'primary' : 'default'"
                   @click="activeCategory = cat.id; search()">{{ cat.name }}</el-button>
      </div>
      <div class="search-input">
        <el-autocomplete v-model="keyword" :fetch-suggestions="fetchSuggestions" :trigger-on-focus="true"
                         :placeholder="aiMode ? '试试：有没有便宜的代步工具' : '搜索商品...'"
                         clearable style="width:280px"
                         @select="handleSelect" @keyup.enter="search">
          <template #prefix><el-icon><Search /></el-icon></template>
          <template #default="{ item }">
            <div class="suggestion-item">
              <span class="suggestion-text">
                <el-icon class="suggestion-icon"><Clock /></el-icon>{{ item.value }}
              </span>
            </div>
          </template>
        </el-autocomplete>
        <el-button type="primary" @click="search">搜索</el-button>
        <!-- 语义搜索开关：开启后用自然语言描述需求，由向量检索匹配 -->
        <el-tooltip content="用自然语言描述需求，如「有没有便宜的代步工具」" placement="top">
          <el-button :type="aiMode ? 'success' : 'default'" @click="toggleAiMode">
            <el-icon><MagicStick /></el-icon>
            {{ aiMode ? '语义搜索中' : '语义搜索' }}
          </el-button>
        </el-tooltip>
      </div>
    </div>

    <!-- 排序 -->
    <div class="sort-bar">
      <span class="total">共 {{ total }} 件商品</span>
      <el-radio-group v-model="sort" size="small" @change="search">
        <el-radio-button value="create_time">最新</el-radio-button>
        <el-radio-button value="price">价格</el-radio-button>
        <el-radio-button value="view_count">热度</el-radio-button>
      </el-radio-group>
    </div>

    <!-- 商品列表 -->
    <div class="product-grid">
      <div v-for="p in products" :key="p.id" class="product-card" @click="$router.push('/product/'+p.id)">
        <div class="product-img">
          <img :src="getFirstImage(p.images)" alt="" />
          <span class="price">¥{{ p.price }}</span>
        </div>
        <div class="product-info">
          <h3 class="product-title">{{ p.title }}</h3>
          <div class="product-meta">
            <el-tag size="small" type="info">{{ p.categoryName }}</el-tag>
            <span class="seller">{{ p.sellerName }}</span>
          </div>
        </div>
      </div>
    </div>
    <el-empty v-if="!loading && products.length === 0" description="暂无商品" />

    <!-- 分页 -->
    <div class="pagination" v-if="total > size">
      <el-pagination background layout="prev, pager, next" :total="total" :page-size="size"
                     :current-page="page" @current-change="p => { page = p; search() }" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { productApi } from '../api/product'
import { categoryApi } from '../api/category'

const route = useRoute()
const categories = ref([])
const products = ref([])
const loading = ref(true)
const keyword = ref('')
/** 语义搜索开关：开启后用自然语言描述需求 */
const aiMode = ref(false)
const activeCategory = ref(null)
const sort = ref('create_time')
const page = ref(1)
const size = ref(12)
const total = ref(0)

// ==================== 搜索历史 ====================
const HISTORY_KEY = 'search_history'
const MAX_HISTORY = 10
const searchHistory = ref([])

const loadHistory = () => {
  try {
    const raw = localStorage.getItem(HISTORY_KEY)
    searchHistory.value = raw ? JSON.parse(raw) : []
  } catch (e) {
    searchHistory.value = []
  }
}

const saveHistory = (kw) => {
  const text = kw.trim()
  if (!text) return
  // 去重后放到最前，超出上限则截断
  const list = searchHistory.value.filter(item => item !== text)
  list.unshift(text)
  searchHistory.value = list.slice(0, MAX_HISTORY)
  localStorage.setItem(HISTORY_KEY, JSON.stringify(searchHistory.value))
}

const clearHistory = () => {
  searchHistory.value = []
  localStorage.removeItem(HISTORY_KEY)
  ElMessage.success('已清空搜索历史')
}

/** el-autocomplete 的回调：返回匹配的历史记录 */
const fetchSuggestions = (queryString, cb) => {
  const list = searchHistory.value
  const result = queryString
    ? list.filter(item => item.toLowerCase().includes(queryString.toLowerCase()))
    : list
  cb(result.map(value => ({ value })))
}

const handleSelect = (item) => {
  keyword.value = item.value
  search()
}

const getFirstImage = (images) => {
  if (!images) return 'data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 200 200"><rect fill="%23f0f0f0" width="200" height="200"/><text x="50%" y="50%" text-anchor="middle" dy=".3em" fill="%23ccc" font-size="14">暂无图片</text></svg>'
  return images.split(',')[0]
}

/** 切换语义搜索开关 */
const toggleAiMode = () => {
  aiMode.value = !aiMode.value
  ElMessage.info(aiMode.value
    ? '语义搜索已开启：用自然语言描述需求，如「有没有便宜的代步工具」'
    : '语义搜索已关闭：使用普通关键词匹配')
  if (keyword.value.trim()) search()
}

const search = async () => {
  loading.value = true
  if (keyword.value.trim()) saveHistory(keyword.value)
  try {
    // 语义搜索：把自然语言交给向量检索，能匹配到字面不含关键词的商品
    // （比如搜「代步工具」能命中「山地车」）
    if (aiMode.value && keyword.value.trim()) {
      const res = await productApi.semanticSearch(keyword.value, size.value)
      products.value = res.data.records
      total.value = res.data.total
      if (!res.data.total) {
        ElMessage.warning('没找到相关商品，试试换个说法')
      }
      return
    }

    // 普通关键词搜索
    const res = await productApi.list({
      page: page.value, size: size.value, keyword: keyword.value || undefined,
      categoryId: activeCategory.value || undefined,
      sort: sort.value, order: sort.value === 'price' ? 'asc' : 'desc'
    })
    products.value = res.data.records
    total.value = res.data.total
  } catch (e) { /* handled */ }
  finally { loading.value = false }
}

onMounted(async () => {
  loadHistory()
  const catRes = await categoryApi.list()
  categories.value = catRes.data
  if (route.query.categoryId) activeCategory.value = Number(route.query.categoryId)
  if (route.query.keyword) keyword.value = route.query.keyword
  search()
})
</script>

<style scoped>
.filter-bar { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; margin-bottom: 16px; flex-wrap: wrap; }
.categories { display: flex; gap: 8px; flex-wrap: wrap; }
.search-input { display: flex; gap: 8px; }
.suggestion-item { display: flex; align-items: center; justify-content: space-between; }
.suggestion-text { display: flex; align-items: center; gap: 6px; font-size: 13px; }
.suggestion-icon { color: #bbb; font-size: 14px; }
.sort-bar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.total { font-size: 14px; color: #999; }
.product-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.product-card { background: #fff; border-radius: 12px; overflow: hidden; cursor: pointer; transition: all .3s; border: 1px solid #eee; }
.product-card:hover { transform: translateY(-4px); box-shadow: 0 8px 24px rgba(0,0,0,.1); }
.product-img { position: relative; height: 180px; overflow: hidden; background: #f5f5f5; }
.product-img img { width: 100%; height: 100%; object-fit: cover; }
.price { position: absolute; bottom: 8px; right: 8px; background: rgba(245,34,45,.9); color: #fff; padding: 4px 10px; border-radius: 6px; font-size: 16px; font-weight: 700; }
.product-info { padding: 12px; }
.product-title { font-size: 15px; font-weight: 500; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; margin-bottom: 8px; }
.product-meta { display: flex; justify-content: space-between; align-items: center; }
.seller { font-size: 12px; color: #999; }
.pagination { display: flex; justify-content: center; margin-top: 32px; }
@media (max-width: 768px) { .product-grid { grid-template-columns: repeat(2, 1fr); } }
</style>