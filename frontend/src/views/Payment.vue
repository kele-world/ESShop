<template>
  <div class="payment-page">
    <div class="cashier" v-loading="loading">
      <!-- 支付成功 -->
      <div v-if="status === 'SUCCESS'" class="result success">
        <el-icon :size="64" color="#52c41a"><CircleCheckFilled /></el-icon>
        <h2>支付成功</h2>
        <p class="amount">¥{{ payInfo.amount }}</p>
        <p class="tip">请尽快联系卖家约定交易地点</p>
        <div class="result-actions">
          <el-button type="primary" @click="$router.push('/orders')">查看订单</el-button>
          <el-button @click="$router.push('/product/list')">继续逛逛</el-button>
        </div>
      </div>

      <!-- 收银台 -->
      <div v-else class="checkout">
        <div class="cashier-header">
          <h2>收银台</h2>
          <el-tag type="warning" size="small" effect="plain">模拟支付</el-tag>
        </div>

        <!-- 订单信息 -->
        <div class="order-info">
          <div class="info-row">
            <span class="label">商品</span>
            <span class="value">{{ payInfo.productTitle }}</span>
          </div>
          <div class="info-row">
            <span class="label">订单号</span>
            <span class="value mono">{{ payInfo.orderNo }}</span>
          </div>
          <div class="info-row">
            <span class="label">支付单号</span>
            <span class="value mono">{{ payInfo.payNo }}</span>
          </div>
        </div>

        <!-- 金额 -->
        <div class="amount-box">
          <span class="amount-label">应付金额</span>
          <span class="amount-value">¥{{ payInfo.amount }}</span>
        </div>

        <!-- 倒计时 -->
        <div class="countdown" v-if="remainSeconds > 0">
          <el-icon><Clock /></el-icon>
          请在 <span class="time">{{ countdownText }}</span> 内完成支付
        </div>
        <div class="countdown expired" v-else>
          <el-icon><WarningFilled /></el-icon>
          支付已超时，请重新发起支付
        </div>

        <!-- 支付方式 -->
        <div class="pay-methods">
          <div class="method-title">选择支付方式</div>
          <div class="method-list">
            <div v-for="m in methods" :key="m.value" class="method-item"
                 :class="{ active: payMethod === m.value }" @click="payMethod = m.value">
              <span class="method-icon" :style="{ background: m.color }">{{ m.icon }}</span>
              <span class="method-name">{{ m.label }}</span>
              <el-icon v-if="payMethod === m.value" class="method-check"><CircleCheckFilled /></el-icon>
            </div>
          </div>
        </div>

        <!-- 操作 -->
        <div class="actions">
          <el-button type="primary" size="large" :loading="paying"
                     :disabled="remainSeconds <= 0" @click="doPay">
            确认支付 ¥{{ payInfo.amount }}
          </el-button>
          <el-button size="large" @click="$router.push('/orders')">取消</el-button>
        </div>

        <div class="notice">
          <el-icon><InfoFilled /></el-icon>
          当前为模拟支付环境，不会产生真实扣款
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { paymentApi } from '../api/payment'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const paying = ref(false)
const payInfo = ref({})
const status = ref('PENDING')
const payMethod = ref('ALIPAY')
const remainSeconds = ref(0)
let timer = null

const methods = [
  { value: 'ALIPAY', label: '支付宝', icon: '支', color: '#1677ff' },
  { value: 'WECHAT', label: '微信支付', icon: '微', color: '#07c160' },
  { value: 'MOCK', label: '模拟支付', icon: '模', color: '#909399' }
]

const countdownText = computed(() => {
  const m = Math.floor(remainSeconds.value / 60)
  const s = remainSeconds.value % 60
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
})

const startCountdown = (seconds) => {
  remainSeconds.value = seconds
  clearInterval(timer)
  timer = setInterval(() => {
    if (remainSeconds.value > 0) {
      remainSeconds.value--
    } else {
      clearInterval(timer)
    }
  }, 1000)
}

const doPay = async () => {
  paying.value = true
  try {
    // 模拟支付的"处理中"过程，让体验更接近真实支付
    await new Promise(resolve => setTimeout(resolve, 800))
    const res = await paymentApi.confirm(payInfo.value.payNo)
    status.value = res.data.status
    clearInterval(timer)
    ElMessage.success('支付成功')
  } catch (e) { /* 拦截器已提示 */ }
  finally { paying.value = false }
}

onMounted(async () => {
  const orderId = route.query.orderId
  if (!orderId) {
    ElMessage.error('缺少订单信息')
    router.push('/orders')
    return
  }
  try {
    // 先查是否已有支付单，没有则创建
    let res = await paymentApi.getByOrderId(orderId)
    if (!res.data) {
      res = await paymentApi.create(orderId, payMethod.value)
    }
    payInfo.value = res.data
    status.value = res.data.status
    payMethod.value = res.data.payMethod || 'ALIPAY'

    if (res.data.status === 'SUCCESS') {
      status.value = 'SUCCESS'
    } else {
      startCountdown(res.data.expireSeconds || 900)
    }
  } catch (e) {
    router.push('/orders')
  } finally {
    loading.value = false
  }
})

onUnmounted(() => clearInterval(timer))
</script>

<style scoped>
.payment-page { display: flex; justify-content: center; padding: 20px 0; }
.cashier { width: 520px; background: #fff; border-radius: 14px; box-shadow: 0 4px 24px rgba(0,0,0,.08); overflow: hidden; }

/* 支付成功 */
.result { padding: 48px 32px; text-align: center; }
.result h2 { font-size: 22px; margin: 16px 0 8px; }
.result .amount { font-size: 32px; color: #f5222d; font-weight: 700; margin: 12px 0; }
.result .tip { font-size: 14px; color: #999; margin-bottom: 28px; }
.result-actions { display: flex; justify-content: center; gap: 12px; }

/* 收银台 */
.checkout { padding: 24px; }
.cashier-header { display: flex; align-items: center; gap: 10px; padding-bottom: 16px; border-bottom: 1px solid #f0f0f0; }
.cashier-header h2 { font-size: 18px; }

.order-info { padding: 16px 0; border-bottom: 1px dashed #eee; }
.info-row { display: flex; justify-content: space-between; font-size: 13px; margin-bottom: 10px; }
.info-row:last-child { margin-bottom: 0; }
.info-row .label { color: #999; }
.info-row .value { color: #333; max-width: 320px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.mono { font-family: Consolas, Monaco, monospace; font-size: 12px; }

.amount-box { text-align: center; padding: 24px 0 16px; }
.amount-label { display: block; font-size: 13px; color: #999; margin-bottom: 8px; }
.amount-value { font-size: 34px; font-weight: 700; color: #f5222d; }

.countdown { text-align: center; font-size: 13px; color: #e6a23c; display: flex; align-items: center; justify-content: center; gap: 6px; margin-bottom: 20px; }
.countdown .time { font-weight: 700; font-family: Consolas, Monaco, monospace; }
.countdown.expired { color: #f5222d; }

.pay-methods { margin-bottom: 24px; }
.method-title { font-size: 13px; color: #666; margin-bottom: 12px; }
.method-list { display: flex; flex-direction: column; gap: 10px; }
.method-item {
  display: flex; align-items: center; gap: 12px; padding: 12px 16px;
  border: 1px solid #e8e8e8; border-radius: 10px; cursor: pointer; transition: all .2s;
}
.method-item:hover { border-color: #409eff; }
.method-item.active { border-color: #409eff; background: #ecf5ff; }
.method-icon { width: 32px; height: 32px; border-radius: 8px; color: #fff; display: flex; align-items: center; justify-content: center; font-size: 14px; font-weight: 600; }
.method-name { flex: 1; font-size: 14px; }
.method-check { color: #409eff; }

.actions { display: flex; gap: 12px; }
.actions .el-button { flex: 1; }

.notice { display: flex; align-items: center; justify-content: center; gap: 6px; margin-top: 16px; font-size: 12px; color: #bbb; }
</style>
