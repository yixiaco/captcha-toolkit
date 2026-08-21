<template>
  <div
    ref="rootRef"
    class="angle-captcha"
    :class="{ 'is-success': status === 'success' }"
    :style="{ width: opts.width + 'px', maxWidth: '100%' }"
  >
    <div
      class="img-wrap"
      :style="{ width: opts.width + 'px', height: opts.height + 'px' }"
    >
      <img
        v-if="image2"
        :src="image2"
        class="angle-disc"
        alt=""
        draggable="false"
        :style="discStyle"
        @error="onImageError"
      >

      <div
        v-if="status === 'loading'"
        class="loading-mask"
      >
        <div class="spinner" />
        <span>{{ opts.loadingText }}</span>
      </div>

      <CaptchaLoadError
        v-if="status === 'error'"
        :text="opts.loadFailedText"
        :retry-text="opts.retryText"
        @retry="loadCaptcha"
      />

      <transition name="fade">
        <div
          v-if="status === 'success'"
          class="success-mask"
        >
          <div class="success-icon">
            ✓
          </div>
        </div>
      </transition>
    </div>

    <div
      ref="trackRef"
      class="slider-track"
      :class="{ shake: shaking }"
    >
      <div
        class="slider-progress"
        :style="{ width: pieceLeft + 'px' }"
      />

      <div
        v-if="status === 'idle' && !dragging"
        class="slider-tip"
      >
        {{ opts.angleTip }}
      </div>

      <div
        class="slider-handle"
        :class="{ dragging }"
        :style="{ left: pieceLeft + 'px', width: opts.handleWidth + 'px' }"
        @pointerdown.prevent="onPointerDown"
      >
        <svg
          v-if="status !== 'success'"
          viewBox="0 0 24 24"
          width="18"
          height="18"
          fill="none"
          stroke="currentColor"
          stroke-width="2.4"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <path d="M9 6l6 6-6 6" />
        </svg>
        <svg
          v-else
          viewBox="0 0 24 24"
          width="18"
          height="18"
          fill="none"
          stroke="currentColor"
          stroke-width="2.8"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <path d="M5 12l5 5 9-10" />
        </svg>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue';
import { useCaptchaOptions } from './options';
import CaptchaLoadError from './CaptchaLoadError.vue';
import type { AngleChallengeData, VerifyResult } from './api';
import type { CaptchaStatus, ClientType } from './types';
import { createTrace, pushNormalizedPoint, buildCompressedTrace } from './trace';
import type { BehaviorTrace } from './trace';

interface Props {
  /** 自定义 API 客户端 */
  api?: object | null
  /** 后端接口前缀 */
  baseUrl?: string | null
  /** 自定义请求函数 */
  request?: unknown
  /** 验证图片宽度（px） */
  width?: number | null
  /** 验证图片高度（px） */
  height?: number | null
  /** 角度验证提示文案 */
  angleTip?: string | null
  /** 滑块手柄宽度（px） */
  handleWidth?: number | null
  /** 是否请求调试答案 */
  debug?: boolean | null
  /** 失败后自动刷新 */
  autoReload?: boolean | null
  /** 加载提示文案 */
  loadingText?: string | null
  /** 加载失败提示文案 */
  loadFailedText?: string | null
  /** 重试按钮文案 */
  retryText?: string | null
  /** 图片 alt 文案 */
  imageAlt?: string | null
  /** 客户端类型：web / h5 / mini_program */
  clientType?: ClientType | null
}

// 布尔可选 props 统一用 null 作为“未传”标记，避免 Vue 默认 false 覆盖全局配置
const props = withDefaults(defineProps<Props>(), {
  debug: null,
  autoReload: null,
});

const emit = defineEmits<{
  (e: 'success', result: VerifyResult): void
  (e: 'fail', result: VerifyResult): void
  (e: 'error', error: unknown): void
}>();

const opts = useCaptchaOptions(props);

const rootRef = ref<HTMLElement | null>(null);
const trackRef = ref<HTMLElement | null>(null);
const status = ref<CaptchaStatus>('loading');
const image2 = ref('');
const captchaId = ref('');
const discSize = ref(0);
const pieceLeft = ref(0);
const rotation = ref(0);
const dragging = ref(false);
const shaking = ref(false);

let trackWidth = 0;
let startClientX = 0;
let startLeft = 0;
/** 当前拖拽的行为轨迹 */
let trace: BehaviorTrace | null = null;
/** 按下时缓存的容器矩形，避免移动中反复读取布局 */
let dragRect: DOMRect | null = null;
/** 上次轨迹采样时间（用于 16ms 节流） */
let lastTraceAt = 0;

function maxLeft() {
  return Math.max(0, trackWidth - opts.handleWidth);
}

/** 图片加载失败：切换到错误回显，隐藏无法显示的图片 */
function onImageError() {
  if (status.value !== 'success') {
    status.value = 'error';
    image2.value = '';
  }
}

/** 圆形图样式：居中 + 当前旋转角度 */
const discStyle = computed(() => ({
  width: `${discSize.value}px`,
  height: `${discSize.value}px`,
  left: '50%',
  top: '50%',
  transform: `translate(-50%, -50%) rotate(${rotation.value}deg)`,
}));

/** 记录滑块当前位置（而不是指针位置，避免手柄偏移导致终点对不上） */
function trackPoint(event: PointerEvent, type: 0 | 1 | 2) {
  // 移动事件只按 16ms（约 60fps）采样一次，避免轨迹无限膨胀
  const now = Date.now();
  if (type === 1 && lastTraceAt > 0 && now - lastTraceAt < 16) return;
  lastTraceAt = now;
  const rootRect = dragRect || rootRef.value!.getBoundingClientRect();
  const y = Math.min(1, Math.max(0, (event.clientY - rootRect.top) / rootRect.height));
  // 角度由手柄最大行程（trackWidth - handleWidth）映射到 360°，轨迹必须同口径
  const x = maxLeft() > 0 ? pieceLeft.value / maxLeft() : 0;
  pushNormalizedPoint(trace!, x, y, type);
}

/** 从后端获取角度验证码：背景图 + 已错位旋转的圆盘图 */
async function loadCaptcha() {
  status.value = 'loading';
  image2.value = '';
  discSize.value = 0;
  trace = null;
  try {
    const res = await opts.api.getCaptcha<AngleChallengeData>({
      type: 'angle',
      debug: opts.debug ? '1' : undefined,
    });
    captchaId.value = res.id;
    image2.value = res.image2 || '';
    discSize.value = res.data?.discSize
      || Math.round(Math.min(opts.width, opts.height) * 0.6);
    pieceLeft.value = 0;
    rotation.value = 0;
    status.value = 'idle';
    if (opts.debug && rootRef.value) {
      rootRef.value.dataset.captchaId = res.id;
      if (res.data?.debugAngle != null) {
        rootRef.value.dataset.debugAngle = String(res.data.debugAngle);
      }
    }
  } catch (error) {
    console.error('加载角度验证码失败', error);
    emit('error', error);
    status.value = 'error';
  }
}

function onPointerDown(event: PointerEvent) {
  if (status.value !== 'idle') return;
  dragging.value = true;
  trace = createTrace(rootRef.value);
  dragRect = rootRef.value!.getBoundingClientRect();
  lastTraceAt = 0;
  trackPoint(event, 0);
  startClientX = event.clientX;
  startLeft = pieceLeft.value;
  window.addEventListener('pointermove', onPointerMove);
  window.addEventListener('pointerup', onPointerUp);
  window.addEventListener('pointercancel', onPointerUp);
}

function onPointerMove(event: PointerEvent) {
  if (!dragging.value) return;
  const next = startLeft + event.clientX - startClientX;
  pieceLeft.value = Math.min(maxLeft(), Math.max(0, next));
  rotation.value = (pieceLeft.value / maxLeft()) * 360;
  trackPoint(event, 1);
}

async function onPointerUp(event: PointerEvent) {
  if (!dragging.value) return;
  dragging.value = false;
  window.removeEventListener('pointermove', onPointerMove);
  window.removeEventListener('pointerup', onPointerUp);
  window.removeEventListener('pointercancel', onPointerUp);
  trackPoint(event, 2);
  const td = await buildCompressedTrace(trace!);
  trace = null;
  dragRect = null;

  try {
    const res = await opts.api.verify({
      id: captchaId.value,
      type: 'angle',
      angle: rotation.value % 360,
      clientType: opts.clientType,
      td,
    });
    if (res.success) {
      status.value = 'success';
      emit('success', res);
    } else {
      emit('fail', res);
      shaking.value = true;
      setTimeout(() => {
        shaking.value = false;
        pieceLeft.value = 0;
        rotation.value = 0;
        if (opts.autoReload) {
          loadCaptcha();
        }
      }, 450);
    }
  } catch (error) {
    console.error('角度验证请求失败', error);
    emit('error', error);
    shaking.value = true;
    setTimeout(() => {
      shaking.value = false;
      pieceLeft.value = 0;
      rotation.value = 0;
    }, 450);
  }
}

onMounted(async () => {
  await nextTick();
  trackWidth = trackRef.value ? trackRef.value.clientWidth : opts.width;
  loadCaptcha();
});

onBeforeUnmount(() => {
  window.removeEventListener('pointermove', onPointerMove);
  window.removeEventListener('pointerup', onPointerUp);
  window.removeEventListener('pointercancel', onPointerUp);
});

defineExpose({ reload: loadCaptcha });
</script>
