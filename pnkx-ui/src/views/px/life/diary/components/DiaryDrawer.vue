<!-- eslint-disable vue/no-mutating-props -->
<!--
 * @Author: PHY
 * @Description: 日记编辑抽屉（心情/天气图标选择 + 富文本）—— 从 index.vue 拆出
 * 说明：diary 对象按引用与父组件共享就地编辑；保存流程由父组件 before-close 处理，
 * 表单校验经 validate(callback) 暴露给父组件
-->
<template>
    <el-drawer
        :title="title"
        size="50%"
        destroy-on-close
        v-model="visibleModel"
        :before-close="(done) => $emit('before-close', done)"
        custom-class="modern-drawer"
    >
        <el-form ref="form" v-loading="saveLoading" :model="diary" :rules="rules"
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
            <el-form-item v-if="visible" prop="content">
                <editor ref="editor" v-model="diary.content" :height="600"/>
            </el-form-item>
        </el-form>
    </el-drawer>
</template>

<script>
/* eslint-disable vue/no-mutating-props */
import IconSelect from '@/components/IconSelect/index.vue'
import Editor from '@/components/Editor/index.vue'

export default {
    name: 'DiaryDrawer',
    components: { IconSelect, Editor },
    emits: ['before-close', 'update:visible'],
    props: {
        // 抽屉可见性（v-model:visible）
        visible: { type: Boolean, default: false },
        // 抽屉标题（新增/编辑）
        title: { type: String, default: '' },
        // 日记表单对象（按引用共享，就地编辑）
        diary: { type: Object, required: true },
        saveLoading: { type: Boolean, default: false },
        rules: { type: Object, default: () => ({}) }
    },
    computed: {
        visibleModel: {
            get() {
                return this.visible
            },
            set(value) {
                this.$emit('update:visible', value)
            }
        }
    },
    methods: {
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
         * 暴露表单校验给父组件（保存流程在父组件）
         */
        validate(callback) {
            this.$refs.form.validate(callback)
        }
    }
}
</script>

<style lang="scss" scoped>
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
</style>
