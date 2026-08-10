<!--
 * @File: index
 * @Author: PHY
 * @Date: 2021-11-28 10:53
 * @Description: 日记 - Modern UI/UX Refactored (双视图模式)
-->
<template>
    <div class="diary-container">
        <!-- ==================== 现代分栏视图 ==================== -->
        <template v-if="viewMode === 'modern'">
            <!-- 左侧列表面板 -->
            <aside class="sidebar">
                <!-- 搜索栏 -->
                <div class="search-wrapper">
                    <div class="search-box">
                        <svg-icon icon-class="搜索" class="search-icon"/>
                        <input
                            v-model="searchCode"
                            placeholder="搜索日记..."
                            class="search-input"
                        >
                    </div>
                </div>

                <!-- 日记列表 -->
                <div
                    v-loading="listLoading"
                    class="diary-list"
                    @contextmenu.prevent.stop="handleContextMenu($event, null)"
                >
                    <div v-if="filteredList.length < 1" class="empty-state">
                        <svg-icon icon-class="备注" class="empty-icon"/>
                        <p>暂无日记</p>
                        <p class="hint">右键或点击右下角按钮新增</p>
                    </div>

                    <transition-group v-else name="diary-list" tag="div" class="diary-items">
                        <div
                            v-for="(item, index) in filteredList"
                            :key="item.id"
                            class="diary-card"
                            :class="{ active: active && active.id === item.id }"
                            :style="{ animationDelay: `${index * 0.05}s` }"
                            @contextmenu.prevent.stop="handleContextMenu($event, item)"
                            @click="handleSelect(item)"
                        >
                            <div class="card-icon-wrapper">
                                <svg-icon :icon-class="item.mood || '备注'" class="card-icon"/>
                            </div>
                            <div class="card-info">
                                <div class="card-date">{{ item.date }}</div>
                                <div class="card-preview">{{ item.content && item.content.replace(regex, '') }}</div>
                            </div>
                            <div class="card-meta">
                                <svg-icon v-if="item.weather" :icon-class="item.weather" class="meta-icon"/>
                                <svg-icon v-if="item.mood" :icon-class="item.mood" class="meta-icon mood"/>
                            </div>
                        </div>
                    </transition-group>
                </div>
            </aside>

            <!-- 右侧详情面板 -->
            <main v-loading="loading" class="detail-area">
                <!-- 空状态 -->
                <div v-if="!active" class="empty-detail">
                    <svg-icon icon-class="备注" class="empty-detail-icon"/>
                    <p>选择一条日记查看详情</p>
                </div>

                <!-- 日记详情 -->
                <div v-else class="diary-detail">
                    <!-- 头部信息 -->
                    <div class="detail-header">
                        <div class="detail-icon-wrapper">
                            <svg-icon :icon-class="active.mood || '备注'" class="detail-icon"/>
                        </div>
                        <div class="detail-title-section">
                            <h2 class="detail-date">{{ active.date }}</h2>
                            <div class="detail-tags">
                <span v-if="active.mood" class="tag mood-tag">
                  <svg-icon :icon-class="active.mood" class="tag-icon"/>
                  心情
                </span>
                                <span v-if="active.weather" class="tag weather-tag">
                  <svg-icon :icon-class="active.weather" class="tag-icon"/>
                  天气
                </span>
                            </div>
                        </div>
                        <div class="detail-actions">
                            <el-button type="primary" size="small" @click="handleEdit">
                                <svg-icon icon-class="编辑" class="action-icon"/>
                                编辑
                            </el-button>
                            <el-button type="danger" size="small" @click="handleDeleteFromDetail">
                                <svg-icon icon-class="删除" class="action-icon"/>
                                删除
                            </el-button>
                        </div>
                    </div>

                    <!-- 日记内容 -->
                    <div class="detail-body">
                        <div class="detail-content" v-html="sanitizeHtml(active.content)"/>
                    </div>
                </div>
            </main>
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
        <el-drawer
            :title="drawerTitle"
            size="50%"
            destroy-on-close
            v-model="diaryVisible"
            :before-close="saveDairy"
            custom-class="modern-drawer"
        >
            <el-form ref="form" v-loading="saveLoading" :model="diary" :rules="diaryRules"
                     class="diary-form modern-form">
                <div class="diary-meta-row">
                    <el-form-item label="心情" prop="mood" class="meta-item">
                        <el-popover
                            placement="bottom-start"
                            width="460"
                            trigger="click"
                            @show="$refs['feelingSelect'].reset()"
                        >
                            <icon-select ref="feelingSelect" prefix="x-" @selected="feelingSelected"/>
                            <template #reference>
                                <el-input v-model="diary.mood" placeholder="点击选择心情" readonly>
                                    <template #prefix>
                                        <svg-icon
                                            v-if="diary.mood"
                                            :icon-class="diary.mood"
                                            class="el-input__icon"
                                            style="height: 32px;width: 16px;"
                                        />
                                        <el-icon v-else>
                                            <Search/>
                                        </el-icon>
                                    </template>
                                </el-input>
                            </template>
                        </el-popover>
                    </el-form-item>
                    <el-form-item label="天气" class="meta-item weather-item" prop="weather">
                        <el-popover
                            placement="bottom-start"
                            width="460"
                            trigger="click"
                            @show="$refs['weatherSelect'].reset()"
                        >
                            <icon-select ref="weatherSelect" prefix="w-" @selected="weatherSelected"/>
                            <template #reference>
                                <el-input v-model="diary.weather" placeholder="点击选择天气" readonly>
                                    <template #prefix>
                                        <svg-icon
                                            v-if="diary.weather"
                                            :icon-class="diary.weather"
                                            class="el-input__icon"
                                            style="height: 32px;width: 16px;"
                                        />
                                        <el-icon v-else>
                                            <Search/>
                                        </el-icon>
                                    </template>
                                </el-input>
                            </template>
                        </el-popover>
                    </el-form-item>
                </div>
                <el-form-item v-if="diaryVisible" prop="content">
                    <editor ref="editor" v-model="diary.content" :height="600" />
                </el-form-item>
            </el-form>
        </el-drawer>

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
</template>

<script>
import IconSelect from '@/components/IconSelect/index.vue'
import {sanitizeHtml} from '@/utils/sanitizeHtml'
import {addDiary, delDiary, getDiary, listDiary, retrievalDiary, updateDiary} from '@/api/px/life/diary'
import Editor from '@/components/Editor/index.vue'
import Calendar from './calendar.vue'

export default {
    name: 'Diary',
    components: {IconSelect, Editor, Calendar},
    data() {
        return {
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
            this.currentMonth = day
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
         * 心情图标选择
         */
        feelingSelected(name) {
            this.diary.mood = name
        },
        /**
         * 天气图标选择
         */
        weatherSelected(name) {
            this.diary.weather = name
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
            this.$refs.form.validate(valid => {
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

.diary-container {
    display: flex;
    height: calc(100vh - 84px);
    background: var(--bg-body);
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif;
    position: relative;
}

// ==================== 现代视图 ====================

// 左侧边栏
.sidebar {
    width: 360px;
    background: var(--bg-card);
    backdrop-filter: blur(20px);
    border-right: 1px solid var(--border-primary);
    display: flex;
    flex-direction: column;
    box-shadow: var(--shadow-sm);
    position: relative;
    z-index: 10;
}

// 搜索栏
.search-wrapper {
    padding: var(--space-5);
    border-bottom: 1px solid var(--border-primary);

    .search-box {
        position: relative;
        display: flex;
        align-items: center;

        .search-icon {
            position: absolute;
            left: 14px;
            font-size: var(--text-base);
            color: var(--text-tertiary);
            pointer-events: none;
        }

        .search-input {
            width: 100%;
            height: 40px;
            padding: 0 var(--space-4) 0 42px;
            border: none;
            border-radius: var(--radius-md);
            background: var(--bg-hover);
            font-size: var(--text-sm);
            color: var(--text-primary);
            box-shadow: var(--shadow-sm);
            transition: all var(--duration-normal) var(--ease-default);

            &::placeholder {
                color: var(--text-tertiary);
            }

            &:focus {
                outline: none;
                box-shadow: 0 0 0 3px rgba(64, 158, 255, 0.12), var(--shadow-md);
            }
        }
    }
}

// 日记列表
.diary-list {
    flex: 1;
    overflow-y: auto;
    padding: var(--space-4);

    &::-webkit-scrollbar {
        width: 6px;
    }

    &::-webkit-scrollbar-track {
        background: transparent;
    }

    &::-webkit-scrollbar-thumb {
        background: var(--border-primary);
        border-radius: 3px;

        &:hover {
            background: var(--text-tertiary);
        }
    }

    .empty-state {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        height: 100%;
        color: var(--text-tertiary);

        .empty-icon {
            font-size: 64px;
            opacity: 0.3;
            margin-bottom: var(--space-4);
        }

        p {
            margin: var(--space-1) 0;
        }

        .hint {
            font-size: var(--text-xs);
            opacity: 0.7;
        }
    }

    .diary-items {
        display: flex;
        flex-direction: column;
        gap: var(--space-2);
    }
}

// 日记卡片
.diary-card {
    display: flex;
    align-items: center;
    padding: 14px var(--space-4);
    background: var(--bg-card);
    border-radius: var(--radius-md);
    cursor: pointer;
    transition: all var(--duration-normal) var(--ease-default);
    box-shadow: var(--shadow-sm);
    border-left: 3px solid transparent;
    animation: fadeSlideIn 0.4s var(--ease-default) forwards;
    opacity: 0;

    &.active {
        background: var(--bg-hover);
        border-left-color: var(--color-primary);
    }

    &:hover {
        transform: translateX(4px);
        box-shadow: var(--shadow-md);
        background: var(--bg-hover);
    }

    .card-icon-wrapper {
        width: 40px;
        height: 40px;
        display: flex;
        align-items: center;
        justify-content: center;
        background: var(--color-primary);
        border-radius: var(--radius-sm);
        margin-right: var(--space-3);
        flex-shrink: 0;

        .card-icon {
            font-size: var(--text-lg);
            color: white;
        }
    }

    .card-info {
        flex: 1;
        min-width: 0;

        .card-date {
            font-size: var(--text-sm);
            font-weight: var(--font-semibold);
            color: var(--text-primary);
            margin-bottom: var(--space-1);
        }

        .card-preview {
            font-size: var(--text-xs);
            color: var(--text-secondary);
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
        }
    }

    .card-meta {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: var(--space-1);
        flex-shrink: 0;
        margin-left: var(--space-3);

        .meta-icon {
            font-size: 18px;

            &.mood {
                font-size: var(--text-base);
            }
        }
    }
}

@keyframes fadeSlideIn {
    from {
        opacity: 0;
        transform: translateY(10px);
    }
    to {
        opacity: 1;
        transform: translateY(0);
    }
}

// 右侧详情区域
.detail-area {
    flex: 1;
    display: flex;
    flex-direction: column;
    overflow: hidden;
}

.empty-detail {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    color: var(--text-tertiary);

    .empty-detail-icon {
        font-size: 80px;
        opacity: 0.2;
        margin-bottom: var(--space-5);
    }

    p {
        font-size: var(--text-base);
    }
}

// 日记详情
.diary-detail {
    flex: 1;
    display: flex;
    flex-direction: column;
    overflow-y: auto;
}

.detail-header {
    display: flex;
    align-items: center;
    gap: var(--space-5);
    padding: 28px 32px 20px;
    background: var(--bg-card);
    border-bottom: 1px solid var(--border-primary);

    .detail-icon-wrapper {
        width: 56px;
        height: 56px;
        display: flex;
        align-items: center;
        justify-content: center;
        background: var(--color-primary);
        border-radius: var(--radius-md);
        flex-shrink: 0;
        box-shadow: var(--shadow-md);

        .detail-icon {
            font-size: 28px;
            color: white;
        }
    }

    .detail-title-section {
        flex: 1;
        min-width: 0;

        .detail-date {
            font-size: var(--text-xl);
            font-weight: var(--font-semibold);
            color: var(--text-primary);
            margin: 0 0 var(--space-2) 0;
        }

        .detail-tags {
            display: flex;
            gap: var(--space-2);

            .tag {
                display: inline-flex;
                align-items: center;
                gap: var(--space-1);
                padding: 2px 10px;
                border-radius: 20px;
                font-size: var(--text-xs);

                .tag-icon {
                    font-size: var(--text-sm);
                }

                &.mood-tag {
                    background: rgba(64, 158, 255, 0.1);
                    color: var(--color-primary);
                }

                &.weather-tag {
                    background: rgba(64, 158, 255, 0.06);
                    color: var(--color-primary-600);
                }
            }
        }
    }

    .detail-actions {
        display: flex;
        gap: var(--space-2);
        flex-shrink: 0;

        .el-button {
            border-radius: var(--radius-sm);

            .action-icon {
                font-size: var(--text-sm);
                margin-right: var(--space-1);
            }
        }
    }
}

.detail-body {
    flex: 1;
    padding: 32px;

    .detail-content {
        background: var(--bg-card);
        border-radius: var(--radius-lg);
        padding: 32px;
        box-shadow: var(--shadow-md);
        line-height: 1.8;
        color: var(--text-primary);
        font-size: 15px;
        min-height: 300px;

        ::v-deep img {
            max-width: 100%;
            border-radius: var(--radius-sm);
        }
    }
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

// 列表动画
.diary-list-enter-active,
.diary-list-leave-active {
    transition: all var(--duration-normal) var(--ease-default);
}

.diary-list-enter,
.diary-list-leave-to {
    opacity: 0;
    transform: translateX(-20px);
}

// ==================== 抽屉样式覆盖 ====================

::v-deep .modern-drawer {
    .el-drawer__header {
        padding: var(--space-5) 24px var(--space-4);
        border-bottom: 1px solid var(--border-primary);
        background: var(--bg-card);
        margin-bottom: 0;

        > :first-child {
            font-size: var(--text-lg);
            font-weight: var(--font-semibold);
            color: var(--text-primary);
        }
    }

    .el-drawer__body {
        padding: 0;
    }
}

// 表单样式
.diary-form {
    padding: var(--space-5) 24px;

    .diary-meta-row {
        display: flex;
        gap: var(--space-4);

        .meta-item {
            flex: 1;
        }

        .weather-item {
            margin-left: 0;
        }
    }
}

.modern-form {
    ::v-deep .el-form-item {
        margin-bottom: var(--space-5);

        .el-form-item__label {
            font-size: var(--text-sm);
            font-weight: var(--font-semibold);
            color: var(--text-secondary);
            padding-bottom: var(--space-2);
        }

        .el-input__inner {
            border-radius: var(--radius-sm);
            border-color: var(--border-primary);
            transition: all var(--duration-normal) var(--ease-default);

            &:focus {
                border-color: var(--color-primary);
                box-shadow: 0 0 0 3px rgba(64, 158, 255, 0.12);
            }
        }
    }
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
