<template>
  <div
    ref="rootRef"
    class="swing-tile-captcha"
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

      <img
        v-if="image2 && curveData"
        :src="image2"
        class="swing-tile-piece"
        alt=""
        draggable="false"
        :style="pieceStyle"
      >

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
        {{ opts.swingTileTip }}
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
import type { ChallengePoint, SwingTileChallengeData, VerifyResult } from './api';
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
  /** 滑块摆动图块提示文案 */
  swingTileTip?: string | null
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
const trackRef = ref<HTMLElement | null>(null);
const status = ref<CaptchaStatus>('loading');
const image1 = ref('');
const image2 = ref('');
const captchaId = ref('');
const pieceLeft = ref(0);
const dragging = ref(false);
const shaking = ref(false);
const imgWidth = ref(opts.width);
const imgHeight = ref(opts.height);

/** 服务端返回的贝塞尔路径与摆动参数 */
let curveData: SwingTileChallengeData | null = null;
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

/** 当前滑块位置（0~1，映射到贝塞尔路径全程） */
function swing() {
  const max = maxLeft();
  return max > 0 ? Math.min(1, Math.max(0, pieceLeft.value / max)) : 0;
}

/** de Casteljau：计算多阶贝塞尔曲线上的点 */
function bezier(points: ChallengePoint[], t: number): ChallengePoint {
  let current = points.map((p) => ({ x: p.x, y: p.y }));
  while (current.length > 1) {
    const next: ChallengePoint[] = [];
    for (let i = 0; i < current.length - 1; i++) {
      next.push({
        x: current[i].x + (current[i + 1].x - current[i].x) * t,
        y: current[i].y + (current[i + 1].y - current[i].y) * t,
      });
    }
    current = next;
  }
  return current[0];
}

/** 变速缓动：smoothstep，与后端一致——起点/终点慢、中间快，不与滑块位移 1:1 */
function ease(t: number) {
  const x = Math.min(1, Math.max(0, t));
  return x * x * (3 - 2 * x);
}

/** 图块当前的位置与方向：沿贝塞尔路径移动，方向随路径摆动，终点对准真凹槽 */
const pieceStyle = computed(() => {
  if (!curveData || !curveData.path || curveData.path.length < 2) return {};
  // 使用缓动后的参数，让图块忽快忽慢，而不是跟着滑块匀速/等距移动
  const u = ease(swing());
  const point = bezier(curveData.path, u);
  const size = curveData.pieceSize || 0;
  const rotation = (curveData.endRotation || 0)
    + ((curveData.startRotation || 0) - (curveData.endRotation || 0)) * (1 - u)
    + (curveData.swingAmplitude || 0) * Math.sin(Math.PI * u);
  return {
    width: `${size}px`,
    height: `${size}px`,
    left: `${point.x - size / 2}px`,
    top: `${point.y - size / 2}px`,
    transform: `rotate(${rotation}deg)`,
  };
});

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
  image2.value = '';
  curveData = null;
  trace = null;
  try {
    const res = await opts.api.getCaptcha<SwingTileChallengeData>({
      type: 'swing-tile',
      debug: opts.debug ? '1' : undefined,
    });
    captchaId.value = res.id;
    image1.value = res.image1;
    image2.value = res.image2 || '';
    curveData = res.data || null;
    imgWidth.value = res.width || opts.width;
    imgHeight.value = res.height || opts.height;
    await nextTick();
    trackWidth = trackRef.value ? trackRef.value.clientWidth : imgWidth.value;
    pieceLeft.value = 0;
    status.value = 'idle';
    if (opts.debug && rootRef.value) {
      rootRef.value.dataset.captchaId = res.id;
      if (res.data?.debugT != null) {
        rootRef.value.dataset.debugT = String(res.data.debugT);
      }
      if (res.data?.debugFakeTargets) {
        rootRef.value.dataset.debugFakeTargets = JSON.stringify(
          res.data.debugFakeTargets.map((p) => ({ x: p.x, y: p.y }))
        );
      }
    }
  } catch (error) {
    console.error('加载滑块摆动图块验证码失败', error);
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
      type: 'swing-tile',
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
        if (opts.autoReload) {
          loadCaptcha();
        }
      }, 450);
    }
  } catch (error) {
    console.error('滑块摆动图块验证请求失败', error);
    emit('error', error);
    shaking.value = true;
    setTimeout(() => {
      shaking.value = false;
      pieceLeft.value = 0;
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
