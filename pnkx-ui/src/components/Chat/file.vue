<!--
 * @File: file
 * @Author: 裴浩宇
 * @Date: 2023/6/7 15:25
 * @Description: 选择文件
-->
<template>
  <div class="app-container">
    <el-form :inline="true" :model="queryParams" label-width="68px" ref="queryForm" v-show="showSearch">
      <el-form-item label="上传端口" prop="port">
        <el-select placeholder="请选择上传端口" v-model="queryParams.port">
          <el-option
              :key="item"
              :label="item"
              :value="item"
              v-for="item in ['博客管理端', '博客客户端', '外部接口端']">
          </el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="文件名称" prop="name">
        <el-input
            @keyup.enter.native="handleQuery"
            clearable
            placeholder="请输入文件名称"
            size="small"
            v-model="queryParams.name"
        />
      </el-form-item>
      <el-form-item label="分类" prop="type">
        <el-select v-model="queryParams.type" placeholder="分类" clearable>
          <el-option
              v-for="dict in typeOptions"
              :key="dict.dictValue"
              :label="dict.dictLabel"
              :value="dict.dictValue"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="排序字段" prop="orderByColumn">
        <el-select placeholder="请选择排序字段" v-model="queryParams.orderByColumn" clearable @change="handleQuery">
          <el-option
              :key="item.value"
              :label="item.label"
              :value="item.value"
              v-for="item in [{label: '创建时间', value: 'createTime'},{label: '点赞', value: 'thumb'},{label: '浏览', value: 'browse'}]">
          </el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="排序类型" prop="isAsc">
        <el-select placeholder="请选择排序类型" v-model="queryParams.isAsc" clearable @change="handleQuery">
          <el-option
              :key="item.value"
              :label="item.label"
              :value="item.value"
              v-for="item in [{label: '升序', value: 'asc'},{label: '降序', value: 'desc'}]">
          </el-option>
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery" icon="Search" size="small" type="primary">搜索</el-button>
        <el-button @click="resetQuery" icon="Refresh" size="small">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table height="40vh" :data="fileList" @selection-change="handleSelectionChange" v-loading="loading">
      <el-table-column align="center" type="selection" width="55"/>
      <el-table-column align="center" label="文件缩略图">
        <template v-slot="scope">
          <el-image :preview-src-list="imageList"
                    v-if="scope.row.isPicture"
                    :src="scope.row.url"
                    fit="scale-down"
                    style="width: 3rem; height: 3rem;">
            <div slot="error" class="image-slot invalid-svg">
              <svg-icon icon-class="已失效2"/>
            </div>
          </el-image>
          <div v-else class="format">
            {{ scope.row.name.slice(scope.row.name.lastIndexOf('.') + 1) }}
          </div>
        </template>
      </el-table-column>
      <el-table-column align="center" label="文件名称" show-overflow-tooltip>
        <template v-slot="scope">
                    <span
                        @click="copyCode('https://pnkx.top/prod-api/profile'+scope.row.filePath.slice(21))"
                        class="theme-blue-text">{{ scope.row.name }}</span>
        </template>
      </el-table-column>
      <el-table-column align="center" label="上传端口" prop="port"/>
      <el-table-column align="center" label="分类" prop="type" :formatter="typeFormat"/>
      <el-table-column align="center" label="上传时间" prop="createTime"/>
      <el-table-column align="center" label="备注" prop="remark"/>
      <el-table-column align="center" class-name="small-padding fixed-width" label="操作" width="100">
        <template v-slot="scope">
          <el-button icon="Finished"
                     size="small"
                     type="text"
                     @click="handleSelect(scope.row)"
          >选择
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
        v-model:limit="queryParams.pageSize"
        v-model:page="queryParams.pageNum"
        :total="total"
        @pagination="getList"
        v-show="total>0"
    />
  </div>
</template>

<script>
import {listFile, getDicts} from "@/api/px/chat";

export default {
  name: "file",
  data() {
    return {
      // 图片格式
      photoFormat: ["bmp", "gif", "jpg", "jpeg", "png"],
      //图片列表
      imageList: [],
      // 遮罩层
      loading: true,
      // 选中数组
      ids: [],
      // 非单个禁用
      single: true,
      // 非多个禁用
      multiple: true,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 文件记录表格数据
      fileList: [],
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        orderByColumn: 'createTime',
        isAsc: 'desc',
        type: '',
        port: null,
        name: null,
        filePath: null,
        version: null,
      },
      // 类型列表
      typeOptions: []
    };
  },
  created() {
    this.getList();
    getDicts("sys_file_type").then(response => {
      this.typeOptions = response.data;
    });
  },
  methods: {
    /**
     * 选择
     */
    handleSelect(row) {
      this.$emit('select-file', row);
    },
    // 菜单状态字典翻译
    typeFormat(row, column) {
      if (!row.type) {
        return '暂未分类'
      }
      return this.selectDictLabel(this.typeOptions, row.type);
    },
    /**
     * 单击复制到粘贴板
     */
    copyCode(content) {
      this.$copyText(content).then(res => {
            this.$notify.success("已成功复制，可直接去粘贴");
          },
          err => {
            this.$notify.error("复制失败");
          })
    },
    /**
     * 判断是否是图片
     * @param name
     */
    judgePicture(name) {
      let result = false;
      this.photoFormat.forEach(item => {
        if (name.endsWith(item)) {
          result = true
        }
      })
      return result
    },
    /** 查询文件记录列表 */
    getList() {
      this.loading = true;
      listFile(this.queryParams).then(response => {
        this.fileList = response.rows;
        this.imageList = [];
        this.fileList.forEach(item => {
          if (this.judgePicture(item.name)) {
            item.isPicture = true;
            this.imageList.push(item.url);
          }
        })
        this.total = response.total;
        this.loading = false;
      });
    },
    // 表单重置
    reset() {
      this.form = {
        id: null,
        name: null,
        filePath: null,
        type: null,
        version: null,
        createBy: null,
        createTime: null,
        updateBy: null,
        updateTime: null,
        remark: null
      };
      this.resetForm("form");
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm("queryForm");
      this.handleQuery();
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.id)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
  }
}
</script>

<style lang="scss" scoped>
.format {
  background-color: var(--color-primary);
  padding: var(--space-2);
  color: var(--bg-card);
  border-radius: var(--radius-sm);
  font-size: var(--text-sm);
  display: inline-block;
}

.img-list {
  display: flex;
  flex-wrap: wrap;
  margin-top: var(--space-4);

  .image {
    width: 10rem;
    margin: 0 var(--space-4) var(--space-4) 0;
    border-radius: var(--radius-md);
    overflow: hidden;
    box-shadow: var(--shadow-sm);
    transition: box-shadow var(--duration-normal) var(--ease-default);

    &:hover {
      box-shadow: var(--shadow-md);
    }

    .image-name {
      margin-top: var(--space-2);
    }
  }
}
</style>

