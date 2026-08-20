<template>
  <div class="demo-page">
    <div class="demo-card">
      <div class="demo-brand">
        <svg
          viewBox="0 0 24 24"
          width="30"
          height="30"
          fill="none"
          stroke="#3b7cff"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <path d="M12 3l7 3v5c0 4.5-3 8.2-7 10-4-1.8-7-5.5-7-10V6l7-3z" />
          <path d="M9 12l2 2 4-4" />
        </svg>
        <h1>行为验证 · 原型</h1>
      </div>
      <p class="demo-subtitle">通用行为验证组件库演示 · 滑块拼图 / 文字点选</p>

      <form class="demo-form" @submit.prevent="onLogin">
        <label>
          <span>账号</span>
          <input v-model="account" type="text" placeholder="请输入账号" />
        </label>
        <label>
          <span>密码</span>
          <input v-model="password" type="password" placeholder="请输入密码" />
        </label>
        <button class="login-btn" type="submit">登 录</button>
      </form>

      <div class="demo-divider"><span>选择验证方式体验</span></div>

      <div class="demo-actions">
        <button class="mode-btn slider" @click="open('slider')">
          <svg
            viewBox="0 0 24 24"
            width="18"
            height="18"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <rect x="3" y="9" width="14" height="8" rx="2" />
            <circle cx="12" cy="13" r="2" />
            <path d="M21 12v-2M18 12h3" />
          </svg>
          滑块拼图
        </button>
        <button class="mode-btn click" @click="open('click')">
          <svg
            viewBox="0 0 24 24"
            width="18"
            height="18"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <path d="M9 11.5V5a1.5 1.5 0 0 1 3 0v4.5M12 9.5V4a1.5 1.5 0 0 1 3 0v6M15 10.5V7a1.5 1.5 0 0 1 3 0v8.5" />
            <path d="M18 15.5V12a1.5 1.5 0 0 1 3 0v4.5c0 3.5-2.5 5.5-6 5.5h-2.2c-1.5 0-2.9-.7-3.9-1.8l-3.3-3.9a1.4 1.4 0 0 1 2.1-1.9l2.3 2.4" />
          </svg>
          点选文字
        </button>
        <button class="mode-btn rotate" @click="open('rotate')">
          <svg
            viewBox="0 0 24 24"
            width="18"
            height="18"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <path d="M21 12a9 9 0 1 1-2.64-6.36" />
            <path d="M21 3v6h-6" />
            <path d="M12 8v4l3 2" />
          </svg>
          图片旋转
        </button>
        <button class="mode-btn random" @click="openRandom">
          <svg
            viewBox="0 0 24 24"
            width="18"
            height="18"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <path d="M4 7h13M13 3l4 4-4 4M20 17H7M11 13l-4 4 4 4" />
          </svg>
          随机模式
        </button>
      </div>

      <div class="demo-divider"><span>嵌入方式</span></div>

      <div class="embed-actions">
        <button
          v-for="item in embedModes"
          :key="item.key"
          class="embed-btn"
          :class="{ active: inlineMode === item.key }"
          @click="inlineMode = item.key"
        >
          {{ item.label }}
        </button>
      </div>

      <div class="embed-panel">
        <Captcha
          display="inline"
          :mode="inlineMode"
          :width="300"
          :height="170"
          :debug="isDev"
          @success="onVerified"
        />
      </div>

      <transition name="fade">
        <div v-if="verified" class="verified-banner">
          <span class="verified-icon">✓</span>
          验证通过，登录成功（演示）
          <span v-if="verifiedTicket" class="verified-ticket">票据：{{ verifiedTicket }}</span>
        </div>
      </transition>
    </div>

    <p class="demo-footer">组件库演示 · 真实校验在后端完成 · 仅供演示参考</p>

    <CaptchaModal
      :visible="captchaVisible"
      :mode="captchaMode"
      :shape="shapeFromUrl"
      :debug="isDev"
      :brand-text="'Captcha Toolkit'"
      :slogan-text="'通用行为验证组件'"
      @close="captchaVisible = false"
      @success="onVerified"
    />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { Captcha, CaptchaModal, PUZZLE_SHAPES } from '../lib'

const isDev = import.meta.env.DEV
const account = ref('')
const password = ref('')
const captchaVisible = ref(false)
const captchaMode = ref('slider')
const shapeFromUrl = ref('')
const verified = ref(false)
const verifiedTicket = ref('')
const inlineMode = ref('slider')
const embedModes = [
  { key: 'slider', label: '滑块' },
  { key: 'click', label: '点选' },
  { key: 'rotate', label: '旋转' },
]

function open(mode) {
  captchaMode.value = mode
  captchaVisible.value = true
}

function openRandom() {
  open(['slider', 'click', 'rotate'][Math.floor(Math.random() * 3)])
}

function onLogin() {
  openRandom()
}

function onVerified(result) {
  verified.value = true
  verifiedTicket.value = result?.ticket || ''
}

// 支持 URL 参数直接打开指定验证方式：?captcha=slider|click|random，滑块可追加 &shape=...
onMounted(() => {
  const params = new URLSearchParams(location.search)
  const modeParam = params.get('captcha')
  const shapeParam = params.get('shape')
  if (modeParam === 'slider' && shapeParam && PUZZLE_SHAPES[shapeParam]) {
    shapeFromUrl.value = shapeParam
  }
  if (modeParam === 'slider' || modeParam === 'click' || modeParam === 'rotate') {
    open(modeParam)
  } else if (modeParam === 'random') {
    openRandom()
  }
})
</script>
