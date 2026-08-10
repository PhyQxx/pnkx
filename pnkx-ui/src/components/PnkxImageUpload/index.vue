<template>
    <div class="component-upload-image">
        <el-upload
            :action="uploadImgUrl"
            :before-upload="handleBeforeUpload"
            :headers="headers"
            :on-error="handleUploadError"
            :on-success="handleUploadSuccess"
            :show-file-list="false"
            list-type="picture-card"
            name="file"
            style="display: inline-block; vertical-align: top"
        >
            <el-image>
                <div slot="error" class="image-slot">
                    <el-icon><Plus /></el-icon>
                </div>
            </el-image>
        </el-upload>
    </div>
</template>

<script>
import {getToken} from "@/utils/auth";

export default {
    props: {
        modelValue: {
            type: String,
            default: "",
        },
        // 兼容旧版
        value: {
            type: String,
            default: "",
        },
    },
    data() {
        return {
            dialogVisible: false,
            uploadImgUrl: import.meta.env.VUE_APP_BASE_API + "/common/upload", // 上传的图片服务器地址
            headers: {
                Authorization: "Bearer " + getToken(),
            },
        };
    },
    methods: {
        removeImage() {
            this.$emit("update:modelValue", "");
            this.$emit("input", "");
        },
        handleUploadSuccess(res) {
            const url = "https://pnkx.top" + import.meta.env.VUE_APP_BASE_API + res.fileName;
            this.$emit("update:modelValue", url);
            this.$emit("input", url);
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

<style lang="scss" scoped>
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
