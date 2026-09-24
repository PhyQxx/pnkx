<template>
    <div :class="{'show':show}" class="header-search">
        <svg-icon class-name="search-icon" icon-class="search" @click.stop="click"/>
        <el-select
            ref="headerSearchSelect"
            v-model="search"
            :loading="retrievalLoading"
            :remote-method="querySearch"
            filterable
            default-first-option
            remote
            placeholder="请输入关键词"
            class="header-search-select"
            @change="change"
        >
            <el-option-group v-for="group in options"
                             :key="group.label"
                             :label="group.label">
                <el-option
                    v-for="item in group.options"
                    :key="item.id"
                    :label="item.title"
                    :value="Object.assign(item, {groupName: group.label})">
                    <div class="option-content">
                        {{ item.title }}
                    </div>
                </el-option>
            </el-option-group>
        </el-select>
    </div>
</template>

<script>
// fuse is a lightweight fuzzy-search module
// make search results more in line with expectations
import Fuse from 'fuse.js';
import {globalSearch} from "../../api";


export default {
    name: 'HeaderSearch',
    data() {
        return {
            search: '',
            options: [],
            searchPool: [],
            show: false,
            fuse: undefined,
            // 检索标志位
            retrievalLoading: false,
            // html文本只显示文字的正则表达式
            regex: /(<([^>]+)>)/ig,
        }
    },
    computed: {
        routes() {
            return this.$store.getters.permission_routes
        }
    },
    watch: {
        routes() {
            this.searchPool = this.generateRoutes(this.routes)
        },
        searchPool(list) {
            this.initFuse(list)
        },
        show(value) {
            if (value) {
                document.body.addEventListener('click', this.close)
            } else {
                document.body.removeEventListener('click', this.close)
            }
        }
    },
    mounted() {
        this.searchPool = this.generateRoutes(this.routes)
    },
    methods: {
        /**
         * 翻译账单类型
         * @param record
         * @returns {string}
         */
        billType(record) {
            if (record.typeObject && record.typeObject.typeDifference === '0') {
                return '收入'
            }
            if (record.typeObject && record.typeObject.typeDifference === '1') {
                return '支出'
            }
            if (record.typeObject && record.typeObject.typeDifference === '2') {
                return '转账'
            }
            if (record.typeObject && record.typeObject.typeDifference === '3') {
                return '修改余额'
            }
        },
        click() {
            this.show = !this.show
            if (this.show) {
                this.$refs.headerSearchSelect && this.$refs.headerSearchSelect.focus()
            }
        },
        close() {
            this.$refs.headerSearchSelect && this.$refs.headerSearchSelect.blur()
            this.options = []
            this.show = false
        },
        change(query) {
            if (query.type === 'article') {
                this.$router.push('/blog/articledetails?adminArticleId='+query.id);
            } else if (query.type === 'todo') {
                this.$router.push('/mytool/todo?toDoId='+query.id);
            } else if (query.type === 'bookkeeping') {
                this.$router.push('/mytool/bookkeeping/record?recordId='+query.id);
            } else if (query.type === 'diary') {
                this.$router.push('/mytool/diary?diaryId=' + query.id);
            } else if (query.type === 'note') {
                this.$router.push('/note?noteId='+query.id);
            } else if (query.route) {
                this.$router.push(query.route + (query.route.includes('?') ? '&' : '?') + 'id=' + query.id)
            }
            this.search = '';
            this.$refs.headerSearchSelect && this.$refs.headerSearchSelect.blur()
            this.options = []
            this.show = false
        },
        initFuse(list) {
            this.fuse = new Fuse(list, {
                shouldSort: true,
                threshold: 0.4,
                location: 0,
                distance: 100,
                maxPatternLength: 32,
                minMatchCharLength: 1,
                keys: [{
                    name: 'title',
                    weight: 0.7
                }, {
                    name: 'path',
                    weight: 0.3
                }]
            })
        },
        // 浏览器兼容的路径解析
        resolvePath(basePath, routePath) {
            if (routePath.startsWith('/')) return routePath;
            return (basePath.endsWith('/') ? basePath : basePath + '/') + routePath;
        },
        // Filter out the routes that can be displayed in the sidebar
        // And generate the internationalized title
        generateRoutes(routes, basePath = '/', prefixTitle = []) {
            let res = []

            for (const router of routes) {
                // skip hidden router
                if (router.hidden) {
                    continue
                }

                const data = {
                    path: !this.ishttp(router.path) ? this.resolvePath(basePath, router.path) : router.path,
                    title: [...prefixTitle]
                }

                if (router.meta && router.meta.title) {
                    data.title = [...data.title, router.meta.title]

                    if (router.redirect !== 'noRedirect') {
                        // only push the routes with title
                        // special case: need to exclude parent router without redirect
                        res.push(data)
                    }
                }

                // recursive child routes
                if (router.children) {
                    const tempRoutes = this.generateRoutes(router.children, data.path, data.title)
                    if (tempRoutes.length >= 1) {
                        res = [...res, ...tempRoutes]
                    }
                }
            }
            return res
        },
        /**
         * 搜索防抖
         */
        querySearch(query) {
            if (query !== '') {
                this.$debounce(() => {
                    this.retrievalLoading = true;
                    globalSearch({q: query}).then(res => {
                        const labels = { article: '博客文章', todo: '待办事项', bookkeeping: '生活账本', diary: '日记', note: '笔记', book: '书籍', recipe: '菜谱', commemoration: '纪念日', subscription: '订阅', shopping: '购物清单' }
                        const groups = new Map()
                        ;(res.data || []).forEach(item => {
                            const label = labels[item.type] || item.type
                            if (!groups.has(label)) groups.set(label, [])
                            groups.get(label).push(item)
                        })
                        this.options = Array.from(groups, ([label, options]) => ({ label, options }))
                        this.retrievalLoading = false;
                    })
                }, 1200)()
            } else {
                this.options = [];
            }
        },
        ishttp(url) {
            return url.indexOf('http://') !== -1 || url.indexOf('https://') !== -1
        }
    }
}
</script>

<style lang="scss" scoped>

.option-content {
    width: 20rem;
    overflow: hidden;
    text-overflow: ellipsis;
    font-size: var(--text-sm);
    color: var(--text-primary);
}

.header-search {
    font-size: 0 !important;
    display: inline-flex;
    align-items: center;

    .search-icon {
        cursor: pointer;
        font-size: 18px;
        color: var(--text-secondary);
        transition: color 0.3s ease;
        flex-shrink: 0;

        &:hover {
            color: var(--color-primary);
        }
    }

    .header-search-select {
        font-size: var(--text-base);
        transition: width 0.3s ease, margin-left 0.3s ease;
        width: 0;
        overflow: hidden;
        background: transparent;
        border-radius: 0;
        display: inline-block;
        vertical-align: middle;

        ::v-deep .el-input__inner {
            border-radius: 0;
            border: 0;
            padding-left: 0;
            padding-right: 0;
            box-shadow: none !important;
            border-bottom: 1px solid var(--border-primary);
            color: var(--text-primary);
            vertical-align: middle;
            transition: border-color 0.3s ease;

            &:focus {
                border-bottom-color: var(--color-primary);
            }
        }
    }

    &.show {
        .header-search-select {
            width: 210px;
            margin-left: var(--space-2);
        }
    }
}
</style>
