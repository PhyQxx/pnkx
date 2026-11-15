<template>
    <div class="function-page">
        <!-- Hero -->
        <div class="function-hero">
            <div>
                <div class="function-hero__title">生活助手</div>
                <div class="function-hero__subtitle">把日子过成喜欢的样子</div>
            </div>
            <button class="function-hero__search" @click="openSearch">
                <van-icon name="search" />
            </button>
        </div>

        <!-- 快捷操作 -->
        <div class="function-quick m-card">
            <div class="function-quick__title">今天想做什么？</div>
            <div class="function-quick__items">
                <div class="function-quick__entry function-quick__entry--active" @click="handleQuick('/m/diary/edit')">
                    <span class="function-quick__icon">📝</span>
                    <span>写日记</span>
                </div>
                <div class="function-quick__entry" @click="handleQuick('/m/bookkeeping/record/add')">
                    <span class="function-quick__icon">💰</span>
                    <span>记一笔</span>
                </div>
                <div class="function-quick__entry" @click="handleQuick('/m/todo/edit')">
                    <span class="function-quick__icon">➕</span>
                    <span>加待办</span>
                </div>
            </div>
        </div>

        <!-- 后端菜单驱动的分组宫格 -->
        <div v-for="(group, groupIndex) in manageList" :key="group.id" class="function-group">
            <div class="m-section-title" :class="'function-accent--' + (groupIndex % 7)">{{ group.name }}</div>
            <div class="m-card function-grid">
                <div
                    v-for="(menu, menuIndex) in group.children"
                    :key="menu.menuId"
                    class="m-grid-item"
                    @click="changeGrid(menu)"
                >
                    <span class="m-grid-item__icon" :class="'function-scheme--' + ((groupIndex * 4 + menuIndex) % 7)">
                        <span v-if="emojiOf(menu)" class="function-grid__emoji">{{ emojiOf(menu) }}</span>
                        <img v-else-if="iconOf(menu)" :src="iconOf(menu)" alt="">
                        <van-icon v-else name="apps-o" />
                    </span>
                    <span class="function-grid__name">{{ menu.menuName }}</span>
                </div>
            </div>
        </div>

        <!-- 空态 -->
        <div v-if="!loading && manageList.length === 0" class="m-empty">
            <span>暂无可用功能</span>
            <span style="font-size: 12px">请在后台开启 App 菜单</span>
        </div>
    </div>
</template>

<script>
import { listByParams } from '@/api/system/menu'
import { uniappPathToMobile } from '@/mobile/router/map'
import { iconUrl } from '@/mobile/utils/icons'
import { showToast } from '@/mobile/utils/ui'

// emoji 兜底表：菜单 icon 找不到对应 svg 时，按菜单名匹配 emoji
const EMOJI_FALLBACK = {
    '纪念日': '🎂', '情侣卡': '💕', '日记': '📝',
    '菜谱': '🍳', '膳食计划': '🥗', '姨妈助手': '🌸',
    '提醒中心': '🔔', '家庭日历': '📅'
}

export default {
    name: 'MFunction',
    data() {
        return {
            // 管理列表（后端菜单驱动），本地缓存兜底
            manageList: JSON.parse(localStorage.getItem('bolgMenu') || '[]'),
            loading: true
        }
    },
    activated() {
        this.getMenuList()
    },
    mounted() {
        this.getMenuList()
    },
    methods: {
        openSearch() {
            // 搜索页按期上线，未注册前给出提示
            if (this.$router.resolve('/m/search').matched.length > 0) {
                this.$router.push('/m/search')
            } else {
                showToast('搜索功能即将上线')
            }
        },
        handleQuick(path) {
            if (this.$router.resolve(path).matched.length > 0) {
                this.$router.push(path)
            } else {
                showToast('功能即将上线')
            }
        },
        emojiOf(menu) {
            if (this.iconOf(menu)) return ''
            return EMOJI_FALLBACK[menu.menuName] || ''
        },
        iconOf(menu) {
            return menu.icon ? iconUrl(menu.icon) : ''
        },
        changeGrid(menu) {
            // 移动端路由：优先用 appPath（uniapp 路径），为空时按 path 推导（兼容旧菜单）
            const target = uniappPathToMobile(menu.appPath)
                || uniappPathToMobile('/pages_life/' + String(menu.path || '').replace(/\//g, '') + '/index')
            if (target && this.$router.resolve(target).matched.length > 0) {
                this.$router.push(target)
            } else {
                showToast('功能即将上线')
            }
        },
        /**
         * 获取路由（后端菜单驱动，isApp='1' 的菜单才显示）
         */
        getMenuList() {
            listByParams({ isApp: '1' }).then(res => {
                const groupMap = new Map()
                ;(res.data || []).forEach(item => {
                    let group = groupMap.get(item.parentId)
                    if (!group) {
                        group = { id: item.parentId, name: item.parentName, children: [] }
                        groupMap.set(item.parentId, group)
                    }
                    group.children.push(item)
                })
                this.manageList = Array.from(groupMap.values())
                localStorage.setItem('bolgMenu', JSON.stringify(this.manageList))
                this.loading = false
            }).catch(() => {
                this.loading = false
            })
        }
    }
}
</script>

<style lang="scss" scoped>
.function-page {
    min-height: 100vh;
    padding: 12px 12px 24px;
    box-sizing: border-box;
    background:
        linear-gradient(180deg, rgba(247, 251, 255, 0.02) 0%, var(--pnkx-bg) 325px),
        url('@/mobile/assets/images/glacier-aurora-bg.jpg') top center / 100% auto no-repeat;
}

.function-hero {
    display: flex;
    align-items: center;
    justify-content: space-between;
    min-height: 84px;

    &__title {
        font-size: 24px;
        line-height: 1.2;
        font-weight: 700;
        color: var(--pnkx-text);
    }

    &__subtitle {
        margin-top: 8px;
        color: var(--pnkx-text-secondary);
        font-size: 13px;
    }

    &__search {
        width: 44px;
        height: 44px;
        border-radius: 50%;
        border: 1px solid rgba(255, 255, 255, 0.9);
        background: rgba(255, 255, 255, 0.84);
        color: var(--pnkx-primary);
        font-size: 19px;
        display: flex;
        align-items: center;
        justify-content: center;

        &:active { opacity: 0.8; }
    }
}

.function-quick {
    padding: 16px 14px 12px;

    &__title {
        font-size: 16px;
        font-weight: 600;
        color: var(--pnkx-text);
    }

    &__items {
        display: flex;
        margin-top: 14px;
    }

    &__entry {
        position: relative;
        width: 33.333%;
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 6px;
        color: var(--pnkx-text);
        font-size: 14px;

        &:not(:last-child)::after {
            content: '';
            position: absolute;
            top: 4px;
            right: 0;
            width: 1px;
            height: 38px;
            background: var(--pnkx-border);
        }

        &--active { color: var(--pnkx-primary); }
    }

    &__icon { font-size: 22px; line-height: 1; }
}

.function-group { margin-bottom: 4px; }

.function-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 14px 8px;
    padding: 16px 10px 12px;

    &__emoji { font-size: 22px; line-height: 1; }

    &__name {
        font-size: 12px;
        color: var(--pnkx-text-secondary);
        font-weight: 500;
        max-width: 100%;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
    }
}

// 分组强调色（标题左条）
.function-accent {
    &--0::before { background: var(--pnkx-primary); }
    &--1::before { background: #f472b6; }
    &--2::before { background: #38bdf8; }
    &--3::before { background: #f472b6; }
    &--4::before { background: #fbbf24; }
    &--5::before { background: #34d399; }
    &--6::before { background: #a78bfa; }
}

// 宫格图标底色轮换
.function-scheme {
    &--0 { background: rgba(91, 141, 239, 0.12); }
    &--1 { background: rgba(244, 114, 182, 0.12); }
    &--2 { background: rgba(56, 189, 248, 0.12); }
    &--3 { background: rgba(251, 191, 36, 0.14); }
    &--4 { background: rgba(52, 211, 153, 0.12); }
    &--5 { background: rgba(167, 139, 250, 0.12); }
    &--6 { background: rgba(91, 141, 239, 0.12); }
}
</style>
