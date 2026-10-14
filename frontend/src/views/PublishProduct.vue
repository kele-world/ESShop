<template>
  <div class="publish-page">
    <div class="publish-card">
      <h2>{{ isEdit ? '编辑商品' : '发布商品' }}</h2>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="80px" size="large">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入商品标题" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="分类" prop="categoryId">
          <el-select v-model="form.categoryId" placeholder="请选择分类" style="width:100%">
            <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="售价" prop="price">
          <el-input-number v-model="form.price" :min="0" :precision="2" :step="1" style="width:100%" placeholder="请输入售价" />
        </el-form-item>
        <el-form-item label="原价">
          <el-input-number v-model="form.originalPrice" :min="0" :precision="2" :step="1" style="width:100%" placeholder="选填" />
        </el-form-item>
        <!-- 图片必填：二手交易靠图看货，没图买家无法判断成色 -->
        <el-form-item label="图片" prop="images">
          <div class="upload-area">
            <div v-for="(img, i) in imageList" :key="i" class="img-item">
              <img :src="img" />
              <span v-if="i === 0" class="cover-tag">封面</span>
              <el-icon class="remove-btn" @click="removeImage(i)"><CircleClose /></el-icon>
            </div>
            <el-upload v-if="imageList.length < 6" :show-file-list="false" :before-upload="handleUpload"
                       accept="image/*" :http-request="() => {}">
              <div class="upload-btn">
                <el-icon :size="28"><Plus /></el-icon>
                <span>上传</span>
              </div>
            </el-upload>
          </div>
          <div class="upload-tip">至少上传 1 张，最多 6 张；第一张作为封面</div>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="5" placeholder="详细描述商品成色、使用情况等" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="submit">{{ isEdit ? '保存修改' : '发布' }}</el-button>
          <el-button @click="$router.back()">取消</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { productApi, uploadApi } from '../api/product'
import { categoryApi } from '../api/category'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const formRef = ref(null)
const submitting = ref(false)
const categories = ref([])
const isEdit = ref(false)
const imageList = ref([])

const form = reactive({
  title: '', categoryId: null, price: null, originalPrice: null, images: '', description: ''
})

const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  price: [{ required: true, message: '请输入价格', trigger: 'blur' }],
  images: [{
    // 用 imageList 判断而不是 form.images 字符串，更直观可靠
    validator: (rule, value, callback) => {
      if (!imageList.value.length) {
        callback(new Error('请至少上传一张商品图片'))
      } else {
        callback()
      }
    },
    trigger: 'change'
  }]
}

const handleUpload = async (file) => {
  try {
    const res = await uploadApi.uploadImage(file)
    imageList.value.push(res.data)
    form.images = imageList.value.join(',')
    // 上传成功后立即清除「请上传图片」的报错
    formRef.value?.validateField('images')
  } catch (e) { /* handled */ }
  return false // 阻止默认上传
}

const removeImage = (i) => {
  imageList.value.splice(i, 1)
  form.images = imageList.value.join(',')
  // 删空后重新触发校验，提示用户图不能少
  formRef.value?.validateField('images')
}

const submit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (isEdit.value) {
      await productApi.update(route.query.id, form)
      ElMessage.success('修改成功')
    } else {
      await productApi.publish(form)
      ElMessage.success('发布成功')
    }
    router.push('/my-products')
  } catch (e) { /* handled */ }
  finally { submitting.value = false }
}

onMounted(async () => {
  const catRes = await categoryApi.list()
  categories.value = catRes.data
  if (route.query.id) {
    isEdit.value = true
    try {
      const res = await productApi.detail(route.query.id)
      Object.assign(form, {
        title: res.data.title, categoryId: res.data.categoryId,
        price: res.data.price, originalPrice: res.data.originalPrice,
        images: res.data.images || '', description: res.data.description || ''
      })
      if (res.data.images) {
        imageList.value = res.data.images.split(',').filter(Boolean)
      }
    } catch (e) { router.push('/my-products') }
  }
})
</script>

<style scoped>
.publish-page { display: flex; justify-content: center; }
.publish-card { width: 680px; background: #fff; padding: 32px; border-radius: 12px; box-shadow: 0 4px 24px rgba(0,0,0,.06); }
.publish-card h2 { margin-bottom: 24px; font-size: 20px; }
.upload-area { display: flex; flex-wrap: wrap; gap: 10px; }
.img-item { position: relative; width: 80px; height: 80px; border-radius: 6px; overflow: hidden; }
.img-item img { width: 100%; height: 100%; object-fit: cover; }
.remove-btn { position: absolute; top: -4px; right: -4px; color: #f5222d; cursor: pointer; font-size: 18px; background: #fff; border-radius: 50%; }
.upload-btn { width: 80px; height: 80px; border: 1px dashed #d9d9d9; border-radius: 6px; display: flex; flex-direction: column; align-items: center; justify-content: center; color: #999; cursor: pointer; transition: all .3s; }
.upload-btn:hover { border-color: #409eff; color: #409eff; }
.upload-btn span { font-size: 12px; margin-top: 4px; }
/* 第一张图标记为封面 */
.cover-tag {
  position: absolute; left: 0; bottom: 0; width: 100%;
  background: rgba(64,158,255,.9); color: #fff;
  font-size: 11px; text-align: center; line-height: 18px;
}
.upload-tip { width: 100%; font-size: 12px; color: #999; margin-top: 6px; line-height: 1.5; }
</style>