<!--
 * @File: index
 * @Author: 裴浩宇
 * @Date: 2023/6/6 10:01
 * @Description: 聊天室
-->
<template>
    <div class="page">
        <div class="chat-room">
            <div :style="chat" class="chat" @click="previewImage($event)">
                <div class="record" ref="record">
                    <div class="phone-info">
                        <div class="phone-online-number">
                            {{ `在线人数（${memberList.length}）` }}
                        </div>
                        <el-icon @click="openInfo"><EditPen /></el-icon>
                    </div>
                    <div class="more" v-if="recordList.length > 50 && showRecordList.length < recordList.length"
                         @click="handleGetMore">
                        <svg-icon icon-class="time"/>
                        查看更多消息
                    </div>
                    <div class="record-one" :class="Number(one.userId) === Number(userInfo.id) ? 'is-me' : ''"
                         v-for="(one, index) in showRecordList"
                         :key="one.userId + index">
                        <div class="left">
                            <el-avatar :size="36" shape="square" :src="one.avatar">
                                <img src="https://cube.elemecdn.com/e/fd/0fc7d20532fdaf769a25683617711png.png"/>
                            </el-avatar>
                        </div>
                        <div class="right">
                            <div class="nick-name" v-if="one.userId === userInfo.id">
                                <el-tooltip class="item" effect="dark" content="回复" placement="top">
                                    <el-icon @click="handleReply(one)"><ChatDotRound /></el-icon>
                                </el-tooltip>
                                <el-tooltip class="item" effect="dark" :content="one.senTime" placement="top">
                                    <span class="time">{{ parseTime(one.senTime, '{h}:{i}') }}</span>
                                </el-tooltip>
                                （{{ one.location && one.location.ip }}）
                                {{ one.nickName }}
                            </div>
                            <div class="nick-name" v-else>
                                {{ one.nickName }}（{{ one.ip }}）
                                <el-tooltip class="item" effect="dark" :content="one.senTime" placement="top">
                                    <span class="time">{{ parseTime(one.senTime, '{h}:{i}') }}</span>
                                </el-tooltip>
                                <el-tooltip class="item" effect="dark" content="回复" placement="top">
                                    <el-icon @click="handleReply(one)"><ChatDotRound /></el-icon>
                                </el-tooltip>
                            </div>
                            <div class="message" v-html="sanitizeHtml(one.message)"></div>
                        </div>
                    </div>
                </div>
                <div class="input-box">
                    <div class="function-button">
                        <el-popover
                            placement="top"
                            title="选择表情"
                            trigger="click">
                            <emoji @select-icon="handleSelectEmoji"/>
                            <template #reference><el-icon><Star /></el-icon></template>
                        </el-popover>
                        <el-popover
                            placement="top"
                            title="选择文件"
                            trigger="click">
                            <file @select-file="handleSelectFile"/>
                            <template #reference><el-icon><Folder /></el-icon></template>
                        </el-popover>
                    </div>
                    <div class="textarea" contenteditable
                         ref="message" @keyup.enter="sendMessage"></div>
                    <el-button
                        :loading="sendMessageLoading"
                        size="small"
                        @click="sendMessage"
                        type="primary">
                        发 送
                    </el-button>
                </div>
            </div>
            <div :style="roomInfo" class="room-info">
                <div class="user-info">
                    <el-icon class="return" @click="returnChat"><ArrowLeft /></el-icon>
                    <div class="message-board-right" v-loading="userInfoLoading">
                        <div class="user-head">
                            <el-image :src="userInfo.avatar" fit="scale-down">
                                <div slot="error" class="image-slot">
                                    获取图片
                                </div>
                            </el-image>
                        </div>
                        <div class="customer-name">
                            <div class="label">昵称：</div>
                            <div class="name">
                                <el-tooltip :content="userInfo.nickName" placement="top" effect="light">
                                    <span>{{ userInfo.nickName }}</span>
                                </el-tooltip>
                            </div>
                            <div class="ip">（{{ userInfo.location.ip }}）</div>
                        </div>
                    </div>
                </div>
                <div class="member">
                    <div class="online-number">
                        {{ `在线人数（${memberList.length}）` }}
                    </div>
                    <div class="member-list">
                        <div class="member-one" v-for="one in memberList" :key="one.userId">
                            <el-avatar :size="24" shape="square" :src="one.avatar">
                                <img src="https://cube.elemecdn.com/e/fd/0fc7d20532fdaf769a25683617711png.png"/>
                            </el-avatar>
                            <div class="nick-name">{{ one.nickName }}（{{ one.location && one.location.ip }}）</div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
        <!--  预览图片  -->
        <el-dialog v-model="imgVisible" :close-on-press-escape="true" class="img-model" :modal="false" v-dom-drag>
            <model-image :previewImage="imgUrl" :imgSrcList="photoUrlList" v-if="imgVisible"/>
        </el-dialog>
    </div>
</template>

<script>
import {getMessageRecord, loginChat, sendMessage, signOut} from "@/api/px/chat";
import emoji from './emoji'
import {WEBSOCKET_MESSAGE_TYPE} from "@/assets/js/common";
import {sanitizeHtml} from '@/utils/sanitizeHtml';

export default {
    name: "Chat",
    components: {
        file: () => import('./file.vue'),
        emoji
    },
    data() {
        return {
            // websocket
            websocket: {},
            // 查看更多聊天记录
            more: 0,
            //图片标志
            imgVisible: false,
            //图片地址
            imgUrl: '',
            //图片url列表
            photoUrlList: [],
            //聊天样式
            chat: '',
            //个人信息样式
            roomInfo: '',
            //发送信息加载标志
            sendMessageLoading: false,
            //个人信息
            userInfo: this.$store.state.user,
            //聊天信息内容
            recordList: [],
            //在线人数
            memberList: [],
            // 用户信息加载
            userInfoLoading: false,
        }
    },
    computed: {
        /**
         * 展示消息
         */
        showRecordList() {
            return this.recordList.slice(-50 - this.more)
        }
    },
    methods: {
        /**
         * 滚动最下面
         */
        scroll() {
            const imgInterval = setInterval(() => {
                let imgFlag = true;
                document.querySelectorAll('.record img').forEach(img => {
                    try {
                        if (img.height === 0) {
                            imgFlag = false
                        }
                    } catch (e) {
                        throw new Error()
                    }
                });
                if (imgFlag) {
                    clearInterval(imgInterval);
                    let ele = this.$refs.record;
                    ele.scrollTop = ele.scrollHeight;
                }
            }, 100);
        },
        /**
         * 回复
         */
        handleReply(message) {
            const html = this.$refs.message;
            const reply = document.createElement("div");
            let reg = /<img.*?src=[\"|\']?(.*?)[\"|\']*?>/g;
            message.message = message.message.replace(reg, '[图片]');
            reply.classList.add("chat-reply");
            reply.setAttribute('contentEditable', 'false');
            reply.innerHTML = `<div class="reply-icon"></div>回复 ${message.nickName}：${message.message}`;
            if (!html.innerHTML) {
                html.innerHTML = `<div>${html.innerHTML}</div>`
            }
            if (html.children[0] && html.children[0].getAttribute('class') === 'chat-reply') {
                html.children[0].innerHTML = `<div class="reply-icon"></div>回复 ${message.nickName}：${message.message}`;
            } else {
                html.insertBefore(reply, html.children[0]);
                html.innerHTML += `<br>`;
            }
        },
        /**
         * 设置光标位置
         */
        handlerFocus(el) {
            if (typeof window.getSelection != "undefined" && typeof document.createRange != "undefined") {
                let range = document.createRange();
                range.selectNodeContents(el);
                range.collapse(false); // 将光标定位到内容的末尾
                let sel = window.getSelection();
                sel.removeAllRanges();
                sel.addRange(range);
            } else if (typeof document.body.createTextRange != "undefined") {
                let textRange = document.body.createTextRange();
                textRange.moveToElementText(el);
                textRange.collapse(false); // 将光标定位到内容的末尾
                textRange.select();
            }
        },
        /**
         * 选择表情
         */
        handleSelectEmoji(emoji) {
            if (emoji.position) {
                this.$refs.message.innerHTML += `<span contentEditable="false" class="item-icon" style="background-position: ${emoji.position}"></span>`;
            } else {
                this.$refs.message.innerHTML += emoji;
            }
            this.handlerFocus(this.$refs.message);
            this.$refs.message.scrollTop = this.$refs.message.scrollHeight;
        },
        /**
         * 选择文件
         */
        handleSelectFile(file) {
            if (file.isPicture) {
                this.$refs.message.innerHTML += `<div class="image"><img src="${file.url}" alt="${file.name}"></div><br>`
            } else {
                this.$refs.message.innerHTML +=
                    `<div class="chat-file" contentEditable="false">
    <div class="top">
        <el-icon><Folder /></el-icon>
        ${file.name}
    </div>
    <div class="bottom">
        <a href="${file.url}">下载</a>
    </div>
</div><br>`;
            }
            this.handlerFocus(this.$refs.message);
            this.$refs.message.scrollTop = this.$refs.message.scrollHeight;
        },
        /**
         * 获取消息
         */
        getMessageRecord() {
            getMessageRecord().then(res => {
                this.recordList = res.data;
                this.recordList.map(record => {
                    const userInfo = this.memberList.find(item => item.userId === record.userId);
                    if (!userInfo) return record;
                    record.nickName = userInfo.nickName;
                    record.avatar = userInfo.avatar;
                    record.ip = userInfo.location.ip;
                    return record
                });
                this.scroll();
            })
        },
        /**
         * 查看更多
         */
        handleGetMore() {
            this.more += 10;
        },
        /**
         * 预览图片
         * @param event
         */
        previewImage(event) {
            if (event.target.nodeName === 'IMG') {
                this.photoUrlList = [];
                let obj = $('.markdown-body img');
                for (let i = 0; i < obj['length']; i++) {
                    this.photoUrlList.push(obj[i].getAttribute('src'));
                }
                this.imgVisible = true;
                this.imgUrl = event.target.getAttribute('src');
            }
        },
        /**
         * 发送信息
         */
        sendMessage() {
            if (this.sendMessageLoading) {
                return
            }
            if (this.websocket.readyState !== 1) {
                this.initWebSocket();
            }
            if (!this.$refs.message.innerHTML.replace(/<div><br><\/div>/g, '')) {
                this.$message.error('发送内容不能为空，请重新输入');
                this.$refs.message.innerHTML = '';
            } else {
                let message = this.$refs.message.innerHTML.replace(/<div><br><\/div>/g, '');
                message = message.replace(/<br>/g, '');
                this.$refs.message.innerHTML = '';
                this.sendMessageLoading = true;
                sendMessage({message: message}).then(res => {
                    if (res.code === 200) {
                        this.sendMessageLoading = false;
                    }
                })
            }
        },
        /**
         * 返回聊天
         */
        returnChat() {
            this.chat = 'display: flex!important;'
            this.roomInfo = 'display: none!important;'
        },
        /**
         * 打开信息
         */
        openInfo() {
            this.chat = 'display: none!important;'
            this.roomInfo = 'display: flex!important;'
        },
        /**
         * 退出登录
         */
        signOut() {
            signOut().then(res => {
                this.memberList = res.data;
                this.recordList = [];
                this.memberList = [];
                this.onbeforeunload();
            });
        },
        /**
         * 初始化webSocket
         */
        initWebSocket() {
            // WebSocket
            if ('WebSocket' in window) {
                this.websocket = new WebSocket(`${import.meta.env.VUE_APP_SOCKET}/${this.userInfo.id}`);
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
            // 监听窗口关闭事件，当窗口关闭时，主动去关闭websocket连接，防止连接还没断开就关闭窗口，server端会抛异常。
            window.onbeforeunload = this.onbeforeunload;
        },
        setErrorMessage() {
            console.error('WebSocket连接发生错误 状态码：' + this.websocket.readyState);
        },
        setOnopenMessage() {
            this.userInfoLoading = true;
            loginChat().then(res => {
                if (res.data.length > 0) {
                    this.memberList = res.data.filter(item => {
                        return item.avatar && item.nickName;
                    });
                    this.userInfoLoading = false;
                    this.getMessageRecord();
                } else {
                    this.$message.error('网络异常，请清理缓存刷新当前页面')
                }
            });
        },
        setOnmessageMessage(event) {
            // 根据服务器推送的消息做自己的业务处理
            if (!event.data) {
                return
            }
            let data = {}
            try {
                data = JSON.parse(event.data)
            } catch (e) {
                console.error('WebSocket message parse error:', e)
                return
            }
            switch (data.webSocket) {
                case WEBSOCKET_MESSAGE_TYPE.CHAT_MESSAGE:
                    const message = data.message;
                    // 消费消息
                    const userInfo = this.memberList.find(item => Number(item.userId) === Number(message.userId));
                    message.nickName = userInfo.nickName;
                    message.avatar = userInfo.avatar;
                    message.ip = userInfo.location.ip;
                    this.recordList.push(message);
                    this.scroll();
                    break;
                case WEBSOCKET_MESSAGE_TYPE.LOGIN:
                    // 登录
                    let flag = true;
                    this.memberList.forEach(item => {
                        if (data.message.userId === item.userId) {
                            flag = false;
                        }
                    });
                    if (flag) {
                        this.memberList.push(data.message)
                    }
                    break;
                case WEBSOCKET_MESSAGE_TYPE.LOG_OUT:
                    // 退出登录
                    this.memberList = this.memberList.filter(item => {
                        return item.userId !== data.userId
                    });
                    break;
            }
        },
        setOncloseMessage() {
        },
        onbeforeunload() {
            this.closeWebSocket();
        },
        closeWebSocket() {
            this.websocket.close()
        }
    },
    beforeUnmount() {
        this.signOut();
    },
}
</script>

<style lang="scss" scoped>
@media screen and (max-width: 1000px) {
    .chat-room {
        height: 94% !important;
        width: 94% !important;

        .user-info {
            .return {
                display: flex !important;
                font-size: var(--text-lg);
                padding: var(--space-3) var(--space-2);
                position: absolute;
            }
        }

        .phone-info {
            display: flex;
            align-items: center;
            justify-content: space-between;
            color: var(--text-tertiary);
            margin-bottom: var(--space-4);

            i {
                font-size: var(--text-lg);
            }
        }

        .room-info {
            display: none !important;
        }
    }
}

.phone-info {
    display: none;
}

#chat-room {
    display: flex;
    height: 90vh;
    justify-content: center;
    align-items: center;
}

.chat-room {
    display: flex;
    height: 82vh;
    border: 1px solid var(--border-primary);
    border-radius: var(--radius-lg);
    background-color: var(--bg-body);
    box-shadow: var(--shadow-lg);

    .chat {
        flex: 7;
        border-right: 1px solid var(--border-primary);
        display: flex;
        flex-flow: column;
        width: 70%;

        ::v-deep .item-icon {
            width: 24px;
            height: 24px;
            display: inline-block;
            background-image: url('../../assets/images/emoji/emoji.png');
            background-size: 1100% 1000%;
            background-position: 0 0;
            cursor: pointer;
        }

        ::v-deep .chat-file {
            background: var(--bg-card);
            border-radius: var(--radius-md);
            border: 1px solid var(--border-primary);
            margin-top: var(--space-2);
            user-select: none;
            transition: border-color var(--duration-fast) var(--ease-default);

            &:first-child {
                margin-top: 0;
            }

            .top {
                display: flex;
                align-items: center;
                padding: var(--space-4);
                color: var(--text-secondary);

                i {
                    font-size: 2rem;
                    color: var(--color-primary);
                    margin-right: var(--space-2);
                }
            }

            .bottom {
                padding: var(--space-2) var(--space-4);
                border-top: 1px solid var(--border-primary);
                color: var(--color-primary);
                font-size: var(--text-sm);
                text-align: right;
                transition: color var(--duration-fast) var(--ease-default);

                &:hover {
                    text-decoration: underline;
                }
            }
        }

        ::v-deep .chat-reply {
            padding: var(--space-2);
            color: var(--text-tertiary);
            background-color: var(--bg-hover);
            border-radius: var(--radius-sm);

            .reply-icon {
                width: 0.2rem;
                height: 1rem;
                background-color: var(--text-tertiary);
                display: inline-block;
                margin-right: var(--space-2);
            }
        }

        .record {
            flex: 5;
            border-bottom: 1px solid var(--border-primary);
            height: 28rem;
            overflow-y: auto;
            padding: var(--space-4) var(--space-4) 0 var(--space-4);

            .more {
                display: flex;
                align-items: center;
                justify-content: center;
                color: var(--color-primary);
                cursor: pointer;
                user-select: none;
                transition: opacity var(--duration-fast) var(--ease-default);

                &:hover {
                    opacity: 0.8;
                }

                .svg-icon {
                    margin-right: var(--space-2);
                }
            }

            .record-one {
                display: flex;
                margin-bottom: var(--space-4);
                transition: opacity var(--duration-normal) var(--ease-default);

                &:hover {
                    .el-icon-s-comment {
                        display: block !important;
                    }
                }

                .left {
                    margin-right: var(--space-2);
                }

                .right {
                    .nick-name {
                        color: var(--text-tertiary);
                        font-size: var(--text-sm);
                        margin-left: var(--space-2);
                        display: flex;
                        align-items: center;
                        height: 1.2rem;

                        .el-icon-s-comment {
                            display: none;
                            font-size: 1.4rem;
                            margin-left: var(--space-2);
                            cursor: pointer;
                            transition: color var(--duration-fast) var(--ease-default);

                            &:hover {
                                color: var(--color-primary);
                            }
                        }
                    }

                    .message {
                        margin-top: var(--space-2);
                        padding: var(--space-2);
                        background-color: var(--bg-card);
                        border-radius: var(--radius-md);
                        width: fit-content;
                        display: flex;
                        align-items: flex-end;
                        flex-wrap: wrap;
                        flex-direction: column;
                        box-shadow: var(--shadow-sm);

                        ::v-deep img {
                            max-width: 10rem;
                            cursor: pointer;
                            max-height: 10rem;
                        }
                    }
                }
            }

            .is-me {
                flex-flow: row-reverse !important;

                .message {
                    -moz-border-radius-topright: 0;
                }

                .right {
                    display: flex;
                    flex-direction: column;
                    align-items: flex-end;

                    .nick-name {
                        text-align: right;
                        margin-right: var(--space-2);

                        .el-icon-s-comment {
                            margin-right: var(--space-2);
                        }
                    }

                    .message {
                        margin-left: 0 !important;
                        margin-right: var(--space-2);
                        background-color: var(--color-primary);
                        color: var(--bg-card);
                    }

                    .message:before {
                        display: none;
                    }
                }
            }
        }

        .input-box {
            flex: 2;
            padding: 0 var(--space-4) var(--space-4) var(--space-4);
            display: flex;
            flex-flow: column;
            overflow: hidden;

            .function-button {
                padding-top: 0.6rem;

                i {
                    font-size: 1.5rem;
                    padding: var(--space-2);
                    margin-right: 0.4rem;
                    border-radius: var(--radius-md);
                    cursor: pointer;
                    transition: background-color var(--duration-fast) var(--ease-default);

                    &:hover {
                        background-color: var(--bg-hover);
                    }
                }
            }

            .textarea {
                flex: 8;
                padding: var(--space-2) 0;
                overflow: scroll;

                ::v-deep img {
                    max-width: 10rem;
                }
            }

            .button {
                flex: 2;
                display: flex;
                justify-content: flex-end;
                padding-top: var(--space-2);
            }
        }

    }

    .room-info {
        flex: 3;
        display: flex;
        flex-flow: column;

        .user-info {
            flex: 2;
            border-bottom: 1px solid var(--border-primary);

            .return {
                display: none;
            }

            .message-board-right {
                display: flex;
                padding: var(--space-4);
                flex-flow: column;
                align-items: center;
                justify-content: space-around;

                .user-head {

                    display: flex;
                    align-items: center;
                    justify-content: center;
                    cursor: pointer;

                    .el-image {
                        border-radius: var(--radius-lg);
                    }
                }

                .customer-name {
                    height: 2rem;
                    width: 100%;
                    margin-top: var(--space-2);
                    display: flex;
                    align-items: center;

                    .label {
                        white-space: nowrap;
                    }

                    .name {
                        white-space: nowrap;
                        overflow: hidden;
                        text-overflow: ellipsis;
                        color: var(--color-primary);
                    }
                }

                .button {
                    padding-top: var(--space-2);
                    display: flex;
                }

                .your-header {
                    width: 6rem;
                    height: 6rem;
                    border: 1px solid var(--border-primary);
                    border-radius: var(--radius-md);
                    display: flex;
                    flex-flow: column;
                    align-items: center;
                    justify-content: center;

                    #headerPhoto {
                        width: 7rem;
                        position: absolute;
                        transform: scale(0.7);
                        margin-top: var(--space-4);
                    }

                    ::v-deep .image-slot {
                        font-size: 0.4rem;
                        transform: scale(0.8);
                        text-align: center;
                    }

                    .close-icon {
                        position: absolute;
                        margin: -5rem -5rem 0 0;
                        color: var(--color-primary-600);
                    }

                    .header-picture {
                        width: 100%;
                        height: 100%;
                        padding: var(--space-2);
                        display: flex;
                        justify-content: center;
                        white-space: nowrap;

                        img {
                            width: 100%;
                            height: 100%;
                        }
                    }
                }
            }

        }

        .member {
            flex: 5;
            padding: var(--space-4);

            .online-number {

            }

            .member-list {
                .member-one {
                    display: flex;
                    align-items: center;
                    padding-top: var(--space-2);

                    .nick-name {
                        margin-left: var(--space-2);
                    }
                }
            }
        }
    }
}

/* 图片预览样式开始*/
.el-image-viewer__canvas {
    width: 60%;
}

.el-image-viewer__next {
    right: calc(40vw + 4rem);
}

.img-model {
    ::v-deep .el-dialog {
        margin: 20vh 25vw !important;
        width: 50vw;
        height: 60vh;
        background-color: var(--bg-hover) !important;
        border-radius: var(--radius-lg);

        .el-dialog__body {
            height: 90%;
            overflow: auto;
        }
    }
}
</style>

