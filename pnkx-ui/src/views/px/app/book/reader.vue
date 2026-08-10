<template>
    <div v-loading="loading" class="reader-page">
        <header class="reader-header">
            <el-button link :icon="Back" @click="backToChapters">返回章节列表</el-button>
            <span class="book-name">《{{ chapter.bookTitle || '' }}》</span>
            <div class="quick-actions">
                <el-button :disabled="!previous" :icon="ArrowLeft" @click="turnPage(previous)">上一页</el-button>
                <el-button type="primary" :disabled="!next" @click="turnPage(next)">
                    下一页<el-icon class="el-icon--right"><ArrowRight /></el-icon>
                </el-button>
            </div>
        </header>

        <main class="reader-paper">
            <h1>{{ chapter.chapterName }}</h1>
            <p class="chapter-meta">第 {{ chapter.chapterNo || '-' }} 章</p>
            <article class="chapter-content">{{ chapter.content || '本章暂无内容' }}</article>
            <nav class="bottom-nav">
                <button :disabled="!previous" @click="turnPage(previous)">
                    <small>上一页</small>
                    <span>{{ previous?.chapterName || '已经是第一页' }}</span>
                </button>
                <button class="next" :disabled="!next" @click="turnPage(next)">
                    <small>下一页</small>
                    <span>{{ next?.chapterName || '已经是最后一页' }}</span>
                </button>
            </nav>
        </main>
    </div>
</template>

<script setup>
import { getCurrentInstance, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { ArrowLeft, ArrowRight, Back } from '@element-plus/icons-vue'
import { getReaderData, updateReadingProgress } from '@/api/px/app/book'

const { proxy } = getCurrentInstance()
const loading = ref(false)
const chapter = reactive({})
const previous = ref(null)
const next = ref(null)

function loadChapter(id) {
    loading.value = true
    getReaderData(id).then(res => {
        const data = res.data || {}
        Object.keys(chapter).forEach(key => delete chapter[key])
        Object.assign(chapter, data.chapter || {})
        previous.value = data.previous || null
        next.value = data.next || null
        if (chapter.id) updateReadingProgress(chapter.id).catch(() => {})
        window.scrollTo({ top: 0, behavior: 'smooth' })
    }).finally(() => { loading.value = false })
}

function turnPage(target) {
    if (!target?.id) return
    proxy.$router.push({ name: 'MyBookReader', params: { chapterId: target.id } })
}

function backToChapters() {
    proxy.$router.push({ name: 'MyBookChapter', query: { bookId: chapter.bookId } })
}

function handleKeydown(event) {
    if (event.key === 'ArrowLeft') turnPage(previous.value)
    if (event.key === 'ArrowRight') turnPage(next.value)
}

watch(() => proxy.$route.params.chapterId, id => loadChapter(id), { immediate: true })
onMounted(() => window.addEventListener('keydown', handleKeydown))
onBeforeUnmount(() => window.removeEventListener('keydown', handleKeydown))
</script>

<style lang="scss" scoped>
.reader-page { min-height: calc(100vh - 84px); background: #f3efe7; padding: 18px; }
.reader-header {
    position: sticky; top: 0; z-index: 3; max-width: 980px; margin: 0 auto 16px; padding: 10px 14px;
    background: rgba(255, 253, 248, .94); backdrop-filter: blur(8px); border: 1px solid #e5ded0; border-radius: 12px;
    display: grid; grid-template-columns: 1fr auto 1fr; align-items: center; gap: 12px;
    .book-name { color: #6e6254; font-size: 14px; }
    .quick-actions { display: flex; justify-content: flex-end; }
}
.reader-paper {
    max-width: 880px; min-height: 70vh; margin: 0 auto; padding: 54px 72px; background: #fffdf8;
    border: 1px solid #e8e0d3; border-radius: 12px; box-shadow: 0 8px 30px rgba(91, 75, 53, .08);
    h1 { margin: 0; text-align: center; color: #2f2a24; font-size: 28px; }
    .chapter-meta { text-align: center; color: #9b8d7b; margin: 12px 0 42px; }
    .chapter-content { white-space: pre-wrap; word-break: break-word; color: #3d3832; font-size: 18px; line-height: 2; }
}
.bottom-nav { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; margin-top: 64px; padding-top: 24px; border-top: 1px solid #eee6da;
    button { border: 0; background: #f6f1e8; border-radius: 8px; padding: 14px 16px; text-align: left; cursor: pointer; color: #574d42; }
    button.next { text-align: right; }
    button:disabled { opacity: .5; cursor: not-allowed; }
    small, span { display: block; }
    small { color: #9b8d7b; margin-bottom: 5px; }
}
@media (max-width: 768px) {
    .reader-page { padding: 8px; }
    .reader-header { grid-template-columns: 1fr auto; .book-name { display: none; } }
    .reader-paper { padding: 36px 22px; h1 { font-size: 23px; } .chapter-content { font-size: 17px; } }
}
</style>
