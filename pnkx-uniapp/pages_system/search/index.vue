<template>
  <view class="seach subpage-shell">
    <uni-search-bar :focus="true" cancelButton="none" v-model="searchValue" @input="search">
    </uni-search-bar>
    <uni-section :title="item.label" type="line" v-for="item in list" :key="item.label">
      <no-data :loading="loading" :text="'暂无' + item.label + '记录'"
               v-if="item.options.length < 1"/>
      <uni-list>
        <uni-list-item v-for="one in item.options" :key="one.id" clickable showArrow
                       :title="one.title"/>
      </uni-list>
    </uni-section>
  </view>
</template>

<script>
import {fullRetrieval} from '@/api/px/index.js'

export default {
  data() {
    return {
      // 搜索值
      searchValue: '',
      // 加载标志
      loading: false,
      // 搜索的内容集合
      list: [{
        label: '待办事项',
        options: []
      },
        {
          label: '生活账本',
          options: []
        }, {
          label: '日记',
          options: []
        },
        {
          label: '笔记',
          options: []
        }
      ]
    };
  },
  methods: {
    /**
     * 翻译账单类型
     * @param record
     * @returns {string}
     */
    billType(record) {
      if (record.typeObject && record.typeObject.typeDifference === '0') {
        return '收入'
      }
      if (record.typeObject && record.typeObject.typeDifference === '1') {
        return '支出'
      }
      if (record.typeObject && record.typeObject.typeDifference === '2') {
        return '转账'
      }
      if (record.typeObject && record.typeObject.typeDifference === '3') {
        return '修改余额'
      }
    },
    /**
     * 搜索
     */
    search() {
      this.$debounce(() => {
        if (!this.searchValue) {
          this.list = [{
            label: '待办事项',
            options: []
          },
            {
              label: '生活账本',
              options: []
            }, {
              label: '日记',
              options: []
            },
            {
              label: '笔记',
              options: []
            }
          ];
          return
        }
        this.loading = true;
        fullRetrieval({
          searchCode: this.searchValue
        }).then(res => {
          this.list = res.data;
          this.list.map(item => {
            if (item.label === '待办事项') {
              item.options.map(option => {
                option.title = option.content;
              })
            }
            if (item.label === '生活账本') {
              item.options.map(option => {
                option.title =
                  `${option.typeObject && option.typeObject.typeName}${option.remark ? '-' + option.remark : ''}-${option.money}-${option.accountObject && option.accountObject.accountName}-${this.billType(option)}`;
              })
            }
            if (item.label === '日记') {
              item.options.map(option => {
                option.title = option.content.replace(this.regex, "");
              })
            }
            if (item.label === '笔记') {
              item.options = item.options.filter(option => Boolean(option
                .content))
            }
            return item
          })
          this.loading = false;
        })
      }, 1200)()
    }
  }
}
</script>

<style lang="scss" scoped>
page {
  background-color: $bg-page;
}

.seach {
  padding: $spacing-sm $page-padding $spacing-xl;
  min-height: 100vh;
}

.seach :deep(.uni-searchbar) {
  margin-bottom: $spacing-md;
}

.seach :deep(.uni-section) {
  background-color: $bg-card;
  border-radius: $radius-xl;
  box-shadow: $shadow-card;
  margin-bottom: $spacing-md;
  overflow: hidden;
}

.seach :deep(.uni-section-content) {
  padding: 0 $spacing-lg;
}
</style>
