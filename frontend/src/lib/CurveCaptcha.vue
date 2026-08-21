<template>
  <div
    ref="rootRef"
    class="curve-captcha"
    :class="{ 'is-success': status === 'success' }"
  >
    <div
      ref="wrapRef"
      class="img-wrap"
      :class="{ shake: shaking }"
      :style="{ width: opts.width + 'px', height: opts.height + 'px' }"
    >
      <img
        v-if="image1"
        ref="imageRef"
        :src="image1"
        class="captcha-img"
        :alt="opts.imageAlt"
        draggable="false"
      >

      <canvas
        ref="canvasRef"
        class="curve-canvas"
        :width="opts.width"
        :height="opts.height"
        @pointerdown="onPointerDown"
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

    <div class="curve-tip">
      <span
        class="curve-dot start"
        aria-hidden="true"
      />
      <span>{{ opts.curveTip }}</span>
      <span
        class="curve-dot end"
        aria-hidden="true"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue';
import { useCaptchaOptions } from './options';
import type { VerifyResult } from './api';
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
  /** 曲线绘制提示文案 */
  curveTip?: string | null
  /** 用户绘制笔迹颜色 */
  curveColor?: string | null
  /** 用户绘制笔迹宽度（px） */
  curveWidth?: number | null
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
const wrapRef = ref<HTMLElement | null>(null);
const imageRef = ref<HTMLImageElement | null>(null);
const canvasRef = ref<HTMLCanvasElement | null>(null);
const status = ref<CaptchaStatus>('loading');
const image1 = ref('');
const captchaId = ref('');
const shaking = ref(false);
const drawing = ref(false);
const submitting = ref(false);

/** 当前绘制的归一化答案点 */
let curvePoints: Array<{ x: number; y: number }> = [];
/** 当前绘制行为轨迹 */
let trace: BehaviorTrace | null = null;
/** 上一次轨迹采样时间（用于 16ms 节流） */
let lastTraceAt = 0;
/** 按下时缓存的画布矩形，避免移动中反复读取布局 */
let drawRect: DOMRect | null = null;
/** 画布 2D 上下文 */
let ctx: CanvasRenderingContext2D | null = null;

/** 清空画布与已绘制的答案点 */
function clearCanvas() {
  curvePoints = [];
  trace = null;
  lastTraceAt = 0;
  drawRect = null;
  if (canvasRef.value) {
    ctx = canvasRef.value.getContext('2d');
    ctx?.clearRect(0, 0, canvasRef.value.width, canvasRef.value.height);
  }
}

/** 记录一个轨迹点，并同步到绘制笔迹 */
function addPoint(event: PointerEvent, type: 0 | 1 | 2) {
  // 移动事件按 16ms（约 60fps）采样，避免轨迹无限膨胀
  const now = Date.now();
  if (type === 1 && lastTraceAt > 0 && now - lastTraceAt < 16) return;
  lastTraceAt = now;

  const rect = drawRect || imageRef.value!.getBoundingClientRect();
  const x = Math.min(1, Math.max(0, (event.clientX - rect.left) / rect.width));
  const y = Math.min(1, Math.max(0, (event.clientY - rect.top) / rect.height));
  const previous = curvePoints[curvePoints.length - 1];
  curvePoints.push({ x, y });

  // 把坐标归一化到画布 0~1，与后端校验口径一致
  pushNormalizedPoint(trace!, x, y, type);

  // 在透明画布上绘制本次笔迹
  if (!ctx) return;
  ctx.strokeStyle = opts.curveColor;
  ctx.lineWidth = opts.curveWidth;
  ctx.lineCap = 'round';
  ctx.lineJoin = 'round';
  ctx.beginPath();
  if (previous) {
    ctx.moveTo(previous.x * rect.width, previous.y * rect.height);
  } else {
    ctx.moveTo(x * rect.width, y * rect.height);
  }
  ctx.lineTo(x * rect.width, y * rect.height);
  ctx.stroke();
}

async function loadCaptcha() {
  status.value = 'loading';
  image1.value = '';
  clearCanvas();
  try {
    const res = await opts.api.getCaptcha({
      type: 'curve',
      debug: opts.debug ? '1' : undefined,
    });
    captchaId.value = res.id;
    image1.value = res.image1;
    status.value = 'idle';
    await nextTick();
    if (opts.debug && rootRef.value) {
      rootRef.value.dataset.captchaId = res.id;
      if (res.debugCurve) {
        rootRef.value.dataset.debugCurve = JSON.stringify(
          res.debugCurve.map((p) => ({ x: p.x, y: p.y }))
        );
      }
    }
  } catch (error) {
    console.error('加载曲线验证码失败', error);
    emit('error', error);
    status.value = 'idle';
  }
}

function onPointerDown(event: PointerEvent) {
  if (status.value !== 'idle' || drawing.value || submitting.value) return;
  drawing.value = true;
  trace = createTrace(imageRef.value);
  drawRect = imageRef.value!.getBoundingClientRect();
  lastTraceAt = 0;
  ctx = canvasRef.value!.getContext('2d');
  addPoint(event, 0);
  window.addEventListener('pointermove', onPointerMove);
  window.addEventListener('pointerup', onPointerUp);
  window.addEventListener('pointercancel', onPointerUp);
}

function onPointerMove(event: PointerEvent) {
  if (!drawing.value) return;
  addPoint(event, 1);
}

async function onPointerUp(event: PointerEvent) {
  if (!drawing.value) return;
  drawing.value = false;
  window.removeEventListener('pointermove', onPointerMove);
  window.removeEventListener('pointerup', onPointerUp);
  window.removeEventListener('pointercancel', onPointerUp);
  addPoint(event, 2);
  submit();
}

/** 提交绘制曲线与行为轨迹到后端 */
async function submit() {
  if (curvePoints.length < 2 || !trace) {
    resetAfterFail();
    return;
  }
  submitting.value = true;
  const td = await buildCompressedTrace(trace);
  trace = null;
  try {
    const res = await opts.api.verify({
      id: captchaId.value,
      type: 'curve',
      curve: curvePoints,
      clientType: opts.clientType,
      td,
    });
    if (res.success) {
      status.value = 'success';
      emit('success', res);
    } else {
      emit('fail', res);
      resetAfterFail();
    }
  } catch (error) {
    console.error('曲线验证请求失败', error);
    emit('error', error);
    resetAfterFail();
  } finally {
    submitting.value = false;
  }
}

/** 失败后的清理：清空笔迹并提示抖动，随后按配置自动刷新 */
function resetAfterFail() {
  clearCanvas();
  shaking.value = true;
  setTimeout(() => {
    shaking.value = false;
    if (opts.autoReload) {
      loadCaptcha();
    }
  }, 450);
}

onMounted(async () => {
  await nextTick();
  loadCaptcha();
});

onBeforeUnmount(() => {
  window.removeEventListener('pointermove', onPointerMove);
  window.removeEventListener('pointerup', onPointerUp);
  window.removeEventListener('pointercancel', onPointerUp);
});

defineExpose({ reload: loadCaptcha });
</script>
