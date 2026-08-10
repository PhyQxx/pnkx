<template>
    <div class="page" v-loading="loading">
        <div class="bottom">
            <div class="bottom-left">
                <div class="article">
                    <div class="article-title"
                         :class="article.createBy === '1' ? 'phy-article-title' : 'qxx-article-title'">
                        {{ article.title }}
                    </div>
                    <div class="article-info">
                        <div class="footer-one">
                            <el-icon><component :is="article.createBy === '1' ? 'Male' : 'Female'" /></el-icon>
                            <div class="author margin-right">{{ article.nickName }}</div>
                        </div>
                        <div class="footer-one">
                            <el-icon><Calendar /></el-icon>
                            <div class="create-time margin-right">{{ article.createTime }}</div>
                        </div>
                        <div class="footer-one pointer theme-blue-text" @click="goToArticleType(article.typeCode)">
                            <el-icon><MagicStick /></el-icon>
                            <div class="type margin-right">{{ article.typeName }}</div>
                        </div>
                        <div class="footer-one">
                            <el-icon><View /></el-icon>
                            <div class="visits-number margin-right">{{ article.visitsNumber }}
                            </div>
                        </div>
                        <div class="footer-one pointer" @click="goToMessage">
                            <el-icon><Present /></el-icon>
                            <div class="message-number margin-right">{{ article.leaveMessageNumber }}枚留言</div>
                        </div>
                        <div class="footer-one pointer theme-blue-text" @click="$copyText(`https://pnkx.top/post/${articleId}`)">
                            <el-icon><Share /></el-icon>
                            <div class="message-number">复制分享链接</div>
                        </div>
                    </div>
                    <CherryMarkdownEditor ref="CherryMarkdown" v-if="!loading" height="fit-content" edit-model="previewOnly" v-model="article.richText"></CherryMarkdownEditor>
                    <div :class="article.createBy === '1' ? 'phy-hr' : 'qxx-hr'"></div>
                    <div class="message-manage-btn">
                        <el-button type="primary" icon="ChatDotRound" size="small" @click="goToMessageManage">评论管理</el-button>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<script>
import {getArticle} from "@/api/px/blog/article";

export default {
    name: 'articledetails',
    data() {
        return {
            //遮罩层
            loading: true,
            //文章信息
            article: {},
            //文章ID
            articleId: this.$route.query.adminArticleId,
            //文章类型分组列表
            articleTypeList: [],
        }
    },
    mounted() {
        this.getArticleById();
    },
    methods: {
        /**
         * 移到留言位置
         */
        goToMessage() {
            let currentY = document.getElementById("messageBoard").offsetTop;
            this.scrollAnimation(0, currentY - 100)
        },
        /**
         * 跳转评论管理
         */
        goToMessageManage() {
            this.$router.push({path: '/blog/article/details/message', query: {articleId: this.articleId}});
        },
        /**
         * 根据ID获取文章
         */
        getArticleById(id) {
            this.loading = true;
            let startReg = new RegExp("<pre.*?<code", "g");
            getArticle(id || this.articleId).then(res => {
                this.article = res.data;
                this.loading = false;
            });
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
    },
}
</script>

<style lang="scss" scoped>

.page {
    padding: var(--space-4);
    background: var(--bg-body);
    min-height: 100%;

    .top {
        display: flex;
        align-items: center;

        ::v-deep .el-breadcrumb__inner {
            cursor: pointer !important;
        }
    }

    .bottom {
        display: flex;

        .bottom-left {
            width: 100%;
            padding: var(--space-4) 0;

            .article {
                .article-title {
                    text-indent: 2rem;
                    width: 100%;
                    text-align: center;
                    color: var(--text-inverse);
                    padding: var(--space-4);
                    font-size: var(--text-4xl);
                    font-weight: var(--font-bold);
                    border-radius: var(--radius-lg);
                }

                .phy-article-title {
                    background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-600) 100%);
                }

                .qxx-article-title {
                    background: linear-gradient(135deg, var(--color-danger) 0%, var(--color-danger-dark) 100%);
                }

                .article-content {
                    padding: var(--space-4);
                    overflow-x: hidden;
                    min-height: 10rem;
                    background: var(--bg-card);
                    border-radius: var(--radius-lg);
                    box-shadow: var(--shadow-sm);

                    ::v-deep img {
                        max-height: 50vh;
                        border-radius: var(--radius-md);
                    }
                }

                .article-info {
                    .margin-right {
                        margin-right: var(--space-2);
                    }

                    display: flex;
                    align-items: center;
                    justify-content: flex-end;
                    padding: var(--space-2);
                    font-size: var(--text-sm);

                    .footer-one {
                        display: flex;
                        align-items: center;
                        line-height: 0;
                    }

                    i {
                        color: var(--text-tertiary);
                        margin-right: 0.2rem;
                    }
                }

                .phy-hr {
                    border-top: 2px solid var(--color-primary);
                    margin: var(--space-4) 0;
                }

                .qxx-hr {
                    border-top: 2px solid var(--color-danger);
                    margin: var(--space-4) 0;
                }

                .message-manage-btn {
                    display: flex;
                    justify-content: flex-end;
                    margin: var(--space-4) 0;
                }

                .message {
                    .message-label {
                        font-size: var(--text-sm);
                        font-weight: var(--font-bold);
                    }

                    .leave-message {
                        display: flex;
                        align-items: flex-start;
                        padding: var(--space-4);

                        .message-left {
                            margin-right: var(--space-4);
                            width: 4rem;
                            display: flex;
                            flex-flow: column;
                            align-items: center;

                            .header-photo {
                                width: 4rem;
                                height: 4rem;
                                border-radius: var(--radius-full);
                                overflow: hidden;
                                display: flex;
                                align-items: center;
                                justify-content: center;

                                img {
                                    width: 100%;
                                    height: 100%;
                                }
                            }

                            .author-name {
                                color: var(--text-secondary);
                                margin-top: var(--space-2);
                                text-align: center;
                            }
                        }

                        .message-right {
                            width: calc(100% - 5rem);
                            background: var(--bg-hover);
                            padding: var(--space-4);
                            border-radius: var(--radius-lg);
                            transition: box-shadow var(--duration-fast) var(--ease-default);

                            &:hover {
                                box-shadow: var(--shadow-sm);
                            }

                            .message-right-top {
                                display: flex;
                                justify-content: space-between;

                                .leave-message-content {
                                    width: calc(100% - 5rem);
                                    font-size: var(--text-sm);
                                    color: var(--text-primary);
                                }

                                .floor {
                                    font-size: var(--text-lg);
                                    width: 5rem;
                                    display: flex;
                                    justify-content: flex-end;
                                }
                            }

                            .leave-message-time {
                                display: flex;
                                justify-content: flex-end;
                                margin-top: var(--space-4);
                                font-size: var(--text-sm);
                                color: var(--text-tertiary);
                            }
                        }

                        .message-right:before {
                            position: absolute;
                            content: "";
                            width: 0;
                            height: 0;
                            border-top: 0.8rem solid transparent;
                            border-right: 1rem solid var(--color-primary-100);
                            border-bottom: 0.8rem solid transparent;
                            margin: -0.1rem 0 0 -2rem;
                        }
                    }
                }
            }

        }
    }
}
</style>
