<!--
 * @File: index
 * @Author: 裴浩宇
 * @Date: 2023/4/19 18:49
 * @Description: 描述
-->
<template>
    <div class="video">
        <video-player
            :style="`width: ${width}; height: ${height}`"
            class="video-player vjs-custom-skin"
            ref="videoPlayer"
            :playsinline="false"
            :options="playerOptions"
            @play="onPlayerPlay($event)"
            @pause="onPlayerPause($event)"
            @ended="onPlayerEnded($event)"
            @waiting="onPlayerWaiting($event)"
            @playing="onPlayerPlaying($event)"
            @loadeddata="onPlayerLoadeddata($event)"
            @timeupdate="onPlayerTimeupdate($event)"
            @canplay="onPlayerCanplay($event)"
            @canplaythrough="onPlayerCanplaythrough($event)"
            @ready="playerReadied"/>
    </div>
</template>

<script>
import {VideoPlayer} from '@videojs-player/vue'

export default {
    name: "Video",
    components: {
        VideoPlayer
    },
    props: {
        url: {
            type: String,
            default: ''
        },
        width: {
            type: String,
            default: '100%'
        },
        height: {
            type: String,
            default: '100%'
        }
    },
    data() {
        return {
            playerOptions: {
                // 播放速度
                playbackRates: [0.7, 1.0, 1.5, 2.0],
                // 如果true,浏览器准备好时开始回放。
                autoplay: false,
                // 控制条
                controls: true,
                // 默认情况下将会消除任何音频。
                muted: false,
                // 导致视频一结束就重新开始。
                loop: false,
                // 建议浏览器在<video>加载元素后是否应该开始下载视频数据。auto浏览器选择最佳行为,立即开始加载视频（如果浏览器支持）
                preload: "auto",
                language: "zh-CN",
                // 将播放器置于流畅模式，并在计算播放器的动态大小时使用该值。值应该代表一个比例 - 用冒号分隔的两个数字（例如"16:9"或"4:3"）
                aspectRatio: "16:9",
                // 当true时，Video.js player将拥有流体大小。换句话说，它将按比例缩放以适应其容器。
                fluid: true,
                sources: [
                    {
                        // 这里的种类支持很多种：基本视频格式、直播、流媒体等，具体可以参看git网址项目
                        type: "video/mp4",
                        // url地址
                        src: "",
                    },
                ],
                // 你的封面地址
                // poster: "../../../../static/full_res.jpg",
                // 播放器宽度
                width: document.documentElement.clientWidth,
                // 允许覆盖Video.js无法播放媒体源时显示的默认信息。
                notSupportedMessage: "此视频暂无法播放，请稍后再试",
                controlBar: {
                    timeDivider: true,
                    durationDisplay: true,
                    remainingTimeDisplay: false,
                    // 全屏按钮
                    fullscreenToggle: true,
                },
            },
        }
    },
    watch: {
        url: {
            handler(newValue) {
                if (newValue) {
                    this.playerOptions.sources[0].src = this.url;
                }
            },
            immediate: true
        }
    },
    methods: {
        // 播放回调
        onPlayerPlay(player) {
            this.$emit('play', player)
        },

        // 暂停回调
        onPlayerPause(player) {
        },

        // 视频播完回调
        onPlayerEnded($event) {
        },

        // DOM元素上的readyState更改导致播放停止
        onPlayerWaiting($event) {
        },

        // 已开始播放回调
        onPlayerPlaying($event) {
        },

        // 当播放器在当前播放位置下载数据时触发
        onPlayerLoadeddata($event) {
        },

        // 当前播放位置发生变化时触发。
        onPlayerTimeupdate($event) {
        },

        //媒体的readyState为HAVE_FUTURE_DATA或更高
        onPlayerCanplay(player) {
        },

        //媒体的readyState为HAVE_ENOUGH_DATA或更高。这意味着可以在不缓冲的情况下播放整个媒体文件。
        onPlayerCanplaythrough(player) {
        },

        //播放状态改变回调
        playerStateChanged(playerCurrentState) {
        },

        //将侦听器绑定到组件的就绪状态。与事件监听器的不同之处在于，如果ready事件已经发生，它将立即触发该函数。。
        playerReadied(player) {
        },

    }
}
</script>

<style lang="scss" scoped>
.video-js .vjs-icon-placeholder {
    width: 80%;
    height: 80%;
    display: block;
}

.videoPlayer ::v-deep .el-dialog, .el-pager li {
    background: none !important;
}

.my-img:hover {
    cursor: pointer;

}

</style>
