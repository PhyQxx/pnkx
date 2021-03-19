<template>
    <div class="page" v-loading="loading">
        <div class="top">
            <div class="label">位置：</div>
            <el-breadcrumb separator-class="el-icon-arrow-right">
                <el-breadcrumb-item class="pointer" :to="{ path: '/homepage' }"><i class="el-icon-collection-tag"/>首页</el-breadcrumb-item>
                <el-breadcrumb-item class="pointer" :to="{
                    name: 'articlelist',
                    params: {
                        code: article.typeCode
                        }
                }">{{article.typeName}}</el-breadcrumb-item>
                <el-breadcrumb-item>{{article.title}}</el-breadcrumb-item>
            </el-breadcrumb>
        </div>
        <div class="bottom">
            <div class="bottom-left">
                <div class="article">
                    <div class="article-title" :class="article.createBy === '1' ? 'phy-article-title' : 'qxx-article-title'">
                        {{article.title}}
                    </div>
                    <div class="article-info">
                        <div class="footer-one">
                            <i :class="article.createBy === '1' ? 'el-icon-male' : 'el-icon-female'"/>
                            <div class="author margin-right">{{article.nickName}}</div>
                        </div>
                        <div class="footer-one">
                            <i class="el-icon-date"/>
                            <div class="create-time margin-right">{{article.createTime}}</div>
                        </div>
                        <div class="footer-one pointer theme-blue-text" @click="goToArticleType(article.typeCode)">
                            <i class="el-icon-magic-stick"/>
                            <div class="type margin-right">{{article.typeName}}</div>
                        </div>
                        <div class="footer-one pointer" @click="goToMessage">
                            <i class="el-icon-present"/>
                            <div class="message-number">{{article.leaveMessageNumber}}枚留言</div>
                        </div>
                    </div>
                    <div class="article-content" v-html="article.richText"></div>
                    <div :class="article.createBy === '1' ? 'phy-hr' : 'qxx-hr'"></div>
                    <message-board id="messageBoard" messageType="0" :articleId="articleId"/>
                </div>
            </div>
            <div class="bottom-right">
                <div class="personal-info" :class="article.createBy === '1' ? 'phy-header-photo' : 'qxx-header-photo'">
                    <div class="personal-profile">
                        <div class="profile" v-if="article.createBy === '1'">爱好广泛，喜欢CSGO，喜欢软件开发，更喜欢俺家二狗子</div>
                        <div class="profile" v-if="article.createBy === '2'">我是秦憨憨</div>
                        <div class="blogger-name" :class="article.createBy === '1' ? 'phy-name' : 'qxx-name'">{{article.createBy === '1' ? '裴浩宇' : '秦晓雪'}}</div>
                    </div>
                </div>
                <div class="article-type">
                    <div class="article-type-label">
                        <i class="el-icon-s-grid"></i>
                        <div class="article-type-label-name">文章分类</div>
                    </div>
                    <div class="article-type-one"
                         @click="goToArticleType(articleType)"
                         v-for="articleType in articleTypeList"
                         :key='articleType.id'>
                        <i class="el-icon-star-on"></i>
                        <div class="article-type-name">{{articleType.name}}</div>
                        <div class="article-type-number">（{{articleType.number}}）</div>
                    </div>
                </div>
            </div>
        </div>

    </div>
</template>

<script>
import { getArticleList, getArticleTypeNumber } from '@/api/px/customer/article.js';
import messageBoard from '@/components/MessageBoard/index'
import { scrollAnimation } from '@/assets/js/public.js';


    export default {
        components: {
            messageBoard
        },
        data() {
            let articleId = sessionStorage.getItem('articleId');
            return {
                //遮罩层
                loading: true,
                //文章信息
                article: {},
                //文章ID
                articleId: articleId,
                //文章类型分组列表
                articleTypeList: [],
            }
        },
        mounted () {
            this.getArticleById();
        },
        methods: {
            /**
             * 移到留言位置
             */
            goToMessage() {
                let currentY = document.getElementById("messageBoard").offsetTop;
                scrollAnimation(0, currentY - 100)
            },
            /**
             * 根据ID获取文章
             */
            getArticleById(id) {
                this.loading = true;
                getArticleList({articleId: id || this.articleId}).then(res => {
                    this.article = res.data[0];
                    this.getArticleTypeNumber();
                    this.loading = false;
                })
            },
            /**
             * 跳转文章分类
             */
            goToArticleType(articleType) {
                this.$router.push({
                    name: 'articlelist',
                    params: {
                        code: articleType.code
                        }
                })
            },
            /**
             * 获取文章分类分组数据
             */
            getArticleTypeNumber() {
                getArticleTypeNumber({createBy: this.article.createBy}).then(res => {
                    this.articleTypeList = res.rows;
                })
            }
        },
        destroyed() {
            // sessionStorage.removeItem('articleId');
        }
    }
</script>

<style lang="scss" scoped>

.page{
    padding: 1rem;
    .top{
        display: flex;
        align-items: center;
        ::v-deep .el-breadcrumb__inner{
            cursor: pointer!important;
        }
    }
    .bottom{
        display: flex;
        .bottom-left{
            width: calc(100% - 18rem);
            padding: 1rem 0;
            .article{
                .article-title{
                    text-indent: 2rem;
                    width: 100%;
                    text-align: center;
                    color: #fff;
                    padding: 1rem;
                    font-size: 1.2rem;
                    font-weight: bold;
                    border-radius: 10px;
                }
                .phy-article-title{
                    background-color: #33ccff;
                }
                .qxx-article-title{
                    background-color: #FF399A;
                }
                .article-content{
                    padding: 1rem;
                    overflow-x: hidden;
                    min-height: 10rem;
                }
                .article-info{
                    .margin-right{
                        margin-right: 0.5rem;
                    }
                    display: flex;
                    align-items: center;
                    justify-content: flex-end;
                    padding: 0.5rem;
                    font-size: 0.9rem;
                    .footer-one{
                        display: flex;
                        align-items: center;
                    }
                    i{
                        color: #999;
                    }
                }
                .phy-hr{
                    border-top: 2px solid #33ccff;
                    margin: 1rem 0;
                }
                .qxx-hr{
                    border-top: 2px solid #FF399A;
                    margin: 1rem 0;
                }
                .message{
                    .message-label{
                        font-size: 0.9rem;
                        font-weight: bold;
                    }
                    .leave-message{
                        display: flex;
                        align-items: center;
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
                                .leave-message-content{
                                    width: calc(100% - 5rem);
                                    font-size: 0.9rem;
                                    color: rgb(34, 32, 32);
                                }
                                .floor{
                                    font-size: 1.2rem;
                                    width: 5rem;
                                    display: flex;
                                    justify-content: flex-end;
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
                    box-shadow: 0 0 0.8rem #ccc;
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
                                background-image: url('../../../../../assets/images/mc.jpg');
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

        }
        .bottom-right{
            width: 18rem;
            padding: 1rem;
            display: flex;
            flex-flow: column;
            align-items: center;
            .personal-info{
                cursor: pointer;
                width: 14rem;
                height: 16rem;
                background-size: 100%;
                background-position: center;
                border: 2px solid #eee;
                box-shadow: 2px 2px 2px #ccc;
                border-radius: 5px;
                display: flex;
                align-items: flex-end;
                .personal-profile{
                    display: none;
                    background-color: rgba(255, 255, 255, 0.8);
                    padding: 0.5rem;
                    .profile{
                        font-size: 0.9rem;
                        color: rgb(78, 76, 76);
                    }
                    .blogger-name{
                        font-weight: bold;
                        margin-top: 0.2rem;
                    }
                    .phy-name{
                        color: #33ccff;
                    }
                    .qxx-name{
                        color: #FF399A;
                    }
                }
            }
            .personal-info:hover{
                .personal-profile{
                        display: block;
                    }
            }
            .phy-header-photo{
                background-image: url('../../../../../assets/images/phy.jpg');
            }
            .qxx-header-photo{
                background-image: url('../../../../../assets/images/qxx.jpg');
            }
            .article-type{
                margin-top: 2rem;
                width: 14rem;
                display: flex;
                flex-flow: column;
                .article-type-label{
                    color: #33ccff;
                    font-weight: bold;
                    display: flex;
                    align-items: center;
                    .article-type-label-name{
                        margin-left: 0.5rem;
                    }
                }
                .article-type-one{
                    cursor: pointer;
                    display: flex;
                    margin-top: 0.5rem;
                    align-items: center;
                    i{
                        color: #FF399A;
                    }
                    .article-type-name{
                        margin: 0 0 0 0.5rem;
                        font-size: 0.9rem;
                        color: #999;
                    }
                    .article-type-number{
                        font-size: 0.9rem;
                    }
                }
                .article-type-one:hover{
                    .article-type-name ,.article-type-number{
                        color: #33ccff!important;
                        text-decoration: underline #33ccff;
                    }
                }
            }

        }
    }
}
</style>
