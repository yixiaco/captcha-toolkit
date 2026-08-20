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
            :shape-labels="opts.shapeLabels"
            :show-shape-picker="opts.showShapePicker"
            :debug="opts.debug"
            :auto-reload="opts.autoReload"
            :handle-width="opts.handleWidth"
            :shape-label="opts.shapeLabel"
            :random-label="opts.randomLabel"
            :slider-tip="opts.sliderTip"
            :loading-text="opts.loadingText"
            :image-alt="opts.imageAlt"
            @success="onCaptchaSuccess"
          />
          <ClickCaptcha
            v-else-if="mode === 'click'"
            :key="`click-${refreshKey}`"
            :api="opts.api"
            :width="opts.width"
            :height="opts.height"
            :prompt-prefix="opts.promptPrefix"
            :mark-min-distance="opts.markMinDistance"
            :debug="opts.debug"
            :auto-reload="opts.autoReload"
            :loading-text="opts.loadingText"
            :image-alt="opts.imageAlt"
            @success="onCaptchaSuccess"
          />
          <RotateCaptcha
            v-if="mode === 'rotate'"
            :key="`rotate-${refreshKey}`"
            :api="opts.api"
            :width="opts.width"
            :height="opts.height"
            :rotate-tip="opts.rotateTip"
            :handle-width="opts.handleWidth"
            :debug="opts.debug"
            :auto-reload="opts.autoReload"
            :loading-text="opts.loadingText"
            :image-alt="opts.imageAlt"
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
import RotateCaptcha from './RotateCaptcha.vue'
import { useCaptchaOptions } from './options'

const props = defineProps({
  /** 是否显示弹窗 */
  visible: { type: Boolean, default: false },
  /** 验证模式：slider / click */
  mode: { type: String, default: 'slider' },
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
  /** 滑块初始形状 */
  shape: { type: String, default: null },
  /** 形状选择器白名单 */
  shapes: { type: Array, default: null },
  /** 形状显示名覆盖 */
  shapeLabels: { type: Object, default: null },
  /** 是否显示形状选择器 */
  showShapePicker: { type: Boolean, default: null },
  /** 是否请求调试答案 */
  debug: { type: Boolean, default: null },
  /** 失败后自动刷新 */
  autoReload: { type: Boolean, default: null },
  /** 滑块手柄宽度（px） */
  handleWidth: { type: Number, default: null },
  /** 点选提示前缀文案 */
  promptPrefix: { type: String, default: null },
  /** 点选去重最小间距（px） */
  markMinDistance: { type: Number, default: null },
  /** 形状选择器标题文案 */
  shapeLabel: { type: String, default: null },
  /** 随机按钮文案 */
  randomLabel: { type: String, default: null },
  /** 拖拽提示文案 */
  sliderTip: { type: String, default: null },
  /** 旋转提示文案 */
  rotateTip: { type: String, default: null },
  /** 加载提示文案 */
  loadingText: { type: String, default: null },
  /** 图片 alt 文案 */
  imageAlt: { type: String, default: null },
  /** 验证成功后弹窗是否自动关闭 */
  closeOnSuccess: { type: Boolean, default: null },
  /** 验证成功后自动关闭/回调延迟（ms） */
  successDelay: { type: Number, default: null },
  /** 弹窗标题文案 */
  title: { type: String, default: null },
  /** 刷新按钮 title 文案 */
  refreshTitle: { type: String, default: null },
  /** 关闭按钮 title 文案 */
  closeTitle: { type: String, default: null },
  /** 左下角品牌文案，空串隐藏 */
  brandText: { type: String, default: null },
  /** 左下角标语文案，空串隐藏 */
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
