<template>
  <div class="click-captcha" :class="{ 'is-success': status === 'success' }">
    <div class="click-prompt">
      <span>请依次点击</span>
      <span v-for="ch in targets" :key="ch" class="prompt-char">{{ ch }}</span>
    </div>

    <div class="img-wrap" :class="{ shake: shaking }" :style="{ height: height + 'px' }">
      <canvas ref="canvasRef" :width="width" :height="height" class="click-canvas" @click="onClick"></canvas>

      <div
        v-for="mark in marks"
        :key="mark.char"
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
import { onMounted, ref } from 'vue'
import { drawScene, rand, shuffle } from '../utils/captchaImages'

const props = defineProps({
  width: { type: Number, default: 340 },
  height: { type: Number, default: 190 },
})

const emit = defineEmits(['success'])

const canvasRef = ref(null)
const status = ref('loading')
const targets = ref([])
const marks = ref([])
const chips = ref([])
const shaking = ref(false)

const CHAR_POOL = ['安', '全', '快', '捷', '智', '能', '验', '证', '风', '控', '点', '选', '文', '字', '极', '简']

let expectedIndex = 0

function build() {
  const shuffled = shuffle(CHAR_POOL)
  const targetChars = shuffled.slice(0, 3)
  const distractorChars = shuffle(CHAR_POOL.filter((c) => !targetChars.includes(c))).slice(0, 6)
  targets.value = targetChars

  const placed = []

  // 1. 先放目标字：保证 3 个目标字一定出现在图中
  for (const char of targetChars) {
    const item = { char, isTarget: true }
    const chip = tryPlace(item, placed, 300) || forcePlace(item)
    placed.push(chip)
  }

  // 2. 再放干扰字：放不下就跳过，不影响验证
  for (const char of shuffle(distractorChars)) {
    const chip = tryPlace({ char, isTarget: false }, placed, 160)
    if (chip) placed.push(chip)
  }

  chips.value = placed
  marks.value = []
  expectedIndex = 0
  draw()
  status.value = 'idle'
  // 仅开发模式暴露点选坐标，便于自动化自检；生产构建不会写入
  if (import.meta.env.DEV && canvasRef.value) {
    canvasRef.value.dataset.chips = JSON.stringify(
      placed.map(({ x, y, size, char, isTarget, rotation }) => ({ x, y, size, char, isTarget, rotation }))
    )
  }
}

function tryPlace(item, placed, maxAttempts) {
  for (let attempt = 0; attempt < maxAttempts; attempt++) {
    const size = Math.round(rand(32, 44))
    const x = rand(26 + size / 2, props.width - 26 - size / 2)
    const y = rand(30 + size / 2, props.height - 16 - size / 2)
    const clear = placed.every(
      (p) => Math.hypot(p.x - x, p.y - y) > (p.size + size) / 2 + 24
    )
    if (clear) {
      return {
        char: item.char,
        isTarget: item.isTarget,
        x,
        y,
        size,
        rotation: rand(-18, 18),
        shear: rand(-0.14, 0.14),
        alpha: rand(0.82, 0.94),
      }
    }
  }
  return null
}

// 兜底：目标字即使在极端情况下没有空位，也强制放入图中
function forcePlace(item) {
  const size = Math.round(rand(32, 44))
  return {
    char: item.char,
    isTarget: item.isTarget,
    x: rand(26 + size / 2, props.width - 26 - size / 2),
    y: rand(30 + size / 2, props.height - 16 - size / 2),
    size,
    rotation: rand(-18, 18),
    shear: rand(-0.14, 0.14),
    alpha: rand(0.82, 0.94),
  }
}

function rgbToHsl(r, g, b) {
  r /= 255
  g /= 255
  b /= 255
  const max = Math.max(r, g, b)
  const min = Math.min(r, g, b)
  let h = 0
  let s = 0
  const l = (max + min) / 2
  if (max !== min) {
    const d = max - min
    s = l > 0.5 ? d / (2 - max - min) : d / (max + min)
    if (max === r) h = (g - b) / d + (g < b ? 6 : 0)
    else if (max === g) h = (b - r) / d + 2
    else h = (r - g) / d + 4
    h /= 6
  }
  return [h * 360, s, l]
}

// 从文字所在位置的背景色取样：保持相近色相，仅靠明度差形成人眼可辨、OCR 难提取的对比
function pickTextColor(r, g, b) {
  const [h, s, l] = rgbToHsl(r, g, b)
  const shift = l > 0.55 ? -(0.32 + rand(0, 0.08)) : 0.32 + rand(0, 0.08)
  const newL = Math.min(0.88, Math.max(0.14, l + shift))
  const newS = Math.min(0.55, Math.max(0.2, s + rand(-0.08, 0.12)))
  const newH = (h + rand(-20, 20) + 360) % 360
  return {
    color: `hsl(${newH.toFixed(1)} ${(newS * 100).toFixed(0)}% ${(newL * 100).toFixed(0)}%)`,
    dark: newL < 0.45,
  }
}

function draw() {
  const canvas = canvasRef.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  ctx.clearRect(0, 0, props.width, props.height)
  drawScene(ctx, props.width, props.height)

  // 先为每个字采样所在位置的背景色，再绘制文字
  for (const chip of chips.value) {
    const px = Math.max(0, Math.min(props.width - 1, Math.round(chip.x)))
    const py = Math.max(0, Math.min(props.height - 1, Math.round(chip.y)))
    const data = ctx.getImageData(px, py, 1, 1).data
    const picked = pickTextColor(data[0], data[1], data[2])
    chip.textColor = picked.color
    chip.textIsDark = picked.dark
  }

  for (const chip of chips.value) {
    ctx.save()
    ctx.translate(chip.x, chip.y)
    ctx.rotate((chip.rotation * Math.PI) / 180)
    ctx.transform(1, chip.shear, 0, 1, 0, 0)
    ctx.font = `700 ${chip.size}px "KaiTi","STKaiti","SimSun",serif`
    ctx.textAlign = 'center'
    ctx.textBaseline = 'middle'
    ctx.globalAlpha = chip.alpha
    ctx.shadowColor = chip.textIsDark ? 'rgba(255,255,255,0.35)' : 'rgba(0,0,0,0.3)'
    ctx.shadowBlur = 3
    ctx.shadowOffsetY = 1
    ctx.fillStyle = chip.textColor
    ctx.fillText(chip.char, 0, 1)
    ctx.restore()
  }

  drawNoise(ctx)
}

// 覆盖干扰线与噪点：增加 OCR 识别难度，同时不影响人眼辨认
function drawNoise(ctx) {
  for (let i = 0; i < 12; i++) {
    ctx.beginPath()
    ctx.moveTo(rand(0, props.width), rand(0, props.height))
    ctx.bezierCurveTo(
      rand(0, props.width), rand(0, props.height),
      rand(0, props.width), rand(0, props.height),
      rand(0, props.width), rand(0, props.height)
    )
    ctx.strokeStyle = `hsla(${rand(0, 360)} ${rand(30, 60)}% ${rand(40, 80)}% / ${rand(0.08, 0.18)})`
    ctx.lineWidth = rand(1, 2.2)
    ctx.stroke()
  }

  for (let i = 0; i < 80; i++) {
    ctx.fillStyle = `hsla(${rand(0, 360)} ${rand(30, 70)}% ${rand(35, 80)}% / ${rand(0.05, 0.18)})`
    ctx.beginPath()
    ctx.arc(rand(0, props.width), rand(0, props.height), rand(0.6, 2.2), 0, Math.PI * 2)
    ctx.fill()
  }
}

function canvasPoint(event) {
  const rect = canvasRef.value.getBoundingClientRect()
  return {
    x: (event.clientX - rect.left) * (props.width / rect.width),
    y: (event.clientY - rect.top) * (props.height / rect.height),
  }
}

function hitTest(x, y) {
  for (let i = chips.value.length - 1; i >= 0; i--) {
    const chip = chips.value[i]
    const halfW = (chip.size + 16) / 2 + 8
    const halfH = (chip.size + 12) / 2 + 8
    if (Math.abs(x - chip.x) <= halfW && Math.abs(y - chip.y) <= halfH) {
      return chip
    }
  }
  return null
}

function onClick(event) {
  if (status.value !== 'idle') return
  const { x, y } = canvasPoint(event)
  const hit = hitTest(x, y)
  if (!hit) return
  if (marks.value.some((m) => m.char === hit.char)) return

  if (hit.isTarget && hit.char === targets.value[expectedIndex]) {
    marks.value.push({ char: hit.char, x: hit.x, y: hit.y, index: expectedIndex + 1 })
    expectedIndex++
    if (expectedIndex === targets.value.length) {
      status.value = 'success'
      emit('success')
    }
  } else {
    marks.value = []
    expectedIndex = 0
    shaking.value = true
    setTimeout(() => {
      shaking.value = false
    }, 420)
  }
}

onMounted(() => {
  setTimeout(build, 450)
})
</script>
