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
      <img v-if="image1" :src="image1" class="captcha-img" alt="验证图片" draggable="false" />

      <img
        v-if="image2"
        :src="image2"
        class="piece"
        alt=""
        draggable="false"
        :style="{
          height: height + 'px',
          transform: `translateX(${pieceLeft - pieceOffsetX}px)`,
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
import { getCaptcha, verifyCaptcha } from '../api/captcha'
import { PUZZLE_SHAPES } from '../utils/puzzleShapes'

const props = defineProps({
  width: { type: Number, default: 340 },
  height: { type: Number, default: 190 },
  initialShape: { type: String, default: '' },
})

const emit = defineEmits(['success'])

const rootRef = ref(null)
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

const HANDLE_WIDTH = 44
let trackWidth = props.width
let startClientX = 0
let startLeft = 0

const shapeOptions = computed(() =>
  Object.entries(PUZZLE_SHAPES).map(([key, cfg]) => ({ key, label: cfg.label }))
)

function maxLeft() {
  return Math.max(0, trackWidth - HANDLE_WIDTH)
}

/**
 * 从后端获取滑块验证码：大图（带缺口）+ 小图（拼图块）
 */
async function loadCaptcha() {
  status.value = 'loading'
  image1.value = ''
  image2.value = ''
  try {
    // 开发环境带 debug=1，后端会返回答案 x 便于自动化自检
    const res = await getCaptcha({
      type: 'slider',
      shape: selectedShape.value || undefined,
      debug: import.meta.env.DEV ? '1' : undefined,
    })
    captchaId.value = res.id
    image1.value = res.image1
    image2.value = res.image2
    // 小图是从拼图块左侧留白处裁剪的，需要把图片整体左移 offset，
    // 让拼图块初始位置正好贴住大图左边缘
    pieceOffsetX.value = res.pieceOffsetX || 0
    pieceLeft.value = 0
    status.value = 'idle'
    // 仅开发模式暴露答案，便于自动化自检；生产构建不会发送 debug 参数
    if (import.meta.env.DEV && rootRef.value) {
      rootRef.value.dataset.captchaId = res.id
      if (res.debugX != null) {
        rootRef.value.dataset.debugX = String(res.debugX)
      }
    }
  } catch (error) {
    console.error('加载滑块验证码失败', error)
    status.value = 'idle'
  }
}

/**
 * 切换拼图形状：通知后端按新形状重新生成
 */
function selectShape(key) {
  if (status.value === 'success') return
  selectedShape.value = key
  loadCaptcha()
}

/**
 * 按下滑块：记录起点并监听全局移动事件
 */
function onPointerDown(event) {
  if (status.value !== 'idle') return
  dragging.value = true
  startClientX = event.clientX
  startLeft = pieceLeft.value
  window.addEventListener('pointermove', onPointerMove)
  window.addEventListener('pointerup', onPointerUp)
}

/**
 * 拖动过程：限制在轨道范围内
 */
function onPointerMove(event) {
  if (!dragging.value) return
  const next = startLeft + event.clientX - startClientX
  pieceLeft.value = Math.min(maxLeft(), Math.max(0, next))
}

/**
 * 松开滑块：把最终位移提交后端校验
 */
async function onPointerUp() {
  if (!dragging.value) return
  dragging.value = false
  window.removeEventListener('pointermove', onPointerMove)
  window.removeEventListener('pointerup', onPointerUp)

  try {
    // x 是滑块走过的像素距离，width 用于后端做缩放换算
    const res = await verifyCaptcha({
      id: captchaId.value,
      type: 'slider',
      x: Math.round(pieceLeft.value),
      width: props.width,
    })
    if (res.success) {
      status.value = 'success'
      emit('success')
    } else {
      // 失败：抖动后重置滑块并换一张新验证码
      shaking.value = true
      setTimeout(() => {
        shaking.value = false
        pieceLeft.value = 0
        loadCaptcha()
      }, 450)
    }
  } catch (error) {
    console.error('滑块验证请求失败', error)
    shaking.value = true
    setTimeout(() => {
      shaking.value = false
      pieceLeft.value = 0
    }, 450)
  }
}

onMounted(async () => {
  await nextTick()
  if (trackRef.value) {
    trackWidth = trackRef.value.clientWidth
  }
  if (props.initialShape && PUZZLE_SHAPES[props.initialShape]) {
    selectedShape.value = props.initialShape
  }
  loadCaptcha()
})

onBeforeUnmount(() => {
  window.removeEventListener('pointermove', onPointerMove)
  window.removeEventListener('pointerup', onPointerUp)
})
</script>
