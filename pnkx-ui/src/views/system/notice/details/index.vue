<template>
    <div class="notice">
        <div class="title">{{ notice.noticeTitle }}</div>
        <div class="notice-info">
            <div class="author">{{ notice.author }}</div>
            <div class="create-time">{{ notice.createTime }}</div>
            <el-tag class="notice-type" :type="dictValueToTagType(notice.noticeType)">
                {{ getDictOne(notice.noticeType, typeOptions) }}
            </el-tag>
        </div>
        <div class="notice-content markdown-body" v-html="sanitizeHtml(notice.noticeContent)"></div>
        <admin-message-board :articleId="notice.noticeId" messageType="6"/>
    </div>
</template>

<script>
import {getNotice} from "@/api/system/notice";
import {sanitizeHtml} from '@/utils/sanitizeHtml';
import {getDicts} from "@/api/system/dict/data";

export default {
    name: "Noticedetail",
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
                console.error('字典项翻译异常：' + e)
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
                this.notice = res.data;
                setTimeout(() => {
                    let imgList = document.getElementsByClassName("markdown-body")[0].getElementsByTagName("img");
                    if (imgList.length > 0) {
                        imgList.forEach(img => {
                            img.setAttribute('style', img.getAttribute('alt'))
                        })
                    }
                }, 0)
            })
        }
    },
}
</script>

<style lang="scss" scoped>
.notice {
    padding: var(--space-6);
    background: var(--bg-card);
    border-radius: var(--radius-lg);
    box-shadow: var(--shadow-sm);

    .title {
        font-size: var(--text-2xl);
        font-weight: var(--font-bold);
        color: var(--text-primary);
        display: flex;
        justify-content: center;
        align-items: center;
    }

    .notice-info {
        display: flex;
        justify-content: center;
        align-items: center;
        margin: var(--space-4) 0;
        font-size: var(--text-sm);
        color: var(--text-tertiary);

        .author, .create-time, .notice-type {
            display: flex;
            justify-content: center;
            align-items: center;
        }

        .create-time, .notice-type {
            margin-left: var(--space-4);
        }
    }

    .notice-content {
        border: 1px solid var(--border-primary);
        border-radius: var(--radius-xl);
        margin: 0 var(--space-8);
        padding: var(--space-6);
        background: var(--bg-body);
        transition: box-shadow var(--duration-normal) var(--ease-default);

        &:hover {
            box-shadow: var(--shadow-sm);
        }

        ::v-deep img {
            max-width: 80vw !important;
            border-radius: var(--radius-md);
            transition: transform var(--duration-fast) var(--ease-default);
        }
    }
}
</style>
