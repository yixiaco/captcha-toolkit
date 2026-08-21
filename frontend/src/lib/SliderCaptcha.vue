<template>
  <div
    ref="rootRef"
    class="slider-captcha"
    :class="{ 'is-success': status === 'success' }"
    :style="{ width: imgWidth + 'px', maxWidth: '100%' }"
  >
    <!-- 形状选择器仅在 debug 模式（前后端都开启）下显示，正常模式由后端决定形状 -->
    <div
      v-if="opts.debug && opts.showShapePicker"
      class="shape-picker"
    >
      <span class="shape-label">{{ opts.shapeLabel }}</span>
      <button
        v-for="option in shapeOptions"
        :key="option.key"
        class="shape-btn"
        :class="{ active: selectedShape === option.key }"
        :data-shape="option.key"
        @click="selectShape(option.key)"
      >
        {{ option.label }}
      </button>
      <button
        class="shape-btn"
        :class="{ active: selectedShape === '' }"
        data-shape="random"
        @click="selectShape('')"
      >
        {{ opts.randomLabel }}
      </button>
    </div>

    <div
      ref="imgWrapRef"
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
        v-if="image2"
        :src="image2"
        class="piece"
        alt=""
        draggable="false"
        :style="{
          height: imgHeight + 'px',
          transform: `translateX(${pieceLeft - pieceOffsetX}px)`,
        }"
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
        {{ opts.sliderTip }}
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
import { getShapeOptions, PUZZLE_SHAPES } from './shapes';
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
  /** 初始形状，空串随机 */
  shape?: string | null
  /** 形状选择器白名单 */
  shapes?: string[] | null
  /** 形状显示名覆盖 */
  shapeLabels?: Record<string, string> | null
  /** 是否显示形状选择器 */
  showShapePicker?: boolean | null
  /** 是否请求调试答案 */
  debug?: boolean | null
  /** 失败后自动刷新 */
  autoReload?: boolean | null
  /** 滑块手柄宽度（px） */
  handleWidth?: number | null
  /** 形状选择器标题文案 */
  shapeLabel?: string | null
  /** 随机按钮文案 */
  randomLabel?: string | null
  /** 拖拽提示文案 */
  sliderTip?: string | null
  /** 加载提示文案 */
  loadingText?: string | null
  /** 图片 alt 文案 */
  imageAlt?: string | null
  /** 客户端类型：web / h5 / mini_program */
  clientType?: ClientType | null
}

// 布尔可选 props 统一用 null 作为“未传”标记，避免 Vue 默认 false 覆盖全局配置
const props = withDefaults(defineProps<Props>(), {
  showShapePicker: null,
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
const imgWrapRef = ref<HTMLElement | null>(null);
const trackRef = ref<HTMLElement | null>(null);
const status = ref<CaptchaStatus>('loading');
const image1 = ref('');
const image2 = ref('');
const captchaId = ref('');
const pieceOffsetX = ref(0);
const pieceLeft = ref(0);
const dragging = ref(false);
const shaking = ref(false);
const selectedShape = ref('');
const imgWidth = ref(opts.width);
const imgHeight = ref(opts.height);

let trackWidth = 0;
let startClientX = 0;
let startLeft = 0;
/** 当前拖拽的行为轨迹 */
let trace: BehaviorTrace | null = null;
/** 按下时缓存的容器矩形，避免移动中反复读取布局 */
let dragRect: DOMRect | null = null;
/** 上次轨迹采样时间（用于 16ms 节流） */
let lastTraceAt = 0;

const shapeOptions = computed(() => getShapeOptions(opts.shapes, opts.shapeLabels));

function maxLeft() {
  return Math.max(0, trackWidth - opts.handleWidth);
}

/** 记录滑块当前位置（而不是指针位置，避免手柄偏移导致终点对不上） */
function trackPoint(event: PointerEvent, type: 0 | 1 | 2) {
  // 移动事件只按 16ms（约 60fps）采样一次，避免轨迹无限膨胀
  const now = Date.now();
  if (type === 1 && lastTraceAt > 0 && now - lastTraceAt < 16) return;
  lastTraceAt = now;
  const rootRect = dragRect || rootRef.value!.getBoundingClientRect();
  const y = Math.min(1, Math.max(0, (event.clientY - rootRect.top) / rootRect.height));
  const x = trackWidth > 0 ? pieceLeft.value / trackWidth : 0;
  pushNormalizedPoint(trace!, x, y, type);
}

/**
 * 从后端获取滑块验证码：大图（带缺口）+ 小图（拼图块）
 */
async function loadCaptcha() {
  status.value = 'loading';
  image1.value = '';
  image2.value = '';
  trace = null;
  try {
    const res = await opts.api.getCaptcha({
      type: 'slider',
      // 正常模式不传 shape，由后端随机决定；debug 模式才允许前端指定
      shape: opts.debug ? selectedShape.value || 'random' : undefined,
      debug: opts.debug ? '1' : undefined,
    });
    captchaId.value = res.id;
    image1.value = res.image1;
    image2.value = res.image2 || '';
    // 以后端实际图片尺寸为准，避免前端配置宽度与后端不一致导致坐标换算错误
    imgWidth.value = res.width || opts.width;
    imgHeight.value = res.height || opts.height;
    await nextTick();
    trackWidth = trackRef.value ? trackRef.value.clientWidth : imgWidth.value;
    // 小图是从拼图块左侧留白处裁剪的，整体左移 offset 让拼图块贴住大图左边缘
    pieceOffsetX.value = res.pieceOffsetX || 0;
    pieceLeft.value = 0;
    status.value = 'idle';
    if (opts.debug && rootRef.value) {
      rootRef.value.dataset.captchaId = res.id;
      if (res.debugX != null) {
        rootRef.value.dataset.debugX = String(res.debugX);
      }
    }
  } catch (error) {
    console.error('加载滑块验证码失败', error);
    emit('error', error);
    status.value = 'idle';
  }
}

function selectShape(key: string) {
  if (status.value === 'success') return;
  selectedShape.value = key;
  loadCaptcha();
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
      type: 'slider',
      xNorm: trackWidth > 0 ? pieceLeft.value / trackWidth : 0,
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
    console.error('滑块验证请求失败', error);
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
  if (opts.shape && PUZZLE_SHAPES[opts.shape]) {
    selectedShape.value = opts.shape;
  }
  loadCaptcha();
});

onBeforeUnmount(() => {
  window.removeEventListener('pointermove', onPointerMove);
  window.removeEventListener('pointerup', onPointerUp);
  window.removeEventListener('pointercancel', onPointerUp);
});

defineExpose({ reload: loadCaptcha });
</script>
