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
import { drawScene, pick, rand, shuffle } from '../utils/captchaImages'

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
// 优先使用手写/装饰字体，让字形本身不规则；缺失时回退到楷体/宋体
const FONT_STACKS = [
  '"STXingkai","华文行楷","KaiTi","STKaiti","SimSun",serif',
  '"STCaiyun","华文彩云","KaiTi","STKaiti","SimSun",serif',
  '"STHupo","华文琥珀","KaiTi","STKaiti","SimSun",serif',
  '"LiSu","隶书","KaiTi","STKaiti","SimSun",serif',
  '"KaiTi","STKaiti","SimSun",serif',
]

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
    const size = Math.round(rand(22, 30))
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
        fontStack: pick(FONT_STACKS),
      }
    }
  }
  return null
}

// 兜底：目标字即使在极端情况下没有空位，也强制放入图中
function forcePlace(item) {
  const size = Math.round(rand(22, 30))
  return {
    char: item.char,
    isTarget: item.isTarget,
    x: rand(26 + size / 2, props.width - 26 - size / 2),
    y: rand(30 + size / 2, props.height - 16 - size / 2),
    size,
    rotation: rand(-18, 18),
    shear: rand(-0.14, 0.14),
    alpha: rand(0.82, 0.94),
    fontStack: pick(FONT_STACKS),
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

// 从文字所在位置的背景色取样：保持相近色相与饱和度，仅靠明度差形成人眼可辨、OCR 难提取的对比
function pickTextColor(r, g, b) {
  const [h, s, l] = rgbToHsl(r, g, b)
  const shift = l > 0.55 ? -(0.27 + rand(0, 0.07)) : 0.27 + rand(0, 0.07)
  const newL = Math.min(0.88, Math.max(0.14, l + shift))
  const newS = Math.min(0.45, Math.max(0.2, s + rand(-0.08, 0.1)))
  const newH = (h + rand(-14, 14) + 360) % 360
  const lightL = Math.min(0.95, newL + 0.12)
  return {
    color: `hsl(${newH.toFixed(1)} ${(newS * 100).toFixed(0)}% ${(newL * 100).toFixed(0)}%)`,
    lightColor: `hsl(${newH.toFixed(1)} ${(newS * 100).toFixed(0)}% ${(lightL * 100).toFixed(0)}%)`,
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
    chip.textColorLight = picked.lightColor
    chip.textIsDark = picked.dark
  }

  for (const chip of chips.value) {
    const glyph = renderGlyph(chip)
    const glyphSize = glyph.width
    ctx.save()
    ctx.translate(chip.x, chip.y)
    ctx.rotate((chip.rotation * Math.PI) / 180)
    ctx.transform(1, chip.shear, 0, 1, 0, 0)
    // 第一遍：低透明度的正常叠加 + 柔和阴影，保证人眼可读
    ctx.globalAlpha = chip.alpha * 0.45
    ctx.shadowColor = chip.textIsDark ? 'rgba(255,255,255,0.35)' : 'rgba(0,0,0,0.3)'
    ctx.shadowBlur = 4
    ctx.shadowOffsetY = 1
    ctx.drawImage(glyph, -glyphSize / 2, -glyphSize / 2, glyphSize, glyphSize)
    // 第二遍：multiply / screen 混合，让字形吸收背景纹理
    ctx.globalAlpha = chip.alpha
    ctx.shadowColor = 'transparent'
    ctx.shadowBlur = 0
    ctx.shadowOffsetY = 0
    ctx.globalCompositeOperation = chip.textIsDark ? 'multiply' : 'screen'
    ctx.drawImage(glyph, -glyphSize / 2, -glyphSize / 2, glyphSize, glyphSize)
    ctx.restore()
  }

  drawOcclusion(ctx)
  drawNoise(ctx)
}

// 把单个文字渲染成“脏字形”：装饰字体 + 波浪形变 + 随机断笔 + 内部斑驳纹理
function renderGlyph(chip) {
  const scale = 3
  const size = Math.ceil((chip.size + 30) * scale)
  const mask = document.createElement('canvas')
  mask.width = size
  mask.height = size
  const mctx = mask.getContext('2d')
  mctx.font = `700 ${chip.size * scale}px ${chip.fontStack}`
  mctx.textAlign = 'center'
  mctx.textBaseline = 'middle'
  mctx.fillStyle = '#fff'
  mctx.fillText(chip.char, size / 2, size / 2 + 2 * scale)

  // 波浪形变：像素级正弦位移，破坏 OCR 的笔画直线与结构
  const source = mctx.getImageData(0, 0, size, size)
  const warped = mctx.createImageData(size, size)
  const ampX = rand(1.4, 2.6) * scale * 0.4
  const ampY = rand(1.4, 2.6) * scale * 0.4
  const freqX = rand(0.05, 0.1)
  const freqY = rand(0.05, 0.1)
  const phaseX = rand(0, Math.PI * 2)
  const phaseY = rand(0, Math.PI * 2)
  for (let y = 0; y < size; y++) {
    for (let x = 0; x < size; x++) {
      const dx = Math.round(Math.sin(y * freqY + phaseY) * ampX)
      const dy = Math.round(Math.sin(x * freqX + phaseX) * ampY)
      const sx = Math.max(0, Math.min(size - 1, x + dx))
      const sy = Math.max(0, Math.min(size - 1, y + dy))
      const si = (sy * size + sx) * 4
      const di = (y * size + x) * 4
      warped.data[di] = source.data[si]
      warped.data[di + 1] = source.data[si + 1]
      warped.data[di + 2] = source.data[si + 2]
      warped.data[di + 3] = source.data[si + 3]
    }
  }
  mctx.putImageData(warped, 0, 0)

  // 随机断笔：在字形内部挖掉若干小洞，人眼可脑补补全，OCR 会断线
  const holes = Math.round(rand(5, 9))
  let made = 0
  let attempts = 0
  while (made < holes && attempts < 240) {
    attempts++
    const x = Math.round(rand(size * 0.15, size * 0.85))
    const y = Math.round(rand(size * 0.15, size * 0.85))
    if (mctx.getImageData(x, y, 1, 1).data[3] > 120) {
      mctx.globalCompositeOperation = 'destination-out'
      mctx.beginPath()
      mctx.arc(x, y, rand(1.1, 2.6) * scale * 0.5, 0, Math.PI * 2)
      mctx.fill()
      mctx.globalCompositeOperation = 'source-over'
      made++
    }
  }

  // 着色：文字主体用相近色相的渐变填充，再叠加内部噪点纹理
  const glyph = document.createElement('canvas')
  glyph.width = size
  glyph.height = size
  const gctx = glyph.getContext('2d')
  const gradient = gctx.createLinearGradient(0, 0, 0, size)
  gradient.addColorStop(0, chip.textColor)
  gradient.addColorStop(1, chip.textColorLight)
  gctx.fillStyle = gradient
  gctx.fillRect(0, 0, size, size)
  gctx.globalCompositeOperation = 'destination-in'
  gctx.drawImage(mask, 0, 0)
  gctx.globalCompositeOperation = 'source-over'

  for (let i = 0; i < 46; i++) {
    const x = rand(0, size)
    const y = rand(0, size)
    const alpha = gctx.getImageData(Math.round(x), Math.round(y), 1, 1).data[3]
    if (alpha > 0) {
      gctx.fillStyle = Math.random() < 0.5 ? 'rgba(255,255,255,0.14)' : 'rgba(0,0,0,0.14)'
      gctx.beginPath()
      gctx.arc(x, y, rand(0.8, 2.4), 0, Math.PI * 2)
      gctx.fill()
    }
  }
  return glyph
}

// 用接近背景色的遮挡线穿过目标字，OCR 会把笔画与背景连成一片
function drawOcclusion(ctx) {
  for (const chip of chips.value.filter((c) => c.isTarget)) {
    const count = Math.random() < 0.6 ? 1 : 2
    for (let i = 0; i < count; i++) {
      const px = Math.max(0, Math.min(props.width - 1, Math.round(chip.x)))
      const py = Math.max(0, Math.min(props.height - 1, Math.round(chip.y)))
      const data = ctx.getImageData(px, py, 1, 1).data
      const [h, s, l] = rgbToHsl(data[0], data[1], data[2])
      ctx.strokeStyle = `hsl(${h.toFixed(1)} ${(s * 100).toFixed(0)}% ${Math.max(0, Math.min(100, l * 100 + rand(-9, 9))).toFixed(0)}% / ${rand(0.26, 0.42)})`
      ctx.lineWidth = rand(1.5, 2.8)
      ctx.beginPath()
      ctx.moveTo(chip.x - chip.size * 0.72, chip.y + rand(-chip.size * 0.4, chip.size * 0.4))
      ctx.quadraticCurveTo(
        chip.x + rand(-8, 8),
        chip.y + rand(-chip.size * 0.55, chip.size * 0.55),
        chip.x + chip.size * 0.72,
        chip.y + rand(-chip.size * 0.4, chip.size * 0.4)
      )
      ctx.stroke()
    }
  }
}

// 覆盖干扰线与噪点：增加 OCR 识别难度，同时不影响人眼辨认
function drawNoise(ctx) {
  for (let i = 0; i < 18; i++) {
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

  for (let i = 0; i < 110; i++) {
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
