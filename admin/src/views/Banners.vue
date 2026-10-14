<template>
  <div class="banners-page">
    <h2>运营位管理</h2>
    <p class="page-tip">首页顶部轮播内容。可设置生效时间，到期自动下线。</p>

    <div class="toolbar">
      <el-button type="primary" @click="showAdd">
        <el-icon><Plus /></el-icon> 新增运营位
      </el-button>
    </div>

    <el-table :data="banners" v-loading="loading" stripe>
      <el-table-column prop="sort" label="排序" width="70" />
      <el-table-column prop="title" label="标题" min-width="160" />
      <el-table-column prop="subtitle" label="副标题" min-width="200" show-overflow-tooltip />
      <el-table-column prop="type" label="类型" width="90">
        <template #default="{row}">
          <el-tag :type="typeTag(row.type)" size="small">{{ typeLabel(row.type) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="跳转" width="80">
        <template #default="{row}">
          <span v-if="row.link" class="link-cell">有</span>
          <span v-else class="muted">无</span>
        </template>
      </el-table-column>
      <el-table-column label="生效时间" width="180">
        <template #default="{row}">
          <div v-if="!row.startTime && !row.endTime" class="muted">长期有效</div>
          <div v-else class="time-cell">
            {{ row.startTime ? row.startTime.substring(0,10) : '不限' }}
            至
            {{ row.endTime ? row.endTime.substring(0,10) : '不限' }}
          </div>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{row}">
          <el-tag :type="statusTag(row)" size="small">{{ statusLabel(row) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{row}">
          <el-button size="small" text type="primary" @click="showEdit(row)">编辑</el-button>
          <el-button size="small" text :type="row.enabled === 1 ? 'warning' : 'success'"
                     @click="toggleEnabled(row)">
            {{ row.enabled === 1 ? '停用' : '启用' }}
          </el-button>
          <el-popconfirm title="确认删除？" @confirm="del(row.id)">
            <template #reference>
              <el-button size="small" text type="danger">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <!-- 编辑对话框 -->
    <el-dialog v-model="dialog" :title="isEdit ? '编辑运营位' : '新增运营位'" width="560px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="90px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="如：开学季 · 教材专场" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="副标题">
          <el-input v-model="form.subtitle" type="textarea" :rows="2" maxlength="200" show-word-limit
                    placeholder="一句话说明，如：毕业学长学姐的教材低价转让" />
        </el-form-item>
        <el-form-item label="类型">
          <el-radio-group v-model="form.type">
            <el-radio value="ACTIVITY">活动</el-radio>
            <el-radio value="ANNOUNCEMENT">公告</el-radio>
            <el-radio value="GUIDE">指引</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="背景图">
          <el-input v-model="form.image" placeholder="图片URL，留空则用渐变色背景" />
          <div class="field-tip">建议尺寸 1200×260，不填会自动用渐变色</div>
        </el-form-item>
        <el-form-item label="跳转地址">
          <el-input v-model="form.link" placeholder="站内：/product/list?categoryId=2　外链：https://..." />
          <div class="field-tip">留空则点击不跳转</div>
        </el-form-item>
        <el-form-item label="按钮文字">
          <el-input v-model="form.buttonText" placeholder="如：去看看（留空则不显示按钮）" maxlength="20" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" :step="1" />
          <span class="field-tip inline">数字越小越靠前</span>
        </el-form-item>
        <el-form-item label="生效时间">
          <el-date-picker v-model="timeRange" type="datetimerange"
                          range-separator="至" start-placeholder="开始时间" end-placeholder="结束时间"
                          style="width:100%" />
          <div class="field-tip">留空表示长期有效；活动到期后自动从首页下线</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { adminApi } from '../api/admin'
import { ElMessage } from 'element-plus'

const banners = ref([])
const loading = ref(true)
const dialog = ref(false)
const isEdit = ref(false)
const editId = ref(null)
const saving = ref(false)
const timeRange = ref(null)
const formRef = ref(null)

const form = reactive({
  title: '', subtitle: '', type: 'ANNOUNCEMENT', image: '',
  link: '', buttonText: '', sort: 0, enabled: 1
})

const rules = { title: [{ required: true, message: '请输入标题', trigger: 'blur' }] }

const typeLabel = (t) => ({ ACTIVITY: '活动', ANNOUNCEMENT: '公告', GUIDE: '指引' }[t] || t)
const typeTag = (t) => ({ ACTIVITY: 'danger', ANNOUNCEMENT: 'warning', GUIDE: 'success' }[t] || 'info')

/** 判断运营位当前是否在生效期内 */
const isInPeriod = (row) => {
  const now = new Date()
  if (row.startTime && new Date(row.startTime) > now) return false
  if (row.endTime && new Date(row.endTime) < now) return false
  return true
}

const statusLabel = (row) => {
  if (row.enabled !== 1) return '已停用'
  const now = new Date()
  if (row.startTime && new Date(row.startTime) > now) return '未开始'
  if (row.endTime && new Date(row.endTime) < now) return '已过期'
  return '展示中'
}

const statusTag = (row) => {
  const label = statusLabel(row)
  return { '展示中': 'success', '未开始': 'info', '已过期': 'info', '已停用': 'warning' }[label] || 'info'
}

const load = async () => {
  loading.value = true
  try {
    const res = await adminApi.bannerList()
    banners.value = res.data
  } catch (e) { /* 拦截器已提示 */ }
  finally { loading.value = false }
}

const showAdd = () => {
  isEdit.value = false
  editId.value = null
  Object.assign(form, {
    title: '', subtitle: '', type: 'ANNOUNCEMENT', image: '',
    link: '', buttonText: '', sort: 0, enabled: 1
  })
  timeRange.value = null
  dialog.value = true
}

const showEdit = (row) => {
  isEdit.value = true
  editId.value = row.id
  Object.assign(form, {
    title: row.title, subtitle: row.subtitle || '', type: row.type,
    image: row.image || '', link: row.link || '', buttonText: row.buttonText || '',
    sort: row.sort, enabled: row.enabled
  })
  timeRange.value = row.startTime && row.endTime ? [new Date(row.startTime), new Date(row.endTime)] : null
  dialog.value = true
}

const submit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    const payload = {
      ...form,
      startTime: timeRange.value?.[0] || null,
      endTime: timeRange.value?.[1] || null
    }
    if (isEdit.value) {
      await adminApi.updateBanner(editId.value, payload)
      ElMessage.success('更新成功')
    } else {
      await adminApi.addBanner(payload)
      ElMessage.success('添加成功')
    }
    dialog.value = false
    load()
  } catch (e) { /* 拦截器已提示 */ }
  finally { saving.value = false }
}

const toggleEnabled = async (row) => {
  try {
    await adminApi.updateBanner(row.id, { ...row, enabled: row.enabled === 1 ? 0 : 1 })
    ElMessage.success(row.enabled === 1 ? '已停用' : '已启用')
    load()
  } catch (e) { /* 拦截器已提示 */ }
}

const del = async (id) => {
  try {
    await adminApi.deleteBanner(id)
    ElMessage.success('已删除')
    load()
  } catch (e) { /* 拦截器已提示 */ }
}

onMounted(load)
</script>

<style scoped>
.banners-page h2 { margin-bottom: 8px; }
.page-tip { font-size: 13px; color: #999; margin-bottom: 16px; }
.toolbar { margin-bottom: 16px; }
.muted { color: #ccc; }
.link-cell { color: #409eff; }
.time-cell { font-size: 12px; color: #666; }
.field-tip { font-size: 12px; color: #999; line-height: 1.6; margin-top: 4px; }
.field-tip.inline { display: inline; margin-left: 10px; }
</style>
