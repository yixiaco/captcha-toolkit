<template>
  <div
    ref="rootRef"
    class="slider-captcha"
    :class="{ 'is-success': status === 'success' }"
    :style="{ width: width + 'px', maxWidth: '100%' }"
  >
    <div class="shape-picker">
      <span class="shape-label">拼图形状</span>
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
        随机
      </button>
    </div>

    <div class="img-wrap" :style="{ width: width + 'px', height: height + 'px' }">
      <img v-if="holeData" :src="holeData" class="captcha-img" alt="验证图片" draggable="false" />

      <img
        v-if="pieceData"
        :src="pieceData"
        class="piece"
        alt=""
        draggable="false"
        :style="{
          width: pieceCanvasSize + 'px',
          height: pieceCanvasSize + 'px',
          left: pieceLeft - pieceMargin + 'px',
          top: targetY - pieceMargin + 'px',
        }"
      />

      <div v-if="status === 'loading'" class="loading-mask">
        <div class="spinner"></div>
        <span>图片加载中...</span>
      </div>

      <transition name="fade">
        <div v-if="status === 'success'" class="success-mask">
          <div class="success-icon">✓</div>
        </div>
      </transition>
    </div>

    <div class="slider-track" ref="trackRef" :class="{ shake: shaking }">
      <div class="slider-progress" :style="{ width: pieceLeft + 'px' }"></div>

      <div class="slider-tip" v-if="status === 'idle' && !dragging">
        按住滑块，拖动完成拼图
      </div>

      <div
        class="slider-handle"
        :class="{ dragging }"
        :style="{ left: pieceLeft + 'px' }"
        @pointerdown.prevent="onPointerDown"
      >
        <svg v-if="status !== 'success'" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round">
          <path d="M9 6l6 6-6 6" />
        </svg>
        <svg v-else viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2.8" stroke-linecap="round" stroke-linejoin="round">
          <path d="M5 12l5 5 9-10" />
        </svg>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { createSliderAssets, PUZZLE_SHAPES } from '../utils/captchaImages'

const props = defineProps({
  width: { type: Number, default: 340 },
  height: { type: Number, default: 190 },
  initialShape: { type: String, default: '' },
})

const emit = defineEmits(['success'])

const status = ref('loading')
const holeData = ref('')
const pieceData = ref('')
const pieceSize = ref(0)
const pieceMargin = ref(0)
const pieceCanvasSize = ref(0)
const targetX = ref(0)
const targetY = ref(0)
const pieceLeft = ref(0)
const dragging = ref(false)
const shaking = ref(false)
const trackRef = ref(null)
const rootRef = ref(null)
const selectedShape = ref('')

const HANDLE_WIDTH = 44
const TOLERANCE = 6
let trackWidth = props.width
let assets = null
let startClientX = 0
let startLeft = 0

const shapeOptions = computed(() =>
  Object.entries(PUZZLE_SHAPES).map(([key, cfg]) => ({ key, label: cfg.label }))
)

function maxLeft() {
  return Math.max(0, trackWidth - HANDLE_WIDTH)
}

function selectShape(key) {
  if (status.value === 'success') return
  selectedShape.value = key
  generate()
}

function generate() {
  status.value = 'loading'
  holeData.value = ''
  pieceData.value = ''

  // 模拟图片异步加载，让加载态可见
  setTimeout(() => {
    assets = createSliderAssets(props.width, props.height, selectedShape.value || undefined)
    holeData.value = assets.hole
    pieceData.value = assets.piece
    pieceSize.value = assets.pieceSize
    pieceMargin.value = assets.margin
    pieceCanvasSize.value = assets.pieceSize + assets.margin * 2
    targetX.value = assets.targetX
    targetY.value = assets.targetY
    pieceLeft.value = 0
    status.value = 'idle'
    // 仅开发模式暴露答案坐标，便于自动化自检；生产构建不会写入
    if (import.meta.env.DEV && rootRef.value) {
      rootRef.value.dataset.targetX = String(assets.targetX)
      rootRef.value.dataset.shape = assets.shape
    }
  }, 450)
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

function onPointerUp() {
  if (!dragging.value) return
  dragging.value = false
  window.removeEventListener('pointermove', onPointerMove)
  window.removeEventListener('pointerup', onPointerUp)

  if (Math.abs(pieceLeft.value - targetX.value) <= TOLERANCE) {
    status.value = 'success'
    emit('success')
  } else {
    shaking.value = true
    setTimeout(() => {
      shaking.value = false
      pieceLeft.value = 0
    }, 420)
  }
}

onMounted(async () => {
  await nextTick()
  if (trackRef.value) trackWidth = trackRef.value.clientWidth
  if (props.initialShape && PUZZLE_SHAPES[props.initialShape]) {
    selectedShape.value = props.initialShape
  }
  generate()
})

onBeforeUnmount(() => {
  window.removeEventListener('pointermove', onPointerMove)
  window.removeEventListener('pointerup', onPointerUp)
})
</script>
