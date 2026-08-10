<template>
    <div :class="classObj" class="app-wrapper" :style="{'--current-color': theme}">
        <div v-if="device==='mobile'&&sidebar.opened" class="drawer-bg" @click="handleClickOutside"/>
        <sidebar class="sidebar-container"/>
        <div :class="{hasTagsView:needTagsView}" class="main-container">
            <div :class="{'fixed-header':fixedHeader, hasTagsView:needTagsView}">
                <navbar/>
                <tags-view v-if="needTagsView"/>
            </div>
            <app-main/>
            <right-panel v-if="showSettings">
                <settings/>
            </right-panel>
        </div>
        <div class="quick-action-dock">
            <button class="quick-action-trigger" type="button" aria-label="快捷菜单">
                <span class="trigger-mark">快</span>
                <span class="trigger-text">快捷</span>
            </button>
            <div class="quick-action-menu">
                <button
                    v-for="item in selectList"
                    :key="item.name"
                    class="quick-action-item"
                    type="button"
                    @click="item.func($event)"
                >
                    <span class="quick-action-icon">{{ item.icon }}</span>
                    <span>{{ item.name }}</span>
                </button>
            </div>
        </div>

        <!--  文件管理  -->
        <el-dialog
            title="文件管理"
            top="1vh"
            v-model="dialogVisible"
            :modal="false"
            width="80vw">
            <history-picture/>
            <template #footer>
                <span class="dialog-footer">
                    <el-button type="primary" @click="dialogVisible = false">确 定</el-button>
                </span>
            </template>
        </el-dialog>

        <!--  聊天室  -->
        <el-dialog
            title="聊天室"
            top="1vh"
            v-model="chatDialogVisible"
            @close="handleCloseChat"
            width="70vw">
            <chat ref="chat"/>
        </el-dialog>

        <!--  AI 助手悬浮窗  -->
        <ai-assistant ref="aiAssistant" external-trigger />
    </div>
</template>

<script>
import RightPanel from '@/components/RightPanel'
import HistoryPicture from '@/views/tool/file/index'
import {AppMain, Navbar, Settings, Sidebar, TagsView} from './components'
import ResizeMixin from './mixin/ResizeHandler'
import {mapState} from 'vuex'
import Chat from "@/components/Chat/index.vue"
import AiAssistant from "@/components/AiAssistant/index.vue"

export default {
    name: 'Layout',
    components: {
        AppMain,
        Navbar,
        RightPanel,
        Settings,
        Sidebar,
        TagsView,
        HistoryPicture,
        Chat,
        AiAssistant
    },
    mixins: [ResizeMixin],
    data() {
        return {
            // 历史图片
            dialogVisible: false,
            // 聊天室标志位
            chatDialogVisible: false,
            // 按钮选项列表
            selectList: [
                {
                    name: '文件管理',
                    icon: '文',
                    active: true,
                    func: () => {
                        this.dialogVisible = true;
                    },
                },
                {
                    name: '聊天室',
                    icon: '聊',
                    active: true,
                    func: () => {
                        this.chatDialogVisible = true;
                        this.$nextTick(() => {
                            this.$refs.chat.initWebSocket();
                        })
                    },
                },
                {
                    name: '写日记',
                    icon: '日',
                    active: true,
                    func: () => {
                        this.$router.push('/mytool/diary?today=1');
                    },
                },
                {
                    name: '写笔记',
                    icon: '笔',
                    active: true,
                    func: () => {
                        this.$router.push('/note');
                    },
                },
                {
                    name: '记账',
                    icon: '账',
                    active: true,
                    func: () => {
                        this.$router.push('/mytool/bookkeeping/record?remember=1');
                    },
                },
                {
                    name: 'AI',
                    icon: 'AI',
                    active: true,
                    func: () => {
                        this.$refs.aiAssistant.toggleOpen();
                    },
                }
            ]
        }
    },
    computed: {
        ...mapState({
            theme: state => state.settings.theme,
            sidebar: state => state.app.sidebar,
            device: state => state.app.device,
            showSettings: state => state.settings.showSettings,
            needTagsView: state => state.settings.tagsView,
            fixedHeader: state => state.settings.fixedHeader
        }),
        classObj() {
            return {
                hideSidebar: !this.sidebar.opened,
                openSidebar: this.sidebar.opened,
                withoutAnimation: this.sidebar.withoutAnimation,
                mobile: this.device === 'mobile'
            }
        },
    },
    methods: {
        handleClickOutside() {
            this.$store.dispatch('app/closeSideBar', {withoutAnimation: false})
        },
        /**
         * 关闭聊天室
         */
        handleCloseChat() {
            this.$refs.chat.closeWebSocket();
        }
    }
}
</script>

<style lang="scss" scoped>
@import "@/assets/styles/mixin.scss";
@import "@/assets/styles/variables.scss";

.app-wrapper {
    @include clearfix;
    position: relative;
    height: 100%;
    width: 100%;

    &.mobile.openSidebar {
        position: fixed;
        top: 0;
    }
}

.drawer-bg {
    background: #000;
    opacity: 0.3;
    width: 100%;
    top: 0;
    height: 100%;
    position: absolute;
    z-index: 999;
}

.fixed-header {
    position: fixed;
    top: 0;
    right: 0;
    z-index: 9;
    width: calc(100% - #{$sideBarWidth});
    transition: width 0.28s;
}

.hideSidebar .fixed-header {
    width: calc(100% - 54px)
}

.mobile .fixed-header {
    width: 100%;
}

.quick-action-dock {
    position: fixed;
    z-index: 4000;
    right: 24px;
    top: 50%;
    display: flex;
    align-items: center;
    gap: 10px;
    transform: translateY(-50%);

    &:hover,
    &:focus-within {
        .quick-action-menu {
            opacity: 1;
            pointer-events: auto;
            transform: translate(0, -50%);
        }
    }

    .quick-action-trigger {
        width: 54px;
        min-height: 86px;
        padding: 8px 6px;
        border: 1px solid rgba(91, 141, 239, 0.24);
        border-radius: 999px;
        background: var(--pnkx-surface);
        box-shadow: var(--pnkx-shadow-2);
        color: var(--pnkx-primary);
        cursor: pointer;
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        gap: 6px;
        transition: border-color 0.16s ease, box-shadow 0.16s ease, transform 0.16s ease;

        &:hover,
        &:focus-visible {
            border-color: rgba(91, 141, 239, 0.42);
            box-shadow: var(--pnkx-shadow-3);
            transform: translateX(-2px);
            outline: none;
        }
    }

    .trigger-mark {
        width: 32px;
        height: 32px;
        border-radius: var(--pnkx-radius-md);
        background: var(--pnkx-primary);
        color: var(--pnkx-text-inverse);
        display: inline-flex;
        align-items: center;
        justify-content: center;
        font-size: 14px;
        font-weight: 760;
    }

    .trigger-text {
        writing-mode: vertical-rl;
        letter-spacing: 0;
        font-size: 12px;
        font-weight: 700;
        line-height: 1;
    }

    .quick-action-menu {
        position: absolute;
        right: 64px;
        top: 50%;
        width: 148px;
        padding: 8px;
        border: 1px solid var(--pnkx-border);
        border-radius: var(--pnkx-radius-lg);
        background: var(--pnkx-surface);
        box-shadow: var(--pnkx-shadow-3);
        opacity: 0;
        pointer-events: none;
        transform: translate(8px, -50%);
        transition: opacity 0.16s ease, transform 0.16s ease;
    }

    .quick-action-item {
        width: 100%;
        height: 38px;
        padding: 0 10px;
        border: 0;
        border-radius: var(--pnkx-radius-md);
        background: transparent;
        color: var(--pnkx-text-secondary);
        cursor: pointer;
        display: flex;
        align-items: center;
        gap: 8px;
        font-size: 13px;
        font-weight: 600;
        text-align: left;
        transition: background-color 0.16s ease, color 0.16s ease;

        &:hover {
            background: var(--pnkx-surface-muted);
            color: var(--pnkx-primary);
        }
    }

    .quick-action-icon {
        width: 24px;
        height: 24px;
        border-radius: var(--pnkx-radius-sm);
        background: var(--pnkx-primary-soft);
        color: var(--pnkx-primary);
        display: inline-flex;
        align-items: center;
        justify-content: center;
        flex: 0 0 auto;
        font-size: 11px;
        font-weight: 760;
    }
}

.mobile {
    .quick-action-dock {
        right: 14px;

        .quick-action-trigger {
            width: 48px;
            min-height: 76px;
        }

        .quick-action-menu {
            right: 56px;
            width: 136px;
        }
    }
}
</style>
