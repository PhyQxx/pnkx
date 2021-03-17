<template>
    <div class="page">
        <div class="phy-box" v-loading="phyLoading">
            <div class="phy" v-if="phyArticleList.length < 1">
                <div class="no-data">暂无内容</div>
            </div>
            <div class="phy" v-if="phyArticleList.length > 0">
                <div class="first">
                    <div class="cursor-pointer" @click="goToArticlePage(phyArticleList[0])">
                        <div class="first-title">{{phyArticleList[0].title}}</div>
                        <div class="first-content" v-html="phyArticleList[0].richText"></div>
                    </div>
                    <div class="first-footer">
                        <div class="footer-one">
                            <i class="el-icon-male"></i>
                            <div class="author margin-right">{{phyArticleList[0].nickName}}</div>
                        </div>
                        <div class="footer-one">
                            <i class="el-icon-date"></i>
                            <div class="create-time margin-right">{{phyArticleList[0].createTime}}</div>
                        </div>
                        <div class="footer-one">
                            <i class="el-icon-magic-stick"></i>
                            <div class="type margin-right">{{phyArticleList[0].typeName}}</div>
                        </div>
                        <div class="footer-one">
                            <i class="el-icon-present"></i>
                            <div class="message-number">{{phyArticleList[0].leaveMessageNumber || 0}}枚留言</div>
                        </div>
                    </div>
                </div>
                <el-collapse v-model="activeNames" @change="handleChange">
                    <el-collapse-item class="article" :name="article.id"  v-for="article in phyArticleList.slice(1)" :key="article.id">
                        <template slot="title">
                            <i class="el-icon-male"/>
                            <div class="title cursor-pointer">{{article.title}}</div>
                        </template>
                        <div class="article-text">
                            <div class="article-text-box" v-html="article.richText"></div>
                        </div>
                        <div class="content-more"><el-button size="mini" type="primary" @click="goToArticlePage(article)">详情</el-button></div>
                    </el-collapse-item>
                </el-collapse>
                <pagination
                    class="pagination"
                    v-show="phyTotal>0"
                    :total="phyTotal"
                    :page.sync="phyParams.pageNum"
                    :limit.sync="phyParams.pageSize"
                    layout="total, prev, pager, next"
                    @pagination="getPhyList"
                />
            </div>
        </div>
        <div class="qxx-box" v-loading="qxxLoading">
            <div class="qxx" v-if="qxxArticleList.length < 1">
                <div class="no-data">暂无内容</div>
            </div>
            <div class="qxx" v-if="qxxArticleList.length > 0">
                <div class="first">
                    <div class="cursor-pointer"  @click="goToArticlePage(qxxArticleList[0])">
                        <div class="first-title cursor-pointer">{{qxxArticleList[0].title}}</div>
                        <div class="first-content" v-html="qxxArticleList[0].richText"></div>
                    </div>
                    <div class="first-footer">
                        <div class="footer-one">
                            <i class="el-icon-female"></i>
                            <div class="author margin-right">{{qxxArticleList[0].nickName}}</div>
                        </div>
                        <div class="footer-one">
                            <i class="el-icon-date"></i>
                            <div class="create-time margin-right">{{qxxArticleList[0].createTime}}</div>
                        </div>
                        <div class="footer-one">
                            <i class="el-icon-magic-stick"></i>
                            <div class="type margin-right">{{qxxArticleList[0].typeName}}</div>
                        </div>
                        <div class="footer-one">
                            <i class="el-icon-present"></i>
                            <div class="message-number">{{qxxArticleList[0].leaveMessageNumber || 0}}枚留言</div>
                        </div>
                    </div>
                </div>
                <el-collapse v-model="activeNames" @change="handleChange">
                    <el-collapse-item class="article" :name="article.id"  v-for="article in qxxArticleList.slice(1)" :key="article.id">
                        <template slot="title">
                            <i class="el-icon-male"/>
                            <div class="title cursor-pointer">{{article.title}}</div>
                        </template>
                        <div class="article-text">
                            <div class="article-text-box" v-html="article.richText"></div>
                        </div>
                        <div class="content-more"><el-button size="mini" type="primary" @click="goToArticlePage(article)">详情</el-button></div>
                    </el-collapse-item>
                </el-collapse>
                <pagination
                    class="pagination"
                    v-show="qxxTotal>0"
                    :total="qxxTotal"
                    :page.sync="qxxParams.pageNum"
                    :limit.sync="qxxParams.pageSize"
                    layout="total, prev, pager, next"
                    @pagination="getQxxList"
                />
            </div>
        </div>
    </div>
</template>

<script>
import { listArticle } from '@/api/px/customer/article.js';
    export default {
        data() {
            return {
                //当前打开的页面
                activeNames: [],
                // 总条数
                phyTotal: 0,
                qxxTotal: 0,
                // 查询参数
                phyParams: {
                    pageNum: 1,
                    pageSize: 10,
                    createBy: '1',
                },
                qxxParams: {
                    pageNum: 1,
                    pageSize: 10,
                    createBy: '2',
                },
                //遮罩层
                phyLoading: true,
                qxxLoading: true,
                //phy文章列表
                phyArticleList: [],
                //qxx文章列表
                qxxArticleList: [],
            }
        },
        mounted () {
            this.getPhyList();
            this.getQxxList();
        },
        methods: {
            /**
             * 选择文章展开内容
             */
            handleChange(val) {
                console.log(val);
            },
            /**
             * 跳转文章详情
             */
            goToArticlePage(article) {
                sessionStorage.setItem('articleId', article.id);
                this.$router.push({name: 'article'});
            },
            /**
             * 获取文章列表
             */
            getPhyList() {
                this.phyLoading = true;
                listArticle(this.phyParams).then(response => {
                    this.phyArticleList = response.rows;
                    this.phyTotal = response.total;
                    this.phyLoading = false;
                });
            },
            getQxxList() {
                this.qxxLoading = true;
                listArticle(this.qxxParams).then(response => {
                    this.qxxArticleList = response.rows;
                    this.qxxTotal = response.total;
                    this.qxxLoading = false;
                });
            },
        },
    }
</script>

<style lang="scss" scoped>
.margin-right{
    margin-right: 0.5rem;
}
.article-text-box{
    pointer-events: none;
}
::v-deep .el-collapse-item__content{
    padding-bottom: 1rem;
}
.page{
    display: flex;
    .phy-box{
        width: 50%;
    }
    .qxx-box{
        width: 50%;
    }
    .no-data{
        text-align: center;
        padding: 2rem 1rem;
    }
    .title{
        white-space: nowrap;
        width: 20rem;
        overflow: hidden;
        text-overflow: ellipsis;
    }
    .first{
        display: flex;
        flex-flow: column;
        height: 16rem;
        margin: 1rem 0;
        justify-content: space-between;
        border-radius: 0.5rem;
        .first-content{
            padding: 1rem;
            font-size: 0.9rem;
            height: 8.4rem;
            overflow: hidden;
            display: -webkit-box;
            -webkit-box-orient: vertical;
            -webkit-line-clamp: 6;
        }
        .first-footer{
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
    }
    .phy{
        flex: 1;
        padding: 1rem 1rem 1rem 3rem;
        .no-data{
            color: #33ccff;
        }
        .first{
            border: 2px solid #33ccff;
            .first-title{
                padding: 1rem;
                font-size: 1.2rem;
                border-bottom: 1px dashed #999;
                color: #33ccff;
                font-weight: bold;
            }
            .first-title:hover{
                animation: phyfirsttitle 1s;
                animation-fill-mode: forwards;
            }
            @keyframes phyfirsttitle
            {
                from {
                    color: #3399FF;
                    font-size: 1.2rem;
                }
                to {
                    color: #3399FF;
                    font-size: 1.2rem;
                    padding-left: 2rem;
                    text-shadow: 0.15em 0.15em 0.15em #6ED3FF
                }
            }
        }
        .article{
            .article-text{
                border-radius: 5px;
                border: 1px solid #33ccff;
                padding: 1rem;
                margin-bottom: 1rem;
                .article-text-box{
                    height: 7rem;
                    overflow: hidden;
                    display: -webkit-box;
                    -webkit-box-orient: vertical;
                    -webkit-line-clamp: 4;
                }
            }
            i{
                color: #33ccff;
                margin-right: 0.5rem;
            }
            .title{
                color: #3399FF;
                font-size: 1.2rem;
            }
            .title:hover{
                animation: phytitle 1s;
                animation-fill-mode: forwards;
            }
            @keyframes phytitle
            {
                from {
                    color: #3399FF;
                    font-size: 1.2rem;
                }
                to {
                    color: #3399FF;
                    font-size: 1.2rem;
                    padding-left: 2rem;
                    text-shadow: 0.15em 0.15em 0.15em #6ED3FF
                }
            }
            .content-more{
                display: flex;
                justify-content: flex-end;
            }
        }
    }
    .qxx{
        flex: 1;
        padding: 1rem 3rem 1rem 1rem;
        .no-data{
            color: #FF399A;
        }
        .first{
            border: 2px solid #FF399A;
            .first-title{
                padding: 1rem;
                font-size: 1.2rem;
                border-bottom: 1px dashed #999;
                color: #FF399A;
                font-weight: bold;
            }
            .first-title:hover{
                animation: qxxfirsttitle 1s;
                animation-fill-mode: forwards;
            }
            @keyframes qxxfirsttitle
            {
                from {
                    color: #FF399A;
                    font-size: 1.2rem;
                }
                to {
                    color: #FF399A;
                    font-size: 1.2rem;
                    padding-left: 2rem;
                    text-shadow: 0.15em 0.15em 0.15em #FF47A1
                }
            }
        }
        .article{
            .article-text{
                border-radius: 5px;
                border: 1px solid #FF47A1;
                padding: 1rem;
                margin-bottom: 1rem;
                .article-text-box{
                    height: 7rem;
                    overflow: hidden;
                    display: -webkit-box;
                    -webkit-box-orient: vertical;
                    -webkit-line-clamp: 4;
                }
            }
            i{
                color: #FF47A1;
                margin-right: 0.5rem;
            }
            .title{
                color: #FF399A;
                font-size: 1.2rem;
            }
            .title:hover{
                animation: qxxtitle 1s;
                animation-fill-mode: forwards;
            }
            @keyframes qxxtitle
            {
                from {
                    color: #FF399A;
                    font-size: 1.2rem;
                }
                to {
                    color: #FF399A;
                    font-size: 1.2rem;
                    padding-left: 2rem;
                    text-shadow: 0.15em 0.15em 0.15em #FF399A
                }
            }
            .content-more{
                display: flex;
                justify-content: flex-end;
            }
        }
    }
}
</style>
