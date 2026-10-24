<template>
  <view class="actions"
    ><assignment-history type="shopping" :source-id="item.id" @changed="$emit('changed')" /><text
      >{{ labels[item.fulfillmentState] || "待购" }}
      {{ item.claimedBy ? "· 认领人 " + item.claimedBy : "" }}</text
    ><view v-if="!item.batchId"
      ><button size="mini" @click="act(item.claimedBy ? 'release' : 'claim')">
        {{ item.claimedBy ? "释放" : "认领" }}</button
      ><button size="mini" @click="act('fulfillment', 'available')">家里已有</button
      ><button
        size="mini"
        @click="act('fulfillment', item.fulfillmentState === 'purchased' ? 'needed' : 'purchased')"
      >
        {{ item.fulfillmentState === "purchased" ? "待购" : "已买到" }}
      </button></view
    ><text v-else>已结算</text></view
  >
</template>
<script>
import request from "@/utils/request";
export default {
  props: { item: { type: Object, required: true } },
  data() {
    return { labels: { needed: "待购", available: "家里已有", purchased: "已买到" } };
  },
  methods: {
    async act(action, state) {
      try {
        await request({
          url: `/shoppingItem/${this.item.id}/${action}`,
          method: "post",
          data: {
            requestId: `shopping_${Date.now()}_${Math.random().toString(36).slice(2)}`,
            expectedRevision: this.item.revision,
            state,
          },
        });
      } finally {
        this.$emit("changed");
      }
    },
  },
};
</script>
<style scoped>
.actions {
  font-size: 22rpx;
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}
.actions button {
  font-size: 22rpx;
  padding: 0 12rpx;
  margin: 6rpx;
  display: inline-block;
}
</style>
