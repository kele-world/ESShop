<template>
  <div class="user-center-page">
    <div class="center-card">
      <h2>个人中心</h2>
      <el-form :model="form" label-width="80px" size="large">
        <el-form-item label="头像">
          <div class="avatar-row">
            <el-avatar :size="64" :src="form.avatar" />
            <el-upload :show-file-list="false" :before-upload="handleAvatarUpload"
                       accept="image/*" :http-request="() => {}">
              <el-button size="small">上传头像</el-button>
            </el-upload>
          </div>
        </el-form-item>
        <el-form-item label="用户名">
          <el-input v-model="form.username" disabled />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="form.nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="角色">
          <el-tag :type="form.role === 'ADMIN' ? 'danger' : 'primary'">{{ form.role === 'ADMIN' ? '管理员' : '普通用户' }}</el-tag>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="save">保存修改</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { userApi } from '../api/user'
import { uploadApi } from '../api/product'
import { ElMessage } from 'element-plus'

const form = reactive({ username: '', nickname: '', phone: '', avatar: '', role: '' })
const saving = ref(false)

const load = async () => {
  try {
    const res = await userApi.getInfo()
    Object.assign(form, res.data)
  } catch (e) { /* handled */ }
}

const handleAvatarUpload = async (file) => {
  try {
    const res = await uploadApi.uploadImage(file)
    form.avatar = res.data
  } catch (e) { /* handled */ }
  return false
}

const save = async () => {
  saving.value = true
  try {
    await userApi.updateInfo({ nickname: form.nickname, phone: form.phone, avatar: form.avatar })
    const user = JSON.parse(localStorage.getItem('user') || '{}')
    user.nickname = form.nickname
    user.avatar = form.avatar
    localStorage.setItem('user', JSON.stringify(user))
    window.dispatchEvent(new Event('user-updated'))
    ElMessage.success('保存成功')
  } catch (e) { /* handled */ }
  finally { saving.value = false }
}

onMounted(load)
</script>

<style scoped>
.user-center-page { display: flex; justify-content: center; }
.center-card { width: 600px; background: #fff; padding: 32px; border-radius: 12px; box-shadow: 0 4px 24px rgba(0,0,0,.06); }
.center-card h2 { margin-bottom: 24px; font-size: 20px; }
.avatar-row { display: flex; align-items: center; gap: 16px; }
</style>