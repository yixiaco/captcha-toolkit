<template>
  <div class="click-captcha" :class="{ 'is-success': status === 'success' }">
    <div class="click-prompt">
      <span>{{ opts.promptPrefix }}</span>
      <span v-for="ch in prompt" :key="ch" class="prompt-char">{{ ch }}</span>
    </div>

    <div class="img-wrap" :class="{ shake: shaking }" :style="{ height: imgHeight + 'px' }">
      <img
        v-if="image1"
        ref="imageRef"
        :src="image1"
        class="click-canvas"
        :alt="opts.imageAlt"
        draggable="false"
        @pointerdown="onPointerDown"
      />

      <div
        v-for="mark in marks"
        :key="mark.index"
        class="click-mark"
        :style="{
          left: mark.x + 'px',
          top: mark.y + 'px',
        }"
      >
        {{ mark.index }}
      </div>

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
  </div>
</template>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useCaptchaOptions } from './options'
import { createTrace, pushPoint, buildCompressedTrace, removeLastEvent } from './trace'

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
  /** 点选提示前缀文案 */
  promptPrefix: { type: String, default: null },
  /** 点选去重最小间距（px） */
  markMinDistance: { type: Number, default: null },
  /** 是否请求调试答案 */
  debug: { type: Boolean, default: null },
  /** 失败后自动刷新 */
  autoReload: { type: Boolean, default: null },
  /** 加载提示文案 */
  loadingText: { type: String, default: null },
  /** 图片 alt 文案 */
  imageAlt: { type: String, default: null },
  /** 客户端类型：web / h5 / mini_program */
  clientType: { type: String, default: null },
})

const emit = defineEmits(['success', 'fail', 'error'])

const opts = useCaptchaOptions(props)

const imageRef = ref(null)
const status = ref('loading')
const image1 = ref('')
const captchaId = ref('')
const prompt = ref([])
const marks = ref([])
const shaking = ref(false)
const submitting = ref(false)
const imgHeight = ref(opts.height)
/** 当前点选的行为轨迹 */
let trace = null
/** 是否正处于一次未完成的按下（用于与松开事件配对） */
let pressAccepted = false
/** 是否已挂载窗口级 pointermove 监听（避免重复挂载） */
let moveListening = false

/** 挂载窗口级移动监听，记录点击之间的接近轨迹 */
function startMoveListening() {
  if (!moveListening) {
    window.addEventListener('pointermove', onPointerMove)
    moveListening = true
  }
}

/** 卸载窗口级移动监听（提交或组件销毁时调用） */
function stopMoveListening() {
  if (moveListening) {
    window.removeEventListener('pointermove', onPointerMove)
    moveListening = false
  }
}

async function loadCaptcha() {
  status.value = 'loading'
  image1.value = ''
  marks.value = []
  stopMoveListening()
  trace = null
  pressAccepted = false
  try {
    const res = await opts.api.getCaptcha({
      type: 'click',
      debug: opts.debug ? '1' : undefined,
    })
    captchaId.value = res.id
    image1.value = res.image1
    prompt.value = res.prompt || []
    // 以后端实际图片高度为准（宽度由父容器 100% 决定）
    imgHeight.value = res.height || opts.height
    status.value = 'idle'
    startMoveListening()
    await nextTick()
    if (opts.debug && imageRef.value) {
      imageRef.value.dataset.captchaId = res.id
      if (res.debugTargets) {
        imageRef.value.dataset.debugTargets = JSON.stringify(
          res.debugTargets.map((p) => ({ x: p.x, y: p.y }))
        )
      }
    }
  } catch (error) {
    console.error('加载点选验证码失败', error)
    emit('error', error)
    status.value = 'idle'
  }
}

/** 首次交互时创建轨迹，并把当前指针位置记为起点 */
function ensureTrace(event) {
  if (!trace) {
    trace = createTrace(imageRef.value)
    pushPoint(trace, event.clientX, event.clientY, 0, imageRef.value)
  }
}

/** 记录指针移动（点击之间的接近轨迹） */
function onPointerMove(event) {
  if (status.value !== 'idle' || submitting.value) return
  ensureTrace(event)
  pushPoint(trace, event.clientX, event.clientY, 1, imageRef.value)
}

/** 按下：去重通过后才记录点击事件，并监听松开 */
function onPointerDown(event) {
  if (status.value !== 'idle' || submitting.value) return
  const rect = imageRef.value.getBoundingClientRect()
  const x = event.clientX - rect.left
  const y = event.clientY - rect.top
  if (marks.value.some((m) => Math.hypot(m.x - x, m.y - y) < opts.markMinDistance)) return

  ensureTrace(event)
  pushPoint(trace, event.clientX, event.clientY, 3, imageRef.value)
  pressAccepted = true
  window.addEventListener('pointerup', onPointerUp)
  window.addEventListener('pointercancel', onPointerUp)
}

/** 松开：在图片内才算一次有效点击，点满目标数后提交 */
function onPointerUp(event) {
  if (!pressAccepted) return
  pressAccepted = false
  window.removeEventListener('pointerup', onPointerUp)
  window.removeEventListener('pointercancel', onPointerUp)

  const rect = imageRef.value.getBoundingClientRect()
  const x = event.clientX - rect.left
  const y = event.clientY - rect.top
  if (x < 0 || y < 0 || x > rect.width || y > rect.height) {
    // 松手在图片外：撤销这次未完成的按下，不污染轨迹
    removeLastEvent(trace, 3)
    return
  }
  pushPoint(trace, event.clientX, event.clientY, 2, imageRef.value)
  marks.value.push({ x, y, index: marks.value.length + 1 })

  if (marks.value.length >= prompt.value.length) {
    submit()
  }
}

/** 点满目标字后一次性提交后端校验 */
async function submit() {
  submitting.value = true
  stopMoveListening()
  const td = await buildCompressedTrace(trace)
  trace = null
  try {
    const rect = imageRef.value.getBoundingClientRect()
    const res = await opts.api.verify({
      id: captchaId.value,
      type: 'click',
      points: marks.value.map((m) => ({
        x: m.x / rect.width,
        y: m.y / rect.height,
      })),
      clientType: opts.clientType,
      td,
    })
    if (res.success) {
      status.value = 'success'
      emit('success', res)
    } else {
      emit('fail', res)
      marks.value = []
      shaking.value = true
      setTimeout(() => {
        shaking.value = false
        if (opts.autoReload) {
          loadCaptcha()
        }
      }, 450)
    }
  } catch (error) {
    console.error('点选验证请求失败', error)
    emit('error', error)
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadCaptcha()
})

onBeforeUnmount(() => {
  stopMoveListening()
  window.removeEventListener('pointerup', onPointerUp)
  window.removeEventListener('pointercancel', onPointerUp)
})

defineExpose({ reload: loadCaptcha })
</script>
