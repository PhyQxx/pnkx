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

        <div class="middle middle-homepage">
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
                                    <div @click="tagsToPage(item)"  v-for="item in tagsList" class="tags">
                                        <tags :name="item.dictLabel"/>
                                    </div>
                                </div>
                            </div>
                        </el-tab-pane>
                    </el-tabs>
                </div>
                <div class="hot-box-middle">
                    <div class="new-message">
                        <div class="new-message-title">
                            <div class="logo"></div>
                            <div class="new-message-label">最新留言</div>
                        </div>
                        <div class="new-message-list">
                            <div class="new-message-one" @click="changArticle(leaveMessage.articleId)" v-for="leaveMessage in newLeaveMessageList">
                                <div class="message-left">
                                    <div class="header-photo">
                                        <el-image
                                            class="header-picture"
                                            :src="leaveMessage.authorHeader"
                                            fit="scale-down">
                                        </el-image>
                                    </div>
                                </div>
                                <div class="message-right">
                                    <div class="message-right-top">
                                        <div class="leave-message-content" v-html="leaveMessage.content"></div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="hot-box-right">
                    <div class="article-type">
                        <div class="article-type-title">
                            <div class="logo"></div>
                            <div class="article-type-label">文章分类</div>
                        </div>
                        <div class="article-type-list">
                            <div class="article-type-one"
                                 @click="goToArticleType(articleType)"
                                 v-for="articleType in articleTypeList"
                                 :key='articleType.id'>
                                <i class="el-icon-star-on"/>
                                <div class="article-type-name">{{articleType.name}}</div>
                                <div class="article-type-number">（{{articleType.number}}）</div>
                            </div>
                        </div>
                        <el-pagination
                            small
                            layout="prev, pager, next"
                            v-show="articleTypeTotal>9"
                            :page.sync="articleTypeParams.pageNum"
                            :limit.sync="articleTypeParams.pageSize"
                            @pagination="getArticleTypeNumber"
                            :total="articleTypeTotal">
                        </el-pagination>
                    </div>
                </div>
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
        <div class="suspension">
            <div class="author-info">
                <div class="we-chat">微信</div>
                <div class="qq">QQ</div>
            </div>
            <div class="to-top" @click="toTop">
                <span class="to-top-text">返回</span>
                <span class="to-top-text">顶部</span>
            </div>
        </div>
    </div>
</template>

<script>
import { getTimeDifference, scrollAnimation } from '@/assets/js/public.js';
import { listArticleNotContent, getHotArticle, getArticleTypeList, getMessageList, getArticleTypeNumber } from '@/api/px/customer/article';
import Tags from '@/components/Tag/index'

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
                url: require('@/assets/images/px.jpg'),
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
                //最新留言
                newLeaveMessageList: [],
                //文章分类列表
                articleTypeList: [],
                //文章分类条数
                articleTypeTotal: 0,
                //文章分类分页参数
                articleTypeParams: {
                    pageNum: 1,
                    pageSize: 10
                }
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
            this.getMessageList();
            this.getArticleTypeNumber();
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
             * 返回顶部
             */
            toTop() {
                const currentY = document.documentElement.scrollTop || document.body.scrollTop;
                scrollAnimation(currentY, 0);
            },
            /**
             * 跳转文章分类
             */
            goToArticleType(articleType) {
                if (this.$route.path === '/articlelist') {
                    this.$children[2].queryParams.type = articleType.code;
                    this.$children[2].getList();
                    window.scrollTo(0,0);
                } else {
                    this.$router.push({
                        name: 'articlelist',
                        params: {
                            code: articleType.code
                        }
                    });
                }
            },
            /**
             * 获取文章分类
             */
            getArticleTypeNumber() {
                getArticleTypeNumber(this.articleTypeParams).then(res => {
                    this.articleTypeList = res.rows;
                    this.articleTypeTotal = res.total;
                })
            },
            /**
             * 获取最新留言
             */
            getMessageList() {
                getMessageList({
                    pageNum: 1,
                    pageSize: 5,
                }).then(res => {
                    this.newLeaveMessageList = res.rows;
                })
            },
            /**
             * 随机标签跳转
             */
            tagsToPage(item) {

                if (this.$route.path === '/articlelist' && item.type === 'album') {
                    this.$router.push({
                        name: 'photo',
                        params: {
                            type: item.dictValue
                        }
                    })
                } else if (this.$route.path === '/articlelist' && item.type === 'article') {
                    this.$children[2].queryParams.type = item.dictValue;
                    this.$children[2].getList();
                    window.scrollTo(0,0);
                } else if (this.$route.path === '/photo' && item.type === 'article') {
                    this.$router.push({
                        name: 'articlelist',
                        params: {
                            code: item.dictValue
                        }
                    })
                } else if (this.$route.path === '/photo' && item.type === 'album') {
                    this.$children[2].$children[2].queryParams.articleId = item.dictValue;
                    this.$children[2].$children[2].getLeaveMessage();
                    window.scrollTo(0,0);
                } else {
                    if (item.type === 'article') {
                        this.$router.push({
                            name: 'articlelist',
                            params: {
                                code: item.dictValue
                            }
                        })
                    } else if (item.type === 'album') {
                        this.$router.push({
                            name: 'photo',
                            params: {
                                type: item.dictValue
                            }
                        })
                    }
                }
            },
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
                                dictValue: item.dictValue,
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
                listArticleNotContent({pageNum: Math.ceil(Math.random()*100), pageSize: 10,}).then(response => {
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
                    window.scrollTo(0,0);
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
                listArticleNotContent({pageNum: 1, pageSize: 10,}).then(response => {
                    this.articleList = response.rows;
                    this.loading = false;
                });
            },
            /**
             * 搜索方法
             */
            remoteMethod(value) {
                this.loading = true;
                listArticleNotContent({title: value}).then(response => {
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
            min-height: 14rem;
            display: flex;
            .hot-box-left{
                width: 33%;
                padding: 1rem 2rem 1rem 1rem;
                .hot-tabs{
                    display: flex;
                    align-items: center;
                    height: 100%;
                    ::v-deep .el-tabs__header{
                        height: 10rem;
                    }
                    ::v-deep .el-tabs__content{
                        width: 100%;
                        height: 100%;
                        .el-tab-pane{
                            height: 100%;
                        }
                    }
                    .hot-content, .random-content{
                        min-height: 14rem;
                        display: flex;
                        flex-flow: column;
                        justify-content: space-between;
                        padding: 0.5rem;
                        .one-article{
                            display: flex;
                            align-items: center;
                            padding: 0 1rem;
                            margin: 0.5rem 0;
                            border: 1px solid rgba(0, 255, 255, 0.6);
                            border-radius: 5px;
                            cursor: pointer;
                            i{
                                margin-right: 0.5rem;
                                color: #FF399A;
                            }
                            .text{
                                color: #3399FF;
                                white-space: nowrap;
                                overflow: hidden;
                                text-overflow: ellipsis;
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
                        height: 100%;
                        .random-tags{
                            display: flex;
                            flex-flow: wrap;
                        }
                    }
                }
            }
            .hot-box-middle{
                width: 33%;
                padding: 0 1rem;
                .new-message{
                    .new-message-title{
                        display: flex;
                        align-items: center;
                        .logo{
                            background-image: url('../../../../assets/images/new-message.png');
                            width: 2rem;
                            height: 2rem;
                            background-size: 100% 100%;
                            background-position: center;
                            margin-right: 1rem;
                        }
                        .new-message-label{
                            font-size: 1.2rem;
                            font-weight: bold;
                            color: #00afff;
                        }
                    }
                    .new-message-list{
                        .new-message-one{
                            display: flex;
                            align-items: center;
                            padding: 1rem 1rem 1rem 0;
                            cursor: pointer;
                            .message-left{
                                margin-right: 1rem;
                                width: 4rem;
                                display: flex;
                                flex-flow: column;
                                align-items: center;
                                .header-photo{
                                    width: 3rem;
                                    height: 3rem;
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

                }
            }
            .hot-box-right{
                width: 33%;
                padding-left: 1rem;
                .article-type{
                    .article-type-title{
                        display: flex;
                        align-items: center;
                        .logo{
                            background-image: url('../../../../assets/images/article-type.png');
                            width: 2rem;
                            height: 2rem;
                            background-size: 100% 100%;
                            background-position: center;
                            margin-right: 1rem;
                        }
                        .article-type-label{
                            font-size: 1.2rem;
                            font-weight: bold;
                            color: #00afff;
                        }
                    }
                    .article-type-list{
                        padding: 1rem;
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
                .el-pagination{
                    display: flex;
                    justify-content: flex-end;
                }
            }
        }
    }
    .middle-homepage{
        min-height: 40rem;
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
    .suspension{
        position: fixed;
        bottom: 10%;
        right: calc(20% - 6rem);
        .we-chat, .qq{
            display: none;
            font-size: 1.2rem;
            font-weight: bold;
            padding: 0.2rem 0 0 6rem;
            position: fixed;
            width: 10rem;
            height: 10rem;
            background-repeat: no-repeat;
            background-size: 100% 100%;
            background-position: center;
            cursor: pointer;
            border-radius: 1rem;
            bottom: calc(10% + 8rem);
        }
        .we-chat{
            color: #FFFFFF;
            background-image: url('../../../../assets/images/we-chat.jpg');
            right: calc(20% - 16rem);
        }
        .qq{
            color: #555555;
            background-image: url('../../../../assets/images/qq-info.jpg');
            right: calc(20% - 26.2rem);
        }
        .author-info, .to-top{
            width: 4rem;
            height: 4rem;
            background-repeat: no-repeat;
            background-size: 78% 78%;
            background-position: center;
            border: 2px solid #FFFFFF;
            border-radius: 50%;
            background-color: #bfe7fa;
            cursor: pointer;
        }
        .author-info{
            background-image: url('../../../../assets/images/author-info.png');
        }
        .author-info:hover{
            .we-chat, .qq{
                display: flex;
            }
        }
        .to-top{
            margin-top: 0.2rem;
            background-image: url('../../../../assets/images/to-top.png');
            display: flex;
            justify-content: center;
            align-items: center;
            padding: 1rem;
            .to-top-text{
                color: #FFFFFF;
                display: none;
            }
        }
        .to-top:hover{
            background-color: #FFFFFF!important;
            .to-top-text{
                color: #bfe7fa!important;
                display: flex;
            }
        }
    }
}
</style>
