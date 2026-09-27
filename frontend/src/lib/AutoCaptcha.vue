<template>
  <component
    :is="innerComponent"
    v-if="challenge"
    :key="challenge.id"
    v-bind="bindings"
    :challenge="challenge"
    @success="onSuccess"
    @fail="onFail"
    @error="onError"
    @refresh="onRefresh"
  />
  <div
    v-else
    class="auto-captcha"
    :style="{ width: opts.width + 'px', maxWidth: '100%' }"
  >
    <div
      class="img-wrap"
      :style="{ width: opts.width + 'px', height: opts.height + 'px' }"
    >
      <CaptchaLoadError
        v-if="status === 'error'"
        :text="opts.loadFailedText"
        :retry-text="opts.retryText"
        @retry="reload"
      />
      <div
        v-else
        class="loading-mask"
      >
        <div class="spinner" />
        <span>{{ opts.loadingText }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, useAttrs } from 'vue';
import type { Component } from 'vue';
import SliderCaptcha from './SliderCaptcha.vue';
import ClickCaptcha from './ClickCaptcha.vue';
import RotateCaptcha from './RotateCaptcha.vue';
import AngleCaptcha from './AngleCaptcha.vue';
import ScratchCaptcha from './ScratchCaptcha.vue';
import CurveCaptcha from './CurveCaptcha.vue';
import SlideCurveCaptcha from './SlideCurveCaptcha.vue';
import SwingTileCaptcha from './SwingTileCaptcha.vue';
import CaptchaLoadError from './CaptchaLoadError.vue';
import { useCaptchaOptions } from './options';
import type { CaptchaChallenge, RequestFunction, VerifyResult } from './api';
import type { ClientType } from './types';

interface Props {
  /**
   * 类型提示：auto 表示完全由后端决定。
   *
   * <p>该提示只在 debug 模式下会随请求带给后端；非 debug 模式下后端会忽略它，
   * 组件一律按响应里的 type 渲染。</p>
   */
  mode?: string | null
  /** 自定义 API 客户端 */
  api?: object | null
  /** 后端接口前缀 */
  baseUrl?: string | null
  /** 自定义请求函数 */
  request?: RequestFunction | null
  /** 是否请求调试答案（同时决定前端能否指定类型/形状） */
  debug?: boolean | null
  /** 初始形状（仅 debug 模式下会带给后端） */
  shape?: string | null
  /** 验证图片宽度（px），用于加载占位 */
  width?: number | null
  /** 验证图片高度（px），用于加载占位 */
  height?: number | null
  /** 加载中提示文案 */
  loadingText?: string | null
  /** 加载失败提示文案 */
  loadFailedText?: string | null
  /** 重试按钮文案 */
  retryText?: string | null
  /** 客户端类型：web / h5 / mini_program */
  clientType?: ClientType | null
}

const props = withDefaults(defineProps<Props>(), {
  mode: 'auto',
  debug: null,
});

// 根节点是 fragment：宿主 attrs 由 bindings 显式透传给具体组件，这里不再自动继承
defineOptions({ inheritAttrs: false });

const emit = defineEmits<{
  (e: 'success', result: VerifyResult): void
  (e: 'fail', result: VerifyResult): void
  (e: 'error', error: unknown): void
}>();

const opts = useCaptchaOptions(props);
const attrs = useAttrs();
const status = ref<'loading' | 'ready' | 'error'>('loading');
const challenge = ref<CaptchaChallenge | null>(null);
/** 具体组件在受控模式下的额外请求参数（如滑块形状） */
let extraParams: Record<string, unknown> = {};

/** 通用配置：所有验证码组件都声明过的 props */
const COMMON_KEYS = ['api', 'baseUrl', 'request', 'width', 'height', 'debug',
  'autoReload', 'loadingText', 'loadFailedText', 'retryText', 'imageAlt', 'clientType'];

/** 各类型的专有配置：只透传该类型组件声明过的 props，避免落到 DOM 上 */
const CHILD_KEYS: Record<string, string[]> = {
  slider: ['shape', 'shapes', 'shapeLabels', 'showShapePicker', 'handleWidth',
    'shapeLabel', 'randomLabel', 'sliderTip'],
  click: ['promptPrefix', 'markMinDistance'],
  'click-shape': ['markMinDistance'],
  rotate: ['rotateTip', 'handleWidth'],
  angle: ['angleTip', 'handleWidth'],
  scratch: ['scratchTip', 'handleWidth'],
  curve: ['curveTip', 'curveColor', 'curveWidth'],
  'slide-curve': ['slideCurveTip', 'slideCurveColor', 'handleWidth'],
  'swing-tile': ['shape', 'swingTileTip', 'handleWidth'],
};

/** 按后端返回的类型选择具体组件 */
function resolveComponent(type?: string): Component {
  switch (type) {
    case 'click':
    case 'click-shape':
      return ClickCaptcha;
    case 'rotate':
      return RotateCaptcha;
    case 'angle':
      return AngleCaptcha;
    case 'scratch':
      return ScratchCaptcha;
    case 'curve':
      return CurveCaptcha;
    case 'slide-curve':
      return SlideCurveCaptcha;
    case 'swing-tile':
      return SwingTileCaptcha;
    default:
      return SliderCaptcha;
  }
}

/** 组件内置可渲染的类型（自定义类型请自行用 challenge 属性接入具体组件） */
const KNOWN_TYPES = ['slider', 'click', 'click-shape', 'rotate', 'angle',
  'scratch', 'curve', 'slide-curve', 'swing-tile'];

const innerComponent = computed<Component>(() => resolveComponent(challenge.value?.type));

/** 透传给具体组件的配置：解析后的全局配置打底，宿主 attrs 优先 */
const bindings = computed<Record<string, unknown>>(() => {
  const type = challenge.value?.type || 'slider';
  const result: Record<string, unknown> = {};
  for (const key of [...COMMON_KEYS, ...(CHILD_KEYS[type] || [])]) {
    result[key] = (opts as unknown as Record<string, unknown>)[key];
  }
  // 点选类共用同一组件，需要显式把类型编码交下去（校验时后端要求与会话类型一致）
  if (type === 'click' || type === 'click-shape') {
    result.type = type;
    if (type === 'click-shape') {
      result.promptPrefix = opts.clickShapeTip;
    }
  }
  return { ...result, ...attrs };
});

/** 组装下发请求：非 debug 下不带 type/shape，类型与形状都由后端决定 */
function buildParams(): Record<string, unknown> {
  const params: Record<string, unknown> = { ...extraParams };
  if (opts.debug) {
    const hint = (props.mode || '').trim();
    params.debug = '1';
    if (hint && hint !== 'auto' && params.type == null) {
      params.type = hint;
    }
    if (params.shape == null && opts.shape) {
      params.shape = opts.shape;
    }
  }
  return params;
}

/** 向后端要一张验证码，并按返回的类型渲染对应组件 */
async function load(params: Record<string, unknown> = {}) {
  extraParams = { ...extraParams, ...params };
  status.value = 'loading';
  challenge.value = null;
  try {
    const res = await opts.api.getCaptcha(buildParams());
    if (!KNOWN_TYPES.includes(res.type)) {
      throw new Error(`后端下发了未知的验证码类型: ${res.type}；`
        + '自定义类型请把响应交给对应的具体组件渲染，或检查 captcha.types 配置');
    }
    challenge.value = res;
    status.value = 'ready';
  } catch (error) {
    console.error('加载验证码失败', error);
    status.value = 'error';
    emit('error', error);
  }
}

function reload() {
  load();
}

/** 具体组件要求换一张：合并它带来的参数（如滑块形状）后重新下发 */
function onRefresh(params?: Record<string, unknown>) {
  load(params || {});
}

function onSuccess(result: VerifyResult) {
  emit('success', result);
}

function onFail(result: VerifyResult) {
  emit('fail', result);
}

function onError(error: unknown) {
  emit('error', error);
}

onMounted(load);

defineExpose({ reload });
</script>
