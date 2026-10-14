<template>
  <div class="categories-page">
    <h2>分类管理</h2>
    <div class="toolbar">
      <el-button type="primary" @click="showAdd">添加分类</el-button>
    </div>
    <el-table :data="categories" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="分类名称" min-width="200" />
      <el-table-column prop="sort" label="排序" width="100" />
      <el-table-column prop="createTime" label="创建时间" width="180">
        <template #default="{row}">{{ row.createTime?.substring(0, 16) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="180">
        <template #default="{row}">
          <el-button size="small" type="primary" @click="showEdit(row)">编辑</el-button>
          <el-popconfirm title="确认删除？" @confirm="del(row.id)">
            <template #reference><el-button size="small" type="danger">删除</el-button></template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <!-- 添加/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑分类' : '添加分类'" width="400px">
      <el-form :model="form" label-width="60px">
        <el-form-item label="名称">
          <el-input v-model="form.name" placeholder="分类名称" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" :step="1" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { adminApi } from '../api/admin'
import { ElMessage } from 'element-plus'

const categories = ref([])
const loading = ref(true)
const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref(null)
const form = reactive({ name: '', sort: 0 })

const load = async () => {
  loading.value = true
  try {
    const res = await adminApi.categoryList()
    categories.value = res.data
  } catch (e) { /* handled */ }
  finally { loading.value = false }
}

const showAdd = () => {
  isEdit.value = false
  editId.value = null
  form.name = ''
  form.sort = 0
  dialogVisible.value = true
}

const showEdit = (row) => {
  isEdit.value = true
  editId.value = row.id
  form.name = row.name
  form.sort = row.sort
  dialogVisible.value = true
}

const submit = async () => {
  try {
    if (isEdit.value) {
      await adminApi.updateCategory(editId.value, form)
      ElMessage.success('更新成功')
    } else {
      await adminApi.addCategory(form)
      ElMessage.success('添加成功')
    }
    dialogVisible.value = false
    load()
  } catch (e) { /* handled */ }
}

const del = async (id) => {
  try { await adminApi.deleteCategory(id); ElMessage.success('已删除'); load() } catch (e) { /* handled */ }
}

onMounted(load)
</script>

<style scoped>
.categories-page h2 { margin-bottom: 16px; }
.toolbar { margin-bottom: 16px; }
</style>