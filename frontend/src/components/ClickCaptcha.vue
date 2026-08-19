<template>
  <div class="click-captcha" :class="{ 'is-success': status === 'success' }">
    <div class="click-prompt">
      <span>请依次点击</span>
      <span v-for="ch in prompt" :key="ch" class="prompt-char">{{ ch }}</span>
    </div>

    <div class="img-wrap" :class="{ shake: shaking }" :style="{ height: height + 'px' }">
      <img
        v-if="image1"
        ref="imageRef"
        :src="image1"
        class="click-canvas"
        alt="验证图片"
        draggable="false"
        @click="onClick"
      />

      <div
        v-for="mark in marks"
        :key="mark.index"
        class="click-mark"
        :style="{ left: mark.x + 'px', top: mark.y + 'px' }"
      >
        {{ mark.index }}
      </div>

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
  </div>
</template>

<script setup>
import { nextTick, onMounted, ref } from 'vue'
import { getCaptcha, verifyCaptcha } from '../api/captcha'

const props = defineProps({
  width: { type: Number, default: 340 },
  height: { type: Number, default: 190 },
})

const emit = defineEmits(['success'])

const imageRef = ref(null)
const status = ref('loading')
const image1 = ref('')
const captchaId = ref('')
const prompt = ref([])
const marks = ref([])
const shaking = ref(false)

let expectedIndex = 0

async function loadCaptcha() {
  status.value = 'loading'
  image1.value = ''
  marks.value = []
  expectedIndex = 0
  try {
    const res = await getCaptcha({
      type: 'click',
      debug: import.meta.env.DEV ? '1' : undefined,
    })
    captchaId.value = res.id
    image1.value = res.image1
    prompt.value = res.prompt || []
    status.value = 'idle'
    await nextTick()
    // 仅开发模式暴露目标坐标，便于自动化自检；生产构建不会发送 debug 参数
    if (import.meta.env.DEV && imageRef.value) {
      imageRef.value.dataset.captchaId = res.id
      if (res.debugTargets) {
        imageRef.value.dataset.debugTargets = JSON.stringify(
          res.debugTargets.map((p) => ({ x: p.x, y: p.y }))
        )
      }
    }
  } catch (error) {
    console.error('加载点选验证码失败', error)
    status.value = 'idle'
  }
}

async function onClick(event) {
  if (status.value !== 'idle') return
  const rect = imageRef.value.getBoundingClientRect()
  const x = Math.round((event.clientX - rect.left) * (props.width / rect.width))
  const y = Math.round((event.clientY - rect.top) * (props.height / rect.height))

  try {
    const res = await verifyCaptcha({
      id: captchaId.value,
      type: 'click',
      points: [{ x, y }],
    })
    if (res.success) {
      marks.value.push({ x, y, index: expectedIndex + 1 })
      expectedIndex++
      if (res.done) {
        status.value = 'success'
        emit('success')
      }
    } else {
      marks.value = []
      expectedIndex = 0
      shaking.value = true
      setTimeout(() => {
        shaking.value = false
        loadCaptcha()
      }, 450)
    }
  } catch (error) {
    console.error('点选验证请求失败', error)
  }
}

onMounted(() => {
  loadCaptcha()
})
</script>
