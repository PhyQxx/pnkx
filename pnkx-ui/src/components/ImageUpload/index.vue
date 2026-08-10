<template>
    <div class="component-upload-image">
        <el-upload
            :action="uploadImgUrl"
            list-type="picture-card"
            :data="params"
            :on-success="handleUploadSuccess"
            :before-upload="handleBeforeUpload"
            :on-error="handleUploadError"
            name="file"
            :show-file-list="false"
            :headers="headers"
            style="display: inline-block; vertical-align: top"
        >
            <el-image v-if="!modelValue" :src="modelValue">
                <div slot="error" class="image-slot">
                    <el-icon><Plus /></el-icon>
                </div>
            </el-image>
            <div v-else class="image">
                <el-image :src="modelValue" :style="`width:150px;height:150px;`" fit="fill">
                    <div slot="error" class="image-slot invalid-svg">
                        <svg-icon icon-class="已失效2"/>
                    </div>
                </el-image>
                <div class="mask">
                    <div class="actions">
            <span title="预览" @click.stop="dialogVisible = true">
              <el-icon><ZoomIn /></el-icon>
            </span>
                        <span title="移除" @click.stop="removeImage">
              <el-icon><Delete /></el-icon>
            </span>
                    </div>
                </div>
            </div>
        </el-upload>
        <el-dialog v-model="dialogVisible" title="预览" width="800" append-to-body>
            <img :src="modelValue" style="display: block; max-width: 100%; margin: 0 auto;">
        </el-dialog>
    </div>
</template>

<script>
import {getToken} from "@/utils/auth";
import {delFile} from "@/api/tool/file";

export default {
    data() {
        return {
            dialogVisible: false,
            uploadImgUrl: import.meta.env.VUE_APP_BASE_API + "/common/upload", // 上传的图片服务器地址
            headers: {
                Authorization: "Bearer " + getToken(),
            },
            params: {
                fileType: this.imageType,
            },
            thumbnail: '',
            imageId: ''
        };
    },
    props: {
        modelValue: {
            type: String,
            default: "",
        },
        imageType: {
            type: String,
            default: "",
        },
    },
    methods: {
        removeImage() {
            this.$emit("update:modelValue", "");
            this.$emit("input", "");
            delFile(this.imageId);
        },
        handleUploadSuccess(res) {
            this.thumbnail = res.thumbnail;
            this.imageId = res.fileId;
            this.$emit("update:modelValue", res.url);
            this.$emit("input", res.url);
            this.loading.close();
        },
        handleBeforeUpload() {
            this.loading = this.$loading({
                lock: true,
                text: "上传中",
                background: "rgba(0, 0, 0, 0.7)",
            });
        },
        handleUploadError() {
            this.$notify({
                type: "error",
                message: "上传失败",
            });
            this.loading.close();
        },
    },
    watch: {},
};
</script>

<style scoped lang="scss">
.image {
    position: relative;

    .mask {
        opacity: 0;
        position: absolute;
        top: 0;
        width: 100%;
        background-color: rgba(0, 0, 0, 0.5);
        transition: all 0.3s;
    }

    &:hover .mask {
        opacity: 1;
    }
}
</style>
