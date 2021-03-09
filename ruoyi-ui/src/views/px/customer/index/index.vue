<template>
    <div class="index-page">
        <div class="nav-box">
            <div class="nav theme-pink">
            <div class="nav-one pointer" :class="nav.isSelected ? 'nav-selected' : ''"
                                                v-for="nav in navList"
                                                :key="nav.name"
                                                @click="selectNav(nav)">{{nav.name}}</div>
            <div class="search">
                <el-input
                    placeholder="搜一搜"
                    suffix-icon="el-icon-search"
                    v-model="keyCode"
                    size="mini">
                </el-input>
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
            <router-view></router-view>
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
    export default {
        components: {
        },
        data() {
            return {
                //搜索关键字
                keyCode: '',
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
            }
        },
        methods: {
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
        padding: 1rem 0 4rem 0;
        background-image: url("../../../../assets/images/conent-bg.png");
        background-repeat: repeat;
        background-size: 100%;
        min-height: 30rem;
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
