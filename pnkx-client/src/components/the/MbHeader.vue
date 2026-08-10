<script setup lang="ts">
import { NAvatar, NDrawer } from 'naive-ui'

const router = useRouter()
const userStore = useUserStore()
const blogStore = useBlogStore()
const searchStore = useSearchStore()

const menuToggle = ref(false)
function handleIconMenu() {
  menuToggle.value = !menuToggle.value
}
function handleSearch() {
  searchStore.setModal(true)
}
function logout() {
  menuToggle.value = false
  userStore.clearUserInfo()
  router.push('/login')
}
</script>

<template>
  <header class="fixed left-0 right-0 top-0 z-50 md:hidden">
    <div class="flex h-14 items-center justify-between px-4 shadow-md backdrop-blur">
      <h1 class="cursor-pointer text-2xl" @click="router.push('/')">
        {{ blogStore.blogConfig?.siteAuthor ?? 'Pei你看雪' }}
      </h1>
      <div class="flex items-center">
        <BaseDarkToggle />
        <div class="mr-4 cursor-pointer">
          <Icon name="icon-park:search" size="22" @click="handleSearch" />
        </div>
        <div class="cursor-pointer md:hidden" @click="handleIconMenu">
          <Icon v-if="menuToggle" name="line-md:menu-to-close-alt-transition" size="26" />
          <Icon v-else name="line-md:close-to-menu-transition" size="26" />
        </div>
      </div>
    </div>
    <n-drawer v-model:show="menuToggle" width="70%" placement="left">
      <div
        class="mb-deader-menu h-full w-full overflow-y-auto rounded-lg p-5 text-center shadow-xl"
      >
        <n-avatar :size="160" round :src="userStore.userInfo?.avatar" />
        <h3 class="mt-2 text-center text-base font-semibold">
          {{ blogStore.blogConfig?.siteAuthor }}
        </h3>
        <p class="my-2 text-15 text-zinc-400">{{ blogStore.blogConfig?.siteIntro }}</p>
        <ul
          class="flex justify-evenly border-b-2 border-dashed border-sky-500 pb-4 text-center text-lg"
        >
          <li>
            <p>文章</p>
            <span>{{ blogStore.articleNumber || 0 }}</span>
          </li>
          <li>
            <p>分类</p>
            <span>{{ blogStore.articleTypeNumber || 0 }}</span>
          </li>
        </ul>
        <ul class="mb-menu py-3">
          <li v-for="(item, index) in blogStore.menuList" :key="index" class="flex justify-center">
            <NuxtLink
              :to="item.path"
              class="mb-3 flex h-12 w-40 items-center justify-center text-base outline-none"
              @click="menuToggle = false"
            >
              <Icon :name="item.icon" />
              <p class="ml-1">{{ item.text }}</p>
            </NuxtLink>
          </li>
          <li class="flex justify-center">
            <div
              v-if="userStore.userInfo"
              class="mb-3 flex h-12 w-40 items-center justify-center text-base outline-none"
              @click="logout"
            >
              <p class="ml-1">登出</p>
            </div>
            <NuxtLink
              v-else
              to="/login"
              class="mb-3 flex h-12 w-40 items-center justify-center text-base outline-none"
              @click="menuToggle = false"
            >
              <p class="ml-1">登录</p>
            </NuxtLink>
          </li>
        </ul>
      </div>
    </n-drawer>
  </header>
</template>
