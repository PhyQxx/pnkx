<script setup lang="ts">
const blogStore = useBlogStore()

const day = computed(() => {
  if (blogStore.blogConfig) {
    const differ = new Date().getTime() - new Date(blogStore.blogConfig.createSiteTime).getTime()
    const day = Math.floor(differ / 1000 / 60 / 60 / 24)
    return day
  } else {
    return '--'
  }
})
</script>

<template>
  <footer>
    <div class="pb-12 pt-20 text-center text-xs">
      <img class="m-auto h-8 w-8 animate-spin-slow" src="~/assets/img/svg/sakura.svg" alt="" />
      <p class="mt-2 text-slate-600">Crafted with by Pei你看雪</p>
      <p class="my-3 text-neutral-400">
        <span> 小破站居然运行了 {{ day }} 天</span>
        <Icon class="heart mx-1 pb-1" name="fxemoji:beating-heart" size="22" />
        <span>访客 {{ blogStore.visitsNumber || '--' }}</span>
      </p>
      <p class="beian text-neutral-400">
        <span class="mr-1">© 2023 Pei你看雪</span>
        <NuxtLink to="https://beian.miit.gov.cn" target="_blank">
          {{ blogStore.blogConfig?.recordNumber }}
        </NuxtLink>
      </p>
    </div>
  </footer>
</template>
