<script setup lang="ts">
import {useMessage, useDialog, useLoadingBar} from "naive-ui";

const {user} = useApi()
const { track } = useAnalytics()
const message = useMessage()
const dialog = useDialog()
const loadingBar = useLoadingBar();

definePageMeta({
  layout: 'no-bottom'
})
useHead({
  title: '登录-Pei你看雪'
})

const route = useRoute();

// 获取激活账号
const activationUserName: any = route.query.activationUserName
const activationToken: any = route.query.activationToken

onMounted(() => {
if (typeof activationUserName === 'string' && typeof activationToken === 'string') {
  const operationId = crypto.randomUUID()
  track('activation_submit', { operationId })
  user.activation(activationUserName, activationToken).then(res => {
    if (res.data.value?.data) {
      track('activation_success', { operationId })
      message.success('激活成功，请登录！')
    } else {
      track('activation_fail', { operationId })
      message.error('激活链接无效或已过期！')
    }
  }).catch(() => { track('activation_fail', { operationId }) })
}
})

// 获取重置账号
const restUserName: any = route.query.restUserName
const restToken: any = route.query.restToken

if (restUserName) {
  user.restPassword(restUserName, restToken).then(res => {
    if (res.data.value?.data) {
      dialog.success({
        title: '重置密码成功',
        content: `您的新密码为：${res.data.value?.data}，请妥善保存！`
      })
    }
  })
}

const imageStore = useImageStore()

const typeForm = ref('login')

const handleToggleForm = (type: string) => {
  typeForm.value = type
}
</script>

<template>
  <client-only>
    <div
        class="login flex h-screen items-center justify-center bg-cover bg-center bg-no-repeat"
        :style="{ backgroundImage: `url(${imageStore.pageList.login})` }"
    >
      <div
          class="box relative w-96 overflow-hidden rounded-3xl border-2 border-solid border-white bg-[hsla(0,0%,10%,0.1)] px-6 py-10 backdrop-blur max-md:mx-4 max-md:w-full"
      >
        <Login ref="loginRef" v-if="typeForm === 'login'" @toggle="handleToggleForm"/>
        <LoginRegister v-else :type="typeForm" @toggle="handleToggleForm"/>
      </div>
    </div>
  </client-only>
</template>
