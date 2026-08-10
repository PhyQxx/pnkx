<script setup lang="ts">
import {NButton, useLoadingBar, useMessage} from 'naive-ui'

interface Props {
  type: string
}

const props = defineProps<Props>()
const loadingBar = useLoadingBar();

const text = computed(() => (props.type === 'register' ? '注册' : '重置密码'))

const emit = defineEmits<{
  (e: 'toggle', type: string): void
}>()

const message = useMessage()
const fromRegister = reactive({userName: '', password: ''})

const {user} = useApi()
// 登录加载标志
const loading = ref(false)
const handleRegister = async () => {
  const reg = /^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\.[a-zA-Z0-9_-]+)+$/
  if (!fromRegister.userName.trim()) {
    message.warning('请输入邮箱号！')
    return
  }
  if (!reg.test(fromRegister.userName)) {
    message.warning('邮箱格式有误！')
    return
  }
  if (props.type === 'forgotPassword') {
    loading.value = true
    loadingBar.start();
    user.sendResetEmail(fromRegister.userName).then(res => {
      if (res.data.value?.data) {
        message.success('重置密码邮件已发送至您的邮箱！')
      }
    }).finally(() => {
      loading.value = false;
      loadingBar.finish();
    })

  } else if (props.type === 'register') {
    if (!fromRegister.password.trim()) {
      message.warning('请输入密码！')
      return
    }
    const params = Object.assign({}, fromRegister)
    loading.value = true
    loadingBar.start()
    user.register(params).then(res => {
      if (res.data.value?.data) {
        message.success('注册成功，激活邮件已发送至您的邮箱！')
      }
    }).finally(() => {
      loading.value = false
      loadingBar.finish();
    })
  }
}
</script>

<template>
  <div class="form h-full w-full text-sm text-white">
    <h3 class="text-center text-3xl">{{ text }}</h3>
    <login-input
        v-model:value="fromRegister.userName"
        class="my-10"
        icon="ph:user-duotone"
        placeholder="邮箱"
    />
    <login-input
        v-if="type === 'register'"
        v-model:value="fromRegister.password"
        class="mb-5"
        type="password"
        icon="ph:lock-key-bold"
        placeholder="密码"
    />
    <div class="pt-5" :class="{'pt-0': type === 'forgotPassword'}">
      <n-button class="btn-grad" :loading="loading" @click="handleRegister">{{ text }}</n-button>
    </div>
    <div class="pt-9 text-center">
      <span>已有账号！</span>
      <button @click="emit('toggle', 'login')">登录</button>
    </div>
  </div>
</template>
