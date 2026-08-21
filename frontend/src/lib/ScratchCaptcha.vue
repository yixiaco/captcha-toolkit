<template>
  <div
    ref="rootRef"
    class="scratch-captcha"
    :class="{ 'is-success': status === 'success' }"
    :style="{ width: opts.width + 'px', maxWidth: '100%' }"
  >
    <div class="scratch-prompt">
      <span class="scratch-prompt-label">{{ opts.scratchTip }}</span>
      <img
        :src="promptImage"
        class="scratch-prompt-img-full"
        alt=""
        draggable="false"
      >
    </div>

    <div
      class="img-wrap"
      :class="{ shake: shaking }"
      :style="{ width: opts.width + 'px', height: opts.height + 'px' }"
    >
      <img
        v-if="image1"
        :src="image1"
        class="captcha-img"
        :alt="opts.imageAlt"
        draggable="false"
        @error="onImageError"
      >

      <canvas
        ref="canvasRef"
        class="scratch-canvas"
      />

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
        {{ opts.scratchTip }}
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
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue';
import { useCaptchaOptions } from './options';
import CaptchaLoadError from './CaptchaLoadError.vue';
import type { ScratchChallengeData, VerifyResult } from './api';
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
  /** 刮刮乐提示文案 */
  scratchTip?: string | null
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
const canvasRef = ref<HTMLCanvasElement | null>(null);
const trackRef = ref<HTMLElement | null>(null);
const status = ref<CaptchaStatus>('loading');
const image1 = ref('');
const captchaId = ref('');
const promptImage = ref('');
const pieceLeft = ref(0);
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
/** 画布 2D 上下文 */
let ctx: CanvasRenderingContext2D | null = null;

function maxLeft() {
  return Math.max(0, trackWidth - opts.handleWidth);
}

/** 当前横扫进度（0~1，与后端答案 xNorm 同口径） */
function sweepX() {
  return maxLeft() > 0 ? pieceLeft.value / maxLeft() : 0;
}

/** 绘制银色蒙版，并按当前横扫进度揭开左侧区域 */
function drawMask() {
  const canvas = canvasRef.value;
  if (!canvas) return;
  const rect = canvas.getBoundingClientRect();
  canvas.width = Math.max(1, Math.round(rect.width));
  canvas.height = Math.max(1, Math.round(rect.height));
  ctx = canvas.getContext('2d');
  if (!ctx) return;
  ctx.globalCompositeOperation = 'source-over';
  const gradient = ctx.createLinearGradient(0, 0, canvas.width, canvas.height);
  gradient.addColorStop(0, '#ccd2da');
  gradient.addColorStop(0.5, '#eef1f5');
  gradient.addColorStop(1, '#b7bfca');
  ctx.fillStyle = gradient;
  ctx.fillRect(0, 0, canvas.width, canvas.height);
  // 细碎噪点，模拟刮刮卡涂层质感
  ctx.fillStyle = 'rgba(255, 255, 255, 0.28)';
  for (let i = 0; i < 140; i++) {
    ctx.beginPath();
    ctx.arc(
      Math.random() * canvas.width,
      Math.random() * canvas.height,
      Math.random() * 1.8 + 0.4,
      0,
      Math.PI * 2,
    );
    ctx.fill();
  }
  // 从左往右揭开：滑块经过的区域露出图形
  ctx.clearRect(0, 0, Math.round(sweepX() * canvas.width), canvas.height);
}

/** 图片加载失败：切换到错误回显 */
function onImageError() {
  if (status.value !== 'success') {
    status.value = 'error';
    image1.value = '';
  }
}

/** 从后端获取刮刮乐验证码 */
async function loadCaptcha() {
  status.value = 'loading';
  image1.value = '';
  promptImage.value = '';
  pieceLeft.value = 0;
  trace = null;
  try {
    const res = await opts.api.getCaptcha<ScratchChallengeData>({
      type: 'scratch',
      debug: opts.debug ? '1' : undefined,
    });
    captchaId.value = res.id;
    image1.value = res.image1;
    promptImage.value = res.data?.promptImage || '';
    await nextTick();
    drawMask();
    status.value = 'idle';
    if (opts.debug && rootRef.value) {
      rootRef.value.dataset.captchaId = res.id;
      if (res.data?.debugX != null) {
        rootRef.value.dataset.debugX = String(res.data.debugX);
      }
      if (res.data?.debugTargets) {
        rootRef.value.dataset.debugTargets = JSON.stringify(res.data.debugTargets);
      }
      if (res.data?.debugPatterns) {
        rootRef.value.dataset.debugPatterns = JSON.stringify(res.data.debugPatterns);
      }
    }
  } catch (error) {
    console.error('加载刮刮乐验证码失败', error);
    emit('error', error);
    status.value = 'error';
  }
}

/** 记录滑块当前位置（而不是指针位置，避免手柄偏移导致终点对不上） */
function trackPoint(event: PointerEvent, type: 0 | 1 | 2) {
  // 移动事件只按 16ms（约 60fps）采样一次，避免轨迹无限膨胀
  const now = Date.now();
  if (type === 1 && lastTraceAt > 0 && now - lastTraceAt < 16) return;
  lastTraceAt = now;
  const rootRect = dragRect || rootRef.value!.getBoundingClientRect();
  const y = Math.min(1, Math.max(0, (event.clientY - rootRect.top) / rootRect.height));
  pushNormalizedPoint(trace!, sweepX(), y, type);
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
  drawMask();
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
      type: 'scratch',
      xNorm: sweepX(),
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
        drawMask();
        if (opts.autoReload) {
          loadCaptcha();
        }
      }, 450);
    }
  } catch (error) {
    console.error('刮刮乐验证请求失败', error);
    emit('error', error);
    shaking.value = true;
    setTimeout(() => {
      shaking.value = false;
      pieceLeft.value = 0;
      drawMask();
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
