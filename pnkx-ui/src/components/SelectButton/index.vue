<!--
 * @File: SelectButton
 * @Author: PHY
 * @Date: 2022/5/29 11:09
 * @Description: 选择按钮
-->
<template>
    <div :style="'bottom: '+bottom+'; right: '+right+';'" class="select-button">
        <el-dropdown trigger="click" @command="handleCommand">
              <span class="el-dropdown-link">
                {{buttonName}}
              </span>
            <el-dropdown-menu slot="dropdown">
                <el-dropdown-item v-for="select in selectList" :command="select.path" class="index-dropdown-item">{{ select.name }}</el-dropdown-item>
            </el-dropdown-menu>
        </el-dropdown>
    </div>
</template>

<script>
export default {
    name: "SelectButton",
    props: {
        // 按钮名称
        buttonName: {
            type: String,
            default: '按钮'
        },
        // 离底部距离
        bottom: {
            type: String,
            default: '6rem'
        },
        // 离右边距离
        right: {
            type: String,
            default: '4rem'
        },
        // 选项列表
        selectList: {
            type: Array,
            default: []
        },
    },
    data() {
        return {

        }
    },
    methods: {
        // 单击事件
        handleCommand(path) {
            this.$router.push(path)
        }
    }
}
</script>

<style lang='scss' scoped>
.select-button {
    font-family: 楷体;
    position: fixed;
    z-index: 9;
    .el-dropdown-link {
        cursor: pointer;
        color: #fff;
        background-color: $theme-blue;
        padding: 1rem;
        width: 6rem;
        height: 6rem;
        border-radius: 50%;
        display: flex;
        text-align: center;
        align-items: center;
        font-size: 1.4rem;
        font-weight: bold;
        box-shadow: 0 4px 6px $theme-blue;
        user-select: none;
        &:active {
            background-color: #5A8DEE5F;
        }
    }
}
</style>
