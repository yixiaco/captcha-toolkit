<template>
  <CaptchaModal
    v-if="display === 'modal'"
    v-bind="attrs"
    :visible="visible"
    :mode="mode"
    @close="emit('close')"
    @success="(result) => emit('success', result)"
  />
  <component
    :is="inlineComponent"
    v-else
    v-bind="attrs"
    @success="(result) => emit('success', result)"
    @fail="(result) => emit('fail', result)"
    @error="(error) => emit('error', error)"
  />
</template>

<script setup>
import { computed, useAttrs } from 'vue'
import CaptchaModal from './CaptchaModal.vue'
import SliderCaptcha from './SliderCaptcha.vue'
import ClickCaptcha from './ClickCaptcha.vue'
import RotateCaptcha from './RotateCaptcha.vue'

const props = defineProps({
  /** 展示方式：inline 嵌入页面 / modal 弹窗 */
  display: { type: String, default: 'modal' },
  /** 验证模式：slider / click / rotate */
  mode: { type: String, default: 'slider' },
  /** 弹窗是否可见（仅 display=modal 生效） */
  visible: { type: Boolean, default: false },
})

const emit = defineEmits(['success', 'close', 'fail', 'error'])
const attrs = useAttrs()

const inlineComponent = computed(() => {
  if (props.mode === 'click') return ClickCaptcha
  if (props.mode === 'rotate') return RotateCaptcha
  return SliderCaptcha
})
</script>
