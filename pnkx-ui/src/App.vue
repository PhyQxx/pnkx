<template>
    <div id="app" ref="app">
        <router-view/>
    </div>
</template>

<script>
export default {
    name: 'App',
    data() {
        return {}
    },
    created() {
        this.$store.dispatch('app/initTheme')
    },
    mounted() {
        let htmlWidth = document.documentElement.clientWidth || document.body.clientWidth;
        let html = document.documentElement;
        html.style.fontSize = htmlWidth / 150 + 'px';
    },
}
</script>
<style lang="scss">
/* 页面小标题 */
.page-title {
    border-left: 0.4rem solid $theme-blue;
    padding-left: 0.5rem;
    font-size: 1.6rem;
    font-weight: bold;
    margin: 1rem 0;
}
.index-dropdown-item {
    font-size: 1.4rem!important;
    font-weight: bold;
    font-family: 楷体;
}
/* 失效图片 */
.invalid-svg {
    width: 100%;
    height: 100%;
    svg {
        width: 100%!important;
        height: 100%!important;
        border-radius: 0.5rem;
    }
}
.water-mark-wrap {
    position: absolute;
    width: 100%;
    height: 100%;
    z-index: 9999;
    pointer-events: none;
    top: 0;
    left: 0;
    display: flex;
    overflow: hidden;
    flex-wrap: wrap;
}
.water-word {
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 18px;
    color: rgba(8, 8, 8, 0.1);
    transform: rotate(-45deg);
    user-select: none;
}
@media screen and (max-width: 1000px) {
    html {
        font-size: 12px !important;
    }
    .index-page .nav {
        width: 100% !important;
    }
    .middle .page-content {
        margin: 1rem 0 0 !important;
    }
    .middle .index-bg {
        height: fit-content !important;
        background-position: center 0 !important;
        background-size: 100% 33% !important;

        .blog-text-box {
            margin-top: 8rem !important;
        }

        .down-i {
            margin-top: 4rem !important;
            animation: down-i-phone 2s infinite;
        }
    }
}

/* 音乐卡片样式开始*/
.music-info {
    border-bottom: 1px solid #999999;
}

/* 音乐卡片样式结束*/

/** 留言板样式开始*/
.leave-message-content {
    img {
        max-width: 100%;
    }
}

/** 留言板样式结束*/

.homepage .index-bg {
    // background-image: url('../../../../../assets/images/background/bg-5.png');
    background-image: url('https://pnkx.cloud:8866/ftp/2022/04/02/075be96a-6dd4-44cb-a920-e91e28ea29ad.jpg');
}

.article-list .index-bg {
    // background-image: url('../../../../../assets/images/background/bg-2.jpg');
     background-image: url('https://pnkx.cloud:8866/ftp/2022/04/02/81093c59-c9fb-4168-bc16-a67ccdf30e85.jpg');
}

.album .index-bg {
    // background-image: url('../../../../../assets/images/background/bg-3.jpg');
    background-image: url('https://ftp.pnkx.top:8/ftp/2023/02/01/873d2b26-1b38-4e9c-aef9-46a66e0032b7.jpg');
}

.video .index-bg {
    // background-image: url('../../../../../assets/images/background/bg-3.jpg');
    background-image: url('https://ftp.pnkx.top:8/ftp/2023/04/20/636293-v2-ac0479b9da7003f5d5a64c3def493fd9_rjpg.jpg');
}

.friend-link .index-bg {
    // background-image: url('../../../../../assets/images/background/bg-3.jpg');
    background-image: url('https://pnkx.cloud:8866/ftp/2022/04/02/71537678-0fd7-4521-a96f-5354b3f126b1.jpg')
}

.message-board-page .index-bg {
    // background-image: url('../../../../../assets/images/background/bg-4.jpg');
    background-image: url('https://pnkx.cloud:8866/ftp/2022/04/02/4c9152bb-79d6-4792-9a39-51a68b4602a8.jpg');
}

.index-bg {
    height: 100vh;
    width: 100%;
    background-position: center;
    background-repeat: no-repeat;
    background-attachment: fixed;
    background-size: 100%;
    background-origin: content-box;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-flow: column;

    .top {
        .top-box {
            margin-top: 2rem;
            width: 16rem;
            display: flex;
            flex-flow: column;
            align-items: center;
            justify-content: space-between;
            padding: 1rem;

            .el-image {
                width: 16rem;
                height: 14rem;
                border-radius: 10px;
                opacity: 0.6;
            }

            .love-time {
                display: flex;
                font-size: 1.5rem;
                background-color: #fff;
                margin-top: 1rem;
                padding: 1rem;
                border-radius: 0.5rem;

                .label {
                    color: #FFB5FF;
                    white-space: nowrap;

                }

                .time {
                    color: #6ED3FF;
                    text-shadow: 0.15em 0.15em 0.15em #6ED3FF;
                    white-space: nowrap;
                }
            }
        }
    }

    .blog-text-box {
        display: flex;
        margin-top: 2rem;

        .blog-text {
            font-size: 2rem;
            font-weight: bold;
            color: #5A8DEE;
            overflow: hidden;
            white-space: nowrap;
        }

        span {
            margin-left: 0.1rem;
            font-size: 2rem;
            animation: cursor steps(1) 1s infinite;
        }
    }

    .down-i {
        cursor: pointer;
        color: #5A8DEE;
        font-size: 4rem;
        font-weight: bold;
        margin-top: 20rem;
        animation: down-i 2s infinite;
    }
}

.page-content-box {
    padding-top: 5rem;

    .page-content {
        border-top-left-radius: 1rem;
        border-top-right-radius: 1rem;
        background-color: rgba(255, 255, 255, 0.9);
        margin: 0 20%;
        min-height: 60rem;
    }
}

.index-article-title {
    width: 12rem;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.el-select-dropdown__item {
    height: fit-content !important;
}

/* 鼠标单击动画开始*/
#random-tag {
    position: fixed;
    z-index: 10001;
    animation: click 3s forwards;
    font-size: 1.2rem;
}

@keyframes click {
    0% {
        display: flex;
        opacity: 1;
    }
    100% {
        margin-top: -3rem;
        opacity: 0;
        display: none;
    }
}

/* 鼠标单击动画结束*/

/** 关闭卡片动画*/
@keyframes close-card {
    0% {
        display: flex;
        opacity: 1;
    }
    100% {
        opacity: 0;
        display: none;
        position: absolute;
    }
}

/** 打开卡片动画*/
@keyframes open-card {
    0% {
        opacity: 0;
    }
    100% {
        opacity: 1;
    }
}

@keyframes down-i {
    0% {
        margin-top: 20rem;
    }
    50% {
        margin-top: 24rem;
    }
}

@keyframes down-i-phone {
    0% {
        margin-top: 4rem;
    }
    50% {
        margin-top: 6rem;
    }
}

@keyframes text {
    0% {
        width: 0;
    }
}

@keyframes cursor {
    0% {
        color: #5A8DEE;
    }
    50% {
        color: rgba(0, 0, 0, 0);
    }
}

/** 旋转动画*/
.rotate-box:hover {
    animation: rotate 2s;
    cursor: pointer;
}

@keyframes rotate {
    0% {
        transform: none;
    }
    100% {
        transform: rotate(360deg);
    }
}

/** 垂直翻转动画开始*/
.vertical-flip-box:hover {
    animation: vertical-flip 2s;
    cursor: pointer;
}

@keyframes vertical-flip {
    0% {
        transform: perspective(400px) rotateX(0deg);
    }
    50% {
        transform: perspective(400px) rotateX(180deg);
    }
}

/** 垂直翻转动画结束*/

/** 右移动画开始*/
.right-animation:hover {
    animation: right-animation 2s;
    cursor: pointer;
}

@keyframes right-animation {
    0% {

    }
    50% {
        padding-left: 1rem;
    }
}

/** 右移动画结束*/
</style>
