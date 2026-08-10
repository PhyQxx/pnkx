<template>
    <div class="model">
        <div class="img" v-dom-drag>
            <el-icon @click="lastImg" class="left" v-if="imgSrcList.length > 0"><ArrowLeft /></el-icon>
            <img :src="url" @mousewheel.prevent="rollImg()" alt="example" class="img" ref="img"/>
            <el-icon @click="nextImg" class="right" v-if="imgSrcList.length > 0"><ArrowRight /></el-icon>
        </div>
        <div class="roll-img">
            <el-icon @click="enlarge"><CirclePlus /></el-icon>
            <el-icon @click="narrow"><ZoomOut /></el-icon>
            <el-icon @click="downloadImage"><Download /></el-icon>
            <el-icon @click="transformRight($event)"><RefreshRight /></el-icon>
            <el-icon @click="transformLeft($event)"><RefreshLeft /></el-icon>
        </div>
    </div>
</template>

<script>
export default {
    props: {
        previewImage: {
            type: String,
            default: ''
        },
        imgSrcList: {
            type: Array,
            default: []
        }
    },
    data() {
        return {
            url: this.previewImage
        }
    },
    methods: {
        /**
         * 上一张图片
         */
        lastImg() {
            let index = this.imgSrcList.indexOf(this.url);
            if (index < 1) {
                this.url = this.imgSrcList[this.imgSrcList.length - 1];
            } else {
                this.url = this.imgSrcList[index - 1];
            }
        },
        /**
         * 下一张图片
         */
        nextImg() {
            let index = this.imgSrcList.indexOf(this.url);
            if (index > this.imgSrcList.length - 2) {
                this.url = this.imgSrcList[0];
            } else {
                this.url = this.imgSrcList[index + 1];

            }
        },
        /**
         * 下载图片
         */
        downloadImage() {
            this.downloadImg(this.url, '下载图片')
        },
        /**
         * 获取 blob
         * @param  {String} url 目标文件地址
         * @return {Promise}
         */
        getBlob(url) {
            return new Promise(resolve => {
                const xhr = new XMLHttpRequest();
                xhr.open('GET', url, true);
                xhr.setRequestHeader('Access-Control-Allow-Origin', '*');
                xhr.responseType = 'blob';
                xhr.onload = () => {
                    if (xhr.status === 200) {
                        resolve(xhr.response);
                    }
                };
                xhr.send();
            });
        },

        /**
         * 保存
         * @param  {Blob} blob
         * @param  {String} filename 想要保存的文件名称
         */
        saveAs(blob, filename) {
            if (window.navigator.msSaveOrOpenBlob) {
                navigator.msSaveBlob(blob, filename);
            } else {
                const link = document.createElement('a');
                const body = document.querySelector('body');

                link.href = window.URL.createObjectURL(blob);
                link.download = filename;

                // fix Firefox
                link.style.display = 'none';
                body.appendChild(link);

                link.click();
                body.removeChild(link);

                window.URL.revokeObjectURL(link.href);
            }
        },

        /**
         * 下载
         * @param  {String} url 目标文件地址
         * @param  {String} filename 想要保存的文件名称
         */
        downloadImg(url, filename) {
            this.getBlob(url).then(blob => {
                this.saveAs(blob, filename);
            });
        },
        /**
         * 向右旋转90度
         */
        transformRight() {
            let current = Number(this.$refs.img.style.transform.replace(/[^0-9]/ig, "")) % 360;
            this.$refs.img.style.transform = `rotate(${current + 90}deg)`
        },
        /**
         * 向左旋转90度
         */
        transformLeft() {
            let current = Number(this.$refs.img.style.transform.replace(/[^0-9]/ig, "")) % 360;
            this.$refs.img.style.transform = `rotate(${current + 270}deg)`
        },
        /**
         * 缩小
         */
        narrow() {
            let zoom = parseInt(this.$refs.img.style.zoom) || 100;
            this.$refs.img.style.zoom = (zoom - 10) + '%';
        },
        /**
         * 放大
         */
        enlarge() {
            let zoom = parseInt(this.$refs.img.style.zoom) || 100;
            this.$refs.img.style.zoom = (zoom + 10) + '%';
        },
        /**
         * 图片缩放
         * @returns {boolean}
         */
        rollImg() {
            let zoom = parseInt(this.$refs.img.style.zoom) || 100;
            zoom += event.wheelDelta / 12;
            this.$refs.img.style.zoom = zoom + '%';
            return false;
        }
    }
}
</script>

<style lang="scss" scoped>
.model {
    width: 100%;
    height: 100%;
    display: flex;
    justify-content: center;
    align-items: center;

    img {
        max-width: 80vw;
        zoom: 60%;
        user-select: none;
        cursor: pointer;
    }

    .img {
        color: #ffffff;
        font-size: 3rem;

        .left {
            position: absolute;
            left: 1rem;
            top: calc(50% - 2rem);
        }

        .right {
            position: absolute;
            right: 1rem;
            top: calc(50% - 2rem);
        }

        i {
            border-radius: 50%;
            background-color: #606266;
            height: 4rem;
            width: 4rem;
            text-align: center;
            line-height: 4rem;
        }
    }

    .roll-img {
        position: absolute;
        left: 50%;
        bottom: 2rem;
        transform: translate(-50%, 0);
        color: #ffffff;
        font-size: 3rem;
        padding: 0.5rem 1.5rem;
        background-color: #606266;
        border-color: #fff;
        border-radius: 2rem;
        display: flex;
        justify-content: center;
        align-items: center;

        i {
            cursor: pointer;
            margin-right: 1rem;
        }

        i:nth-child(5) {
            margin-right: 0;
        }
    }
}
</style>
