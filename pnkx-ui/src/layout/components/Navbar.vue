<template>
    <div class="navbar">
        <hamburger id="hamburger-container" :is-active="sidebar.opened" class="hamburger-container"
                   @toggleClick="toggleSideBar"/>

        <breadcrumb id="breadcrumb-container" class="breadcrumb-container"/>

        <div class="right-menu">
            <template v-if="device!=='mobile'">
                <search id="header-search" class="right-menu-item navbar-action"/>
                <div class="notice textBox navbar-action">
                    <transition name="slide" v-if="noticeArr.length > 0">
                        <el-tooltip class="item" effect="dark" :content="notice.title" placement="bottom">
                            <p class="text" :key="notice.number" @click="toNoticeDetails(notice)">
                                <el-tag :type="notice.typeValue">{{ notice.typeLabel }}</el-tag>
                                <span class="notice-content">{{ notice.title }}</span>
                            </p>
                        </el-tooltip>
                    </transition>
                    <span class="no-notice" v-else>暂无最新通知公告</span>
                </div>
            </template>

            <reminder-bell/>

            <el-tooltip :content="themeMode === 'dark' ? '切换浅色' : '切换深色'" placement="bottom">
                <button
                    class="theme-toggle navbar-action"
                    type="button"
                    :aria-label="themeMode === 'dark' ? '切换浅色' : '切换深色'"
                    @click="toggleTheme"
                >
                    <el-icon>
                        <Sunny v-if="themeMode === 'dark'"/>
                        <Moon v-else/>
                    </el-icon>
                </button>
            </el-tooltip>

            <el-dropdown class="avatar-container right-menu-item hover-effect navbar-action" trigger="click">
                <div class="avatar-wrapper">
                    <img :src="avatar" class="user-avatar">
                    <i class="el-icon-caret-bottom"/>
                </div>
                <template #dropdown>
                    <el-dropdown-menu>
                        <router-link to="/user/profile">
                            <el-dropdown-item>个人中心</el-dropdown-item>
                        </router-link>
                        <el-dropdown-item @click="openSettings">
                            <span>布局设置</span>
                        </el-dropdown-item>
                        <el-dropdown-item divided @click="logout">
                            <span>退出登录</span>
                        </el-dropdown-item>
                    </el-dropdown-menu>
                </template>
            </el-dropdown>
        </div>
    </div>
</template>

<script>
import {mapGetters} from 'vuex'
import {mapState} from 'vuex'
import Breadcrumb from '@/components/Breadcrumb'
import Hamburger from '@/components/Hamburger'
import Screenfull from '@/components/Screenfull'
import SizeSelect from '@/components/SizeSelect'
import Search from '@/components/HeaderSearch'
import PnkxGit from '@/components/Pnkx/Git'
import PnkxDoc from '@/components/Pnkx/Doc'
import ReminderBell from '@/components/ReminderBell'
import {getUserProfile} from "@/api/system/user";
import {getDicts} from "@/api/system/dict/data";
import {getUnreadNoticeList} from "@/api/system/notice";


export default {
    components: {
        Breadcrumb,
        Hamburger,
        Screenfull,
        SizeSelect,
        Search,
        PnkxGit,
        PnkxDoc,
        ReminderBell
    },
    computed: {
        ...mapGetters([
            'sidebar',
            'avatar',
            'device'
        ]),
        ...mapState({
            themeMode: state => state.app.theme
        }),
        setting: {
            get() {
                return this.$store.state.settings.showSettings
            },
            set(val) {
                this.$store.dispatch('settings/changeSetting', {
                    key: 'showSettings',
                    value: val
                })
            }
        },
        notice() {
            return {
                number: this.number,
                id: this.noticeArr && this.noticeArr[this.number].noticeId,
                typeValue: this.dictValueToTagType(this.noticeArr[this.number].noticeType),
                typeLabel: this.getDictOne(this.noticeArr[this.number].noticeType, this.typeOptions),
                title: this.noticeArr[this.number].noticeTitle,
            }
        }
    },
    data() {
        return {
            noticeArr: [],
            number: 0,
            //通知类型字典项
            typeOptions: []
        }
    },
    mounted() {
        this.getDictList();
        this.getUnreadNoticeList();
        this.startMove();
    },
    methods: {
        /**
         * 跳转到通知详情页面
         */
        toNoticeDetails(row) {
            if (this.$route.query.noticeId !== row.id.toString()) {
                this.$router.push({
                    path: '/notice/noticedetail',
                    query: {
                        noticeId: row.id
                    }
                })
            }
        },
        /**
         * 获取未读通知公告
         */
        getUnreadNoticeList() {
            getUnreadNoticeList().then(res => {
                this.noticeArr = res;
            })
        },
        /**
         * 通知类型返回标签类型
         */
        dictValueToTagType(value) {
            let tagType = '';
            if (value === '1') {
                tagType = ''
            } else if (value === '2') {
                tagType = 'success'
            } else if (value === '3') {
                tagType = 'info'
            } else if (value === '4') {
                tagType = 'warning'
            } else if (value === '5') {
                tagType = 'danger'
            }
        },
        /**
         * 字典项翻译
         */
        getDictOne(value, list) {
            let label = '';
            try {
                list.forEach(item => {
                    if (item.dictValue === value) {
                        label = item.dictLabel
                    }
                });
            } catch (e) {
                console.error('字典项翻译异常：' + e)
            }
            return label
        },
        /**
         * 获取字典项公告类型
         */
        getDictList() {
            getDicts('sys_notice_type').then(res => {
                this.typeOptions = res.data;
            })
        },
        /**
         * 公告滚动
         */
        startMove() {
            let timer = setTimeout(() => {
                if (this.number === this.noticeArr.length - 1) {
                    this.number = 0;
                } else {
                    this.number += 1;
                }
                this.startMove();
            }, 5000); // 滚动不需要停顿则将2000改成动画持续时间
        },
        toggleSideBar() {
            this.$store.dispatch('app/toggleSideBar')
        },
        toggleTheme() {
            this.$store.dispatch('app/toggleTheme')
        },
        openSettings() {
            window.setTimeout(() => {
                this.setting = true
            }, 0)
        },
        async logout() {
            this.$confirm('确定注销并退出系统吗？', '提示', {
                confirmButtonText: '确定',
                cancelButtonText: '取消',
                type: 'warning'
            }).then(() => {
                this.$store.dispatch('LogOut').then(() => {
                    location.href = '/homepage';
                })
            })
        }
    }
}
</script>

<style lang="scss" scoped>
.navbar {
    height: 52px;
    overflow: hidden;
    position: relative;
    background: var(--pnkx-surface);
    border-bottom: 1px solid var(--pnkx-border);
    box-shadow: none;

    .hamburger-container {
        line-height: 52px;
        height: 100%;
        float: left;
        cursor: pointer;
        transition: background .3s;
        -webkit-tap-highlight-color: transparent;

        &:hover {
            background: var(--pnkx-surface-muted)
        }
    }

    .breadcrumb-container {
        float: left;
    }

    .errLog-container {
        display: inline-block;
        vertical-align: top;
    }

    .right-menu {
        float: right;
        height: 100%;
        line-height: 52px;
        display: flex;
        justify-content: center;
        align-items: center;

        #header-search {
            margin-right: 2rem;
        }

        .notice {
            width: 24rem !important;
            cursor: pointer;
        }

        .textBox {
            width: 100%;
            height: 40px;
            margin: 0 auto;
            overflow: hidden;
            position: relative;
            text-align: center;
        }

        .text {
            width: 100%;
            position: absolute;
            bottom: 0;
            height: 100%;
            margin-bottom: 0;
            display: flex;
            justify-content: center;
            align-items: center;

            .el-tag {
                width: 6rem !important;
            }

            .notice-content {
                width: calc(100% - 6rem);
                height: 1.5rem;
                line-height: 1.5rem;
                align-items: center;
                justify-content: center;
                text-align: left;
                white-space: nowrap;
                overflow: hidden;
                text-overflow: ellipsis;
                text-indent: 1rem;
            }
        }

        .slide-enter-active, .slide-leave-active {
            transition: all 0.5s linear;
        }

        .slide-enter {
            transform: translateY(20px) scale(1);
            opacity: 1;
        }

        .slide-leave-to {
            transform: translateY(-20px) scale(0.8);
            opacity: 0;
        }

        &:focus {
            outline: none;
        }

        .right-menu-item {
            display: inline-block;
            padding: 0 8px;
            height: 100%;
            font-size: 18px;
            color: var(--pnkx-text-secondary);
            vertical-align: text-bottom;

            &.hover-effect {
                cursor: pointer;
                transition: background .3s;

                &:hover {
                    background: var(--pnkx-surface-muted)
                }
            }
        }

        .navbar-action {
            color: var(--pnkx-text-secondary);
        }

        .theme-toggle {
            width: 36px;
            height: 36px;
            margin-right: 10px;
            border: 1px solid var(--pnkx-border);
            border-radius: var(--pnkx-radius-md);
            background: var(--pnkx-surface-muted);
            color: var(--pnkx-text-secondary);
            cursor: pointer;
            display: inline-flex;
            align-items: center;
            justify-content: center;
            transition: background-color 0.16s ease, border-color 0.16s ease, color 0.16s ease, box-shadow 0.16s ease;

            &:hover,
            &:focus-visible {
                border-color: var(--pnkx-primary);
                background: var(--pnkx-primary-soft);
                color: var(--pnkx-primary);
                box-shadow: var(--pnkx-shadow-1);
                outline: none;
            }
        }

        .avatar-container {
            margin-right: 30px;

            .avatar-wrapper {
                margin-top: 5px;
                position: relative;

                .user-avatar {
                    cursor: pointer;
                    width: 40px;
                    height: 40px;
                    border-radius: 10px;
                }

                .el-icon-caret-bottom {
                    cursor: pointer;
                    position: absolute;
                    right: -20px;
                    top: 25px;
                    font-size: 12px;
                }
            }
        }
    }
}

::v-deep .el-breadcrumb__inner {
    color: var(--pnkx-text-secondary);
    font-weight: 500;
}
</style>
