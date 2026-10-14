<template>
  <div class="order-page">
    <h2>我的订单</h2>
    <el-tabs v-model="tab" @tab-change="load">
      <el-tab-pane label="我买到的" name="buy" />
      <el-tab-pane label="我卖出的" name="sell" />
    </el-tabs>

    <!-- 状态筛选 -->
    <div class="status-filter">
      <el-radio-group v-model="statusFilter" size="small" @change="load">
        <el-radio-button value="">全部</el-radio-button>
        <el-radio-button value="PENDING_PAYMENT">待付款</el-radio-button>
        <el-radio-button value="PAID">已付款</el-radio-button>
        <el-radio-button value="COMPLETED">已完成</el-radio-button>
        <el-radio-button value="CANCELLED">已取消</el-radio-button>
      </el-radio-group>
    </div>

    <!-- 订单卡片列表 -->
    <div class="order-list" v-loading="loading">
      <div v-for="order in orders" :key="order.id" class="order-card">
        <!-- 卡片头：订单号 + 状态 -->
        <div class="order-head">
          <span class="order-no">订单号：{{ order.orderNo }}</span>
          <el-tag :type="statusTag(order.status)" size="small">{{ statusLabel(order.status) }}</el-tag>
        </div>

        <!-- 商品信息 -->
        <div class="order-body">
          <img v-if="order.productImage" :src="order.productImage" class="order-thumb" />
          <div v-else class="order-thumb placeholder"><el-icon :size="24"><Goods /></el-icon></div>
          <div class="order-info">
            <div class="order-title">{{ order.productTitle }}</div>
            <div class="order-sub">
              {{ tab === 'buy' ? '卖家' : '买家' }}：{{ tab === 'buy' ? order.sellerName : order.buyerName }}
              · {{ order.createTime?.substring(0, 16) }}
            </div>
          </div>
          <div class="order-amount">¥{{ order.amount }}</div>
        </div>

        <!-- 状态流转：取消的订单不显示进度条 -->
        <div class="order-steps" v-if="order.status !== 'CANCELLED'">
          <el-steps :active="stepActive(order.status)" align-center finish-status="success" simple>
            <el-step title="待付款" />
            <el-step title="已付款" />
            <el-step title="已完成" />
          </el-steps>
        </div>
        <div class="order-cancelled" v-else>
          <el-icon><CircleClose /></el-icon> 该订单已取消，商品已重新上架
        </div>

        <!-- 操作按钮 -->
        <div class="order-actions">
          <!-- 联系对方：买家联系卖家，卖家联系买家 -->
          <el-button size="small" plain @click="contactPeer(order)">
            <el-icon><ChatDotRound /></el-icon>
            {{ tab === 'buy' ? '联系卖家' : '联系买家' }}
          </el-button>
          <el-button v-if="order.status === 'PENDING_PAYMENT' && tab === 'buy'" type="primary" size="small" @click="pay(order.id)">立即付款</el-button>
          <el-button v-if="order.status === 'PAID'" type="success" size="small" @click="complete(order.id)">确认完成</el-button>
          <!-- 已完成订单：买家可评价，已评价的显示标记 -->
          <el-tag v-if="order.status === 'COMPLETED' && tab === 'buy' && order.reviewed" type="info" size="small">已评价</el-tag>
          <el-button v-if="order.status === 'COMPLETED' && tab === 'buy' && !order.reviewed"
                     type="warning" size="small" @click="openReview(order)">评价卖家</el-button>
          <el-button v-if="order.status === 'PENDING_PAYMENT' || order.status === 'PAID'" type="danger" size="small" plain @click="cancel(order.id)">取消订单</el-button>
          <!-- 已结束的订单可以删除 -->
          <el-popconfirm v-if="order.status === 'CANCELLED' || order.status === 'COMPLETED'"
                         title="确认删除该订单？" @confirm="delOrder(order.id)">
            <template #reference>
              <el-button type="danger" size="small" plain>删除</el-button>
            </template>
          </el-popconfirm>
        </div>
      </div>
      <el-empty v-if="!loading && orders.length === 0" description="暂无订单" />
    </div>

    <!-- 评价卖家对话框 -->
    <!-- 二手商品一件只交易一次，评价对象是卖家而非商品 -->
    <el-dialog v-model="reviewDialog" title="评价卖家" width="440px">
      <div class="review-product" v-if="reviewOrder">
        <img v-if="reviewOrder.productImage" :src="reviewOrder.productImage" class="review-thumb" />
        <div class="review-meta">
          <div class="review-name">{{ reviewOrder.productTitle }}</div>
          <div class="review-seller">卖家：{{ reviewOrder.sellerName }}</div>
        </div>
      </div>
      <div class="review-field">
        <div class="review-label">评价这次交易体验</div>
        <el-rate v-model="reviewForm.rating" :texts="rateTexts" show-text size="large" />
      </div>
      <div class="review-field">
        <div class="review-label">评价内容</div>
        <el-input v-model="reviewForm.content" type="textarea" :rows="4"
                  placeholder="发货快不快？沟通顺不顺畅？描述和实物一致吗？（选填）"
                  maxlength="500" show-word-limit />
      </div>
      <template #footer>
        <el-button @click="reviewDialog = false">取消</el-button>
        <el-button type="primary" :loading="reviewSubmitting" @click="submitReview">提交评价</el-button>
      </template>
    </el-dialog>
    <div class="pagination" v-if="total > size">
      <el-pagination background layout="prev, pager, next" :total="total" :page-size="size"
                     :current-page="page" @current-change="p => { page = p; load() }" />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { orderApi } from '../api/order'
import { productApi } from '../api/product'
import { chatMessageApi } from '../api/chatMessage'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()

const tab = ref('buy')
const statusFilter = ref('')
const orders = ref([])
const loading = ref(true)
const page = ref(1)
const size = ref(10)
const total = ref(0)

const statusTag = (s) => ({ PENDING_PAYMENT: 'warning', PAID: 'primary', COMPLETED: 'success', CANCELLED: 'info' }[s] || 'info')
const statusLabel = (s) => ({ PENDING_PAYMENT: '待付款', PAID: '已付款', COMPLETED: '已完成', CANCELLED: '已取消' }[s] || s)

/** 订单状态映射到步骤条的下标：0=待付款, 1=已付款, 2=已完成 */
const stepActive = (status) => {
  if (status === 'PENDING_PAYMENT') return 0
  if (status === 'PAID') return 1
  return 2
}

const load = async () => {
  loading.value = true
  try {
    const api = tab.value === 'buy' ? orderApi.myOrders : orderApi.mySold
    const res = await api({ page: page.value, size: size.value, status: statusFilter.value || undefined })
    orders.value = res.data.records
    total.value = res.data.total
  } catch (e) { /* handled */ }
  finally { loading.value = false }
}

const pay = (id) => {
  // 跳转到收银台完成支付
  router.push({ path: '/payment', query: { orderId: id } })
}

/** 联系订单的对方：买家→卖家，卖家→买家 */
const contactPeer = async (order) => {
  const peerId = tab.value === 'buy' ? order.sellerId : order.buyerId
  try {
    const res = await chatMessageApi.createConversation(peerId, order.productId)
    router.push({ path: '/messages', query: { conversationId: res.data.id } })
  } catch (e) { /* 拦截器已提示 */ }
}

const delOrder = async (id) => {
  try {
    await orderApi.deleteOrder(id)
    ElMessage.success('已删除')
    load()
  } catch (e) { /* 拦截器已提示 */ }
}
const complete = async (id) => {
  try { await ElMessageBox.confirm('确认订单已完成？', '提示', { type: 'info' }); await orderApi.complete(id); ElMessage.success('订单已完成'); load() } catch (e) { /* cancelled */ }
}
const cancel = async (id) => {
  try { await ElMessageBox.confirm('确认取消订单？', '提示', { type: 'warning' }); await orderApi.cancel(id); ElMessage.success('已取消'); load() } catch (e) { /* cancelled */ }
}

// ==================== 评价 ====================
const reviewDialog = ref(false)
const reviewOrder = ref(null)
const reviewSubmitting = ref(false)
const reviewForm = reactive({ rating: 5, content: '' })
const rateTexts = ['很差', '较差', '一般', '满意', '非常满意']

const openReview = (order) => {
  reviewOrder.value = order
  reviewForm.rating = 5
  reviewForm.content = ''
  reviewDialog.value = true
}

const submitReview = async () => {
  reviewSubmitting.value = true
  try {
    await productApi.submitReview({
      orderId: reviewOrder.value.id,
      rating: reviewForm.rating,
      content: reviewForm.content
    })
    ElMessage.success('评价成功，感谢你的反馈！')
    reviewDialog.value = false
    load()
  } catch (e) { /* 拦截器已提示 */ }
  finally { reviewSubmitting.value = false }
}

onMounted(load)
</script>

<style scoped>
.order-page h2 { font-size: 20px; margin-bottom: 16px; }
.status-filter { margin-bottom: 16px; }
.pagination { display: flex; justify-content: center; margin-top: 24px; }

/* 订单卡片 */
.order-card { background: #fff; border-radius: 12px; margin-bottom: 16px; border: 1px solid #eee; overflow: hidden; }
.order-head { display: flex; justify-content: space-between; align-items: center; padding: 12px 20px; background: #fafbfc; border-bottom: 1px solid #f0f0f0; }
.order-no { font-size: 13px; color: #999; }
.order-body { display: flex; align-items: center; gap: 14px; padding: 16px 20px; }
.order-thumb { width: 64px; height: 64px; object-fit: cover; border-radius: 8px; background: #f5f5f5; flex-shrink: 0; }
.order-thumb.placeholder { display: flex; align-items: center; justify-content: center; color: #ddd; }
.order-info { flex: 1; min-width: 0; }
.order-title { font-size: 15px; font-weight: 500; margin-bottom: 6px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.order-sub { font-size: 12px; color: #999; }
.order-amount { font-size: 18px; font-weight: 700; color: #f5222d; flex-shrink: 0; }
.order-steps { padding: 0 20px 16px; }
.order-cancelled { padding: 10px 20px 16px; font-size: 13px; color: #999; display: flex; align-items: center; gap: 6px; }
.order-actions { padding: 12px 20px; border-top: 1px solid #f5f5f5; display: flex; justify-content: flex-end; gap: 10px; align-items: center; }

/* 评价对话框 */
.review-product { display: flex; align-items: center; gap: 10px; padding: 10px 12px; background: #f7f8fa; border-radius: 8px; margin-bottom: 18px; }
.review-thumb { width: 44px; height: 44px; object-fit: cover; border-radius: 6px; }
.review-meta { flex: 1; min-width: 0; }
.review-name { font-size: 14px; color: #333; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.review-seller { font-size: 12px; color: #999; margin-top: 3px; }
.review-field { margin-bottom: 16px; }
.review-label { font-size: 13px; color: #666; margin-bottom: 8px; }
</style>