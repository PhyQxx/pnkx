<template>
    <div class="app-container">
        <el-form :model="queryParams" ref="queryForm" :inline="true" v-show="showSearch" label-width="78px">
            <el-form-item label="标题" prop="title">
                <el-input
                    v-model="queryParams.title"
                    placeholder="请输入标题"
                    clearable
                    size="small"
                    @keyup.enter.native="handleQuery"
                />
            </el-form-item>
            <el-form-item label="云盘" prop="diskType">
                <el-select v-model="queryParams.diskType" placeholder="请选择云盘" clearable size="small">
                    <el-option v-for="item in diskOptions" :key="item" :label="item" :value="item"/>
                </el-select>
            </el-form-item>
            <el-form-item label="类型" prop="resourceType">
                <el-select v-model="queryParams.resourceType" placeholder="请选择类型" clearable size="small">
                    <el-option v-for="item in typeOptions" :key="item" :label="item" :value="item"/>
                </el-select>
            </el-form-item>
            <el-form-item label="标签" prop="tags">
                <el-select v-model="queryParams.tags" placeholder="请选择标签" clearable size="small">
                    <el-option v-for="item in labelOptions" :key="item" :label="item" :value="item"/>
                </el-select>
            </el-form-item>
            <el-form-item label="状态" prop="status">
                <el-select v-model="queryParams.status" placeholder="请选择状态" clearable size="small">
                    <el-option label="启用" value="1"/>
                    <el-option label="停用" value="0"/>
                </el-select>
            </el-form-item>
            <el-form-item>
                <el-button type="primary" icon="Search" size="small" @click="handleQuery">搜索</el-button>
                <el-button icon="Refresh" size="small" @click="resetQuery">重置</el-button>
            </el-form-item>
        </el-form>

        <el-row :gutter="10" class="mb8">
            <el-col :span="1.5">
                <el-button
                    type="primary"
                    icon="Plus"
                    size="small"
                    @click="handleAdd"
                    v-hasPermi="['px:share:add']"
                >新增
                </el-button>
            </el-col>
            <el-col :span="1.5">
                <el-button
                    type="success"
                    icon="Edit"
                    size="small"
                    :disabled="single"
                    @click="handleUpdate"
                    v-hasPermi="['px:share:edit']"
                >修改
                </el-button>
            </el-col>
            <el-col :span="1.5">
                <el-button
                    type="danger"
                    icon="Delete"
                    size="small"
                    :disabled="multiple"
                    @click="handleDelete"
                    v-hasPermi="['px:share:remove']"
                >删除
                </el-button>
            </el-col>
            <el-col :span="1.5">
                <el-button
                    type="warning"
                    icon="Download"
                    size="small"
                    @click="handleExport"
                    v-hasPermi="['px:share:export']"
                >导出
                </el-button>
            </el-col>
            <el-col :span="1.5">
                <el-button
                    type="info"
                    icon="Document"
                    size="small"
                    @click="handleImport"
                    v-hasPermi="['px:share:add']"
                >文本导入
                </el-button>
            </el-col>
            <right-toolbar :showSearch="showSearch" @queryTable="getList"></right-toolbar>
        </el-row>

        <div class="table-main-area">
            <el-table v-loading="loading" :data="shareList" @selection-change="handleSelectionChange">
                <el-table-column type="selection" width="55" align="center"/>
                <el-table-column label="标题" align="center" prop="title" min-width="130"/>
                <el-table-column label="封面" align="center" prop="cover" width="100">
                    <template v-slot="scope">
                        <el-image
                            v-if="scope.row.cover"
                            :preview-src-list="[scope.row.cover]"
                            :src="scope.row.cover"
                            fit="cover"
                            style="width: 4rem; height: 3rem; border-radius: .25rem;"
                        />
                        <span v-else>-</span>
                    </template>
                </el-table-column>
                <el-table-column label="云盘" align="center" prop="diskType" width="110"/>
                <el-table-column label="类型" align="center" prop="resourceType" width="90"/>
                <el-table-column label="链接" align="center" prop="shareUrl" min-width="220">
                    <template v-slot="scope">
                        <span class="theme-blue-text" @click="handleOpenLink(scope.row.shareUrl)">
                            {{ scope.row.shareUrl }}
                        </span>
                    </template>
                </el-table-column>
                <el-table-column label="提取码" align="center" prop="extractCode" width="90"/>
                <el-table-column label="二维码" align="center" prop="qrCode" width="100">
                    <template v-slot="scope">
                        <el-image
                            v-if="scope.row.qrCode"
                            :preview-src-list="[scope.row.qrCode]"
                            :src="scope.row.qrCode"
                            fit="scale-down"
                            style="width: 3.5rem; height: 3.5rem;"
                        />
                        <span v-else>-</span>
                    </template>
                </el-table-column>
                <el-table-column label="标签" align="center" prop="tags" min-width="130">
                    <template v-slot="scope">
                        <el-tag
                            v-for="tag in splitTags(scope.row.tags)"
                            :key="tag"
                            size="small"
                            style="margin-right: .25rem;"
                        >{{ tag }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="排序" align="center" prop="sortOrder" width="80"/>
                <el-table-column label="点击次数" align="center" prop="clickCount" width="100"/>
                <el-table-column label="状态" align="center" prop="status" width="90">
                    <template v-slot="scope">
                        <el-tag :type="scope.row.status === '1' ? 'success' : 'info'">
                            {{ scope.row.status === '1' ? '启用' : '停用' }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="备注" align="center" prop="remark" min-width="160"/>
                <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="210">
                    <template v-slot="scope">
                        <el-button
                            size="small"
                            type="text"
                            icon="CopyDocument"
                            @click="handleCopy(scope.row)"
                        >复制
                        </el-button>
                        <el-button
                            size="small"
                            type="text"
                            icon="Edit"
                            @click="handleUpdate(scope.row)"
                            v-hasPermi="['px:share:edit']"
                        >修改
                        </el-button>
                        <el-button
                            size="small"
                            type="text"
                            icon="Delete"
                            @click="handleDelete(scope.row)"
                            v-hasPermi="['px:share:remove']"
                        >删除
                        </el-button>
                    </template>
                </el-table-column>
            </el-table>
        </div>

        <pagination
            v-if="total>0"
            :total="total"
            v-model:page="queryParams.pageNum"
            v-model:limit="queryParams.pageSize"
            @pagination="getList"
        />

        <el-dialog :title="title" v-model="open" width="720px" append-to-body>
            <el-form ref="form" :model="form" :rules="rules" label-width="90px">
                <el-form-item label="标题" prop="title">
                    <el-input v-model="form.title" placeholder="请输入标题"/>
                </el-form-item>
                <el-form-item label="云盘" prop="diskType">
                    <el-select v-model="form.diskType" filterable allow-create placeholder="请选择或输入云盘">
                        <el-option v-for="item in diskOptions" :key="item" :label="item" :value="item"/>
                    </el-select>
                </el-form-item>
                <el-form-item label="类型" prop="resourceType">
                    <el-select v-model="form.resourceType" filterable allow-create placeholder="请选择或输入类型">
                        <el-option v-for="item in typeOptions" :key="item" :label="item" :value="item"/>
                    </el-select>
                </el-form-item>
                <el-form-item label="链接" prop="shareUrl">
                    <el-input v-model="form.shareUrl" placeholder="请输入分享链接"/>
                </el-form-item>
                <el-form-item label="封面" prop="cover">
                    <div class="share-cover-editor">
                        <imageUpload v-model="form.cover" image-type="share"/>
                        <el-input
                            v-model="form.cover"
                            class="cover-url-input"
                            clearable
                            placeholder="也可以手动输入封面 URL"
                        />
                    </div>
                </el-form-item>
                <el-form-item label="提取码" prop="extractCode">
                    <el-input v-model="form.extractCode" placeholder="请输入提取码"/>
                </el-form-item>
                <el-form-item label="二维码" prop="qrCode">
                    <el-input v-model="form.qrCode" placeholder="请输入二维码图片地址"/>
                    <el-image
                        v-if="form.qrCode"
                        class="qr-preview"
                        :preview-src-list="[form.qrCode]"
                        :src="form.qrCode"
                        fit="scale-down"
                    />
                </el-form-item>
                <el-form-item label="标签" prop="tags">
                    <el-select
                        v-model="tagValues"
                        multiple
                        filterable
                        allow-create
                        default-first-option
                        placeholder="请选择或输入标签"
                    >
                        <el-option v-for="item in labelOptions" :key="item" :label="item" :value="item"/>
                    </el-select>
                </el-form-item>
                <el-form-item label="排序" prop="sortOrder">
                    <el-input-number v-model="form.sortOrder" :min="0" :max="9999"/>
                </el-form-item>
                <el-form-item label="状态" prop="status">
                    <el-radio-group v-model="form.status">
                        <el-radio label="1">启用</el-radio>
                        <el-radio label="0">停用</el-radio>
                    </el-radio-group>
                </el-form-item>
                <el-form-item label="备注" prop="remark">
                    <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入备注"/>
                </el-form-item>
            </el-form>
            <template #footer>
                <div class="dialog-footer">
                    <el-button type="primary" @click="submitForm">确 定</el-button>
                    <el-button @click="cancel">取 消</el-button>
                </div>
            </template>
        </el-dialog>

        <el-dialog title="分享文本导入" v-model="importOpen" width="90vw" append-to-body>
            <div class="import-layout">
                <div class="import-paste-area">
                    <div
                        v-for="(item, index) in importTexts"
                        :key="item.id"
                        class="import-text-item"
                    >
                        <div class="import-text-header">
                            <span>分享文本 {{ index + 1 }}</span>
                            <el-button
                                v-if="importTexts.length > 1"
                                type="danger"
                                text
                                icon="Delete"
                                @click="removeImportText(index)"
                            >移除
                            </el-button>
                        </div>
                        <el-input
                            v-model="item.content"
                            type="textarea"
                            :rows="7"
                            placeholder="粘贴网盘分享文案，例如：通过百度网盘分享的图片：妖娆御姐&#10;链接:https://pan.baidu.com/s/...&#10;提取码:mb3o&#10;复制这段内容打开「百度网盘APP 即可获取」"
                        />
                    </div>
                    <div class="import-actions">
                        <el-button icon="Plus" @click="addImportText">添加文本域</el-button>
                        <el-button type="primary" icon="Reading" @click="parseImportTexts">解析成数据</el-button>
                    </div>
                </div>
                <div class="import-result-area">
                    <el-table :data="importRows" border height="560">
                        <el-table-column label="标题" min-width="160">
                            <template v-slot="scope">
                                <el-input
                                    v-model="scope.row.title"
                                    placeholder="标题"
                                    @blur="syncImportRowTags(scope.row, $event)"
                                />
                            </template>
                        </el-table-column>
                        <el-table-column label="云盘" width="140">
                            <template v-slot="scope">
                                <el-select v-model="scope.row.diskType" filterable allow-create>
                                    <el-option v-for="item in diskOptions" :key="item" :label="item" :value="item"/>
                                </el-select>
                            </template>
                        </el-table-column>
                        <el-table-column label="类型" width="120">
                            <template v-slot="scope">
                                <el-select v-model="scope.row.resourceType" filterable allow-create>
                                    <el-option v-for="item in typeOptions" :key="item" :label="item" :value="item"/>
                                </el-select>
                            </template>
                        </el-table-column>
                        <el-table-column label="链接" min-width="220">
                            <template v-slot="scope">
                                <el-input v-model="scope.row.shareUrl" placeholder="分享链接"/>
                            </template>
                        </el-table-column>
                        <el-table-column label="提取码" width="110">
                            <template v-slot="scope">
                                <el-input v-model="scope.row.extractCode" placeholder="提取码"/>
                            </template>
                        </el-table-column>
                        <el-table-column label="封面地址" min-width="180">
                            <template v-slot="scope">
                                <el-input v-model="scope.row.cover" placeholder="可手动填封面 URL"/>
                            </template>
                        </el-table-column>
                        <el-table-column label="标签" min-width="160">
                            <template v-slot="scope">
                                <el-input v-model="scope.row.tags" placeholder="英文逗号分隔"/>
                            </template>
                        </el-table-column>
                        <el-table-column label="排序" width="90">
                            <template v-slot="scope">
                                <el-input-number v-model="scope.row.sortOrder" :min="0" :max="9999" size="small"/>
                            </template>
                        </el-table-column>
                        <el-table-column label="备注" min-width="220">
                            <template v-slot="scope">
                                <el-input v-model="scope.row.remark" type="textarea" :rows="2"/>
                            </template>
                        </el-table-column>
                        <el-table-column label="操作" width="80" fixed="right">
                            <template v-slot="scope">
                                <el-button type="danger" text icon="Delete" @click="removeImportRow(scope.$index)">移除</el-button>
                            </template>
                        </el-table-column>
                    </el-table>
                </div>
            </div>
            <template #footer>
                <div class="dialog-footer">
                    <el-button @click="importOpen = false">取 消</el-button>
                    <el-button type="primary" :loading="importLoading" @click="submitImportRows">批量新增</el-button>
                </div>
            </template>
        </el-dialog>
    </div>
</template>

<script>
import {
    listShare,
    getShare,
    delShare,
    addShare,
    updateShare,
    exportShare,
    getShareLabelList
} from "@/api/px/blog/share";
import ImageUpload from '@/components/ImageUpload/index.vue';

export default {
    name: "Share",
    components: {
        ImageUpload
    },
    data() {
        return {
            defaultRemark: '复制这段内容打开「百度网盘APP 即可获取」',
            loading: true,
            ids: [],
            single: true,
            multiple: true,
            showSearch: true,
            total: 0,
            shareList: [],
            labelOptions: [],
            diskOptions: ['百度网盘', '阿里云盘', '夸克网盘', '迅雷云盘', '其它'],
            typeOptions: ['文件', '文件夹', '链接', '图片', '视频', '其它'],
            title: "",
            open: false,
            importOpen: false,
            importLoading: false,
            importTexts: [],
            importRows: [],
            tagValues: [],
            queryParams: {
                pageNum: 1,
                pageSize: 10,
                title: null,
                diskType: null,
                resourceType: null,
                tags: null,
                status: null
            },
            form: {},
            rules: {
                title: [
                    {required: true, message: "标题不能为空", trigger: "blur"}
                ],
                diskType: [
                    {required: true, message: "云盘不能为空", trigger: "change"}
                ],
                resourceType: [
                    {required: true, message: "类型不能为空", trigger: "change"}
                ],
                shareUrl: [
                    {required: true, message: "分享链接不能为空", trigger: "blur"}
                ]
            }
        };
    },
    created() {
        this.getList();
        this.getLabelList();
    },
    methods: {
        getLabelList() {
            getShareLabelList().then(res => {
                this.labelOptions = res.data || [];
            })
        },
        splitTags(tags) {
            return tags ? tags.split(',').filter(Boolean) : [];
        },
        buildShareText(row) {
            const type = row.resourceType || '文件';
            const lines = [
                `通过${row.diskType || '网盘'}分享的${type}：${row.title || ''}`,
                `链接:${row.shareUrl || ''}`
            ];
            if (row.extractCode) lines.push(`提取码:${row.extractCode}`);
            lines.push(row.remark || this.defaultRemark);
            return lines.join('\n');
        },
        handleOpenLink(url) {
            if (url) window.open(url, '_blank');
        },
        handleCopy(row) {
            this.$copyText(this.buildShareText(row));
        },
        createImportText(content = '') {
            return {
                id: `${Date.now()}-${Math.random()}`,
                content
            };
        },
        handleImport() {
            this.importTexts = [this.createImportText()];
            this.importRows = [];
            this.importOpen = true;
        },
        addImportText() {
            this.importTexts.push(this.createImportText());
        },
        removeImportText(index) {
            this.importTexts.splice(index, 1);
        },
        removeImportRow(index) {
            this.importRows.splice(index, 1);
        },
        normalizeImportText(text) {
            return (text || '')
                .replace(/\u00a0/g, ' ')
                .replace(/\r\n/g, '\n')
                .trim();
        },
        detectDiskType(text) {
            if (text.includes('百度网盘') || text.includes('百度云盘')) return '百度网盘';
            if (text.includes('阿里云盘')) return '阿里云盘';
            if (text.includes('夸克网盘')) return '夸克网盘';
            if (text.includes('迅雷云盘')) return '迅雷云盘';
            return '其它';
        },
        splitTitleTags(title) {
            return (title || '')
                .split('-')
                .map(item => item.trim())
                .filter(Boolean);
        },
        syncImportRowTags(row, event) {
            const title = event?.target?.value ?? row.title;
            row.title = title;
            const titleTags = this.splitTitleTags(title);
            if (titleTags.length) {
                row.tags = titleTags.join(',');
            }
        },
        parseShareText(text, index) {
            const content = this.normalizeImportText(text);
            if (!content) return null;
            const headerMatch = content.match(/通过(.+?)分享的(.+?)[：:](.+)/);
            const urlMatch = content.match(/链接\s*[：:]\s*(https?:\/\/\S+)/i);
            const codeMatch = content.match(/提取码\s*[：:]\s*([^\s\n]+)/i);
            const remarkMatch = content.match(/(复制这段内容打开.+)$/m);
            const diskType = '百度网盘';
            const resourceType = '图片';
            const title = headerMatch ? headerMatch[3].trim() : '';
            const titleTags = this.splitTitleTags(title);
            const tags = (titleTags.length ? titleTags : [diskType, '分享']).filter(Boolean).join(',');
            return {
                title,
                diskType,
                resourceType,
                shareUrl: urlMatch ? urlMatch[1].trim() : '',
                cover: null,
                extractCode: codeMatch ? codeMatch[1].trim() : null,
                qrCode: null,
                tags,
                sortOrder: index + 1,
                status: '1',
                remark: remarkMatch ? remarkMatch[1].trim() : this.defaultRemark
            };
        },
        parseImportTexts() {
            const rows = this.importTexts
                .map((item, index) => this.parseShareText(item.content, index))
                .filter(Boolean);
            if (!rows.length) {
                this.msgInfo('请先粘贴至少一条分享文本');
                return;
            }
            this.importRows = rows;
        },
        validateImportRows() {
            const invalidIndex = this.importRows.findIndex(row => !row.title || !row.diskType || !row.resourceType || !row.shareUrl);
            if (invalidIndex !== -1) {
                this.msgInfo(`第 ${invalidIndex + 1} 条数据缺少标题、云盘、类型或链接`);
                return false;
            }
            return true;
        },
        async submitImportRows() {
            if (!this.importRows.length) {
                this.msgInfo('请先解析分享文本');
                return;
            }
            if (!this.validateImportRows()) return;
            this.importLoading = true;
            try {
                for (const row of this.importRows) {
                    await addShare({
                        ...row,
                        remark: row.remark || this.defaultRemark
                    });
                }
                this.msgSuccess('批量新增成功');
                this.importOpen = false;
                this.getList();
                this.getLabelList();
            } finally {
                this.importLoading = false;
            }
        },
        getList() {
            this.loading = true;
            listShare(this.queryParams).then(response => {
                this.shareList = response.rows;
                this.total = response.total;
                this.loading = false;
            });
        },
        cancel() {
            this.open = false;
            this.reset();
        },
        reset() {
            this.tagValues = [];
            this.form = {
                id: null,
                title: null,
                diskType: '百度网盘',
                resourceType: '文件',
                shareUrl: null,
                cover: null,
                extractCode: null,
                qrCode: null,
                tags: null,
                sortOrder: 0,
                clickCount: 0,
                status: '1',
                delFlag: null,
                version: null,
                createBy: null,
                createTime: null,
                updateBy: null,
                updateTime: null,
                remark: this.defaultRemark
            };
            this.resetForm("form");
        },
        handleQuery() {
            this.queryParams.pageNum = 1;
            this.getList();
        },
        resetQuery() {
            this.resetForm("queryForm");
            this.handleQuery();
        },
        handleSelectionChange(selection) {
            this.ids = selection.map(item => item.id);
            this.single = selection.length !== 1;
            this.multiple = !selection.length;
        },
        handleAdd() {
            this.reset();
            this.open = true;
            this.title = "添加分享资源";
        },
        handleUpdate(row) {
            this.reset();
            const id = row.id || this.ids;
            getShare(id).then(response => {
                this.form = response.data;
                this.tagValues = this.splitTags(this.form.tags);
                this.open = true;
                this.title = "修改分享资源";
            });
        },
        submitForm() {
            this.$refs["form"].validate(valid => {
                if (valid) {
                    this.form.tags = this.tagValues.join(',');
                    if (this.form.id != null) {
                        updateShare(this.form).then(() => {
                            this.msgSuccess("修改成功");
                            this.open = false;
                            this.getList();
                            this.getLabelList();
                        });
                    } else {
                        addShare(this.form).then(() => {
                            this.msgSuccess("新增成功");
                            this.open = false;
                            this.getList();
                            this.getLabelList();
                        });
                    }
                }
            });
        },
        handleDelete(row) {
            const ids = row.id || this.ids;
            this.$confirm('是否确认删除分享资源编号为"' + ids + '"的数据项?', "警告", {
                confirmButtonText: "确定",
                cancelButtonText: "取消",
                type: "warning"
            }).then(function () {
                return delShare(ids);
            }).then(() => {
                this.getList();
                this.msgSuccess("删除成功");
            })
        },
        handleExport() {
            const queryParams = this.queryParams;
            this.$confirm('是否确认导出所有分享资源数据项?', "警告", {
                confirmButtonText: "确定",
                cancelButtonText: "取消",
                type: "warning"
            }).then(function () {
                return exportShare(queryParams);
            }).then(response => {
                this.download(response.msg);
            })
        }
    }
};
</script>

<style lang="scss" scoped>
@import "@/assets/styles/mixin.scss";

.app-container {
    @include adaptive-table-layout(96px);
    padding: var(--space-4);
    background: var(--bg-body);
}

.theme-blue-text {
    color: var(--color-primary);
    cursor: pointer;
    text-decoration: underline;

    &:hover {
        color: var(--color-primary-600);
    }
}

.qr-preview {
    display: block;
    width: 6rem;
    height: 6rem;
    margin-top: .75rem;
    border-radius: var(--radius-md);
}

.import-layout {
    display: grid;
    grid-template-columns: minmax(18rem, 26rem) minmax(0, 1fr);
    gap: var(--space-4);
}

.import-paste-area {
    display: flex;
    flex-direction: column;
    gap: var(--space-3);
    max-height: 560px;
    overflow: auto;
    padding-right: .25rem;
}

.import-text-item {
    padding: var(--space-3);
    border: 1px solid var(--border-light);
    border-radius: var(--radius-md);
    background: var(--bg-card);
}

.import-text-header,
.import-actions {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: var(--space-2);
    margin-bottom: var(--space-2);
}

.import-result-area {
    min-width: 0;
}

.share-cover-editor {
    width: 100%;
}

.cover-url-input {
    margin-top: var(--space-2);
    max-width: 32rem;
}

@media (max-width: 960px) {
    .import-layout {
        grid-template-columns: 1fr;
    }
}
</style>
