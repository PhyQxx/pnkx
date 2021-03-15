<template>
    <div class="index-page">
        <div class="nav-box">
            <div class="nav theme-pink">
            <div class="nav-one pointer" :class="nav.isSelected ? 'nav-selected' : ''"
                                                v-for="nav in navList"
                                                :key="nav.name"
                                                @click="selectNav(nav)">{{nav.name}}</div>
            <div class="search">
                <el-select
                    v-model="keyCode"
                    size="mini"
                    filterable
                    remote
                    placeholder="搜一搜"
                    :remote-method="remoteMethod"
                    @change="changArticle(keyCode)"
                    @focus="initArticleList"
                    :loading="loading">
                    <el-option
                        v-for="item in articleList"
                        :key="item.id"
                        :label="item.title"
                        :value="item.id">
                    </el-option>
                </el-select>
            </div>
        </div>
        </div>
        <div class="top">
            <el-image :src="url">
            </el-image>
            <div class="love-time">
                <div class="label">我们相恋：</div>
                <div class="time">{{loveTime}}</div>
            </div>
        </div>

        <div class="middle">
            <router-view ref="routerView"/>
        </div>
        <div class="middle">
            <div class="hot-box">
                <div class="hot-box-left">
                    <el-tabs class="hot-tabs" tab-position="left">
                        <el-tab-pane label="最热">
                            <div class="hot-content">
                                <div class="one-article" @click="changArticle(item.id)" v-for="item in hotArticleList" :key="item.id">
                                    <i class="el-icon-wind-power"/>
                                    <div class="text">{{item.title}}</div>
                                </div>
                            </div>
                        </el-tab-pane>
                        <el-tab-pane label="随机">
                            <div class="random-content">
                                <div class="one-article" @click="changArticle(item.id)" v-for="item in randomArticleList" :key="item.id">
                                    <i class="el-icon-wind-power"/>
                                    <div class="text">{{item.title}}</div>
                                </div>
                            </div>
                        </el-tab-pane>
                        <el-tab-pane label="标签">
                            <div class="tags-content">
                                <div class="random-tags">
                                    <tags :name="item.dictLabel" v-for="item in tagsList"/>
                                </div>
                            </div>
                        </el-tab-pane>
                    </el-tabs>
                </div>
                <div class="hot-box-right"></div>
            </div>
        </div>
        <div class="footer">
            <div class="text">
                {{loveDeclaration}}
            </div>
            <div class="page-info">
                <div class="copyright">{{pageInfo.copyright}}</div>
                <div class="name">{{pageInfo.name}}</div>
                <div class="frame">{{pageInfo.frame}}</div>
                <div class="by">by</div>
                <div class="author">{{pageInfo.author}}</div>
            </div>
        </div>
    </div>
</template>

<script>
import { getTimeDifference } from '@/assets/js/public.js';
import { listArticle, getHotArticle } from '@/api/px/customer/article';
import Tags from '@/components/Tag/index'
import { getArticleTypeList } from '@/api/px/customer/article';

    export default {
        components: {
            Tags
        },
        data() {
            return {
                //搜索关键字
                keyCode: '',
                //远程搜索加载
                loading: true,
                //文章列表
                articleList: [],
                //导航栏功能
                navList: [
                    {name: '首页', isSelected: true, path: 'homepage'},
                    {name: '文章', isSelected: false, path: 'articlelist'},
                    {name: '相册', isSelected: false, path: 'album'},
                    {name: '留言板', isSelected: false, path: 'messageboard'}
                ],
                //相恋时间
                loveTime: '1314天1小时3分钟14秒',
                //图片地址
                url: require('@/assets/images/px.png'),
                //爱情宣言
                loveDeclaration: '陪伴是最长情的告白',
                //页面信息
                pageInfo: {
                    copyright: `Copyright©2016-${(new Date()).getFullYear()}`,
                    name: '《pei你看雪》',
                    frame: 'Vue',
                    author: '裴浩宇'
                },
                //最热文章
                hotArticleList: [],
                //随机文章
                randomArticleList: [],
                //随机标签
                tagsList: [],
            }
        },
        mounted () {
            /**
             * 更新时间
             */
            setInterval(() => {
                this.loveTime = getTimeDifference('2016-09-30 22:22:22');
            }, 1000);
            this.getHotArticle();
            this.getRandomArticle();
            this.getRandomTags();
        },
        watch: {
            $route: {
                handler(val, oldVal) {
                    switch (val.name) {
                        case 'homepage':
                            this.navList[0].isSelected = true;
                            this.navList[1].isSelected = false;
                            this.navList[2].isSelected = false;
                            this.navList[3].isSelected = false;
                            break;
                        case 'articlelist':
                            this.navList[0].isSelected = false;
                            this.navList[1].isSelected = true;
                            this.navList[2].isSelected = false;
                            this.navList[3].isSelected = false;
                            break;
                        case 'album':
                            this.navList[0].isSelected = false;
                            this.navList[1].isSelected = false;
                            this.navList[2].isSelected = true;
                            this.navList[3].isSelected = false;
                            break;
                        case 'messageboard':
                            this.navList[0].isSelected = false;
                            this.navList[1].isSelected = false;
                            this.navList[2].isSelected = false;
                            this.navList[3].isSelected = true;
                            break;
                    }
                },
                deep: true
            },
            tagsList(list) {
                if (list.length < 20) {
                    this.tagsList = list;
                } else {
                    this.tagsList = list.slice((Math.random()*(list.length-21)), 20)
                }
            }
        },
        methods: {
            /**
             * 获取随机标签
             */
            getRandomTags() {
                getArticleTypeList({dictType: 'px_article_type'}).then(res => {
                    if (res.data) {
                        res.data.forEach(item => {
                            this.tagsList.push({
                                type: 'article',
                                dictLabel: item.dictLabel,
                                dictValue: item.dictLabel,
                            })
                        })
                    }
                });
                getArticleTypeList({dictType: 'px_album_name'}).then(res => {
                    if (res.data) {
                        res.data.forEach(item => {
                            this.tagsList.push({
                                type: 'album',
                                dictLabel: item.dictLabel,
                                dictValue: item.dictValue,
                            })
                        })
                    }
                });

            },
            /**
             * 获取随机文章
             */
            getRandomArticle() {
                listArticle({pageNum: Math.ceil(Math.random()*100), pageSize: 5,}).then(response => {
                    this.randomArticleList = response.rows;
                });
            },
            /**
             * 获取最热文章
             */
            getHotArticle() {
                getHotArticle().then(res => {
                    this.hotArticleList = res.data;
                })
            },
            /**
             * 选中文章
             */
            changArticle(id) {
                if (this.$route.path === '/article') {
                    sessionStorage.setItem('articleId', id);
                    this.$refs.routerView.getArticleById(id);
                } else {
                    sessionStorage.setItem('articleId', id);
                    this.$router.push({name: 'article'});
                }
            },
            /**
             * 初始化搜索内容
             */
            initArticleList() {
                this.loading = true;
                listArticle({pageNum: 1, pageSize: 10,}).then(response => {
                    this.articleList = response.rows;
                    this.loading = false;
                });
            },
            /**
             * 搜索方法
             */
            remoteMethod(value) {
                this.loading = true;
                listArticle({title: value}).then(response => {
                    this.articleList = response.rows;
                    this.loading = false;
                });
            },
            /**
             * 选择导航
             */
            selectNav(nav) {
                this.navList.forEach(element => {
                    element.isSelected = element.name === nav.name;
                });
                this.$router.push({name: nav.path})
            },
        },
    }
</script>

<style lang="scss" scoped>
    ::v-deep .el-input--medium .el-input__inner{
        color: #00afff!important;
    }
    ::v-deep .el-select{
        color: #00afff!important;
    }
.index-page{
    background-color: #bfe7fa!important;
    .nav-box{
        background-color: #bfe7fa;
        position: sticky;
        top: 0;
        z-index: 9;
    }
    .nav{
        background:  url("../../../../assets/images/nav-bg.png") no-repeat;
        background-size: 100% 100%;
        display: flex;
        justify-content: space-around;
        align-items: center;
        margin: 0 20%;
        height: 5rem;
        padding: 0 4rem 2rem 2rem;
        .nav-one{
            height: 100%;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 1.4rem;
            font-family: serif;
            padding: 0 2rem;
            color: #ff9fe5;
        }
        .nav-one:hover{
            background-color: #e6f6ff;
            color: #ff0080;
        }
        .nav-selected{
            background-color: #e6f6ff;
        }
        .search{
            width: 16rem;
            margin-left: 4rem;
        }

    }
    .top{
        margin: 0 20%;
        background-image: url("../../../../assets/images/header-bg.jpg");
        background-repeat: no-repeat;
        background-size: 100% 50%;
        background-position: bottom;
        height: 16rem;
        display: flex;
        flex-flow: column;
        align-items: center;
        justify-content: space-between;
        padding: 1rem;
        .el-image{
            width: 16rem;
            height: 12rem;
            border-radius: 10px;
            opacity:0.4;
        }
        .love-time{
            display: flex;
            .label{
                color: #FFB5FF;
            }
            .time{
                color: #6ED3FF;
                text-shadow: 0.15em 0.15em 0.15em #6ED3FF
            }
        }
    }
    .middle{
        margin: 0 20%;
        padding: 1rem 0;
        background-image: url("../../../../assets/images/conent-bg.png");
        background-repeat: repeat;
        background-size: 100%;
        .hot-box{
            min-height: 10rem;
            .hot-box-left{
                width: 50%;
                .hot-tabs{
                    .hot-content, .random-content{
                        height: 10rem;
                        display: flex;
                        flex-flow: column;
                        justify-content: space-between;
                        padding: 0.5rem;
                        .one-article{
                            display: flex;
                            align-items: center;
                            padding: 0 1rem;
                            width: 20rem;
                            border: 1px solid rgba(0, 255, 255, 0.4);
                            border-radius: 5px;
                            cursor: pointer;
                            i{
                                margin-right: 0.5rem;
                                color: #FF399A;
                            }
                            .text{
                                color: #3399FF;
                            }
                        }
                        .one-article:hover{
                            background-color: rgba(255, 57, 154, 0.8);
                            i{
                                color: #FFF;
                            }
                            .text{
                                color: #FFF;
                            }
                        }
                    }
                    .tags-content{
                        display: flex;
                        flex-flow: column;
                        justify-content: center;
                        align-items: center;
                        height: 10rem;
                        .random-tags{
                            display: flex;
                            flex-flow: wrap;
                        }
                    }
                }
            }
            .hot-box-right{
                width: 50%;

            }
        }
    }
    .footer{
        margin: 0 20%;
        background-image: url("../../../../assets/images/footer-bg.jpg");
        background-repeat: no-repeat;
        background-size: 100% 50%;
        background-position: top;
        height: 10rem;
        display: flex;
        flex-flow: column;
        align-items: center;
        .text{
            font-size: 0.9rem;
            color: #999;
        }
        .page-info{
            margin-top: 0.5rem;
            font-size: 0.9rem;
            display: flex;
            .name{
                margin: 0 1rem;
                color: #BFE7FA;
            }
            .frame{
                color: #4FC08D;
            }
            .by{
                margin: 0 0.2rem;
            }
        }
    }
}
</style>
