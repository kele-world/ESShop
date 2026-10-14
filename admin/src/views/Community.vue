<template>
  <div class="community-page">
    <h2>社区管理</h2>

    <el-tabs v-model="tab" @tab-change="handleTabChange">
      <el-tab-pane label="待审核" name="pending" />
      <el-tab-pane label="全部帖子" name="all" />
      <el-tab-pane label="已下架" name="off-shelf" />
    </el-tabs>

    <el-table :data="posts" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="title" label="标题" min-width="180" />
      <el-table-column prop="authorName" label="发布者" width="120" />
      <el-table-column label="图片" width="80">
        <template #default="{row}">
          <span v-if="row.images">{{ row.images.split(',').filter(Boolean).length }}张</span>
          <span v-else>无</span>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{row}">
          <el-tag :type="statusTag(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="发布时间" width="160">
        <template #default="{row}">{{ row.createTime?.substring(0, 16) }}</template>
      </el-table-column>
      <el-table-column label="操作" fixed="right" :width="actionWidth">
        <template v-if="tab === 'pending'" #default="{row}">
          <el-button size="small" type="success" @click="approve(row.id)">通过</el-button>
          <el-button size="small" type="danger" @click="reject(row.id)">拒绝</el-button>
        </template>
        <template v-else-if="tab === 'all'" #default="{row}">
          <el-button v-if="row.status === 'APPROVED'" size="small" type="warning" @click="offShelf(row.id)">下架</el-button>
          <el-button v-if="row.status === 'PENDING'" size="small" type="success" @click="approve(row.id)">通过</el-button>
          <el-button v-if="row.status === 'PENDING'" size="small" type="danger" @click="reject(row.id)">拒绝</el-button>
          <el-popconfirm title="确认删除？" @confirm="del(row.id)">
            <template #reference><el-button size="small" type="danger">删除</el-button></template>
          </el-popconfirm>
        </template>
        <template v-else #default="{row}">
          <el-button size="small" type="success" @click="reList(row.id)">重新上架</el-button>
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
import { ref, computed, onMounted } from 'vue'
import { adminApi } from '../api/admin'
import { ElMessage } from 'element-plus'

const tab = ref('pending')
const posts = ref([])
const loading = ref(true)
const page = ref(1)
const size = ref(10)
const total = ref(0)

const statusTag = (s) => ({ PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger', OFF_SHELF: 'info' }[s] || 'info')
const statusLabel = (s) => ({ PENDING: '待审核', APPROVED: '已通过', REJECTED: '已拒绝', OFF_SHELF: '已下架' }[s] || s)

const actionWidth = computed(() => {
  if (tab.value === 'pending') return '180'
  if (tab.value === 'all') return '220'
  return '180'
})

const handleTabChange = () => {
  page.value = 1
  load()
}

const load = async () => {
  loading.value = true
  try {
    let api
    if (tab.value === 'pending') api = adminApi.communityPending
    else if (tab.value === 'all') api = adminApi.communityAll
    else api = adminApi.communityOffShelf
    const res = await api({ page: page.value, size: size.value })
    posts.value = res.data.records
    total.value = res.data.total
  } catch (e) { /* handled */ }
  finally { loading.value = false }
}

const approve = async (id) => {
  try { await adminApi.communityApprove(id); ElMessage.success('已通过'); load() } catch (e) { /* handled */ }
}
const reject = async (id) => {
  try { await adminApi.communityReject(id); ElMessage.success('已拒绝'); load() } catch (e) { /* handled */ }
}
const offShelf = async (id) => {
  try { await adminApi.communityOffShelfPost(id); ElMessage.success('已下架'); load() } catch (e) { /* handled */ }
}
const reList = async (id) => {
  try { await adminApi.communityReList(id); ElMessage.success('已重新上架'); load() } catch (e) { /* handled */ }
}
const del = async (id) => {
  try { await adminApi.communityDelete(id); ElMessage.success('已删除'); load() } catch (e) { /* handled */ }
}

onMounted(load)
</script>

<style scoped>
.community-page h2 { margin-bottom: 16px; }
.pagination { display: flex; justify-content: center; margin-top: 24px; }
</style>