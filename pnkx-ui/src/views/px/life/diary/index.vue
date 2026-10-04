<!--
 * @File: index
 * @Author: PHY
 * @Date: 2021-11-28 10:53
 * @Description: 日记 - Modern UI/UX Refactored (双视图模式)
-->
<template>
    <div class="diary-page">
        <el-tabs v-model="activeTab" class="diary-tabs">
            <el-tab-pane label="日记" name="diary">
                <div class="diary-container">
        <!-- ==================== 现代分栏视图 ==================== -->
        <template v-if="viewMode === 'modern'">
            <!-- 左侧列表面板 -->
            <diary-sidebar
                v-model="searchCode"
                :list="filteredList"
                :list-loading="listLoading"
                :active-id="active && active.id"
                :regex="regex"
                @select="handleSelect"
                @contextmenu="handleContextMenu"
            />

            <!-- 右侧详情面板 -->
            <diary-detail-panel
                :active="active"
                :loading="loading"
                @edit="handleEdit"
                @delete="handleDeleteFromDetail"
            />
        </template>

        <!-- ==================== 日历视图 ==================== -->
        <template v-else>
            <div class="calendar-view">
                <div class="calendar-toolbar">
                    <el-select
                        v-model="retrievalValue"
                        :loading="retrievalLoading"
                        :remote-method="retrievalRemoteMethod"
                        filterable
                        placeholder="请输入关键词"
                        remote
                        reserve-keyword
                        @change="handleChangeDate"
                    >
                        <el-option
                            v-for="item in retrievalOptions"
                            :key="item.id"
                            :label="item.date"
                            :value="JSON.stringify(item)"
                        >
                            <div class="retrieval-content">
                                <div class="date">{{ item.date }}</div>
                                <div class="mood">
                                    <svg-icon
                                        v-if="item.mood"
                                        :icon-class="item.mood"
                                        style="height: 1.5rem;width: 1.5rem;margin-right: 0.5rem;"
                                    />
                                    <svg-icon
                                        v-if="item.weather"
                                        :icon-class="item.weather"
                                        style="height: 1.5rem;width: 1.5rem;"
                                    />
                                </div>
                                <div class="content">
                                    {{ item.content.replace(regex, '') }}
                                </div>
                            </div>
                        </el-option>
                    </el-select>
                </div>
                <calendar
                    :diary-list="list"
                    :open-diary="handleOpenDiary"
                    @date-change="handleDateChange"
                />
            </div>
        </template>

        <!-- 视图切换按钮 -->
        <div class="view-toggle" @click="toggleView">
            <svg-icon :icon-class="viewMode === 'modern' ? 'date' : '编辑02'" class="toggle-icon"/>
            <span class="toggle-label">{{ viewMode === 'modern' ? '日历' : '列表' }}</span>
        </div>

        <!-- 浮动新增按钮 -->
        <div class="fab-action" title="新增日记" @click="handleAdd">
            <el-icon>
                <Plus/>
            </el-icon>
        </div>

        <!-- 日记编辑抽屉 -->
        <diary-drawer
            ref="diaryDrawer"
            v-model:visible="diaryVisible"
            :title="drawerTitle"
            :diary="diary"
            :save-loading="saveLoading"
            :rules="diaryRules"
            @before-close="saveDairy"
        />

        <!-- 右键菜单（仅现代视图） -->
        <transition name="context-menu">
            <div
                v-if="contextMenuVisible"
                v-clickOutSide="closeContextMenu"
                class="context-menu"
                :style="contextMenuStyle"
            >
                <div
                    v-for="item in contextMenuItems"
                    :key="item.id"
                    class="menu-item"
                    @click="handleContextAction(item)"
                >
                    <svg-icon :icon-class="item.icon" class="menu-icon"/>
                    <span>{{ item.name }}</span>
                </div>
            </div>
        </transition>
            </div>
            </el-tab-pane>
            <el-tab-pane label="日记分析" name="analysis">
                <diary-analysis v-if="activeTab === 'analysis'" />
            </el-tab-pane>
        </el-tabs>
    </div>
</template>

<script>
import {addDiary, delDiary, getDiary, listDiary, retrievalDiary, updateDiary} from '@/api/px/life/diary'
import Calendar from './calendar.vue'
import DiaryAnalysis from './analysis.vue'
import DiarySidebar from './components/DiarySidebar.vue'
import DiaryDetailPanel from './components/DiaryDetailPanel.vue'
import DiaryDrawer from './components/DiaryDrawer.vue'

export default {
    name: 'Diary',
    components: {Calendar, DiaryAnalysis, DiarySidebar, DiaryDetailPanel, DiaryDrawer},
    data() {
        return {
            // 当前激活的 tab
            activeTab: 'diary',
            // 视图模式：modern 分栏 / calendar 日历
            viewMode: 'calendar',
            // 列表加载标志
            listLoading: false,
            // 详情加载标志
            loading: false,
            // 保存加载标志
            saveLoading: false,
            // 搜索关键字
            searchCode: '',
            // 日记列表
            list: [],
            // 当前选中
            active: null,
            // 当前月份（日历视图用）
            currentMonth: new Date(),
            // html文本只显示文字的正则表达式
            regex: /(<([^>]+)>)/ig,
            // 日记抽屉
            diaryVisible: false,
            // 抽屉标题
            drawerTitle: '记录好心情',
            // 日记表单
            diary: {
                id: '',
                date: '',
                richText: '',
                content: '',
                mood: '',
                weather: ''
            },
            // 日记缓存（用于对比是否修改）
            diaryCache: '',
            // 表单校验
            diaryRules: {
                content: [
                    {required: true, message: '请输入内容', trigger: 'blur'}
                ]
            },
            // 检索相关（日历视图用）
            retrievalValue: '',
            retrievalLoading: false,
            retrievalOptions: [],
            // 右键菜单
            contextMenuVisible: false,
            contextMenuStyle: '',
            contextMenuItems: [],
            contextMenuTarget: null
        }
    },
    computed: {
        filteredList() {
            if (!this.searchCode) return this.list
            const keyword = this.searchCode.toLowerCase()
            return this.list.filter(item =>
                (item.date && item.date.toLowerCase().includes(keyword)) ||
                (item.content && item.content.replace(this.regex, '').toLowerCase().includes(keyword))
            )
        }
    },
    mounted() {
        this.getList()
        if (this.$route.query.today) {
            this.handleOpenDiary({day: new Date()})
        }
        if (this.$route.query.diaryId && this.$route.query.diaryId !== 'undefined') {
            this.handleOpenDiary(null, {id: this.$route.query.diaryId})
        }
    },
    methods: {
        /**
         * 切换视图模式
         */
        toggleView() {
            this.viewMode = this.viewMode === 'modern' ? 'calendar' : 'modern'
        },
        /**
         * 获取日记列表
         */
        getList() {
            this.listLoading = true
            listDiary({date: this.parseTime(this.currentMonth)}).then(res => {
                this.list = res.rows
                this.listLoading = false
                if (this.viewMode === 'modern' && this.list.length > 0 && !this.active) {
                    this.active = this.list[0]
                }
            })
        },
        /**
         * 选中日记
         */
        handleSelect(item) {
            this.active = item
        },
        /**
         * 新增日记
         */
        handleAdd() {
            this.diary = {
                id: '',
                date: this.parseTime(new Date(), '{y}-{m}-{d}'),
                richText: '',
                content: '',
                mood: 'x-可爱',
                weather: 'w-晴'
            }
            this.drawerTitle = '记录好心情'
            this.diaryVisible = true
        },
        /**
         * 编辑日记（从详情面板或右键菜单）
         */
        handleEdit() {
            if (!this.active) return
            this.loading = true
            getDiary(this.active.id).then(res => {
                this.diary = res.data
                this.diaryCache = JSON.stringify(this.diary)
                this.drawerTitle = '编辑日记'
                this.diaryVisible = true
                this.loading = false
            })
        },
        /**
         * 删除日记（从详情面板或右键菜单）
         */
        handleDeleteFromDetail() {
            if (!this.active) return
            this.$confirm('确认删除该日记?', '删除', {
                type: 'warning'
            }).then(() => {
                return delDiary(this.active.id)
            }).then(() => {
                this.$notify.success('删除成功')
                this.active = null
                this.getList()
            }).catch(() => {
            })
        },
        /**
         * 打开日记（两种视图共用）
         */
        handleOpenDiary(data, item) {
            if (item && item.id) {
                this.loading = true
                getDiary(item.id).then(res => {
                    this.diary = res.data
                    this.diaryCache = JSON.stringify(this.diary)
                    this.drawerTitle = '编辑日记'
                    this.active = res.data
                    this.diaryVisible = true
                    this.loading = false
                })
            } else {
                this.diary = {
                    id: '',
                    date: this.parseTime(data.day, '{y}-{m}-{d}'),
                    richText: '',
                    content: '',
                    mood: 'x-可爱',
                    weather: 'w-晴'
                }
                this.drawerTitle = '记录好心情'
                this.diaryVisible = true
            }
        },
        /**
         * 日历月份切换
         */
        handleDateChange(day) {
            const oldMonth = this.parseTime(this.currentMonth, '{y}-{m}')
            const newMonth = this.parseTime(day, '{y}-{m}')
            if (oldMonth !== newMonth) {
                this.currentMonth = day
                this.getList()
            }
        },
        /**
         * 检索选中（日历视图用）
         */
        handleChangeDate(item) {
            item = JSON.parse(item)
            if (item && item.id) {
                this.handleOpenDiary(null, item)
            }
        },
        /**
         * 关键字搜索（日历视图用）
         */
        retrievalRemoteMethod(query) {
            if (query !== '') {
                this.retrievalLoading = true
                retrievalDiary({searchCode: query}).then(res => {
                    this.retrievalOptions = res.data
                    this.retrievalLoading = false
                })
            } else {
                this.retrievalOptions = []
            }
        },
        /**
         * 保存日记
         */
        saveDairy(done) {
            if (!this.diary.content) {
                done()
                return
            }
            // 如果没有改变，则直接关闭
            if (this.diaryCache === JSON.stringify(this.diary)) {
                done()
                this.diaryVisible = false
                this.diary = {}
                return
            }
            this.$refs.diaryDrawer.validate(valid => {
                if (valid) {
                    this.saveLoading = true
                    if (this.diary.id) {
                        updateDiary(this.diary).then(() => {
                            this.$notify.success('修改日记成功')
                            this.diaryVisible = false
                            this.getList()
                        }).finally(() => {
                            this.diary = {}
                            this.saveLoading = false
                        })
                    } else {
                        addDiary(this.diary).then(() => {
                            this.$notify.success('新增日记成功')
                            this.diaryVisible = false
                            this.getList()
                        }).finally(() => {
                            this.diary = {}
                            this.saveLoading = false
                        })
                    }
                }
            })
        },
        /**
         * 右键菜单
         */
        handleContextMenu(event, item) {
            if (item) {
                this.active = item
                this.contextMenuTarget = item
                this.contextMenuItems = [
                    {id: 1, name: '编辑', icon: '编辑'},
                    {id: 2, name: '删除', icon: '删除'}
                ]
            } else {
                this.contextMenuItems = [
                    {id: 3, name: '新增日记', icon: '编辑02'}
                ]
            }
            this.contextMenuVisible = true
            this.contextMenuStyle = `top: ${Math.min(event.y, window.innerHeight - this.contextMenuItems.length * 48)}px; left: ${Math.min(event.x - 180, window.innerWidth - 200)}px;`
        },
        /**
         * 关闭右键菜单
         */
        closeContextMenu() {
            this.contextMenuVisible = false
        },
        /**
         * 右键菜单操作
         */
        handleContextAction(item) {
            this.contextMenuVisible = false
            switch (item.id) {
                case 1:
                    this.handleEdit()
                    break
                case 2:
                    this.handleDeleteFromDetail()
                    break
                case 3:
                    this.handleAdd()
                    break
            }
        }
    }
}
</script>

<style lang="scss" scoped>
// ==================== 容器 ====================

.diary-page {
    height: calc(100vh - 84px);
    padding: 16px;
    box-sizing: border-box;
    display: flex;
    flex-direction: column;
}

.diary-tabs {
    flex: 1;
    display: flex;
    flex-direction: column;
    overflow: hidden;

    :deep(.el-tabs__content) {
        flex: 1;
        overflow: hidden;
    }
}

.diary-container {
    display: flex;
    height: 100%;
    overflow: hidden;
    background: var(--bg-body);
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif;
    position: relative;
}

// ==================== 日历视图 ====================

.calendar-view {
    flex: 1;
    display: flex;
    flex-direction: column;
    overflow-y: auto;

    .calendar-toolbar {
        padding: var(--space-4) var(--space-5);
        background: var(--bg-card);
        border-bottom: 1px solid var(--border-primary);
        display: flex;
        align-items: center;

        .el-select {
            width: 100%;
        }
    }
}

.retrieval-content {
    display: flex;
    align-items: center;

    .mood {
        margin: 0 0.5rem;
        display: flex;
        align-items: center;
    }

    .content {
        flex: 1;
        width: 20rem;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
    }
}

// ==================== 视图切换按钮 ====================

.view-toggle {
    position: fixed;
    right: 32px;
    top: calc(84px + 16px);
    display: flex;
    align-items: center;
    gap: 6px;
    padding: var(--space-2) var(--space-4);
    background: var(--bg-card);
    border-radius: var(--radius-lg);
    box-shadow: var(--shadow-md);
    cursor: pointer;
    transition: all var(--duration-normal) var(--ease-default);
    z-index: 100;
    border: 1px solid var(--border-primary);

    .toggle-icon {
        font-size: var(--text-base);
        color: var(--color-primary);
    }

    .toggle-label {
        font-size: var(--text-sm);
        font-weight: var(--font-semibold);
        color: var(--text-primary);
    }

    &:hover {
        box-shadow: var(--shadow-lg);
        transform: translateY(-1px);

        .toggle-icon {
            color: var(--color-primary-600);
        }
    }

    &:active {
        transform: scale(0.96);
    }
}

// ==================== 浮动新增按钮 ====================

.fab-action {
    position: fixed;
    right: 32px;
    bottom: 32px;
    width: 56px;
    height: 56px;
    border-radius: 50%;
    background: var(--color-primary);
    color: white;
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: var(--shadow-lg);
    cursor: pointer;
    transition: all var(--duration-normal) var(--ease-default);
    z-index: 100;

    i {
        font-size: var(--text-xl);
    }

    &:hover {
        transform: scale(1.1) rotate(90deg);
        box-shadow: var(--shadow-lg);
    }

    &:active {
        transform: scale(0.95);
    }
}

// ==================== 右键菜单 ====================

.context-menu {
    position: fixed;
    background: var(--bg-card);
    border-radius: var(--radius-md);
    box-shadow: var(--shadow-lg);
    padding: var(--space-2) 0;
    min-width: 180px;
    z-index: 9999;
    border: 1px solid var(--border-primary);

    .menu-item {
        display: flex;
        align-items: center;
        gap: var(--space-3);
        padding: var(--space-3) var(--space-4);
        font-size: var(--text-sm);
        color: var(--text-primary);
        cursor: pointer;
        transition: all var(--duration-normal) var(--ease-default);

        .menu-icon {
            font-size: var(--text-base);
            color: var(--text-secondary);
        }

        &:hover {
            background: var(--bg-hover);
            color: var(--color-primary);

            .menu-icon {
                color: var(--color-primary);
            }
        }
    }
}

// 右键菜单动画
.context-menu-enter-active,
.context-menu-leave-active {
    transition: all var(--duration-fast) var(--ease-default);
}

.context-menu-enter,
.context-menu-leave-to {
    opacity: 0;
    transform: scale(0.95) translateY(-8px);
}

// Loading 美化
::v-deep .el-loading-mask {
    background-color: rgba(255, 255, 255, 0.9);
    backdrop-filter: blur(4px);
}

// 通知美化
::v-deep .el-notification {
    border-radius: var(--radius-md);
    box-shadow: var(--shadow-lg);
    border: none;
}
</style>
