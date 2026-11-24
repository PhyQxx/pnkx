<script setup lang="ts">
interface Props {
  src: string
  alt?: string
}
const props = withDefaults(defineProps<Props>(), { alt: '' })
const failed = ref(false)
const attempt = ref(0)

watch(() => props.src, () => {
  failed.value = false
  attempt.value++
}, { flush: 'sync' })

function retry() {
  failed.value = false
  attempt.value++
}
</script>

<template>
  <div class="group relative h-full w-full">
    <!-- Native lazy loading avoids a separate preload Image request. -->
    <img
      v-if="src && !failed"
      :key="attempt"
      class="h-full w-full rounded-[inherit] object-cover transition-transform duration-500 group-hover:scale-110"
      :src="src"
      :alt="alt"
      loading="lazy"
      decoding="async"
      @error="failed = true"
    />
    <div
      v-else
      class="flex h-full min-h-24 w-full flex-col items-center justify-center gap-2 rounded-[inherit] bg-zinc-100 text-sm text-zinc-500 dark:bg-zinc-800 dark:text-zinc-400"
      role="status"
    >
      <span>{{ src ? '图片暂时无法加载' : '暂无图片' }}</span>
      <button v-if="src" type="button" class="underline" @click.stop.prevent="retry">重新加载</button>
    </div>
  </div>
</template>
