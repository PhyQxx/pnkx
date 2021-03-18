<template>
    <div class="notice">
        <div class="title">{{notice.noticeTitle}}</div>
        <div class="notice-info">
            <div class="author">{{notice.author}}</div>
            <div class="create-time">{{notice.createTime}}</div>
            <el-tag class="notice-type" :type="dictValueToTagType(notice.noticeType)">{{getDictOne(notice.noticeType, typeOptions)}}</el-tag>
        </div>
        <div class="notice-content" v-html="notice.noticeContent"></div>
    </div>
</template>

<script>
    import { getNotice } from "@/api/system/notice";
    import { getDicts } from "@/api/system/dict/data";

    export default {
        name: "Noticedetails",
        data() {
            return {
                //通知ID
                id: this.$route.query.noticeId,
                //通知详情
                notice: {},
                //通知类型字典项
                typeOptions: []
            }
        },
        watch: {
          $route(to, from) {
              this.id = this.$route.query.noticeId;
              this.getNotice();
          }
        },
        mounted() {
            this.getDictList();
            this.getNotice();
        },
        methods: {
            /**
             * 字典项翻译
             */
            getDictOne(value, list) {
                let label = '';
                try {
                    list.forEach(item => {
                        if (item.dictValue === value) {
                            label = item.dictLabel
                        }
                    });
                } catch (e) {
                    console.log('字典项翻译异常：' + e)
                }
                return label
            },
            /**
             * 通知类型返回标签类型
             */
            dictValueToTagType(value) {
                let tagType = '';
                if (value === '1') {
                    tagType = ''
                } else if (value === '2') {
                    tagType = 'success'
                } else if (value === '3') {
                    tagType = 'info'
                } else if (value === '4') {
                    tagType = 'warning'
                } else if (value === '5') {
                    tagType = 'danger'
                }
            },
            /**
             * 获取字典项公告类型
             */
            getDictList() {
                getDicts('sys_notice_type').then(res => {
                    this.typeOptions = res.data;
                })
            },
            /**
             * 获取通知详情
             */
            getNotice() {
                getNotice(this.id).then(res => {
                    this.notice =res.data;
                })
            }
        },
    }
</script>

<style lang='scss' scoped>
    .notice{
        padding: 1rem;
        .title{
            font-size: 1.5rem;
            font-weight: bold;
            display: flex;
            justify-content: center;
            align-items: center;
        }
        .notice-info{
            display: flex;
            justify-content: center;
            align-items: center;
            margin: 1rem 0;
            .author, .create-time, .notice-type{
                display: flex;
                justify-content: center;
                align-items: center;
            }
            .create-time, .notice-type{
                margin-left: 1rem;
            }
        }
        .notice-content{
            border: 1px solid #d9d9d9;
            border-radius: 1rem;
            margin: 0 2rem;
            padding: 1rem;
            text-align: center;
        }
    }
</style>
