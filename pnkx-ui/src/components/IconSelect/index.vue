<!-- @author zhengjie -->
<template>
    <div class="icon-body">
        <el-input v-model="name" style="position: relative;" clearable placeholder="请输入图标名称" @clear="filterIcons"
                  @input.native="filterIcons">
            <template #suffix><el-icon><Search /></el-icon></template>
        </el-input>
        <div class="icon-list">
            <div v-for="(item, index) in iconList" :key="index" @click="selectedIcon(item)">
                <svg-icon :icon-class="item" :style="'height: '+ height+';width: '+width+';'"/>
                <span>{{ item[1] === '-' ? item.slice(2) : item }}</span>
            </div>
        </div>
    </div>
</template>

<script>
import icons from './requireIcons'

export default {
    name: 'IconSelect',
    props: {
        prefix: '',
        height: {
            type: String,
            default: '30px'
        },
        width: {
            type: String,
            default: '16px'
        }
    },
    data() {
        return {
            name: '',
            iconList: icons
        }
    },
    watch: {
        prefix: {
            handler() {
                this.filterIcons()
            },
            deep: true,
            immediate: true
        }
    },
    methods: {
        filterIcons() {
            this.iconList = icons;
            if (this.prefix + this.name) {
                this.iconList = this.iconList.filter(item => item.includes((this.prefix ? this.prefix : '') + this.name));
            }
        },
        selectedIcon(name) {
            this.$emit('selected', name);
            document.body.click();
        },
        reset() {
            this.name = '';
            this.filterIcons();
        }
    }
}
</script>

<style rel="stylesheet/scss" lang="scss" scoped>
.icon-body {
    width: 100%;
    padding: 10px;

    .icon-list {
        &:before {
            content: '';
        }

        height: 200px;
        overflow-y: scroll;

        div {
            height: 30px;
            line-height: 30px;
            margin-bottom: -5px;
            cursor: pointer;
            width: 33%;
            float: left;
        }

        span {
            display: inline-block;
            vertical-align: -0.18em;
            fill: currentColor;
            overflow: hidden;
            margin-left: 0.5em;
        }
    }
}
</style>
