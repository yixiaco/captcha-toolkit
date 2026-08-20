<template>
  <div
    ref="rootRef"
    class="rotate-captcha"
    :class="{ 'is-success': status === 'success' }"
  >
    <div class="img-wrap" :style="{ width: opts.width + 'px', height: opts.height + 'px' }">
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
        class="rotate-piece"
        alt=""
        draggable="false"
        :style="{ transform: `rotate(${rotation}deg)` }"
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
        {{ opts.rotateTip }}
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
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
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
  /** 旋转提示文案 */
  rotateTip: { type: String, default: null },
  /** 滑块手柄宽度（px） */
  handleWidth: { type: Number, default: null },
  /** 是否请求调试答案 */
  debug: { type: Boolean, default: null },
  /** 失败后自动刷新 */
  autoReload: { type: Boolean, default: null },
  /** 加载提示文案 */
  loadingText: { type: String, default: null },
  /** 图片 alt 文案 */
  imageAlt: { type: String, default: null },
})

const emit = defineEmits(['success', 'fail', 'error'])

const opts = useCaptchaOptions(props)

const rootRef = ref(null)
const trackRef = ref(null)
const status = ref('loading')
const image1 = ref('')
const image2 = ref('')
const captchaId = ref('')
const pieceLeft = ref(0)
const rotation = ref(0)
const dragging = ref(false)
const shaking = ref(false)

let trackWidth = 0
let startClientX = 0
let startLeft = 0

function maxLeft() {
  return Math.max(0, trackWidth - opts.handleWidth)
}

async function loadCaptcha() {
  status.value = 'loading'
  image1.value = ''
  image2.value = ''
  try {
    const res = await opts.api.getCaptcha({
      type: 'rotate',
      debug: opts.debug ? '1' : undefined,
    })
    captchaId.value = res.id
    image1.value = res.image1
    image2.value = res.image2
    pieceLeft.value = 0
    rotation.value = 0
    status.value = 'idle'
    if (opts.debug && rootRef.value) {
      rootRef.value.dataset.captchaId = res.id
      if (res.debugAngle != null) {
        rootRef.value.dataset.debugAngle = String(res.debugAngle)
      }
    }
  } catch (error) {
    console.error('加载旋转验证码失败', error)
    emit('error', error)
    status.value = 'idle'
  }
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
  rotation.value = (pieceLeft.value / maxLeft()) * 360
}

async function onPointerUp() {
  if (!dragging.value) return
  dragging.value = false
  window.removeEventListener('pointermove', onPointerMove)
  window.removeEventListener('pointerup', onPointerUp)

  try {
    const res = await opts.api.verify({
      id: captchaId.value,
      type: 'rotate',
      angle: rotation.value % 360,
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
        rotation.value = 0
        if (opts.autoReload) {
          loadCaptcha()
        }
      }, 450)
    }
  } catch (error) {
    console.error('旋转验证请求失败', error)
    emit('error', error)
    shaking.value = true
    setTimeout(() => {
      shaking.value = false
      pieceLeft.value = 0
      rotation.value = 0
    }, 450)
  }
}

onMounted(async () => {
  await nextTick()
  trackWidth = trackRef.value ? trackRef.value.clientWidth : opts.width
  loadCaptcha()
})

onBeforeUnmount(() => {
  window.removeEventListener('pointermove', onPointerMove)
  window.removeEventListener('pointerup', onPointerUp)
})

defineExpose({ reload: loadCaptcha })
</script>
