<script setup lang="ts">
import {
  NForm,
  NFormItem,
  NInput,
  NAutoComplete,
  useMessage,
  NButton,
  useLoadingBar
} from 'naive-ui'
import type { FormInst, FormRules } from 'naive-ui'


import type { Link } from '~/types/link'
import {addLink} from "~/apis/link";

const blogStore = useBlogStore()
const imageStore = useImageStore()
const message = useMessage()

useHead({
  title: '友链-Pei你看雪'
})

const {link} = useApi()
// 随机颜色
const getRandomColor = () => `#${Math.floor(Math.random() * 0xffffff).toString(16)}`
// 友链列表
const loadingBar = useLoadingBar();
loadingBar.start();
// 友链列表
const linkList = ref<Link[]>([]);
const failureLinkList = ref<Link[]>([]);
link.getLinkList(
    {
      pageNum: 1,
      pageSize: 999
    }).then(res => {
  linkList.value = res.data.value?.data.filter(item => item.status === '1') || [];
  failureLinkList.value = res.data.value?.data.filter(item => item.status === '2') || [];
  loadingBar.finish();
})

/**
 * 复制文本到粘贴板
 * @param text
 * @param msg
 */
const copyText = (text: string, msg = '已成功复制到粘贴板') => {
  const input = document.createElement('input');
  document.body.appendChild(input);
  input.setAttribute('value', text);
  input.select();
  if (document.execCommand('copy')) {
    document.execCommand('copy');
    message.success(msg);
  }
  document.body.removeChild(input);
}
const formRef = ref<FormInst | null>(null)
// 友链表单
const model = ref<Link>({
  title: '',
  remark: '',
  url: '',
  img: '',
  email: '',
  status: ''
})
// 友链表单校验
const rules: FormRules = {
  title: [
    {
      required: true,
      message: '请输入博客名称',
      trigger: ['input', 'blur']
    }
  ],
  remark: [
    {
      required: true,
      message: '请输入博客简介',
      trigger: ['input', 'blur']
    }
  ],
  url: [
    {
      required: true,
      message: '请输入博客地址',
      trigger: ['input', 'blur']
    }
  ],
  img: [
    {
      required: true,
      message: '请输入博客logo',
      trigger: ['input', 'blur']
    }
  ],
  email: [
    {
      required: true,
      message: '请输入正确格式的邮箱',
      validator(_, value: string) {
        const reg =
            /[\w!#$%&'*+/=?^_`{|}~-]+(?:\.[\w!#$%&'*+/=?^_`{|}~-]+)*@(?:[\w](?:[\w-]*[\w])?\.)+[\w](?:[\w-]*[\w])?/
        if (!value) {
          return new Error('请输入邮箱')
        } else if (!reg.test(value)) {
          return new Error('邮箱不正确！')
        }
        return true
      },
      trigger: ['input', 'blur']
    }
  ]
}

// 邮箱自动填充
const options = computed(() => {
  return ['@gmail.com', '@163.com', '@qq.com'].map((suffix) => {
    const prefix = model.value.email.split('@')[0]
    return {
      label: prefix + suffix,
      value: prefix + suffix
    }
  })
})

// 提交加载标志
const loading = ref(false)
/**
 * 提交表单
 */
const handleSubmit = () => {
  formRef.value?.validate((errors) => {
    if (!errors) {
      loading.value = true
      loadingBar.start();
      addLink(model).then(() => {
        message.success('提交成功，请耐心等待审核')
      }).finally(() => {
        loading.value = false
        loadingBar.finish();
      })
    }
  })
}
</script>

<template>
  <div class="min-h-screen">
    <ThePageBanner :bg-cover="imageStore.pageList.link" title="友链"/>
    <BaseBox class="max-w-4xl mx-auto mt-5">
      <div class="mt-4 text-base">
        <h3
            class="border-l-[3px] border-orange-400 pl-3 text-xl text-[#fe9600] dark:border-indigo-600 dark:text-blue-600"
        >本站信息💕</h3>
        <p class="cursor-pointer mt-2">★ 博客名称：
          <span class="blog-info-link" @click="copyText(blogStore.blogConfig?.siteName ||'')">
            {{ blogStore.blogConfig?.siteName }}（单击复制到粘贴板）
          </span>
        </p>
        <p class="cursor-pointer mt-2">★ 博客简介：
          <span class="blog-info-link" @click="copyText(blogStore.blogConfig?.siteIntro ||'')">
            {{ blogStore.blogConfig?.siteIntro }}（单击复制到粘贴板）
          </span>
        </p>
        <p class="cursor-pointer mt-2">★ 博客地址：
          <span class="blog-info-link" @click="copyText(blogStore.blogConfig?.siteAddress ||'')">
            {{ blogStore.blogConfig?.siteAddress }}（单击复制到粘贴板）
          </span>
        </p>
        <p class="cursor-pointer mt-2">★ 博客logo：
          <span class="blog-info-link" @click="copyText(blogStore.blogConfig?.blogDefaultPicture ||'')">
            头像地址（单击复制到粘贴板）
          </span>
        </p>
        <p class="cursor-pointer mt-2">★ 博客邮箱：
          <span class="blog-info-link" @click="copyText(blogStore.blogConfig?.siteEmail ||'')">
            {{ blogStore.blogConfig?.siteEmail }}（单击复制到粘贴板）
          </span>
        </p>
      </div>
      <div class="mt-4 text-base">
        <h3
            class="border-l-[3px] border-orange-400 pl-3 text-xl text-[#fe9600] dark:border-indigo-600 dark:text-blue-600"
        >欢迎交换友链💕</h3>
        <n-form ref="formRef"
                class="mt-3"
                label-placement="left"
                label-width="5rem"
                label-align="left"
                :model="model"
                :rules="rules">
          <n-form-item path="title" label="博客名称">
            <n-input v-model:value="model.title" placeholder="博客名称" @keydown.enter.prevent/>
          </n-form-item>
          <n-form-item path="remark" label="博客简介">
            <n-input v-model:value="model.remark" placeholder="博客简介" @keydown.enter.prevent/>
          </n-form-item>
          <n-form-item path="url" label="博客地址">
            <n-input v-model:value="model.url" placeholder="博客地址 http(s)://格式" @keydown.enter.prevent/>
          </n-form-item>
          <n-form-item path="img" label="博客logo">
            <n-input v-model:value="model.img" placeholder="博客logo" @keydown.enter.prevent/>
          </n-form-item>
          <n-form-item path="email" label="博客邮箱">
            <n-auto-complete
                v-model:value="model.email"
                :input-props="{autocomplete: 'disabled'}"
                :options="options"
                placeholder="博客邮箱"
                @keydown.enter.prevent
            />
          </n-form-item>
          <n-button class="w-24" :loading="loading" @click="handleSubmit"> 提交</n-button>
        </n-form>
      </div>
      <div class="my-10">
        <h3
            class="border-l-[3px] border-orange-400 pl-3 text-xl text-[#fe9600] dark:border-indigo-600 dark:text-blue-600"
        >
          本站友链💕
        </h3>
        <div class="my-5 ml-3 flex flex-wrap">
          <LinkBuddy
              v-for="item in linkList"
              :key="item.id"
              :avatar="item.img"
              :title="item.title"
              :link="item.url"
              :intro="item.remark"
              :color="getRandomColor()"
          />
        </div>
      </div>
      <div class="my-10">
        <h3
            class="border-l-[3px] border-orange-400 pl-3 text-xl text-[#fe9600] dark:border-indigo-600 dark:text-blue-600"
        >
          失效的友链，博主请留言最新博客信息💕
        </h3>
        <div class="my-5 ml-3 flex flex-wrap">
          <LinkBuddy
              v-for="item in failureLinkList"
              :key="item.id"
              :avatar="item.img"
              :title="item.title"
              :link="item.url"
              :intro="item.remark"
              :color="getRandomColor()"
          />
        </div>
      </div>
      <Comment type="3"/>
    </BaseBox>
  </div>
</template>
