<template>
  <div class="users-page">
    <h2>用户管理</h2>
    <div class="search-bar">
      <el-input v-model="keyword" placeholder="搜索用户名/昵称" clearable style="width:260px" @keyup.enter="load" />
      <el-button type="primary" @click="load">搜索</el-button>
    </div>
    <el-table :data="users" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="username" label="用户名" width="140" />
      <el-table-column prop="nickname" label="昵称" width="140" />
      <el-table-column prop="phone" label="手机号" width="140" />
      <el-table-column prop="role" label="角色" width="100">
        <template #default="{row}">
          <el-tag :type="row.role === 'ADMIN' ? 'danger' : 'primary'" size="small">{{ row.role === 'ADMIN' ? '管理员' : '普通用户' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{row}">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">{{ row.status === 1 ? '正常' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="注册时间" min-width="160">
        <template #default="{row}">{{ row.createTime?.substring(0, 16) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="{row}">
          <el-button v-if="row.status === 1" size="small" type="danger" @click="toggleStatus(row.id, 0)">禁用</el-button>
          <el-button v-else size="small" type="success" @click="toggleStatus(row.id, 1)">启用</el-button>
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
import { ref, onMounted } from 'vue'
import { adminApi } from '../api/admin'
import { ElMessage } from 'element-plus'

const users = ref([])
const loading = ref(true)
const keyword = ref('')
const page = ref(1)
const size = ref(10)
const total = ref(0)

const load = async () => {
  loading.value = true
  try {
    const res = await adminApi.userList({ page: page.value, size: size.value, keyword: keyword.value || undefined })
    users.value = res.data.records
    total.value = res.data.total
  } catch (e) { /* handled */ }
  finally { loading.value = false }
}

const toggleStatus = async (id, status) => {
  try {
    await adminApi.updateUserStatus(id, status)
    ElMessage.success('更新成功')
    load()
  } catch (e) { /* handled */ }
}

onMounted(load)
</script>

<style scoped>
.users-page h2 { margin-bottom: 16px; }
.search-bar { display: flex; gap: 12px; margin-bottom: 16px; }
.pagination { display: flex; justify-content: center; margin-top: 24px; }
</style>