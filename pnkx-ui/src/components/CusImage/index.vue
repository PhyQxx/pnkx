<!--
 * @File: index
 * @Author: PHY
 * @Date: 2021/12/23 10:00
 * @Description: 描述
-->
<template>
    <div class="cus-image" :style="width + ';' + height">
        <el-image
            style="width: 100%; height: 100%"
            :src="src"
            :fit="fit"
            :alt="alt"
            :referrerPolicy="referrerPolicy"
            :zIndex="zIndex"
            :preview-src-list="previewSrcList"
            @click="cusPreviewImage"
        > <div slot="error" class="image-slot invalid-svg">
            <svg-icon icon-class="已失效2"/>
        </div>
        </el-image>
        <div class="remark" v-if="dnFlag">
            备注：{{ remark }}
        </div>
        <message-board v-if="!isAdmin && dnFlag"
                       class="message-board"
                       messageType="4"
                       :articleId="imageId"/>
        <admin-message-board v-if="isAdmin && dnFlag"
                             class="message-board"
                             messageType="4"
                             :articleId="imageId"/>
    </div>
</template>
<script>
export default {
    name: "cusImage",
    data() {
        return {
            dnFlag: false,
            wrapperElem: null,
            hidElClassNameLists: [
                "el-image-viewer__mask",
                "el-image-viewer__btn el-image-viewer__close",
                "el-icon-close",
            ],
        };
    },
    props: {
        isAdmin: false,
        src: {
            type: String,
            default: "",
        },
        remark: '',
        imageId: undefined,
        previewSrcList: {
            type: Array,
            default: function () {
                return [];
            },
        },
        width: {
            type: String,
            default: "100%",
        },
        height: {
            type: String,
            default: "100%",
        },
        fit: {
            type: String,
            default: "",
        },
        alt: {
            type: String,
            default: "",
        },
        referrerPolicy: {
            type: String,
            default: "",
        },
        zIndex: {
            type: Number,
            default: 2000,
        },
    },
    methods: {
        cusPreviewImage() {
            this.dnFlag = true;
            this.checkElements();
        },
        checkElements() {
            this.$nextTick(() => {
                let wrapper = document.getElementsByClassName(
                    "el-image-viewer__wrapper"
                );
                if (wrapper.length > 0) {
                    this.wrapperElem = wrapper[0];
                    this.cusClickHandler();
                } else {
                    this.checkElements();
                }
            });
        },
        cusClickHandler() {
            this.wrapperElem.addEventListener("click", this.hideCusBtn);
        },
        hideCusBtn(e) {
            let className = e.target.className;
            if (this.hidElClassNameLists.includes(className)) {
                this.dnFlag = false;
            }
        },
    },
};
</script>
<style scoped lang="scss">
.cus-image {
    display: inline-block;

    .remark {
        background-color: #fff;
        border-radius: 0.5rem;
        right: 1rem;
        position: fixed;
        width: 40vw;
        z-index: 2001;
        padding: 1rem;
    }

    .message-board {
        z-index: 9999!important; /* 如果该组件需要传递 z-index 的值，这个值也需要做成动态的 props */
        cursor: pointer;
        position: fixed;
        right: 1rem;
        background-color: #FFF;
        border-radius: 0.5rem;
        width: 40vw;
        bottom: 1rem;
    }

    .el-image-viewer__wrapper {
        width: 2rem;
    }
}
</style>
