<template>
  <CaptchaModal
    v-if="display === 'modal'"
    v-bind="attrs"
    :visible="visible"
    :mode="mode"
    @close="onClose"
    @success="onSuccess"
  />
  <FloatingCaptcha
    v-else-if="display === 'floating'"
    v-bind="attrs"
    :mode="mode"
    @success="onSuccess"
    @fail="onFail"
    @error="onError"
    @close="onClose"
  />
  <component
    :is="inlineComponent"
    v-else
    v-bind="attrs"
    @success="onSuccess"
    @fail="onFail"
    @error="onError"
  />
</template>

<script setup lang="ts">
import { computed, useAttrs } from 'vue';
import type { Component } from 'vue';
import CaptchaModal from './CaptchaModal.vue';
import FloatingCaptcha from './FloatingCaptcha.vue';
import SliderCaptcha from './SliderCaptcha.vue';
import ClickCaptcha from './ClickCaptcha.vue';
import RotateCaptcha from './RotateCaptcha.vue';
import AngleCaptcha from './AngleCaptcha.vue';
import ScratchCaptcha from './ScratchCaptcha.vue';
import CurveCaptcha from './CurveCaptcha.vue';
import SlideCurveCaptcha from './SlideCurveCaptcha.vue';
import SwingTileCaptcha from './SwingTileCaptcha.vue';
import type { VerifyResult } from './api';
import type { CaptchaMode } from './types';

interface Props {
  /** 展示方式：inline 嵌入页面 / modal 弹窗 / floating 浮动按钮 */
  display?: string
  /** 验证模式：slider / click / rotate */
  mode?: CaptchaMode | string
  /** 弹窗是否可见（仅 display=modal 生效） */
  visible?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  display: 'modal',
  mode: 'slider',
  visible: false,
});

const emit = defineEmits<{
  (e: 'success', result: VerifyResult): void
  (e: 'close'): void
  (e: 'fail', result: VerifyResult): void
  (e: 'error', error: unknown): void
}>();
const attrs = useAttrs();

const inlineComponent = computed<Component>(() => {
  if (props.mode === 'click') return ClickCaptcha;
  if (props.mode === 'rotate') return RotateCaptcha;
  if (props.mode === 'angle') return AngleCaptcha;
  if (props.mode === 'scratch') return ScratchCaptcha;
  if (props.mode === 'curve') return CurveCaptcha;
  if (props.mode === 'slide-curve') return SlideCurveCaptcha;
  if (props.mode === 'swing-tile') return SwingTileCaptcha;
  return SliderCaptcha;
});

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
