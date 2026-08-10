<script setup lang="ts">

interface Props {
  id: number
  replyNumber: number | null
  data: Comment[] | null
}

defineProps<Props>()
const emit = defineEmits<{
  (e: 'reloadReply', id: number): void
}>()
const showMore = ref(true)

function reloadReply(id: number) {
  emit('reloadReply', id)
  showMore.value = false
}
</script>

<template>
  <ul>
    <CommentItem
      v-for="item in data"
      :key="item.id"
      reply
      :data="item"
      @reload-reply="reloadReply"
    />
    <div class="pl-10 text-xs">
      <!-- <span v-if="loading">加载中...</span> -->
      <div v-show="showMore && replyNumber && replyNumber > 3">
        <span class="text-gray-400">共{{ replyNumber }}条回复，</span>
        <span
          class="inline-flex cursor-pointer items-center text-blue-500"
          @click="reloadReply(id)"
        >
          <i class="mr-1 not-italic">点击查看</i>
          <Icon name="streamline:interface-arrows-button-down-arrow-down-keyboard" />
        </span>
      </div>
    </div>
  </ul>
</template>
