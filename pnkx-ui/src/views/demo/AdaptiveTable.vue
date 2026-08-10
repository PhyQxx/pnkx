<template>
  <div class="app-container adaptive-container">
    <!-- 1. 搜索表单区域 (固定高度或自适应高度) -->
    <el-form :model="queryParams" ref="queryForm" :inline="true" class="search-form">
      <el-form-item label="名称" prop="name">
        <el-input v-model="queryParams.name" placeholder="请输入名称" clearable size="small" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" size="small" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" size="small" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 2. 操作按钮区域 (固定高度) -->
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" icon="Plus" size="small">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" icon="Edit" size="small">修改</el-button>
      </el-col>
    </el-row>

    <!-- 3. 表格区域 (自适应剩余高度) -->
    <div class="table-wrapper">
      <!-- 关键点：设置 height="100%" 使表格内部出现滚动条 -->
      <el-table :data="tableData" height="100%" v-loading="loading" border>
        <el-table-column type="index" label="序号" width="50" align="center" />
        <el-table-column prop="name" label="名称" align="center" />
        <el-table-column prop="desc" label="描述" align="center" show-overflow-tooltip />
        <el-table-column prop="time" label="创建时间" align="center" width="180" />
      </el-table>
    </div>

    <!-- 4. 分页区域 (固定高度) -->
    <div class="pagination-wrapper">
      <el-pagination
        :current-page="queryParams.pageNum"
        :page-size="queryParams.pageSize"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>
  </div>
</template>

<script>
export default {
  name: "AdaptiveTableDemo",
  data() {
    return {
      loading: false,
      total: 100,
      queryParams: {
        pageNum: 1,
        pageSize: 20,
        name: ""
      },
      // 模拟大量数据
      tableData: Array.from({ length: 50 }, (_, i) => ({
        id: i + 1,
        name: `测试数据 ${i + 1}`,
        desc: `这是一段很长的描述信息，用于测试自适应布局。测试数据编号为 ${i + 1}。`,
        time: "2024-05-20 12:00:00"
      }))
    };
  },
  methods: {
    handleQuery() {
      this.loading = true;
      setTimeout(() => { this.loading = false; }, 500);
    },
    resetQuery() {
      this.queryParams.name = "";
    },
    handleSizeChange(val) {
      this.queryParams.pageSize = val;
    },
    handleCurrentChange(val) {
      this.queryParams.pageNum = val;
    }
  }
};
</script>

<style lang="scss" scoped>
/* 核心样式 */
.adaptive-container {
  /* 1. 限制容器高度为视口高度减去顶部导航栏高度 */
  /* 假设 Navbar + TagsView 高度约为 86px，padding 为 20px * 2 */
  height: calc(100vh - 86px);
  display: flex;
  flex-direction: column;
  overflow: hidden; /* 禁止页面级滚动 */
  padding: 20px;
  box-sizing: border-box;
}

.search-form {
  flex-shrink: 0; /* 禁止表单区域被压缩 */
  margin-bottom: 15px;
}

.mb8 {
  flex-shrink: 0; /* 禁止按钮区域被压缩 */
  margin-bottom: 8px;
}

.table-wrapper {
  flex: 1; /* 表格区域占据剩余所有空间 */
  overflow: hidden; /* 必须设置，否则 flex: 1 无法正常工作且内部表格无法滚动 */
  position: relative;
}

.pagination-wrapper {
  flex-shrink: 0; /* 禁止分页区域被压缩 */
  padding-top: 15px;
  display: flex;
  justify-content: flex-end;
}

/* 兼容原有样式中的一些边距，确保布局紧凑 */
::v-deep .el-table {
  margin-bottom: 0 !important;
}
</style>
