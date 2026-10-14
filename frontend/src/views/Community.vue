<template>
  <div class="community-page">
    <div class="page-header">
      <h2>社区</h2>
      <el-button type="primary" @click="showPublish = true" v-if="user">
        <el-icon><Edit /></el-icon> 发布图文
      </el-button>
    </div>

    <!-- 分类筛选 + 搜索 -->
    <div class="filter-bar">
      <div class="category-bar">
        <span :class="{ active: !activeCat }" @click="activeCat = null; load()">全部</span>
        <span v-for="cat in categories" :key="cat.id"
              :class="{ active: activeCat === cat.id }" @click="activeCat = cat.id; load()">{{ cat.name }}</span>
      </div>
      <el-input v-model="keyword" placeholder="搜索文章..." size="small" clearable
                @keyup.enter="load" style="width:200px" class="search-input">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
    </div>

    <!-- 帖子卡片网格 -->
    <div class="post-grid" v-loading="loading">
      <div v-for="post in posts" :key="post.id" class="post-card" @click="showDetail(post)">
        <div class="post-img" v-if="post.images">
          <img :src="getFirstImg(post.images)" />
        </div>
        <div class="post-img post-img-placeholder" v-else>
          <el-icon :size="40"><Document /></el-icon>
        </div>
        <div class="post-body">
          <h3 class="post-title">{{ post.title }}</h3>
          <p class="post-content" v-if="post.content">{{ plainText(post.content) }}</p>
          <div class="post-footer">
            <div class="post-author">
              <!-- 点头像/昵称进入作者主页的「社区」Tab，需阻止冒泡以免打开详情弹窗 -->
              <el-avatar :size="20" :src="post.authorAvatar" class="author-link"
                         @click.stop="goProfile(post.userId)" />
              <span class="author-link" @click.stop="goProfile(post.userId)">{{ post.authorName }}</span>
              <el-tag v-if="post.categoryName" size="small" type="info">{{ post.categoryName }}</el-tag>
            </div>
            <div class="post-stats">
              <span :class="{ liked: post.liked }" @click.stop="toggleLike(post)">
            <svg class="like-icon" viewBox="0 0 24 24" :fill="post.liked ? '#409eff' : 'none'" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M14 9V5a3 3 0 0 0-3-3l-4 9v11h11.28a2 2 0 0 0 2-1.7l1.38-9a2 2 0 0 0-2-2.3H14zM7 22H4a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2h3"/>
            </svg>
            {{ post.likeCount }}
          </span>
              <span><el-icon><ChatDotRound /></el-icon> {{ post.commentCount }}</span>
            </div>
          </div>
        </div>
      </div>
      <el-empty v-if="!loading && posts.length === 0" description="暂无帖子" />
    </div>

    <div class="pagination" v-if="total > size">
      <el-pagination background layout="prev, pager, next" :total="total" :page-size="size"
                     :current-page="page" @current-change="p => { page = p; load() }" />
    </div>

    <!-- 详情弹窗 -->
    <el-dialog v-model="showDetailDialog" :title="detailPost?.title" width="640px">
      <div v-if="detailPost" class="detail-body">
        <div class="detail-header">
          <el-avatar :size="36" :src="detailPost.authorAvatar" class="author-link"
                     @click.stop="goProfile(detailPost.userId)" />
          <div>
            <div class="detail-author">
              {{ detailPost.authorName }}
              <el-tag v-if="detailPost && detailPost.categoryName" size="small" type="info" style="margin-left:8px">{{ detailPost.categoryName }}</el-tag>
            </div>
            <div class="detail-time">{{ detailPost.createTime?.substring(0, 16) }} · {{ detailPost.viewCount }}次浏览</div>
          </div>
        </div>
        <!-- 渲染 Markdown 正文 -->
        <MdPreview v-if="detailPost.content" :modelValue="detailPost.content" class="detail-md" />
        <div class="detail-images" v-if="detailPost.images">
          <el-image v-for="(img, i) in detailImages" :key="i" :src="img"
                    :preview-src-list="detailImages" :initial-index="i"
                    fit="cover" preview-teleported />
        </div>
        <div class="detail-actions">
          <el-button :type="detailPost.liked ? 'primary' : 'default'" size="small" @click="toggleLike(detailPost)">
            <svg class="like-icon" viewBox="0 0 24 24" :fill="detailPost.liked ? '#fff' : 'none'" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="width:14px;height:14px">
              <path d="M14 9V5a3 3 0 0 0-3-3l-4 9v11h11.28a2 2 0 0 0 2-1.7l1.38-9a2 2 0 0 0-2-2.3H14zM7 22H4a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2h3"/>
            </svg>
            {{ detailPost.likeCount }}
          </el-button>
          <el-button :type="detailPost.favorited ? 'warning' : 'default'" size="small" @click="toggleFav(detailPost)">
            <el-icon><Collection /></el-icon> {{ detailPost.favorited ? '已收藏' : '收藏' }} {{ detailPost.favoriteCount || 0 }}
          </el-button>
          <el-button size="small" @click="sharePost(detailPost)">
            <el-icon><Share /></el-icon> 分享
          </el-button>
        </div>
        <!-- 评论 -->
        <div class="comment-section">
          <h4>评论 ({{ detailPost.commentCount }})</h4>
          <div class="comment-list" v-if="commentList.length">
            <div v-for="c in commentList" :key="c.id" class="comment-item">
              <el-avatar :size="28" :src="c.authorAvatar" class="author-link"
                         @click.stop="goProfile(c.userId)" />
              <div class="comment-body">
                <div class="comment-author">
                {{ c.authorName }}
                <el-tag v-if="detailPost && c.userId === detailPost.userId" size="small" type="primary" style="margin-left:4px">作者</el-tag>
                <span class="comment-time">{{ c.createTime?.substring(0,16) }}</span>
              </div>
                <div class="comment-content">{{ c.content }}</div>
              </div>
              <el-button v-if="user && user.userId === c.userId" size="small" text type="danger" @click="delComment(c.id)">删除</el-button>
            </div>
          </div>
          <div class="comment-input">
            <el-input v-model="commentText" placeholder="写评论..." @keyup.enter="sendComment" size="small" />
            <el-button type="primary" size="small" @click="sendComment">发送</el-button>
          </div>
        </div>
      </div>
    </el-dialog>

    <!-- 发布弹窗 -->
    <el-dialog v-model="showPublish" title="发布图文" width="900px" top="5vh"
               :close-on-click-modal="false" class="publish-dialog">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="60px">
        <el-form-item label="分类">
          <el-select v-model="form.categoryId" placeholder="选择分类" style="width:220px">
            <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入标题" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="封面">
          <div class="upload-area">
            <div v-for="(img, i) in imageList" :key="i" class="img-item">
              <img :src="img" />
              <el-icon class="remove-btn" @click="removeImage(i)"><CircleClose /></el-icon>
            </div>
            <el-upload v-if="imageList.length < 6" :show-file-list="false" :before-upload="handleUpload"
                       accept="image/*" :http-request="() => {}">
              <div class="upload-btn"><el-icon :size="22"><Plus /></el-icon></div>
            </el-upload>
          </div>
        </el-form-item>
        <el-form-item label="正文">
          <!-- Markdown 编辑器：左侧编写，右侧实时预览 -->
          <MdEditor v-model="form.content" :toolbars="toolbars" :style="{ height: '420px' }"
                    placeholder="用 Markdown 记录你的闲置故事..."
                    @onUploadImg="handleEditorUpload" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="md-tip">支持 Markdown，可插入图片</span>
        <el-button @click="showPublish = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">发布</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { communityApi } from '../api/community'
import { uploadApi } from '../api/product'
import { ElMessage } from 'element-plus'
import { MdEditor, MdPreview } from 'md-editor-v3'
import 'md-editor-v3/lib/style.css'

const router = useRouter()
const user = ref(JSON.parse(localStorage.getItem('user') || 'null'))
const posts = ref([]); const loading = ref(true); const page = ref(1); const size = ref(12); const total = ref(0)
const activeCat = ref(null); const keyword = ref(''); const categories = ref([])
const showPublish = ref(false); const submitting = ref(false)
const imageList = ref([]); const formRef = ref(null)
const form = reactive({ categoryId: null, title: '', content: '', images: '' })
const rules = { title: [{ required: true, message: '请输入标题' }] }

/** 列表卡片上只显示纯文本预览，去掉 Markdown 标记 */
const plainText = (md) => {
  if (!md) return ''
  return md
    .replace(/!\[[^\]]*\]\([^)]*\)/g, '')   // 图片
    .replace(/\[([^\]]*)\]\([^)]*\)/g, '$1') // 链接保留文字
    .replace(/[#*`>~\-|]/g, '')              // 各类标记符号
    .replace(/\s+/g, ' ')
    .trim()
}

/** 编辑器工具栏：只保留写文章常用的几个，避免按钮太多显得杂乱 */
const toolbars = [
  'bold', 'italic', 'strikethrough', 'title', '-',
  'quote', 'unorderedList', 'orderedList', 'code', '-',
  'link', 'image', 'table', '=', 'preview', 'revoke', 'next'
]

/** 编辑器内插入图片：走平台的上传接口 */
const handleEditorUpload = async (files, callback) => {
  try {
    const urls = await Promise.all(
      files.map(async (file) => {
        const res = await uploadApi.uploadImage(file)
        return res.data
      })
    )
    callback(urls)
  } catch (e) {
    ElMessage.error('图片上传失败')
    callback([])
  }
}

const showDetailDialog = ref(false); const detailPost = ref(null)
const commentList = ref([]); const commentText = ref('')

const detailImages = computed(() => {
  if (!detailPost.value?.images) return []
  return detailPost.value.images.split(',').filter(Boolean)
})

const getFirstImg = (images) => images ? images.split(',')[0] : ''

const load = async () => {
  loading.value = true
  try {
    const res = await communityApi.list({ page: page.value, size: size.value, categoryId: activeCat.value || undefined, keyword: keyword.value || undefined })
    posts.value = res.data.records; total.value = res.data.total
  } catch (e) { /* */ } finally { loading.value = false }
}

/** 进入某人的主页，并定位到「社区」Tab */
const goProfile = (userId) => {
  if (!userId) return
  router.push({ path: '/user/' + userId, query: { tab: 'posts' } })
}

/** 把详情对象的状态同步回列表中的同一条，保持两处显示一致 */
const syncToList = (source) => {
  if (!source) return
  const target = posts.value.find(p => p.id === source.id)
  if (!target) return
  target.liked = source.liked
  target.likeCount = source.likeCount
  target.favorited = source.favorited
  target.favoriteCount = source.favoriteCount
  target.commentCount = source.commentCount
  target.viewCount = source.viewCount
}

const showDetail = async (post) => {
  try {
    const res = await communityApi.detail(post.id)
    detailPost.value = res.data
    showDetailDialog.value = true
    // 详情接口会让浏览数 +1，同步回列表，避免关掉弹窗后数字对不上
    syncToList(detailPost.value)
    const cres = await communityApi.comments(post.id)
    commentList.value = cres.data
  } catch (e) { /* */ }
}

const handleUpload = async (file) => {
  try { const res = await uploadApi.uploadImage(file); imageList.value.push(res.data); form.images = imageList.value.join(',') } catch (e) { /* */ }
  return false
}
const removeImage = (i) => { imageList.value.splice(i, 1); form.images = imageList.value.join(',') }

const submit = async () => {
  const valid = await formRef.value.validate().catch(() => false); if (!valid) return
  submitting.value = true
  try {
    await communityApi.publish(form)
    ElMessage.success('发布成功，等待审核'); showPublish.value = false
    form.title = ''; form.content = ''; form.images = ''; form.categoryId = null; imageList.value = []
    load()
  } catch (e) { /* */ } finally { submitting.value = false }
}

const toggleLike = async (post) => {
  try {
    if (post.liked) {
      await communityApi.unlike(post.id)
      post.liked = false
      post.likeCount = Math.max(0, post.likeCount - 1)
    } else {
      await communityApi.like(post.id)
      post.liked = true
      post.likeCount = post.likeCount + 1
    }
    // 详情弹窗里的对象和列表里的对象是两个引用，需要手动同步，否则关掉弹窗后列表数字不变
    syncToList(post)
  } catch (e) { /* */ }
}

const toggleFav = async (post) => {
  try {
    if (post.favorited) {
      await communityApi.unfavorite(post.id)
      post.favorited = false
      post.favoriteCount = Math.max(0, (post.favoriteCount || 0) - 1)
    } else {
      await communityApi.favorite(post.id)
      post.favorited = true
      post.favoriteCount = (post.favoriteCount || 0) + 1
    }
    // 同步回列表
    syncToList(post)
  } catch (e) { /* */ }
}

const sendComment = async () => {
  if (!commentText.value.trim()) return
  try {
    await communityApi.addComment(detailPost.value.id, commentText.value)
    commentText.value = ''
    const cres = await communityApi.comments(detailPost.value.id)
    commentList.value = cres.data
    detailPost.value.commentCount = commentList.value.length
    syncToList(detailPost.value)
  } catch (e) { /* */ }
}
const delComment = async (id) => {
  try {
    await communityApi.deleteComment(id)
    ElMessage.success('已删除')
    const cres = await communityApi.comments(detailPost.value.id)
    commentList.value = cres.data
    detailPost.value.commentCount = commentList.value.length
    syncToList(detailPost.value)
  } catch (e) { /* */ }
}

const sharePost = (post) => {
  navigator.clipboard.writeText(`${window.location.origin}/community`).then(() => ElMessage.success('链接已复制'))
}

onMounted(async () => {
  try { const res = await communityApi.categoryList(); categories.value = res.data } catch (e) { /* */ }
  load()
})
</script>

<style scoped>
.community-page { max-width: 1200px; margin: 0 auto; padding: 20px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-header h2 { font-size: 22px; font-weight: 700; }
.category-bar { display: flex; gap: 6px; flex-wrap: wrap; }
.category-bar span { padding: 6px 18px; font-size: 13px; color: #666; border-radius: 20px; cursor: pointer; transition: all .2s; border: 1px solid #e8e8e8; }
.category-bar span:hover { border-color: #409eff; color: #409eff; }
.category-bar span.active { background: #409eff; color: #fff; border-color: #409eff; }
.filter-bar { display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; margin-bottom: 20px; flex-wrap: wrap; }
.search-input :deep(.el-input__wrapper) { border-radius: 20px; }
.like-icon { width: 16px; height: 16px; vertical-align: -3px; margin-right: 2px; color: #999; }
.liked .like-icon { color: #409eff; }
.post-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.post-card { background: #fff; border-radius: 12px; overflow: hidden; cursor: pointer; transition: all .3s; border: 1px solid #eee; }
.post-card:hover { transform: translateY(-4px); box-shadow: 0 8px 24px rgba(0,0,0,.1); }
.post-img { height: 180px; overflow: hidden; background: #f5f5f5; }
.post-img img { width: 100%; height: 100%; object-fit: cover; }
.post-img-placeholder { display: flex; align-items: center; justify-content: center; color: #ddd; }
.post-body { padding: 14px; }
.post-title { font-size: 15px; font-weight: 600; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; margin-bottom: 6px; }
.post-content { font-size: 13px; color: #999; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; margin-bottom: 12px; }
.post-footer { display: flex; justify-content: space-between; align-items: center; }
.post-author { display: flex; align-items: center; gap: 6px; font-size: 12px; color: #666; min-width: 0; }
.post-author > span { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
/* 头像/昵称可点击进入用户主页 */
.author-link { cursor: pointer; }
.author-link:hover { color: #409eff; }
.post-stats { display: flex; gap: 10px; font-size: 12px; color: #bbb; }
.post-stats span { display: flex; align-items: center; gap: 2px; }
.pagination { display: flex; justify-content: center; margin-top: 32px; }

.detail-body { padding: 0 4px; }
.detail-header { display: flex; align-items: center; gap: 10px; margin-bottom: 16px; }
.detail-author { font-size: 14px; font-weight: 600; }
.detail-time { font-size: 12px; color: #999; }
.detail-content { font-size: 15px; color: #333; line-height: 1.8; margin-bottom: 16px; white-space: pre-wrap; }
.detail-images { display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 16px; }
.detail-images img { max-width: 200px; max-height: 200px; border-radius: 8px; }
.detail-actions { display: flex; gap: 8px; margin-bottom: 20px; padding-bottom: 16px; border-bottom: 1px solid #eee; }
.comment-section h4 { font-size: 14px; margin-bottom: 12px; }
.comment-list { max-height: 260px; overflow-y: auto; margin-bottom: 12px; }
.comment-item { display: flex; gap: 8px; padding: 8px 0; border-bottom: 1px solid #f5f5f5; align-items: flex-start; }
.comment-body { flex: 1; }
.comment-author { font-size: 13px; font-weight: 600; }
.comment-time { font-weight: 400; color: #ccc; margin-left: 6px; font-size: 12px; }
.comment-content { font-size: 13px; color: #555; margin-top: 2px; }
.comment-input { display: flex; gap: 8px; }
/* Markdown 正文渲染 */
.detail-md { margin-bottom: 16px; font-size: 15px; line-height: 1.8; color: #333; }
.detail-md :deep(img) { max-width: 100%; border-radius: 8px; }
.detail-md :deep(h1), .detail-md :deep(h2), .detail-md :deep(h3) { margin: 16px 0 10px; font-weight: 600; }
.detail-md :deep(p) { margin: 10px 0; }
.detail-md :deep(blockquote) { border-left: 3px solid #409eff; padding-left: 12px; color: #666; margin: 12px 0; background: #f7f9fc; }
.detail-md :deep(code) { background: #f5f5f5; padding: 2px 6px; border-radius: 4px; font-size: 13px; }
.detail-md :deep(pre) { background: #f5f5f5; padding: 12px; border-radius: 8px; overflow-x: auto; }
.detail-md :deep(ul), .detail-md :deep(ol) { padding-left: 22px; margin: 10px 0; }
.detail-md :deep(table) { border-collapse: collapse; width: 100%; margin: 12px 0; }
.detail-md :deep(th), .detail-md :deep(td) { border: 1px solid #e8e8e8; padding: 8px 12px; }

/* 发布弹窗 */
.publish-dialog :deep(.el-dialog__footer) { display: flex; justify-content: flex-end; align-items: center; gap: 12px; }
.md-tip { flex: 1; text-align: left; font-size: 12px; color: #bbb; }

.upload-area { display: flex; gap: 8px; flex-wrap: wrap; }
.img-item { position: relative; width: 64px; height: 64px; border-radius: 6px; overflow: hidden; }
.img-item img { width: 100%; height: 100%; object-fit: cover; }
.remove-btn { position: absolute; top: -4px; right: -4px; color: #f5222d; cursor: pointer; font-size: 16px; background: #fff; border-radius: 50%; }
.upload-btn { width: 64px; height: 64px; border: 1px dashed #d9d9d9; border-radius: 6px; display: flex; align-items: center; justify-content: center; color: #999; cursor: pointer; }
.upload-btn:hover { border-color: #409eff; color: #409eff; }
@media (max-width: 768px) { .post-grid { grid-template-columns: repeat(2, 1fr); } }
</style>