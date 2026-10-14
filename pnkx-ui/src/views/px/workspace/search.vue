<template>
  <section class="resource-search">
    <h1>搜索资料</h1>
    <p>查找笔记、剪藏、待办和生活记录，筛选后统一分页。</p>
    <el-form inline @submit.prevent="search(1)">
      <el-form-item label="关键词"><el-input v-model="query.q" clearable placeholder="标题或正文" /></el-form-item>
      <el-form-item label="类型"><el-select v-model="query.type" style="width: 140px"><el-option v-for="(label, value) in types" :key="value" :value="value" :label="label" /></el-select></el-form-item>
      <el-form-item label="标签"><el-input v-model="query.tag" clearable placeholder="完整标签名称" /></el-form-item>
      <el-form-item label="创建日期"><el-date-picker v-model="dates" type="daterange" value-format="YYYY-MM-DD" start-placeholder="开始日期" end-placeholder="结束日期" /></el-form-item>
      <el-form-item><el-button type="primary" native-type="submit">搜索</el-button></el-form-item>
    </el-form>
    <p aria-live="polite">{{ loading ? '正在搜索…' : `共 ${total} 条结果` }}</p>
    <el-empty v-if="!loading && !rows.length" description="没有匹配的资料" />
    <div v-loading="loading" class="results">
      <article v-for="row in rows" :key="row.type + ':' + row.id" class="result">
        <el-tag size="small">{{ types[row.type] }}</el-tag>
        <router-link :to="row.route" class="title">{{ row.title }}</router-link>
        <p>{{ row.subtitle }}</p>
        <div><el-tag v-for="tag in row.tags" :key="tag" size="small" effect="plain">{{ tag }}</el-tag></div>
        <small>{{ row.updatedAt }}</small>
      </article>
    </div>
    <el-pagination v-if="total" :current-page="page" :page-size="20" :total="total" layout="prev, pager, next, total" @current-change="search" />
  </section>
</template>
<script>
import { globalSearchPage } from '@/api'
export default {
  name: 'ResourceSearch',
  data() {
    return {
      query: { q: this.$route.query.q || '', type: 'all', tag: '' }, dates: [],
      rows: [], total: 0, page: 1, loading: false, sequence: 0,
      types: { all: '全部', file: '文件', note: '笔记', webArticle: '剪藏', todo: '待办', article: '文章', diary: '日记',
        bookkeeping: '账本', commemoration: '纪念日', subscription: '订阅', recipe: '菜谱', shopping: '购物清单', book: '书籍' }
    }
  },
  mounted() { this.search(1) },
  beforeUnmount() { this.sequence++ },
  methods: {
    async search(page = 1) {
      const sequence = ++this.sequence
      this.loading = true
      try {
        const response = await globalSearchPage({ ...this.query, startDate: this.dates?.[0], endDate: this.dates?.[1], pageNum: page, pageSize: 20 })
        if (sequence !== this.sequence) return
        this.rows = response.data.rows
        this.total = response.data.total
        this.page = page
      } finally { if (sequence === this.sequence) this.loading = false }
    }
  }
}
</script>
<style scoped>
.resource-search { padding: 24px; max-width: 1200px; margin: auto; }
.results { display: grid; gap: 12px; margin-bottom: 24px; }
.result { padding: 18px; border: 1px solid var(--border-primary); border-radius: 10px; background: var(--bg-card); }
.title { margin-left: 12px; font-size: 17px; font-weight: 600; overflow-wrap: anywhere; }
p { color: var(--text-secondary); white-space: pre-wrap; overflow-wrap: anywhere; }
small { display: block; margin-top: 10px; color: var(--text-tertiary); }
</style>
