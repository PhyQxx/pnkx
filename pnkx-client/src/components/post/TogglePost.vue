<script setup lang="ts">
const blogStore = useBlogStore()

defineProps({
  lastArticle: {
    type: Object,
    default: () => {}
  },
  nextArticle: {
    type: Object,
    default: () => {}
  }
})

const router = useRouter()
</script>

<template>
  <div class="post-nav-item overflow-hidden rounded md:flex">
    <div
      v-if="lastArticle?.id"
      class="group/previous prev relative h-[150px] flex-1 cursor-pointer"
      @click="router.push(`/post/${lastArticle.id}`)"
    >
      <img class="h-full w-full object-cover" :src="lastArticle.cover || blogStore.blogConfig?.blogDefaultPicture" alt="" />
      <div
        class="absolute inset-0 bg-[rgba(0,0,0,0.3)] transition-colors duration-500 group-hover/previous:bg-transparent"
      >
        <div class="mx-10 mt-10 text-sm uppercase">
          <p class="mb-2 text-slate-300">Previous Post</p>
          <h4 class="single-line-ellipsis text-white">
            {{ lastArticle.title }}
          </h4>
        </div>
      </div>
    </div>
    <div
      v-if="nextArticle?.id"
      class="group/next next relative h-[150px] flex-1 cursor-pointer"
      @click="router.push(`/post/${nextArticle.id}`)"
    >
      <img class="h-full w-full object-cover" :src="nextArticle.cover || blogStore.blogConfig?.blogDefaultPicture" alt="" />
      <div
        class="absolute inset-0 bg-[rgba(0,0,0,0.3)] transition-colors duration-500 group-hover/next:bg-transparent"
      >
        <div class="mx-10 mt-10 text-sm uppercase">
          <p class="mb-2 text-slate-300">Next Post</p>
          <h4 class="single-line-ellipsis text-white">
            {{ nextArticle.title }}
          </h4>
        </div>
      </div>
    </div>
  </div>
</template>
