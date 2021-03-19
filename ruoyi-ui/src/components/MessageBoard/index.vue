<template>
    <div class="message-all" v-loading="loading">
        <div class="message">
            <no-data text="暂无留言" v-if="leaveMessageList.length === 0"/>
            <div class="no-leave-message message-label" v-if="leaveMessageList.length === 0">
                还没有童鞋留言，快来留言吧！
            </div>
            <div class="message-label" v-if="leaveMessageList.length > 0">已经有{{leaveMessageList.length}}位童鞋留言了，还不来盖楼！</div>
            <div class="leave-message" v-for="(leaveMessage, index) in leaveMessageList" :key='leaveMessage.id' :id="leaveMessage.id">
                <div class="message-left">
                    <div class="header-photo">
                        <el-image
                            class="header-picture"
                            :src="leaveMessage.authorHeader"
                            fit="scale-down">
                        </el-image>
                    </div>
                    <div class="author-name">
                        {{leaveMessage.authorName}}
                    </div>
                </div>
                <div class="message-right">
                    <div class="message-right-top">
                        <div class="message-content">
                            <div class="reply-box">
                                <div class="reply-name" @click="goToMessage(leaveMessage.id, leaveMessage.replyId)" v-if="leaveMessage.replyId">回复：<span>{{getMessageById(leaveMessage.replyId).authorName}}</span>
                                    <span class="reply-floor">{{getMessageById(leaveMessage.replyId).index + 1 + 'F'}}</span>
                                </div>
                            </div>
                            <div class="leave-message-content" v-html="leaveMessage.content"></div>
                        </div>
                        <div class="floor">
                            <span class="reply" @click="reply(leaveMessage, index)">回复</span>
                            <span>{{leaveMessageList.length - index}}F</span>
                        </div>
                    </div>
                    <div class="leave-message-time">
                        {{leaveMessage.createTime}}
                    </div>
                </div>
            </div>
            <pagination
                class="pagination"
                v-show="total>0"
                :total="total"
                :page.sync="queryParams.pageNum"
                :limit.sync="queryParams.pageSize"
                @pagination="getLeaveMessage"
            />
        </div>
        <div class="message-board">
            <div class="message-board-left">
                <div class="message-board-left-top">
                    <div class="message-logo"></div>
                    <div class="message-text">
                        <div class="text-left">To：</div>
                        <div class="text-right">pei你看雪</div>
                    </div>
                </div>
                <div class="message-board-left-bottom">
                    <div class="reply-box">
                        <div class="reply-name" v-if="messageForm.replyId">回复：<span>{{replyMessage && replyMessage.authorName}}</span>
                            <span class="reply-floor">{{replyMessage && replyMessage.index + 1 + 'F'}}</span>
                            <span class="reply-delete" @click="deleteReply()"><i class="el-icon-circle-close"/></span>
                        </div>
                    </div>
                    <div class="textarea"   :contenteditable='inputFlag'
                         ref="leaveMessage"
                         @input="leaveMessageChange($event)"></div>
                    <div class="tips">您还可以输入<span class="text-number">{{textNumber}}</span>字</div>
                </div>
            </div>
            <div class="message-board-right">
                <div class="your-header">
                    <el-image
                        class="header-picture"
                        :src="messageForm.authorHeader"
                        fit="scale-down">
                        <div slot="error" class="image-slot">
                            请上传头像
                        </div>
                    </el-image>
                    <i @click="deleteHeader" class="el-icon-circle-close close-icon" v-if="messageForm.authorHeader"/>
                    <input v-if="!messageForm.authorHeader" type="file" id="headerPhoto" capture="camera" accept="image/*" @change="uploadHeader($event)"/>
                </div>
                <div class="customer-name">
                    <div class="label">您的姓名：</div>
                    <div class="name">
                        <el-input v-model="messageForm.authorName" placeholder="请输入您的姓名"/>
                    </div>
                </div>
                <div class="customer-mail">
                    <div class="label">您的邮箱： </div>
                    <div class="name">
                        <el-input v-model="messageForm.authorMailbox" placeholder="请输入您的邮箱"/>
                    </div>
                </div>
                <div class="button">
                    <el-button type="primary" @click="addMessage">提交</el-button>
                </div>
            </div>
        </div>
    </div>

</template>

<script>
    import { addMessage, getLeaveMessageByArticleId, getMessageList } from '@/api/px/customer/article.js';
    import { compressImage } from '@/utils/compressImage'
    import { scrollAnimation } from '@/assets/js/public.js';

    export default {
        name: "index",
        props: {
            articleId: {
                type: String,
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
                    pageSize: 10,
                    //文章或相册ID
                    articleId: this.articleId,
                    messageBoard: this.messageType
                },
                //留言内容最大长度
                textMaxNumber: 500,
                //留言内容长度
                textNumber: 500,
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
                    //游客姓名
                    authorName: '',
                    //游客邮箱
                    authorMailbox: '',
                    //头像的URL
                    authorHeader: '',
                    //是否是留言板留言
                    messageBoard: this.messageType,
                },
                config: {
                    width: 64, // 压缩后图片的宽
                    height: 64, // 压缩后图片的高
                    quality: 1 // 压缩后图片的清晰度，取值0-1，值越小，所绘制出的图像越模糊
                },
                //当前回复留言
                replyMessage: {
                    id: '',
                    authorName: '',
                    index: ''
                }
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
            /**
             * 移到回复留言
             */
            goToMessage(id, replyId) {
                const startY = document.getElementById(id).offsetTop;
                const endY = document.getElementById(replyId).offsetTop;
                scrollAnimation(startY, endY - 100);
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
                this.replyMessage.authorName = message.authorName;
                this.replyMessage.index = this.leaveMessageList.length - index;
            },
            /**
             * 获取留言板留言
             */
            getLeaveMessage() {
                this.loading = true;
                if (this.messageType === '1') {
                    getMessageList(this.queryParams).then(res => {
                        this.leaveMessageList = res.rows;
                        this.total = res.total;
                        this.loading = false;
                    })
                } else {
                    getLeaveMessageByArticleId(this.queryParams).then(res => {
                        this.leaveMessageList = res.data;
                        this.total = res.data.length;
                        this.loading = false;
                    })
                }

            },
            /**
             * 新增留言
             */
            addMessage() {
                this.messageForm.content = this.$refs.leaveMessage.innerHTML;
                if (this.messageForm.content === '') {
                    this.$message.warning('请输入留言内容')
                } else if (this.messageForm.authorHeader === '') {
                    this.$message.warning('请上传头像')
                } else if (this.messageForm.authorName === '') {
                    this.$message.warning('请留下您的姓名')
                } else if (this.messageForm.authorMailbox === 'authorMailbox') {
                    this.$message.warning('请留下您的邮箱')
                } else {
                    this.loading = true;
                    addMessage(this.messageForm).then(res => {
                        if (res.data === 1) {
                            this.$message.success('留言成功');
                            this.getLeaveMessage();
                            sessionStorage.setItem('messageForm', JSON.stringify(this.messageForm));
                            this.messageForm.content = '';
                            this.textNumber = 500;
                            this.$refs.leaveMessage.innerHTML = '';
                        }
                    })
                }
            },
            /**
             * 删除头像
             */
            deleteHeader() {
                this.messageForm.authorHeader = '';
            },
            blobToDataURL(blob,cb) {
                let reader = new FileReader();
                reader.onload = function (evt) {
                    let base64 = evt.target.result;
                    cb(base64)
                };
                reader.readAsDataURL(blob);
            },
            /**
             * 上传头像
             */
            uploadHeader(e) {
                if(e.target.files[0]){
                    compressImage(e.target.files[0], this.config)
                        .then(result => { // result 为压缩后二进制文件
                            this.blobToDataURL(result, (base64Url) => {
                                this.messageForm.authorHeader = base64Url;
                            })
                        });
                }
            },
            /**
             * 监听留言板
             */
            leaveMessageChange(e) {
                this.textNumber = this.textMaxNumber - e.target.innerText.length;
                if ((this.textMaxNumber - e.target.innerText.length < 1)) {
                    this.inputFlag = false
                }
            },
        },
    }
</script>

<style lang='scss' scoped>
    .message-all{
        padding: 1rem;
        .message{
            padding-bottom: 1rem;
            .message-label{
                font-size: 0.9rem;
                font-weight: bold;
            }
            .leave-message{
                display: flex;
                padding: 1rem;
                align-items: flex-start;
                .message-left{
                    margin-right: 1rem;
                    width: 4rem;
                    display: flex;
                    flex-flow: column;
                    align-items: center;
                    .header-photo{
                        width: 4rem;
                        height: 4rem;
                        border-radius: 50%;
                        overflow: hidden;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        img{
                            width: 100%;
                            height: 100%;
                        }
                    }
                    .author-name{
                        color: #999;
                        margin-top: 0.5rem;
                        text-align: center;
                        text-shadow: 1px 1px 2px #0CF;
                    }
                }
                .message-right{
                    width: calc(100% - 5rem);
                    background: #C6F1FD;
                    padding: 1rem;
                    border-radius: 10px;
                    .message-right-top{
                        display: flex;
                        justify-content: space-between;
                        .reply-box{
                            .reply-name{
                                color: #249cff;
                                .reply-floor{
                                    margin-left: 0.5rem;
                                }
                            }
                        }
                        .leave-message-content{
                            font-size: 0.9rem;
                            color: rgb(34, 32, 32);
                        }
                        .floor{
                            font-size: 1.2rem;
                            width: 5rem;
                            display: flex;
                            justify-content: flex-end;
                            align-items: center;
                            .reply{
                                margin-right: 1rem;
                                color: #999;
                                cursor: pointer;
                                font-size: 1rem;
                            }
                            .reply:hover{
                                color: #00afff;
                            }
                        }
                    }
                    .leave-message-time{
                        display: flex;
                        justify-content: flex-end;
                        margin-top: 1rem;
                        font-size: 0.9rem;
                        color: rgb(78, 75, 75);
                    }
                }
                .message-right:before{
                    position: absolute;
                    content: "";
                    width: 0;
                    height: 0;
                    border-top: 0.8rem solid transparent;
                    border-right: 1rem solid #C6F1FD;
                    border-bottom: 0.8rem solid transparent;
                    margin: -0.1rem 0 0 -2rem;
                }
            }
        }
        .message-board{
            margin: 1rem;
            padding: 1rem;
            border: 3px dashed #ACEBFF;
            background-color: #fdfdfd;
            border-radius: 10px;
            height: 300px;
            box-shadow: inset 0 0 1rem #0CF;
            display: flex;
            align-items: center;
            .message-board-left{
                width: 60%;
                .message-board-left-top{
                    display: flex;
                    .message-logo{
                        width: 5rem;
                        height: 3rem;
                        margin: 1rem;
                        background-size: 100%;
                        background-image: url('../../assets/images/mc.jpg');
                        background-position: center;
                    }
                    .message-text{
                        display: flex;
                        align-items: center;
                        .text-left{
                            font-size: 1.2rem;
                            color: #ccc;
                        }
                        .text-right{
                            font-weight: bold;
                        }
                    }
                }
                .message-board-left-bottom{
                    .reply-box{
                        height: 2rem;
                        cursor: pointer;
                        .reply-name{
                            span{
                                color: #00afff;
                            }
                            .reply-delete{
                                display: none;
                                margin-left: 1rem;
                            }
                            .reply-floor{
                                font-size: 1rem;
                                color: #999999;
                                margin-left: 1rem;
                            }
                        }
                    }
                    .reply-box:hover{
                        .reply-delete{
                            display: inline-block;
                            color: red;
                        }
                    }
                    .textarea{
                        width: 100%;
                        height: 10rem;
                        padding: 1rem;
                        border: 1px solid #ccc;
                        border-radius: 5px;
                        overflow: scroll;
                    }
                    .tips{
                        font-size: 0.9rem;
                        color: #999;
                        padding: 0.5rem 0;
                        .text-number{
                            color: #FF399A;
                            font-style: italic;
                        }
                    }
                }
            }
            .message-board-right{
                width: 40%;
                display: flex;
                padding-left: 1rem;
                height: 100%;
                flex-flow: column;
                align-items: center;
                justify-content: space-around;
                .customer-name, .customer-mail{
                    display: flex;
                    align-items: center;
                    .label{
                        white-space: nowrap;
                    }
                }
                .button{
                    width: 6rem;
                    .el-button{
                        width: 100%;
                    }
                }
                .your-header{
                    width: 6rem;
                    height: 6rem;
                    border: 1px solid #999;
                    border-radius: 5px;
                    display: flex;
                    flex-flow: column;
                    align-items: center;
                    justify-content: center;
                    .close-icon{
                        position: absolute;
                        margin: -5rem -5rem 0 0;
                        color: red;
                    }
                    #headerPhoto{
                        width: 4.5rem;
                        position: absolute;
                    }
                    .header-picture{
                        width: 100%;
                        height: 100%;
                        padding: 0.5rem;
                        display: flex;
                        justify-content: center;
                        white-space: nowrap;
                        img{
                            width: 100%;
                            height: 100%;
                        }
                    }
                }
            }
        }
    }
</style>
