<!--
 * @File: Upload
 * @Author: 裴浩宇
 * @Date: 2023/03/02 8:56
 * @Description: 上传组件
-->
<template>
  <el-upload
    :action="uploadUrl"
    :http-request="handleUpload"
    :before-upload="handleBeforeUpload"
    :on-success="handleSuccess"
    :on-error="handleError"
    :show-file-list="false"
    :disabled="uploading"
    drag
  >
    <template v-if="uploading">
      <div class="upload-progress">
        <el-progress :percentage="uploadPercent" :stroke-width="8" />
        <p class="upload-progress-text">正在上传 {{ uploadPercent }}%</p>
      </div>
    </template>
    <template v-else>
      <el-icon class="el-icon--upload"><upload-filled /></el-icon>
      <div class="el-upload__text">
        将文件拖到此处，或<em>点击上传</em>
      </div>
    </template>
    <template #tip>
      <div class="el-upload__tip">
        请上传指定类型的文件
      </div>
    </template>
  </el-upload>
</template>

<script>
import axios from 'axios'
import { getToken } from '@/utils/auth'
import { ElNotification } from 'element-plus'

export default {
  name: 'Upload',
  // 阻止父组件的事件监听器穿透到 el-upload（避免 @change 与 el-upload 内部 change 事件冲突）
  inheritAttrs: false,
  props: {
    type: {
      type: Array,
      default: () => [
        // 图片
        'bmp', 'gif', 'jpg', 'jpeg', 'png', 'webp',
        // office
        'doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx',
        // 压缩文件
        'rar', 'zip', 'gz', 'bz2', '7z',
        // 视频
        'mp4', 'avi', 'rmvb',
        // 其他
        'html', 'htm', 'txt', 'pdf', 'apk', 'vue', 'js', 'css', 'java', 'xml', 'jar', 'wgt', 'exe'
      ]
    },
    fileType: {
      type: String,
      default: ''
    },
    uploadPath: {
      type: String,
      default: ''
    },
    isThumbnail: {
      type: Boolean,
      default: true
    }
  },
  data() {
    return {
      uploading: false,
      uploadPercent: 0
    }
  },
  computed: {
    uploadUrl() {
      // 优先使用 VUE_APP_BASE_API，如果不存在则回退到 /dev-api
      const baseUrl = import.meta.env.VUE_APP_BASE_API || '/dev-api';
      return `${baseUrl}/system/file/uploadLarge`
    }
  },
  methods: {
    /**
     * 上传前的钩子
     * @param {File} file
     */
    handleBeforeUpload(file) {
      const fileExtension = file.name.slice(file.name.lastIndexOf('.') + 1).toLowerCase()
      if (!this.type.includes(fileExtension)) {
        ElNotification({
          title: '错误',
          message: `请上传指定类型的文件，支持的格式有: ${this.type.join(', ')}`,
          type: 'error'
        })
        return false
      }
      return true
    },
    /**
     * 自定义上传请求
     * @param {Object} options
     */
    async handleUpload(options) {
      // 防止并发上传
      if (this.uploading) return
      const { file, onProgress, onSuccess, onError } = options
      const CHUNK_SIZE = 5 * 1024 * 1024 // 5MB
      const fileSize = file?.size || 0
      const identifier = Date.now() + '_' + Math.random().toString(36).substr(2, 9)
      const totalChunks = Math.ceil(fileSize / CHUNK_SIZE)

      const headers = {
        'Authorization': 'Bearer ' + getToken()
      }
      const params = {
        fileType: this.fileType,
        uploadPath: this.uploadPath,
        isThumbnail: this.isThumbnail
      }

      this.uploading = true
      this.uploadPercent = 0

      try {
        // 小文件直接上传（≤5MB）
        if (fileSize <= CHUNK_SIZE) {
          const formData = new FormData()
          formData.append('file', file)
          formData.append('filename', file.name)
          formData.append('chunkNumber', '1')
          formData.append('totalChunks', '1')
          formData.append('identifier', identifier)

          const res = await axios.post(this.uploadUrl, formData, {
            headers,
            params,
            onUploadProgress: (progressEvent) => {
              const percent = Math.round((progressEvent.loaded * 100) / progressEvent.total)
              this.uploadPercent = percent
              onProgress({ percent })
            }
          })
          this.uploadPercent = 100
          onSuccess(res.data)
          return
        }

        // 大文件分片上传
        for (let i = 1; i <= totalChunks; i++) {
          const start = (i - 1) * CHUNK_SIZE
          const end = Math.min(start + CHUNK_SIZE, file.size)
          const chunk = file.slice(start, end)

          const formData = new FormData()
          formData.append('file', chunk)
          formData.append('filename', file.name)
          formData.append('chunkNumber', String(i))
          formData.append('totalChunks', String(totalChunks))
          formData.append('identifier', identifier)

          const res = await axios.post(this.uploadUrl, formData, { headers, params })

          const percent = Math.round((i / totalChunks) * 100)
          this.uploadPercent = percent
          onProgress({ percent })

          // 最后一个分片上传完成后，后端会合并文件并返回结果
          if (i === totalChunks) {
            onSuccess(res.data)
            return
          }
        }
      } catch (err) {
        console.error('[Upload] 上传失败:', err)
        onError(err)
      }
    },
    /**
     * 上传成功事件
     * @param {object} response
     * @param {object} file
     */
    handleSuccess(response, file) {
      // 防止重复回调
      if (!this.uploading) return
      this.uploading = false
      this.uploadPercent = 0
      if (response?.code === 200) {
        const { url, name } = { url: response.msg, name: file.name }
        this.$emit('change', { url, name })
        ElNotification({
          title: '成功',
          message: '文件上传成功',
          type: 'success'
        })
      } else {
        this.handleError(response)
      }
    },
    /**
     * 上传失败
     * @param {object|Error} err
     */
    handleError(err) {
      this.uploading = false
      this.uploadPercent = 0
      const message = err.msg || err.response?.data?.msg || '文件上传失败，请稍后重试'
      ElNotification({
        title: '错误',
        message: message,
        type: 'error'
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.el-upload__tip {
  text-align: center;
  color: #999;
  margin-top: 10px;
}

.upload-progress {
  padding: 20px 30px;
  text-align: center;

  .upload-progress-text {
    margin: 10px 0 0;
    font-size: 14px;
    color: #999;
  }
}
</style>
