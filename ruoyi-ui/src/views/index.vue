<template>
    <div class="index">
        <div class="button">
            <el-button size="mini" type="primary" @click="sendAllWebSocket">群体发送</el-button>
            <el-button size="mini" type="primary" @click="sendOneWebSocket">单体发送</el-button>
        </div>
    </div>
</template>

<script>
    import { sendAllWebSocket, sendOneWebSocket } from '@/api/px/admin/websocket/index'
    import { getUserProfile } from "@/api/system/user";

    export default {
        data() {
            return {
                websocket: {}
            }
        },
        mounted () {
            this.initWebSocket()
        },
        beforeDestroy () {
            this.onbeforeunload()
        },
        methods: {
            /**
             * 发送全体消息
             */
            sendAllWebSocket() {
                sendAllWebSocket().then(res => {
                    console.log(res)
                });
            },
            /**
             * 发送全体消息
             */
            sendOneWebSocket() {
                sendOneWebSocket('2').then(res => {
                    console.log(res)
                });
            },
            /**
             * 初始化webSocket
             */
            initWebSocket () {
                // WebSocket
                getUserProfile().then(response => {
                    if ('WebSocket' in window) {
                        this.websocket = new WebSocket('ws://192.168.20.218:8068/websocket/' + response.data.userId);
                        // 连接错误
                        this.websocket.onerror = this.setErrorMessage;
                        // 连接成功
                        this.websocket.onopen = this.setOnopenMessage;
                        // 收到消息的回调
                        this.websocket.onmessage = this.setOnmessageMessage;
                        // 连接关闭的回调
                        this.websocket.onclose = this.setOncloseMessage;
                    } else {
                        alert('当前浏览器 Not support websocket')
                    }
                });
                // 监听窗口关闭事件，当窗口关闭时，主动去关闭websocket连接，防止连接还没断开就关闭窗口，server端会抛异常。
                window.onbeforeunload = this.onbeforeunload;
            },
            setErrorMessage () {
                console.log('WebSocket连接发生错误   状态码：' + this.websocket.readyState)
            },
            setOnopenMessage () {
                console.log('WebSocket连接成功    状态码：' + this.websocket.readyState)
            },
            setOnmessageMessage (event) {
                // 根据服务器推送的消息做自己的业务处理
                console.log('服务端返回：' + event.data)
            },
            setOncloseMessage () {
                console.log('WebSocket连接关闭    状态码：' + this.websocket.readyState)
            },
            onbeforeunload () {
                this.closeWebSocket()
            },
            closeWebSocket () {
                this.websocket.close()
            },
            toggleSideBar() {
                this.$store.dispatch('app/toggleSideBar')
            },
        }
    }
</script>

<style lang="scss" scoped>
    .index{
        .button{
            margin: 1rem;
        }
    }
</style>
