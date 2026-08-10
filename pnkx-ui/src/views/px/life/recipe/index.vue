<!--
 * @File: recipe
 * @Author: PHY
 * @Date: 2026/07/04
 * @Description: 菜谱库 - 卡片网格 + 编辑弹窗(含食材管理)
-->
<template>
    <div class="app-container recipe-center">
        <!-- 搜索 / 工具栏 -->
        <div class="recipe-toolbar">
            <div class="toolbar-left">
                <h2 class="recipe-title">菜谱库</h2>
                <el-tag size="small" effect="plain">共 {{ total }} 个</el-tag>
            </div>
            <div class="toolbar-right">
                <el-input
                    v-model="queryParams.title"
                    placeholder="搜索菜谱名称..."
                    clearable
                    style="width: 220px"
                    @keyup.enter="handleQuery"
                    @clear="handleQuery"
                >
                    <template #prefix>
                        <el-icon><Search/></el-icon>
                    </template>
                </el-input>
                <el-button type="primary" icon="Plus" @click="handleAdd">新增菜谱</el-button>
            </div>
        </div>

        <!-- 卡片网格 -->
        <div v-loading="loading" class="recipe-grid">
            <el-row :gutter="16">
                <el-col
                    v-for="item in list"
                    :key="item.id"
                    :xs="24" :sm="12" :md="8" :lg="6"
                >
                    <div class="recipe-card" @click="handleEdit(item)">
                        <div class="card-head">
                            <span class="card-title">{{ item.title }}</span>
                            <el-tag v-if="item.servings" size="small" type="info" effect="plain">
                                {{ item.servings }} 人份
                            </el-tag>
                        </div>
                        <div v-if="item.url" class="card-url">
                            <el-icon><Link/></el-icon>
                            <a :href="item.url" target="_blank" rel="noopener" @click.stop>查看做法</a>
                        </div>
                        <div v-if="item.notes" class="card-notes">{{ item.notes }}</div>
                        <div class="card-foot">
                            <el-icon class="card-edit">
                                <EditPen/>
                            </el-icon>
                            <span class="card-foot-text">点击编辑</span>
                        </div>
                    </div>
                </el-col>
            </el-row>

            <div v-if="!loading && list.length === 0" class="empty-tip">
                <el-icon style="font-size: 40px;"><Bowl/></el-icon>
                <p>还没有菜谱，点击右上角新增吧</p>
            </div>
        </div>

        <pagination
            v-show="total > 0"
            :total="total"
            v-model:page="queryParams.pageNum"
            v-model:limit="queryParams.pageSize"
            @pagination="loadList"
        />

        <!-- 新增/编辑弹窗 -->
        <el-dialog
            v-model="dialog.visible"
            :title="dialog.isEdit ? '编辑菜谱' : '新增菜谱'"
            width="600px"
            append-to-body
        >
            <el-form :model="dialog.form" label-width="80px">
                <el-form-item label="名称">
                    <el-input v-model="dialog.form.title" placeholder="菜谱名称" maxlength="50" show-word-limit/>
                </el-form-item>
                <el-form-item label="链接">
                    <el-input v-model="dialog.form.url" placeholder="做法链接（可选）">
                        <template #prefix>
                            <el-icon><Link/></el-icon>
                        </template>
                    </el-input>
                </el-form-item>
                <el-form-item label="份数">
                    <el-input-number v-model="dialog.form.servings" :min="1" :max="99" controls-position="right"/>
                    <span class="form-hint">人份</span>
                </el-form-item>
                <el-form-item label="备注">
                    <el-input
                        v-model="dialog.form.notes"
                        type="textarea"
                        :rows="2"
                        placeholder="备注（可选）"
                    />
                </el-form-item>
                <el-form-item label="食材">
                    <div class="ingredient-box">
                        <div class="ingredient-head">
                            <span>名称</span>
                            <span>用量</span>
                            <span class="ingredient-op-head">操作</span>
                        </div>
                        <div
                            v-for="(ing, idx) in dialog.form.ingredients"
                            :key="idx"
                            class="ingredient-row"
                        >
                            <el-input v-model="ing.name" placeholder="如：番茄" size="small"/>
                            <el-input v-model="ing.quantity" placeholder="如：2个" size="small"/>
                            <el-icon class="ingredient-del" @click="removeIngredientRow(idx)">
                                <Delete/>
                            </el-icon>
                        </div>
                        <div v-if="!dialog.form.ingredients.length" class="ingredient-empty">
                            暂无食材
                        </div>
                        <el-button
                            size="small"
                            type="primary"
                            plain
                            icon="Plus"
                            class="ingredient-add"
                            @click="addIngredientRow"
                        >
                            添加食材
                        </el-button>
                    </div>
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="dialog.visible = false">取消</el-button>
                <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script>
import {Search, Plus, Link, EditPen, Delete, Bowl} from '@element-plus/icons-vue'
import {
    listRecipe,
    getRecipeWithIngredients,
    addRecipe,
    updateRecipe,
    delRecipe
} from '@/api/px/life/shopping'

export default {
    name: 'RecipeLibrary',
    components: {Search, Plus, Link, EditPen, Delete, Bowl},
    data() {
        return {
            loading: false,
            saving: false,
            list: [],
            total: 0,
            queryParams: {
                pageNum: 1,
                pageSize: 12,
                title: undefined
            },
            dialog: {
                visible: false,
                isEdit: false,
                form: this.emptyForm()
            }
        }
    },
    created() {
        this.loadList()
    },
    methods: {
        emptyForm() {
            return {
                id: undefined,
                title: '',
                url: '',
                servings: 1,
                notes: '',
                ingredients: []
            }
        },
        /** 加载列表 */
        loadList() {
            this.loading = true
            listRecipe(this.queryParams).then(res => {
                this.list = res.rows || []
                this.total = res.total || 0
            }).finally(() => {
                this.loading = false
            })
        },
        handleQuery() {
            this.queryParams.pageNum = 1
            this.loadList()
        },
        /** 新增 */
        handleAdd() {
            this.dialog.isEdit = false
            this.dialog.form = this.emptyForm()
            this.dialog.visible = true
        },
        /** 编辑（带食材） */
        handleEdit(item) {
            this.loading = true
            getRecipeWithIngredients(item.id).then(res => {
                const data = res.data || {}
                this.dialog.isEdit = true
                this.dialog.form = {
                    id: data.id,
                    title: data.title || '',
                    url: data.url || '',
                    servings: data.servings || 1,
                    notes: data.notes || '',
                    ingredients: (data.ingredients || []).map(i => ({
                        id: i.id,
                        name: i.name || '',
                        quantity: i.quantity || ''
                    }))
                }
                this.dialog.visible = true
            }).finally(() => {
                this.loading = false
            })
        },
        /** 保存 */
        handleSave() {
            if (!this.dialog.form.title || !this.dialog.form.title.trim()) {
                this.$message.warning('请输入菜谱名称')
                return
            }
            // 过滤掉空行食材
            const payload = {
                ...this.dialog.form,
                title: this.dialog.form.title.trim(),
                ingredients: (this.dialog.form.ingredients || []).filter(
                    i => (i.name && i.name.trim()) || (i.quantity && i.quantity.trim())
                )
            }
            this.saving = true
            const action = this.dialog.isEdit ? updateRecipe(payload) : addRecipe(payload)
            action.then(() => {
                this.$message.success('保存成功')
                this.dialog.visible = false
                this.loadList()
            }).finally(() => {
                this.saving = false
            })
        },
        /** 删除 */
        handleDelete(item) {
            this.$modal.confirm(`确认删除菜谱「${item.title}」？`).then(() => {
                return delRecipe(item.id)
            }).then(() => {
                this.$modal.msgSuccess('删除成功')
                this.loadList()
            }).catch(() => {
            })
        },
        addIngredientRow() {
            this.dialog.form.ingredients.push({name: '', quantity: ''})
        },
        removeIngredientRow(idx) {
            this.dialog.form.ingredients.splice(idx, 1)
        }
    }
}
</script>

<style lang="scss" scoped>
.recipe-center {
    .recipe-toolbar {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 16px;
        flex-wrap: wrap;
        gap: 8px;

        .toolbar-left {
            display: flex;
            align-items: center;
            gap: 10px;

            .recipe-title {
                margin: 0;
                font-size: 18px;
                font-weight: 600;
                color: var(--pnkx-text, #303133);
            }
        }

        .toolbar-right {
            display: flex;
            gap: 8px;
            align-items: center;
        }
    }

    .recipe-grid {
        min-height: 200px;
    }

    .recipe-card {
        background: var(--pnkx-surface, #fff);
        border: 1px solid var(--pnkx-border, #ebeef5);
        border-radius: var(--pnkx-radius-md, 8px);
        padding: 14px;
        margin-bottom: 16px;
        cursor: pointer;
        display: flex;
        flex-direction: column;
        gap: 8px;
        transition: box-shadow 0.16s, transform 0.16s, border-color 0.16s;
        min-height: 130px;

        &:hover {
            box-shadow: var(--pnkx-shadow-2, 0 4px 16px rgba(0, 0, 0, 0.08));
            transform: translateY(-2px);
            border-color: var(--pnkx-primary, #409eff);
        }

        .card-head {
            display: flex;
            justify-content: space-between;
            align-items: flex-start;
            gap: 8px;

            .card-title {
                font-size: 15px;
                font-weight: 600;
                color: var(--pnkx-text, #303133);
                line-height: 1.5;
                word-break: break-all;
            }
        }

        .card-url {
            display: flex;
            align-items: center;
            gap: 4px;
            font-size: 12px;
            color: var(--pnkx-primary, #409eff);

            a {
                color: inherit;
                text-decoration: none;
                overflow: hidden;
                text-overflow: ellipsis;
                white-space: nowrap;

                &:hover {
                    text-decoration: underline;
                }
            }
        }

        .card-notes {
            font-size: 13px;
            color: var(--pnkx-text-secondary, #909399);
            line-height: 1.5;
            display: -webkit-box;
            -webkit-line-clamp: 2;
            -webkit-box-orient: vertical;
            overflow: hidden;
            flex: 1;
        }

        .card-foot {
            display: flex;
            align-items: center;
            gap: 4px;
            font-size: 12px;
            color: var(--pnkx-text-placeholder, #c0c4cc);
            border-top: 1px dashed var(--pnkx-border, #ebeef5);
            padding-top: 8px;
        }
    }

    .empty-tip {
        text-align: center;
        color: var(--pnkx-text-placeholder, #c0c4cc);
        padding: 60px 0;
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 12px;

        p {
            margin: 0;
            font-size: 14px;
        }
    }

    /* ===== 食材子表 ===== */
    .ingredient-box {
        width: 100%;
        border: 1px solid var(--pnkx-border, #ebeef5);
        border-radius: 6px;
        padding: 10px;

        .ingredient-head {
            display: grid;
            grid-template-columns: 1fr 1fr 50px;
            gap: 8px;
            font-size: 12px;
            color: var(--pnkx-text-secondary, #909399);
            padding: 0 4px 6px;
            border-bottom: 1px dashed var(--pnkx-border, #ebeef5);
            margin-bottom: 6px;

            .ingredient-op-head {
                text-align: center;
            }
        }

        .ingredient-row {
            display: grid;
            grid-template-columns: 1fr 1fr 50px;
            gap: 8px;
            align-items: center;
            margin-bottom: 6px;

            .ingredient-del {
                text-align: center;
                justify-self: center;
                font-size: 16px;
                color: var(--pnkx-text-placeholder, #c0c4cc);
                cursor: pointer;
                transition: color 0.16s;

                &:hover {
                    color: var(--pnkx-danger, #f56c6c);
                }
            }
        }

        .ingredient-empty {
            text-align: center;
            color: var(--pnkx-text-placeholder, #c0c4cc);
            font-size: 12px;
            padding: 8px 0;
        }

        .ingredient-add {
            width: 100%;
            margin-top: 4px;
            border-style: dashed;
        }
    }

    .form-hint {
        margin-left: 8px;
        color: var(--pnkx-text-secondary, #909399);
        font-size: 13px;
    }
}
</style>
