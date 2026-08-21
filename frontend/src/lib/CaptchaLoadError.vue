<template>
  <div class="load-error-mask">
    <svg
      viewBox="0 0 24 24"
      width="26"
      height="26"
      fill="none"
      stroke="#e5484d"
      stroke-width="2"
      stroke-linecap="round"
      stroke-linejoin="round"
    >
      <path d="M12 3l9 16H3l9-16z" />
      <path d="M12 10v4" />
      <path d="M12 17.5v.5" />
    </svg>
    <span class="load-error-text">{{ text }}</span>
    <button
      class="load-error-btn"
      type="button"
      @click="$emit('retry')"
    >
      <svg
        viewBox="0 0 24 24"
        width="14"
        height="14"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
        stroke-linejoin="round"
      >
        <path d="M21 12a9 9 0 1 1-2.64-6.36" />
        <path d="M21 3v6h-6" />
      </svg>
      {{ retryText }}
    </button>
  </div>
</template>

<script setup lang="ts">
interface Props {
  /** 加载失败提示文案 */
  text?: string
  /** 重试按钮文案 */
  retryText?: string
}

withDefaults(defineProps<Props>(), {
  text: '加载失败',
  retryText: '重试',
});

defineEmits<{
  (e: 'retry'): void
}>();
</script>
