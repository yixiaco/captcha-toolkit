<template>
  <!-- Vue 2 要求单根，这里统一用一个宿主节点包住三种形态（display: contents 不参与宿主布局） -->
  <div class="captcha-host">
    <CaptchaModal
      v-if="props.display === 'modal'"
      v-bind="attrs"
      :visible="props.visible"
      :mode="props.mode"
      @close="onClose"
      @success="onSuccess"
    />
    <FloatingCaptcha
      v-else-if="props.display === 'floating'"
      v-bind="attrs"
      :mode="props.mode"
      @success="onSuccess"
      @fail="onFail"
      @error="onError"
      @close="onClose"
    />
    <AutoCaptcha
      v-else
      v-bind="attrs"
      :mode="props.mode"
      @success="onSuccess"
      @fail="onFail"
      @error="onError"
    />
  </div>
</template>

<script setup lang="ts">
import CaptchaModal from './CaptchaModal.vue';
import FloatingCaptcha from './FloatingCaptcha.vue';
import AutoCaptcha from './AutoCaptcha.vue';
import { useAttrsCompat } from './attrs';
import type { VerifyResult } from '../core/api';

interface Props {
  /** 展示方式：inline 嵌入页面 / modal 弹窗 / floating 浮动按钮 */
  display?: string
  /**
   * 类型提示：auto 表示完全由后端决定；指定具体类型时仅在 debug 模式下生效。
   * 无论传什么，组件都按后端响应里的 type 渲染。
   */
  mode?: string
  /** 弹窗是否可见（仅 display=modal 生效） */
  visible?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  display: 'modal',
  mode: 'auto',
  visible: false,
});

const emit = defineEmits<{
  (e: 'success', result: VerifyResult): void
  (e: 'close'): void
  (e: 'fail', result: VerifyResult): void
  (e: 'error', error: unknown): void
}>();
const attrs = useAttrsCompat();

function onSuccess(result: VerifyResult) {
  emit('success', result);
}

function onClose() {
  emit('close');
}

function onFail(result: VerifyResult) {
  emit('fail', result);
}

function onError(error: unknown) {
  emit('error', error);
}
</script>
