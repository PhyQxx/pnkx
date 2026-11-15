<template>
    <div class="avatar-page">
        <div class="avatar-cropper">
            <VueCropper
                ref="cropper"
                :img="options.img"
                :output-size="options.outputSize"
                :output-type="options.outputType"
                :info="options.info"
                :full="options.full"
                :can-move="options.canMove"
                :can-move-box="options.canMoveBox"
                :fixed-box="options.fixedBox"
                :original="options.original"
                :auto-crop="options.autoCrop"
                :auto-crop-width="options.autoCropWidth"
                :auto-crop-height="options.autoCropHeight"
                :center-box="options.centerBox"
                :high="options.high"
                mode="cover"
                :fixed="true"
                :fixed-number="[1, 1]"
                :max-img-size="options.maxImgSize"
            />
        </div>

        <div class="avatar-toolbar">
            <van-button size="small" plain round @click="rotateLeft">左旋转</van-button>
            <van-button size="small" plain round @click="rotateRight">右旋转</van-button>
            <van-button size="small" plain round @click="changeScale(1)">放大</van-button>
            <van-button size="small" plain round @click="changeScale(-1)">缩小</van-button>
        </div>

        <div class="avatar-actions">
            <van-button block plain round @click="chooseImage">选择图片</van-button>
            <van-button block type="primary" round :loading="uploading" @click="uploadAvatar">提交</van-button>
        </div>

        <input
            ref="fileInput"
            type="file"
            accept="image/*"
            style="display: none"
            @change="onFileChange"
        >
    </div>
</template>

<script>
import { VueCropper } from 'vue-cropper'
import 'vue-cropper/dist/index.css'
import { uploadAvatar } from '@/api/system/user'
import { mError, mSuccess } from '@/mobile/utils/ui'

export default {
    name: 'MMineAvatar',
    components: { VueCropper },
    data() {
        return {
            uploading: false,
            options: {
                img: '',
                outputSize: 1,
                outputType: 'png',
                info: false,
                full: false,
                canMove: true,
                canMoveBox: true,
                fixedBox: false,
                original: false,
                autoCrop: true,
                autoCropWidth: 260,
                autoCropHeight: 260,
                centerBox: true,
                high: true,
                maxImgSize: 3000
            }
        }
    },
    mounted() {
        // 已有头像作为初始裁剪图
        const avatar = this.$store.state.user.avatar
        if (avatar) this.options.img = avatar
    },
    methods: {
        chooseImage() {
            this.$refs.fileInput.click()
        },
        onFileChange(e) {
            const file = e.target.files[0]
            if (!file) return
            if (file.type.indexOf('image/') === -1) {
                mError('文件格式错误，请上传图片类型')
                return
            }
            const reader = new FileReader()
            reader.readAsDataURL(file)
            reader.onload = () => {
                this.options.img = reader.result
            }
            e.target.value = ''
        },
        rotateLeft() {
            this.$refs.cropper.rotateLeft()
        },
        rotateRight() {
            this.$refs.cropper.rotateRight()
        },
        changeScale(num) {
            this.$refs.cropper.changeScale(num || 1)
        },
        uploadAvatar() {
            if (!this.options.img) {
                mError('请先选择图片')
                return
            }
            this.$refs.cropper.getCropBlob(data => {
                const formData = new FormData()
                formData.append('avatarfile', data, `用户-${this.$store.state.user.name}-头像.png`)
                this.uploading = true
                uploadAvatar(formData).then(response => {
                    const url = import.meta.env.VUE_APP_BASE_API + response.imgUrl
                    this.$store.commit('SET_AVATAR', url)
                    mSuccess('修改成功')
                    this.$router.back()
                }).finally(() => {
                    this.uploading = false
                })
            })
        }
    }
}
</script>

<style lang="scss" scoped>
.avatar-page {
    display: flex;
    flex-direction: column;
    height: calc(100vh - 46px);
    background: var(--pnkx-bg);
}

.avatar-cropper {
    flex: 1;
    min-height: 0;
}

.avatar-toolbar {
    display: flex;
    gap: 8px;
    justify-content: center;
    padding: 10px 12px;
}

.avatar-actions {
    display: flex;
    gap: 10px;
    padding: 0 12px calc(12px + env(safe-area-inset-bottom));
}
</style>
