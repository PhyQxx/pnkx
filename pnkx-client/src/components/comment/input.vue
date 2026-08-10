<script setup lang="ts">
import { NButton, NInput } from 'naive-ui'
import { OnClickOutside } from '@vueuse/components'

interface Props {
  placeholder?: string
  btnText?: string
  value: string
}

const props = withDefaults(defineProps<Props>(), {
  placeholder: '来发一针见血的评论吧!',
  btnText: '提交评论'
})

const disabled = ref(true)

const emit = defineEmits<{
  (e: 'hide', event: Event): void
  (e: 'submit'): void
  (e: 'update:value', value: string): void
}>()

function onClickOutsideHandler(event: Event) {
  if (props.value.trim() !== '') return
  emit('hide', event)
}

function onInput(value: string) {
  emit('update:value', value)
  if (value.trim() === '') {
    disabled.value = true
  } else {
    disabled.value = false
  }
}

function onSubmit() {
  emit('submit')
}

function onAddEmoji(key: string) {
  emit('update:value', props.value + key)
  disabled.value = false
}
</script>

<template>
  <OnClickOutside
    :options="{
      ignore: ['.n-popover__content']
    }"
    class="flex-1"
    @trigger="onClickOutsideHandler"
  >
    <n-input
      :value="value"
      :autosize="{
        minRows: 3
      }"
      autofocus
      type="textarea"
      :placeholder="placeholder"
      class="comment_textarea"
      @input="onInput"
    />

    <div class="mt-2 flex justify-between">
      <CommentEmoji @add-emoji="onAddEmoji" />
      <n-button ghost :disabled="disabled" class="vsubmit" @click="onSubmit">
        {{ btnText }}
      </n-button>
    </div>
  </OnClickOutside>
</template>
