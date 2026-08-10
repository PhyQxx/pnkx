<!--
 * @File: index
 * @Author: PHY
 * @Date: 2022/4/14 17:58
 * @Description: 描述
-->
<template>
    <!--修改图片对话框-->
    <el-dialog v-model="componentOpen" append-to-body title="修改图片" width="800px" @opened="modalOpened">
        <div class="edit-avatar">
            <el-row>
                <el-col :md="12" :style="{height: '350px'}" :xs="24">
                    <vue-cropper
                        v-if="visible"
                        ref="cropper"
                        :autoCrop="options.autoCrop"
                        :autoCropHeight="options.autoCropHeight"
                        :autoCropWidth="options.autoCropWidth"
                        :fixedBox="options.fixedBox"
                        :img="options.img"
                        :info="true"
                        @realTime="realTime"
                    />
                </el-col>
                <el-col :md="12" :style="{height: '350px'}" :xs="24">
                    <div class="avatar-upload-preview">
                        <img :src="previews.url" :style="previews.img"/>
                    </div>
                </el-col>
            </el-row>
            <br/>
            <el-row>
                <el-col :lg="2" :md="2">
                    <el-upload :before-upload="beforeUpload" :http-request="requestUpload" :show-file-list="false"
                               action="#">
                        <el-button size="small">
                            选择
                            <el-icon><Upload /></el-icon>
                        </el-button>
                    </el-upload>
                </el-col>
                <el-col :lg="{span: 1, offset: 2}" :md="2">
                    <el-button icon="Plus" size="small" @click="changeScale(1)"></el-button>
                </el-col>
                <el-col :lg="{span: 1, offset: 1}" :md="2">
                    <el-button icon="Minus" size="small" @click="changeScale(-1)"></el-button>
                </el-col>
                <el-col :lg="{span: 1, offset: 1}" :md="2">
                    <el-button icon="RefreshLeft" size="small" @click="rotateLeft()"></el-button>
                </el-col>
                <el-col :lg="{span: 1, offset: 1}" :md="2">
                    <el-button icon="RefreshRight" size="small" @click="rotateRight()"></el-button>
                </el-col>
                <el-col :lg="{span: 2, offset: 6}" :md="2">
                    <el-button size="small" type="primary" @click="uploadImg()">提 交</el-button>
                </el-col>
            </el-row>
        </div>
    </el-dialog>
</template>

<script>
import {VueCropper} from "vue-cropper";
import messageBoard from "@/components/MessageBoard/admin.vue";

export default {
    name: "index",
    components: {
        VueCropper,
    },
    props: {
        open: {
            type: Boolean,
            default: false
        }
    },
    watch: {
        open: {
            handler(newValue) {
                this.componentOpen = newValue;
            },
            deep: true,
            immediate: true
        }
    },
    data() {
        return {
            // 组件标志
            componentOpen: false,
            visible: false,
            // 图片配置
            options: {
                img: '', //裁剪图片的地址
                autoCrop: true, // 是否默认生成截图框
                autoCropWidth: 200, // 默认生成截图框宽度
                autoCropHeight: 200, // 默认生成截图框高度
                fixedBox: false // 固定截图框大小 不允许改变
            },
            previews: {},
        }
    },
    methods: {
        // 覆盖默认的上传行为
        requestUpload() {
        },
        // 向左旋转
        rotateLeft() {
            this.$refs.cropper.rotateLeft();
        },
        // 向右旋转
        rotateRight() {
            this.$refs.cropper.rotateRight();
        },
        // 图片缩放
        changeScale(num) {
            num = num || 1;
            this.$refs.cropper.changeScale(num);
        },
        // 上传预处理
        beforeUpload(file) {
            if (file.type.indexOf("image/") === -1) {
                this.msgError("文件格式错误，请上传图片类型,如：JPG，PNG后缀的文件。");
            } else {
                const reader = new FileReader();
                reader.readAsDataURL(file);
                reader.onload = () => {
                    this.options.img = reader.result;
                };
            }
        },
        // 上传图片
        uploadImg() {
            this.$refs.cropper.getCropBlob(data => {
                const reader = new FileReader();
                reader.onload = (e) => {
                    this.$emit('getImg', e.target.result);
                    this.componentOpen = false;
                };
                reader.readAsDataURL(data);
            })
        },
        // 实时预览
        realTime(data) {
            this.previews = data;
        },
        // 打开弹出层结束时的回调
        modalOpened() {
            this.visible = true;
        },
    }
}
</script>

<style lang='scss' scoped>

</style>
