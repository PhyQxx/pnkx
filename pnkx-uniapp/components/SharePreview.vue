<template>
  <view @click.stop
    ><button size="mini" @click="open">预览我的共享记录与引用</button
    ><view v-if="visible" class="preview"
      ><button size="mini" @click="visible = false">收起</button
      ><text>按业务域共享现有及未来记录。这里只展示你自己的内容，不是逐条授权清单。</text
      ><picker
        :range="moduleLabels"
        @change="
          module = modules[Number($event.detail.value)];
          page = 1;
          load();
        "
        ><view>当前范围：{{ labels[module] || "未选择" }}</view></picker
      ><text v-if="error">{{ error }}</text
      ><text>{{ data.referenceNotice }}</text
      ><view v-for="item in data.items || []" :key="item.id" class="object"
        ><text>记录 #{{ item.id }}</text
        ><view v-for="(value, key) in item.fields" :key="key"
          ><text
            >{{ fieldLabels[key] || key }}：{{ value == null ? "—" : String(value) }}</text
          ></view
        ><text v-if="item.truncated">长正文已截断，其余内容仍可能共享，请核对原记录。</text
        ><view v-if="item.references.length"
          ><text>识别到的引用（未加载）</text
          ><text v-for="ref in item.references" :key="ref">{{ ref }}</text></view
        ></view
      ><text v-if="!loading && !(data.items || []).length">此范围暂无记录</text
      ><button
        size="mini"
        :disabled="page === 1 || loading"
        @click="
          page--;
          load();
        "
      >
        上一页</button
      ><text>第{{ page }}页 · 共{{ data.total || 0 }}条</text
      ><button
        size="mini"
        :disabled="page * 20 >= (data.total || 0) || loading"
        @click="
          page++;
          load();
        "
      >
        下一页
      </button></view
    ></view
  >
</template>
<script>
import request from "@/utils/request";
export default {
  props: {
    modules: { type: Array, default: () => [] },
    groupId: { type: [Number, String], default: null },
  },
  data: () => ({
    visible: false,
    module: "",
    page: 1,
    loading: false,
    error: "",
    data: {},
    serial: 0,
    labels: {
      todo: "待办",
      shopping: "购物条目",
      meal: "餐饮",
      commemoration: "纪念日",
      budget: "预算",
    },
    fieldLabels: {
      id: "编号",
      content: "内容",
      performer: "执行者",
      plan_start_time: "开始时间",
      plan_end_time: "截止时间",
      status: "状态",
      label: "标签",
      remark: "备注",
      list_id: "清单编号",
      name: "名称",
      quantity: "数量",
      fulfillment_state: "采购状态",
      claimed_by: "认领人",
      title: "标题",
      plan_date: "日期",
      meal_type: "餐次",
      servings: "份数",
      notes: "说明",
      date: "日期",
      repeat: "每年重复",
      month: "月份",
      type_id: "分类编号",
      amount: "预算金额",
    },
  }),
  computed: {
    moduleLabels() {
      return this.modules.map((m) => this.labels[m] || m);
    },
  },
  watch: {
    groupId() {
      this.serial++;
      this.visible = false;
      this.data = {};
    },
    modules() {
      this.serial++;
      this.visible = false;
      this.data = {};
    },
  },
  methods: {
    async open() {
      this.module = this.modules[0] || "";
      this.page = 1;
      this.visible = true;
      await this.load();
    },
    async load() {
      const serial = ++this.serial;
      this.data = {};
      this.error = "";
      if (!this.module) {
        this.loading = false;
        return;
      }
      this.loading = true;
      try {
        const r = await request({
          url: "/system/dataGroup/share-preview",
          params: { module: this.module, groupId: this.groupId || undefined, pageNum: this.page },
        });
        if (serial === this.serial) this.data = r.data;
      } catch (_) {
        if (serial === this.serial) this.error = "预览加载失败，请重试";
      } finally {
        if (serial === this.serial) this.loading = false;
      }
    },
  },
};
</script>
<style scoped>
.preview {
  padding: 20rpx;
  background: #f5f7fa;
  color: #303133;
}
.preview text {
  display: block;
  word-break: break-all;
  white-space: pre-wrap;
}
.object {
  margin: 20rpx 0;
  padding: 12rpx;
  border: 1px solid #ddd;
}
</style>
