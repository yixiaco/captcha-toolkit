<template>
  <div
    ref="rootRef"
    class="slider-captcha"
    :class="{ 'is-success': status === 'success' }"
    :style="{ width: imgWidth + 'px', maxWidth: '100%' }"
  >
    <!-- 形状选择器仅在 debug 模式（前后端都开启）下显示，正常模式由后端决定形状 -->
    <div v-if="opts.debug && opts.showShapePicker" class="shape-picker">
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
      />

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
      />

      <div v-if="status === 'loading'" class="loading-mask">
        <div class="spinner"></div>
        <span>{{ opts.loadingText }}</span>
      </div>

      <transition name="fade">
        <div v-if="status === 'success'" class="success-mask">
          <div class="success-icon">✓</div>
        </div>
      </transition>
    </div>

    <div class="slider-track" ref="trackRef" :class="{ shake: shaking }">
      <div class="slider-progress" :style="{ width: pieceLeft + 'px' }"></div>

      <div v-if="status === 'idle' && !dragging" class="slider-tip">
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

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { getShapeOptions, PUZZLE_SHAPES } from './shapes'
import { useCaptchaOptions } from './options'

const props = defineProps({
  /** 自定义 API 客户端 */
  api: { type: Object, default: null },
  /** 后端接口前缀 */
  baseUrl: { type: String, default: null },
  /** 自定义请求函数 */
  request: { type: Function, default: null },
  /** 验证图片宽度（px） */
  width: { type: Number, default: null },
  /** 验证图片高度（px） */
  height: { type: Number, default: null },
  /** 初始形状，空串随机 */
  shape: { type: String, default: null },
  /** 形状选择器白名单 */
  shapes: { type: Array, default: null },
  /** 形状显示名覆盖 */
  shapeLabels: { type: Object, default: null },
  /** 是否显示形状选择器 */
  showShapePicker: { type: Boolean, default: null },
  /** 是否请求调试答案 */
  debug: { type: Boolean, default: null },
  /** 失败后自动刷新 */
  autoReload: { type: Boolean, default: null },
  /** 滑块手柄宽度（px） */
  handleWidth: { type: Number, default: null },
  /** 形状选择器标题文案 */
  shapeLabel: { type: String, default: null },
  /** 随机按钮文案 */
  randomLabel: { type: String, default: null },
  /** 拖拽提示文案 */
  sliderTip: { type: String, default: null },
  /** 加载提示文案 */
  loadingText: { type: String, default: null },
  /** 图片 alt 文案 */
  imageAlt: { type: String, default: null },
})

const emit = defineEmits(['success', 'fail', 'error'])

const opts = useCaptchaOptions(props)

const rootRef = ref(null)
const imgWrapRef = ref(null)
const trackRef = ref(null)
const status = ref('loading')
const image1 = ref('')
const image2 = ref('')
const captchaId = ref('')
const pieceOffsetX = ref(0)
const pieceLeft = ref(0)
const dragging = ref(false)
const shaking = ref(false)
const selectedShape = ref('')
const imgWidth = ref(opts.width)
const imgHeight = ref(opts.height)

let trackWidth = 0
let startClientX = 0
let startLeft = 0

const shapeOptions = computed(() => getShapeOptions(opts.shapes, opts.shapeLabels))

function maxLeft() {
  return Math.max(0, trackWidth - opts.handleWidth)
}

/**
 * 从后端获取滑块验证码：大图（带缺口）+ 小图（拼图块）
 */
async function loadCaptcha() {
  status.value = 'loading'
  image1.value = ''
  image2.value = ''
  try {
    const res = await opts.api.getCaptcha({
      type: 'slider',
      // 正常模式不传 shape，由后端随机决定；debug 模式才允许前端指定
      shape: opts.debug ? selectedShape.value || 'random' : undefined,
      debug: opts.debug ? '1' : undefined,
    })
    captchaId.value = res.id
    image1.value = res.image1
    image2.value = res.image2
    // 以后端实际图片尺寸为准，避免前端配置宽度与后端不一致导致坐标换算错误
    imgWidth.value = res.width || opts.width
    imgHeight.value = res.height || opts.height
    await nextTick()
    trackWidth = trackRef.value ? trackRef.value.clientWidth : imgWidth.value
    // 小图是从拼图块左侧留白处裁剪的，整体左移 offset 让拼图块贴住大图左边缘
    pieceOffsetX.value = res.pieceOffsetX || 0
    pieceLeft.value = 0
    status.value = 'idle'
    if (opts.debug && rootRef.value) {
      rootRef.value.dataset.captchaId = res.id
      if (res.debugX != null) {
        rootRef.value.dataset.debugX = String(res.debugX)
      }
    }
  } catch (error) {
    console.error('加载滑块验证码失败', error)
    emit('error', error)
    status.value = 'idle'
  }
}

function selectShape(key) {
  if (status.value === 'success') return
  selectedShape.value = key
  loadCaptcha()
}

function onPointerDown(event) {
  if (status.value !== 'idle') return
  dragging.value = true
  startClientX = event.clientX
  startLeft = pieceLeft.value
  window.addEventListener('pointermove', onPointerMove)
  window.addEventListener('pointerup', onPointerUp)
}

function onPointerMove(event) {
  if (!dragging.value) return
  const next = startLeft + event.clientX - startClientX
  pieceLeft.value = Math.min(maxLeft(), Math.max(0, next))
}

async function onPointerUp() {
  if (!dragging.value) return
  dragging.value = false
  window.removeEventListener('pointermove', onPointerMove)
  window.removeEventListener('pointerup', onPointerUp)

  try {
    const track = trackRef.value
    const actualWidth = Math.round(
      track ? track.clientWidth : imgWrapRef.value ? imgWrapRef.value.clientWidth : imgWidth.value
    )
    const actualHeight = Math.round(
      imgWrapRef.value ? imgWrapRef.value.clientHeight : imgHeight.value
    )
    const res = await opts.api.verify({
      id: captchaId.value,
      type: 'slider',
      x: Math.round(pieceLeft.value),
      clientWidth: actualWidth,
      clientHeight: actualHeight,
    })
    if (res.success) {
      status.value = 'success'
      emit('success', res)
    } else {
      emit('fail', res)
      shaking.value = true
      setTimeout(() => {
        shaking.value = false
        pieceLeft.value = 0
        if (opts.autoReload) {
          loadCaptcha()
        }
      }, 450)
    }
  } catch (error) {
    console.error('滑块验证请求失败', error)
    emit('error', error)
    shaking.value = true
    setTimeout(() => {
      shaking.value = false
      pieceLeft.value = 0
    }, 450)
  }
}

onMounted(async () => {
  await nextTick()
  trackWidth = trackRef.value ? trackRef.value.clientWidth : opts.width
  if (opts.shape && PUZZLE_SHAPES[opts.shape]) {
    selectedShape.value = opts.shape
  }
  loadCaptcha()
})

onBeforeUnmount(() => {
  window.removeEventListener('pointermove', onPointerMove)
  window.removeEventListener('pointerup', onPointerUp)
})

defineExpose({ reload: loadCaptcha })
</script>
