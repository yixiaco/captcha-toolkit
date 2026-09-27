<template>
  <div class="captcha-portal">
    <slot />
  </div>
</template>

<script lang="ts">
import { defineComponent } from 'vue';

// Vue 2.7 没有 Teleport：挂载后把根节点搬到 body，效果与 Teleport to="body" 一致
// （弹窗/浮动组件因此不受宿主的层叠上下文与 overflow 影响）
export default defineComponent({
  name: 'CaptchaPortal',
  mounted() {
    const el = this.$el as Element | undefined;
    if (el && el.parentNode !== document.body) {
      document.body.appendChild(el);
    }
  },
});
</script>
