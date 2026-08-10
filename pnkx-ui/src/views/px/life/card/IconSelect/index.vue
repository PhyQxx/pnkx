<template>
    <div class="icon-body">
        <el-input v-model="name" clearable placeholder="请输入图标名称" style="position: relative;" @clear="filterIcons"
                  @input.native="filterIcons">
            <template #suffix><el-icon><Search /></el-icon></template>
        </el-input>
        <div class="icon-list">
            <div v-for="(item, index) in iconList" :key="index" class="icon" @click="selectedIcon(item)">
                <span>{{ item[1] === '-' ? item.slice(2) : item }}</span>
                <svg-icon :icon-class="item" :style="'height: '+ height+';width: '+width+';'"/>
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
            default: '8rem'
        },
        width: {
            type: String,
            default: '10rem'
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
        filterIcons(value) {
            this.iconList = icons;
            if (!this.prefix) {
                this.iconList = this.iconList.filter(item => {
                    return item.slice(1, 2) !== '-';
                })
                return;
            }
            if (this.prefix +( value ? value.data : this.name)) {
                this.iconList = this.iconList.filter(item => item.includes((this.prefix || '') + (value ? (value.data || '') : this.name)));
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

<style lang="scss" rel="stylesheet/scss" scoped>
.icon-body {
    width: 100%;
    padding: 10px;

    .icon-list {
        display: flex;
        flex-wrap: wrap;
        &:before {
            content: '';
        }
        .icon {
            flex: 1;
            border: 1px solid #555;
            border-radius:0.5rem;
            margin-top: 1rem;
            margin-right: 1rem;
            display: flex;
            flex-flow: column;
            align-items: center;
            justify-content: center;
            padding: 1rem;
        }

        height: 200px;
        overflow-y: scroll;

        div {
            margin-bottom: -5px;
            cursor: pointer;
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
