<template>
  <div class="click-captcha" :class="{ 'is-success': status === 'success' }">
    <div class="click-prompt">
      <span>{{ opts.promptPrefix }}</span>
      <span v-for="ch in prompt" :key="ch" class="prompt-char">{{ ch }}</span>
    </div>

    <div class="img-wrap" :class="{ shake: shaking }" :style="{ height: opts.height + 'px' }">
      <img
        v-if="image1"
        ref="imageRef"
        :src="image1"
        class="click-canvas"
        :alt="opts.imageAlt"
        draggable="false"
        @click="onClick"
      />

      <div
        v-for="mark in marks"
        :key="mark.index"
        class="click-mark"
        :style="{
          left: (mark.x / opts.width) * 100 + '%',
          top: (mark.y / opts.height) * 100 + '%',
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
import { nextTick, onMounted, ref } from 'vue'
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

async function loadCaptcha() {
  status.value = 'loading'
  image1.value = ''
  marks.value = []
  try {
    const res = await opts.api.getCaptcha({
      type: 'click',
      debug: opts.debug ? '1' : undefined,
    })
    captchaId.value = res.id
    image1.value = res.image1
    prompt.value = res.prompt || []
    status.value = 'idle'
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

/**
 * 点击图片：本地先标记，点满目标字数量后一次性提交后端校验。
 */
async function onClick(event) {
  if (status.value !== 'idle' || submitting.value) return
  const rect = imageRef.value.getBoundingClientRect()
  const x = Math.round((event.clientX - rect.left) * (opts.width / rect.width))
  const y = Math.round((event.clientY - rect.top) * (opts.height / rect.height))

  if (marks.value.some((m) => Math.hypot(m.x - x, m.y - y) < opts.markMinDistance)) return
  marks.value.push({ x, y, index: marks.value.length + 1 })

  if (marks.value.length < prompt.value.length) return
  submitting.value = true
  try {
    const res = await opts.api.verify({
      id: captchaId.value,
      type: 'click',
      points: marks.value.map((m) => ({ x: m.x, y: m.y })),
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

defineExpose({ reload: loadCaptcha })
</script>
