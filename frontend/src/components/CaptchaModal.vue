<template>
  <Teleport to="body">
    <div v-if="visible" class="modal-mask" @click.self="close">
      <div class="captcha-modal">
        <div class="modal-head">
          <div class="brand">
            <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="#3b7cff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M12 3l7 3v5c0 4.5-3 8.2-7 10-4-1.8-7-5.5-7-10V6l7-3z" />
              <path d="M9 12l2 2 4-4" />
            </svg>
            <span>安全验证</span>
          </div>
          <div class="head-actions">
            <button class="icon-btn" title="换一张" @click="refresh">
              <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M21 12a9 9 0 1 1-2.64-6.36" />
                <path d="M21 3v6h-6" />
              </svg>
            </button>
            <button class="icon-btn" title="关闭" @click="close">
              <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round">
                <path d="M6 6l12 12M18 6L6 18" />
              </svg>
            </button>
          </div>
        </div>

        <div class="modal-body">
          <SliderCaptcha
            v-if="mode === 'slider'"
            :key="`slider-${refreshKey}`"
            :initial-shape="shape"
            @success="onCaptchaSuccess"
          />
          <ClickCaptcha
            v-else
            :key="`click-${refreshKey}`"
            @success="onCaptchaSuccess"
          />
        </div>

        <div class="modal-foot">
          <span class="geetest-logo">
            <span class="gt-name">GeeTest</span>
            <span class="gt-cn">极验</span>
          </span>
          <span class="geetest-slogan">安全、智能、高效</span>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { ref, watch } from 'vue'
import SliderCaptcha from './SliderCaptcha.vue'
import ClickCaptcha from './ClickCaptcha.vue'

const props = defineProps({
  visible: { type: Boolean, default: false },
  mode: { type: String, default: 'slider' },
  shape: { type: String, default: '' },
})

const emit = defineEmits(['close', 'success'])

const refreshKey = ref(0)

watch(
  () => props.visible,
  (value) => {
    if (value) refreshKey.value++
  }
)

function refresh() {
  refreshKey.value++
}

function close() {
  emit('close')
}

function onCaptchaSuccess() {
  setTimeout(() => {
    emit('success')
    emit('close')
  }, 750)
}
</script>
