<template>
  <Teleport to="body">
    <div v-if="visible" class="modal-mask" @click.self="close">
      <div class="captcha-modal">
        <div class="modal-head">
          <div class="brand">
            <svg
              viewBox="0 0 24 24"
              width="18"
              height="18"
              fill="none"
              stroke="#3b7cff"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <path d="M12 3l7 3v5c0 4.5-3 8.2-7 10-4-1.8-7-5.5-7-10V6l7-3z" />
              <path d="M9 12l2 2 4-4" />
            </svg>
            <span>{{ opts.title }}</span>
          </div>
          <div class="head-actions">
            <button class="icon-btn" :title="opts.refreshTitle" @click="refresh">
              <svg
                viewBox="0 0 24 24"
                width="16"
                height="16"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <path d="M21 12a9 9 0 1 1-2.64-6.36" />
                <path d="M21 3v6h-6" />
              </svg>
            </button>
            <button class="icon-btn" :title="opts.closeTitle" @click="close">
              <svg
                viewBox="0 0 24 24"
                width="16"
                height="16"
                fill="none"
                stroke="currentColor"
                stroke-width="2.2"
                stroke-linecap="round"
              >
                <path d="M6 6l12 12M18 6L6 18" />
              </svg>
            </button>
          </div>
        </div>

        <div class="modal-body">
          <SliderCaptcha
            v-if="mode === 'slider'"
            :key="`slider-${refreshKey}`"
            :api="opts.api"
            :width="opts.width"
            :height="opts.height"
            :shape="opts.shape"
            :shapes="opts.shapes"
            :show-shape-picker="opts.showShapePicker"
            :debug="opts.debug"
            :auto-reload="opts.autoReload"
            :handle-width="opts.handleWidth"
            @success="onCaptchaSuccess"
          />
          <ClickCaptcha
            v-else
            :key="`click-${refreshKey}`"
            :api="opts.api"
            :width="opts.width"
            :height="opts.height"
            :prompt-prefix="opts.promptPrefix"
            :mark-min-distance="opts.markMinDistance"
            :debug="opts.debug"
            :auto-reload="opts.autoReload"
            @success="onCaptchaSuccess"
          />
        </div>

        <div v-if="opts.brandText || opts.sloganText" class="modal-foot">
          <span v-if="opts.brandText" class="brand-logo">
            <span class="brand-name">{{ opts.brandText }}</span>
          </span>
          <span v-if="opts.sloganText" class="brand-slogan">{{ opts.sloganText }}</span>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { ref, watch } from 'vue'
import SliderCaptcha from './SliderCaptcha.vue'
import ClickCaptcha from './ClickCaptcha.vue'
import { useCaptchaOptions } from './options'

const props = defineProps({
  visible: { type: Boolean, default: false },
  mode: { type: String, default: 'slider' },
  api: { type: Object, default: null },
  baseUrl: { type: String, default: null },
  request: { type: Function, default: null },
  width: { type: Number, default: null },
  height: { type: Number, default: null },
  shape: { type: String, default: null },
  shapes: { type: Array, default: null },
  showShapePicker: { type: Boolean, default: null },
  debug: { type: Boolean, default: null },
  autoReload: { type: Boolean, default: null },
  closeOnSuccess: { type: Boolean, default: null },
  successDelay: { type: Number, default: null },
  title: { type: String, default: null },
  refreshTitle: { type: String, default: null },
  closeTitle: { type: String, default: null },
  brandText: { type: String, default: null },
  sloganText: { type: String, default: null },
})

const emit = defineEmits(['close', 'success'])

const opts = useCaptchaOptions(props)
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

function onCaptchaSuccess(result) {
  setTimeout(() => {
    // 把验证结果（含一次性票据 ticket）透传给宿主业务代码
    emit('success', result)
    if (opts.closeOnSuccess) {
      emit('close')
    }
  }, opts.successDelay)
}
</script>
