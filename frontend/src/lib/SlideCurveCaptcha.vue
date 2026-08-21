<template>
  <div
    ref="rootRef"
    class="slide-curve-captcha"
    :class="{ 'is-success': status === 'success' }"
    :style="{ width: imgWidth + 'px', maxWidth: '100%' }"
  >
    <div
      class="img-wrap"
      :style="{ width: imgWidth + 'px', height: imgHeight + 'px' }"
    >
      <img
        v-if="image1"
        :src="image1"
        class="captcha-img"
        :alt="opts.imageAlt"
        draggable="false"
      >

      <canvas
        ref="canvasRef"
        class="slide-curve-canvas"
        :width="imgWidth"
        :height="imgHeight"
      />

      <div
        v-if="status === 'loading'"
        class="loading-mask"
      >
        <div class="spinner" />
        <span>{{ opts.loadingText }}</span>
      </div>

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
        {{ opts.slideCurveTip }}
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
import type { ChallengePoint, SlideCurveChallengeData, VerifyResult } from './api';
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
  /** 滑动曲线提示文案 */
  slideCurveTip?: string | null
  /** 摆动曲线颜色 */
  slideCurveColor?: string | null
  /** 滑块手柄宽度（px） */
  handleWidth?: number | null
  /** 是否请求调试答案 */
  debug?: boolean | null
  /** 失败后自动刷新 */
  autoReload?: boolean | null
  /** 加载提示文案 */
  loadingText?: string | null
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
const pieceLeft = ref(0);
const dragging = ref(false);
const shaking = ref(false);
const imgWidth = ref(opts.width);
const imgHeight = ref(opts.height);

/** 服务端返回的摆动曲线参数 */
let curveData: SlideCurveChallengeData | null = null;
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

/** 当前摆动量（0~1，与后端校验口径一致） */
function swing() {
  return trackWidth > 0 ? pieceLeft.value / trackWidth : 0;
}

/** 在透明画布上绘制当前摆动量下的曲线与固定端点 */
function drawCurve() {
  const canvas = canvasRef.value;
  const ctx = canvas?.getContext('2d');
  if (!canvas || !ctx || !curveData) return;
  ctx.clearRect(0, 0, canvas.width, canvas.height);

  const endpoints = curveData.endpoints || [];
  const shape = curveData.shape || [];
  const amplitude = curveData.amplitude || 0;
  if (endpoints.length < 2 || shape.length < 2) return;

  const start = endpoints[0];
  const end = endpoints[endpoints.length - 1];
  const factor = (swing() * 2 - 1) * amplitude;
  const points: ChallengePoint[] = [];
  for (let i = 0; i < shape.length; i++) {
    const u = i / (shape.length - 1);
    points.push({
      x: start.x + (end.x - start.x) * u,
      y: start.y + (end.y - start.y) * u + factor * shape[i],
    });
  }

  // 曲线先画深色底边保证可见，再画亮色曲线
  ctx.lineCap = 'round';
  ctx.lineJoin = 'round';
  ctx.strokeStyle = 'rgba(0, 0, 0, 0.35)';
  ctx.lineWidth = 6;
  strokePath(ctx, points);
  ctx.strokeStyle = opts.slideCurveColor;
  ctx.lineWidth = 4;
  strokePath(ctx, points);

  // 两端固定点标记
  for (const anchor of [start, end]) {
    ctx.fillStyle = 'rgba(15, 20, 30, 0.7)';
    ctx.beginPath();
    ctx.arc(anchor.x, anchor.y, 7, 0, Math.PI * 2);
    ctx.fill();
    ctx.fillStyle = '#ffffff';
    ctx.beginPath();
    ctx.arc(anchor.x, anchor.y, 6, 0, Math.PI * 2);
    ctx.fill();
    ctx.fillStyle = opts.slideCurveColor;
    ctx.beginPath();
    ctx.arc(anchor.x, anchor.y, 3, 0, Math.PI * 2);
    ctx.fill();
  }
}

/** 沿点列表绘制一条折线路径 */
function strokePath(ctx: CanvasRenderingContext2D, points: ChallengePoint[]) {
  ctx.beginPath();
  ctx.moveTo(points[0].x, points[0].y);
  for (let i = 1; i < points.length; i++) {
    ctx.lineTo(points[i].x, points[i].y);
  }
  ctx.stroke();
}

/** 记录滑块当前位置（而不是指针位置，避免手柄偏移导致终点对不上） */
function trackPoint(event: PointerEvent, type: 0 | 1 | 2) {
  // 移动事件只按 16ms（约 60fps）采样一次，避免轨迹无限膨胀
  const now = Date.now();
  if (type === 1 && lastTraceAt > 0 && now - lastTraceAt < 16) return;
  lastTraceAt = now;
  const rootRect = dragRect || rootRef.value!.getBoundingClientRect();
  const y = Math.min(1, Math.max(0, (event.clientY - rootRect.top) / rootRect.height));
  const x = swing();
  pushNormalizedPoint(trace!, x, y, type);
}

async function loadCaptcha() {
  status.value = 'loading';
  image1.value = '';
  curveData = null;
  trace = null;
  try {
    const res = await opts.api.getCaptcha<SlideCurveChallengeData>({
      type: 'slide-curve',
      debug: opts.debug ? '1' : undefined,
    });
    captchaId.value = res.id;
    image1.value = res.image1;
    curveData = res.data || null;
    // 以后端实际图片尺寸为准，避免前端配置宽度与后端不一致导致坐标换算错误
    imgWidth.value = res.width || opts.width;
    imgHeight.value = res.height || opts.height;
    await nextTick();
    trackWidth = trackRef.value ? trackRef.value.clientWidth : imgWidth.value;
    pieceLeft.value = 0;
    drawCurve();
    status.value = 'idle';
    if (opts.debug && rootRef.value) {
      rootRef.value.dataset.captchaId = res.id;
      if (res.data?.debugSwing != null) {
        rootRef.value.dataset.debugSwing = String(res.data.debugSwing);
      }
      if (res.data?.debugFakeTargets) {
        rootRef.value.dataset.debugFakeTargets = JSON.stringify(
          res.data.debugFakeTargets.map((p) => ({ x: p.x, y: p.y }))
        );
      }
    }
  } catch (error) {
    console.error('加载滑动曲线验证码失败', error);
    emit('error', error);
    status.value = 'idle';
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
  drawCurve();
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
      type: 'slide-curve',
      xNorm: swing(),
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
        drawCurve();
        if (opts.autoReload) {
          loadCaptcha();
        }
      }, 450);
    }
  } catch (error) {
    console.error('滑动曲线验证请求失败', error);
    emit('error', error);
    shaking.value = true;
    setTimeout(() => {
      shaking.value = false;
      pieceLeft.value = 0;
      drawCurve();
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
