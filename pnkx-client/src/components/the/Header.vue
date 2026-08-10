<script setup lang="ts">
import { NDropdown, NAvatar } from 'naive-ui'

interface IOption {
  label: string
  key: string | number
}

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const blogStore = useBlogStore()
const searchStore = useSearchStore()

const options = ref<IOption[]>([])

watch(
  () => userStore.userInfo,
  (val) => {
    if (val) {
      options.value = [
        {
          label: '个人中心',
          key: 'user'
        },
        {
          label: '登出',
          key: 'logout'
        }
      ]
    } else {
      options.value = [
        {
          label: '登录',
          key: 'login'
        }
      ]
    }
  },
  { immediate: true }
)

const handleSelect = (key: string | number) => {
  const { path } = route
  switch (key) {
    case 'user':
      router.push('/user')
      break
    case 'logout':
      userStore.clearUserInfo()
      if (path === '/user') {
        router.push('/login')
      }
      break
    case 'login':
      router.push(`/login`)
      break
    default:
      break
  }
}

const { y } = useWindowScroll()
const showMenu = computed(() => y.value === 0)

// 搜索
function handleSearch() {
  searchStore.setModal(true)
}
</script>

<template>
  <header
    class="header-nav group/nav fixed left-0 right-0 top-0 z-50 flex h-14 items-center justify-between bg-[rgba(255,255,255,0.7)] px-4 transition-colors duration-700 dark:bg-[rgba(38,38,38,0.7)] max-md:hidden"
    :class="{ active: showMenu }"
  >
    <h1 class="site-author cursor-pointer text-2xl" @click="router.push('/')">
      {{ blogStore.blogConfig?.siteAuthor ?? 'Pei你看雪' }}
    </h1>
    <nav class="group-hover/nav:block" :class="{ hidden: showMenu }">
      <ul class="flex">
        <li
          v-for="(item, index) in blogStore.menuList"
          :key="index"
          class="nav-item mx-4 flex cursor-pointer items-center text-base font-semibold hover:text-orange-500 dark:hover:text-indigo-500"
          :class="item.class"
        >
          <NuxtLink :to="`${item.path}`" class="flex items-center">
            <Icon :class="`icon-${index + 1}`" size="20" :name="item.icon" />
            <span class="pl-1">{{ item.text }}</span>
          </NuxtLink>
        </li>
      </ul>
    </nav>
    <div class="flex items-center">
      <BaseDarkToggle />
      <div class="menu-item-search mr-4 cursor-pointer" @click="handleSearch">
        <Icon name="icon-park:search" size="22" />
      </div>
      <div>
        <n-dropdown :options="options" @select="handleSelect">
          <n-avatar :src="userStore.userInfo?.avatar ?? ''" round class="mt-1 h-[30px] w-[30px] auth-header" />
        </n-dropdown>
      </div>
    </div>
  </header>
</template>
<style>
.auth-header img {
  width: 30px;
  height: 30px;
}
</style>
