<template>
    <div id="messageBoard" v-loading="loading" class="message-all">
        <div class="message">
            <no-data text="暂无留言" v-if="leaveMessageList.length === 0"/>
            <div class="no-leave-message message-label" v-if="leaveMessageList.length === 0">
                还没有童鞋留言，快来留言吧！
            </div>
            <div class="message-label" v-if="leaveMessageList.length > 0">
                已经有{{ leaveMessageList.length }}位童鞋留言了，还不来盖楼！
            </div>
            <template v-for="(leaveMessage, index) in leaveMessageList" :key="leaveMessage.id">
            <div class="leave-message"
                 v-if="leaveMessage.state !== '2'"
                 :class="leaveMessage.createBy ? 'admin-leave-message' : ''"
                 :id="leaveMessage.id">
                <a :href="leaveMessage.authorAddress" target="_blank">
                    <div class="message-left">
                        <div class="header-photo">
                            <el-image
                                :src="leaveMessage.avatar"
                                class="header-picture rotate-box pointer"
                                fit="scale-down">
                                <template #error>
                                    <div class="image-slot invalid-svg">
                                    <svg-icon icon-class="已失效2"/>
                                    </div>
                                </template>
                            </el-image>
                        </div>
                        <div class="author-name">
                            {{ leaveMessage.authorName ? leaveMessage.authorName : leaveMessage.nickName }}
                        </div>
                    </div>
                </a>
                <div class="message-right">
                    <div class="message-right-content">
                        <div class="message-right-top">
                            <div class="message-content markdown-body">
                                <div class="reply-box">
                                    <div class="reply-name" @click="goToMessage(leaveMessage.id, leaveMessage.replyId)"
                                         v-if="leaveMessage.replyId">
                                        回复：<span>{{ getMessageById(leaveMessage.replyId).authorName }}</span>
                                        <span
                                            class="reply-floor">{{
                                                getMessageById(leaveMessage.replyId).index + 1 + 'F'
                                            }}</span>
                                    </div>
                                </div>
                                <div v-html="sanitizeHtml(leaveMessage.content)"></div>
                                <div v-if="leaveMessage.state !== '1'" class="delete-message">
                                    {{ deleteMsg(leaveMessage.state) }}
                                </div>
                            </div>
                            <div class="floor">
                                <span class="reply" @click="reply(leaveMessage, index)">回复</span>
                                <span>{{ leaveMessageList.length - index }}F</span>
                            </div>
                        </div>
                        <div class="leave-message-time">
                            <div class="delete" @click="deleteMessage(leaveMessage)"><span
                                v-if="leaveMessage.state === '1'">删除</span></div>
                            <div class="time">{{ leaveMessage.createTime }}</div>
                        </div>
                    </div>
                </div>
            </div>
            </template>
            <pagination
                class="pagination"
                v-show="total>queryParams.pageSize"
                :total="total"
                v-model:page="queryParams.pageNum"
                v-model:limit="queryParams.pageSize"
                @pagination="getLeaveMessage"
            />
        </div>
        <div class="message-board">
            <div class="message-board-left">
                <div class="reply-box">
                    <div class="reply-name" v-if="messageForm.replyId">
                        回复：<span>{{ replyMessage && replyMessage.nickName }}</span>
                        <span class="reply-delete" @click="deleteReply()"><el-icon><CircleClose /></el-icon></span>
                    </div>
                </div>
                <div class="message-textarea"
                     contenteditable
                     @keyup.enter="addMessage"
                     ref="message">
                </div>
            </div>
            <div class="message-board-right">
                <div class="button theme-blue-text" @click="addMessage">
                    提交
                </div>
            </div>
        </div>
    </div>
</template>

<script>
import {addMessage, getMessageList, updateMessage} from '@/api/px/blog/message';
import {sanitizeHtml} from '@/utils/sanitizeHtml';

export default {
    name: "index",
    props: {
        articleId: {
            default: ''
        },
        messageType: {
            type: String,
            default: '0'
        }
    },
    data() {
        return {
            //留言板遮罩
            loading: true,
            //留言列表
            leaveMessageList: [],
            // 总条数
            total: 0,
            //参数
            queryParams: {
                pageNum: 1,
                pageSize: 50,
                //文章或相册ID
                articleId: this.articleId,
                messageBoard: this.messageType
            },
            //输入标志
            inputFlag: true,
            //留言表单
            messageForm: {
                //文章或相册ID
                articleId: this.articleId,
                //回复ID
                replyId: '',
                //留言内容
                content: '',
                //留言内容html格式
                contentHtml: '',
                //游客姓名
                authorName: '',
                //游客邮箱
                authorMailbox: '',
                //头像的URL
                authorHeader: '',
                //是否是留言板留言
                messageBoard: this.messageType,
            },
            //当前回复留言
            replyMessage: {
                id: '',
                authorName: '',
                index: '',
                emailFlag: true,
                content: '',
                authorMailbox: ''
            }
        }
    },
    watch: {
        articleId: {
            handler(newValue) {
                this.queryParams.articleId = newValue;
                this.messageForm.articleId = newValue;
                this.getLeaveMessage();
            },
            deep: true,
            immediate: true
        }
    },
    mounted() {
        this.getLeaveMessage();
        if (sessionStorage.getItem('messageForm')) {
            let header = JSON.parse(sessionStorage.getItem('messageForm')).authorHeader;
            let authorName = JSON.parse(sessionStorage.getItem('messageForm')).authorName;
            let authorMailbox = JSON.parse(sessionStorage.getItem('messageForm')).authorMailbox;
            this.$nextTick(() => {
                setTimeout(() => {
                    this.messageForm.authorHeader = header;
                }, 500);
                this.messageForm.authorName = authorName;
                this.messageForm.authorMailbox = authorMailbox;
            })
        }
    },
    methods: {
        /** 删除操作 */
        deleteMessage(row) {
            this.$confirm(`是否确认删除?`, "删除", {
                confirmButtonText: "确定",
                cancelButtonText: "取消",
                type: "warning"
            }).then(() => {
                updateMessage({id: row.id, state: '3'}).then(response => {
                    this.msgSuccess(`删除成功`);
                    this.getLeaveMessage();
                });
            })
        },
        /**
         * 返回提示语
         */
        deleteMsg(state) {
            let msg = '';
            if (state === '0') {
                msg = '内容待审核'
            } else if (state === '3') {
                msg = '内容已被管理员删除'
            }
            return msg;
        },
        /**
         * 返回头像
         */
        avatar(avatar) {
            if (avatar.indexOf('http') !== -1) {
                return avatar
            }
            return import.meta.env.VUE_APP_BASE_API + avatar
        },
        /**
         * 移到回复留言
         */
        goToMessage(id, replyId) {
            const startY = document.getElementById(id).offsetTop;
            const endY = document.getElementById(replyId).offsetTop;
            this.scrollAnimation(startY, endY - 100);
            document.getElementById(replyId).style.backgroundColor = '#C1E6C6';
            setTimeout(() => {
                document.getElementById(replyId).style.backgroundColor = '';
            }, 1000);
        },
        /**
         * 根据回复ID返回回复留言内容
         */
        getMessageById(id) {
            let messageRes = {};
            this.leaveMessageList.forEach((message, index) => {
                if (message.id === id) {
                    messageRes = message;
                    messageRes.index = this.leaveMessageList.length - index - 1;
                }
            });
            return messageRes
        },
        /**
         * 取消回复
         */
        deleteReply() {
            this.messageForm.replyId = '';
        },
        /**
         * 回复留言
         */
        reply(message, index) {
            this.messageForm.replyId = message.id;
            this.messageForm.replyUserId = message.createBy;
            this.messageForm.parentId = message.id;
            this.replyMessage.nickName = message.nickName;
            this.replyMessage.emailFlag = message.emailFlag;
            this.replyMessage.content = message.content;
            this.replyMessage.authorMailbox = message.authorMailbox;
            this.replyMessage.index = this.leaveMessageList.length - index;
            const startY = document.getElementById(message.id).offsetTop;
            const endY = document.getElementsByClassName("message-board")[0].offsetTop;
            this.scrollAnimation(startY, endY - 100);
        },
        /**
         * 获取留言板留言
         */
        getLeaveMessage() {
            this.loading = true;
            getMessageList(this.queryParams).then(res => {
                this.leaveMessageList = res.rows;
                this.total = res.total;
                this.loading = false;
            })

        },
        /**
         * 新增留言
         */
        addMessage() {
            if (!this.$refs.message.innerHTML.replace(/<div><br><\/div>/g, '')) {
                this.$message.error('发送内容不能为空，请重新输入');
                this.$refs.message.innerHTML = '';
            } else {
                let message = this.$refs.message.innerHTML.replace(/<div><br><\/div>/g, '');
                this.messageForm.content = message.replace(/<br>/g, '');
                this.$refs.message.innerHTML = '';
                this.loading = true;
                addMessage(this.messageForm).then(res => {
                    if (res.data === 1) {
                        this.$notify.success('留言成功');
                        this.getLeaveMessage();
                        this.messageForm.content = '';
                        this.messageForm.replyId = '';
                    }
                })
            }
        },
        sanitizeHtml,
    },
}
</script>

<style lang='scss' scoped>
::v-deep .markdown-body {
    padding: 0 !important;
    font-size: 14px;
    color: var(--text-primary);
}

.message-all {
    padding: var(--space-4);
    background: var(--bg-body);
    border-radius: var(--radius-lg);

    .message {
        padding-bottom: var(--space-4);

        .message-label {
            font-size: 14px;
            font-weight: 600;
            color: var(--text-primary);
            margin-bottom: var(--space-4);
            padding-left: var(--space-2);
            border-left: 4px solid var(--color-primary);
        }

        .leave-message {
            display: flex;
            padding: var(--space-4);
            align-items: flex-start;
            border-radius: var(--radius-lg);
            margin-bottom: var(--space-4);
            background: var(--bg-card);
            box-shadow: var(--shadow-sm);
            transition: all var(--duration-normal) var(--ease-default);

            &:hover {
                box-shadow: var(--shadow-md);
                transform: translateY(-2px);
            }

            .message-left {
                margin-right: var(--space-4);
                width: 60px;
                display: flex;
                flex-flow: column;
                align-items: center;

                .header-photo {
                    width: 48px;
                    height: 48px;
                    border-radius: var(--radius-full);
                    overflow: hidden;
                    box-shadow: var(--shadow-sm);
                    border: 2px solid var(--border-primary);

                    img {
                        width: 100%;
                        height: 100%;
                    }
                }

                .author-name {
                    color: var(--text-secondary);
                    font-size: 12px;
                    margin-top: 8px;
                    text-align: center;
                    width: 100%;
                    overflow: hidden;
                    text-overflow: ellipsis;
                    white-space: nowrap;
                }
            }

            .message-right {
                flex: 1;
                min-width: 0;

                .message-right-top {
                    display: flex;
                    justify-content: space-between;

                    .message-content {
                        flex: 1;
                        padding-right: var(--space-4);
                    }

                    .reply-box {
                        margin-bottom: 8px;
                        .reply-name {
                            font-size: 13px;
                            color: var(--color-primary);
                            background: var(--bg-hover);
                            padding: 4px 8px;
                            border-radius: 4px;

                            .reply-floor {
                                margin-left: 8px;
                                opacity: 0.7;
                            }
                        }
                    }

                    .delete-message {
                        font-size: 12px;
                        color: var(--text-muted);
                        background: var(--bg-hover);
                        padding: 8px;
                        border-radius: var(--radius-md);
                        margin-top: 8px;
                    }

                    .floor {
                        font-size: 14px;
                        color: var(--text-muted);
                        display: flex;
                        align-items: flex-start;
                        gap: 12px;

                        .reply {
                            opacity: 0;
                            color: var(--color-primary);
                            cursor: pointer;
                            font-weight: 500;
                            transition: opacity var(--duration-fast);
                        }
                    }
                }

                .leave-message-time {
                    display: flex;
                    justify-content: space-between;
                    align-items: center;
                    margin-top: var(--space-3);
                    font-size: 12px;
                    color: var(--text-muted);

                    .delete {
                        span {
                            opacity: 0;
                            color: var(--color-danger);
                            cursor: pointer;
                            transition: opacity var(--duration-fast);
                        }
                    }
                }
            }

            &:hover {
                .message-right-top .floor .reply,
                .leave-message-time .delete span {
                    opacity: 1;
                }
            }
        }

        .admin-leave-message {
            background: var(--color-primary-50);
            border: 1px solid var(--color-primary-100);
            
            .author-name {
                color: var(--color-primary);
                font-weight: 600;
            }
        }
    }

    .message-board {
        margin-top: var(--space-6);
        padding: var(--space-4);
        background: var(--bg-card);
        border: 1px solid var(--border-primary);
        border-radius: var(--radius-lg);
        box-shadow: var(--shadow-sm);

        .message-board-left {
            width: 100%;

            .reply-box {
                height: auto;
                margin-bottom: 8px;

                .reply-name {
                    font-size: 13px;
                    display: inline-flex;
                    align-items: center;
                    background: var(--bg-hover);
                    padding: 4px 12px;
                    border-radius: 20px;
                    color: var(--text-secondary);

                    span {
                        color: var(--color-primary);
                        font-weight: 600;
                        margin: 0 4px;
                    }

                    .reply-delete {
                        margin-left: 8px;
                        cursor: pointer;
                        display: flex;
                        align-items: center;
                        color: var(--text-muted);
                        
                        &:hover {
                            color: var(--color-danger);
                        }
                    }
                }
            }

            .message-textarea {
                padding: var(--space-3);
                border: 1px solid var(--border-primary);
                border-radius: var(--radius-md);
                width: 100%;
                min-height: 100px;
                background: var(--bg-body);
                transition: border-color var(--duration-fast);

                &:focus {
                    outline: none;
                    border-color: var(--color-primary);
                    box-shadow: 0 0 0 2px rgba(var(--color-primary), 0.1);
                }

                ::v-deep img {
                    max-width: 100px;
                    border-radius: 4px;
                }
            }
        }

        .message-board-right {
            display: flex;
            justify-content: flex-end;
            margin-top: var(--space-3);

            .button {
                background: var(--color-primary);
                color: white;
                padding: 8px 24px;
                border-radius: var(--radius-md);
                cursor: pointer;
                font-weight: 500;
                transition: all var(--duration-fast);

                &:hover {
                    background: var(--color-primary-600);
                    transform: translateY(-1px);
                    box-shadow: var(--shadow-sm);
                }

                &:active {
                    transform: translateY(0);
                }
            }
        }
    }
}

.pagination {
    margin-top: var(--space-4);
    display: flex;
    justify-content: flex-end;
}
</style>
