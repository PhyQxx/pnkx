<script setup lang="ts">
import {NButton, useLoadingBar, useMessage} from 'naive-ui'

const userStore = useUserStore()
const router = useRouter()
const message = useMessage()

const {setToken} = useToken()
const {user} = useApi()
const loadingBar = useLoadingBar();

const emit = defineEmits<{
  (e: 'toggle', type: string): void
}>()

const formLogin = reactive({userName: '', password: ''})

// 登录加载标志
const loading = ref(false)

const handleLogin = () => {
  if (!formLogin.userName) {
    message.warning('请输入邮箱号！')
    return
  }

  if (!/^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\.[a-zA-Z0-9_-]+)+$/.test(formLogin.userName) && (!['15006732580', '13220556672'].includes(formLogin.userName))) {
    message.warning('邮箱格式有误！')
    return
  }

  if (!formLogin.password) {
    message.warning('请输入密码！')
  }

  // 取消useFetch响应式参数自动发送请求
  const params = Object.assign({}, formLogin)
  loading.value = true
  loadingBar.start();
  user.login(params).then(res => {
    if (res.data.value?.token) {
      setToken(res.data.value.token)
      message.success('登录成功')
      router.push('/user').then(() => {
        userStore.getUserInfo()
      })
    }
  }).finally(() => {
    loading.value = false
    loadingBar.finish();
  })
}
</script>

<template>
  <div class="form h-full w-full text-sm text-white">
    <h3 class="text-center text-3xl">登录</h3>
    <login-input
        v-model:value="formLogin.userName"
        class="my-10"
        icon="ph:user-duotone"
        placeholder="邮箱"
    />
    <login-input
        v-model:value="formLogin.password"
        class="mb-5"
        type="password"
        icon="ph:lock-key-bold"
        placeholder="密码"
    />
    <div class="flex justify-end">
      <button @click="emit('toggle', 'forgotPassword')">忘记密码？</button>
    </div>
    <div class="pt-10">
      <n-button class="btn-grad" :loading="loading" @click="handleLogin">登录</n-button>
    </div>
    <div class="pt-9 text-center">
      <span>没有帐户？</span>
      <button @click="emit('toggle', 'register')">注册</button>
    </div>
  </div>
</template>
