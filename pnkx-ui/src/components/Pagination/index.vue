<template>
    <div :class="{'hidden':hidden}" class="pagination-container">
        <el-pagination
            :background="background"
            :current-page="mutablePage"
            :page-size="mutableLimit"
            :layout="layout"
            :page-sizes="pageSizes"
            :total="total"
            v-bind="$attrs"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
        />
    </div>
</template>

<script>
import {scrollTo} from '@/utils/scroll-to'

export default {
    name: 'Pagination',
    emits: ['update:page', 'update:limit', 'pagination'],
    props: {
        total: {
            required: true,
            type: Number
        },
        page: {
            type: Number,
            default: 1
        },
        limit: {
            type: Number,
            default: 20
        },
        pageSizes: {
            type: Array,
            default() {
                return [10, 20, 30, 50]
            }
        },
        layout: {
            type: String,
            default: 'total, sizes, prev, pager, next, jumper'
        },
        background: {
            type: Boolean,
            default: true
        },
        autoScroll: {
            type: Boolean,
            default: true
        },
        hidden: {
            type: Boolean,
            default: false
        }
    },
    data() {
        return {
            mutablePage: this.page,
            mutableLimit: this.limit
        }
    },
    watch: {
        page(val) {
            this.mutablePage = val
        },
        limit(val) {
            this.mutableLimit = val
        }
    },
    methods: {
        handleSizeChange(val) {
            this.mutableLimit = val
            this.mutablePage = 1
            this.$emit('update:limit', val)
            this.$emit('update:page', 1)
            this.$emit('pagination', {page: 1, limit: val})
            if (this.autoScroll) {
                scrollTo(0, 800)
            }
        },
        handleCurrentChange(val) {
            this.mutablePage = val
            this.$emit('update:page', val)
            this.$emit('pagination', {page: val, limit: this.mutableLimit})
            if (this.autoScroll) {
                scrollTo(0, 800)
            }
        }
    }
}
</script>

<style scoped>
.pagination-container {
    height: 44px!important;
    padding: 32px 16px;
    margin-top: 0!important;
    margin-bottom: 0!important;
}

.pagination-container .el-pagination {
    padding: 0!important;
}

.pagination-container.hidden {
    display: none;
}
</style>
