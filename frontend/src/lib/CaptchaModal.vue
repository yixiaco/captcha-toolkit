<template>
  <Teleport to="body">
    <div
      v-if="visible"
      class="modal-mask"
      @click.self="close"
    >
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
            <button
              class="icon-btn"
              :title="opts.refreshTitle"
              @click="refresh"
            >
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
            <button
              class="icon-btn"
              :title="opts.closeTitle"
              @click="close"
            >
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
            @fail="onCaptchaFail"
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
            @fail="onCaptchaFail"
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
            @fail="onCaptchaFail"
          />
          <CurveCaptcha
            v-if="mode === 'curve'"
            :key="`curve-${refreshKey}`"
            :api="opts.api"
            :width="opts.width"
            :height="opts.height"
            :curve-tip="opts.curveTip"
            :curve-color="opts.curveColor"
            :curve-width="opts.curveWidth"
            :debug="opts.debug"
            :auto-reload="opts.autoReload"
            :loading-text="opts.loadingText"
            :image-alt="opts.imageAlt"
            @success="onCaptchaSuccess"
            @fail="onCaptchaFail"
          />
          <SlideCurveCaptcha
            v-if="mode === 'slide-curve'"
            :key="`slide-curve-${refreshKey}`"
            :api="opts.api"
            :width="opts.width"
            :height="opts.height"
            :slide-curve-tip="opts.slideCurveTip"
            :slide-curve-color="opts.slideCurveColor"
            :handle-width="opts.handleWidth"
            :debug="opts.debug"
            :auto-reload="opts.autoReload"
            :loading-text="opts.loadingText"
            :image-alt="opts.imageAlt"
            @success="onCaptchaSuccess"
            @fail="onCaptchaFail"
          />
        </div>

        <div
          v-if="opts.brandText || opts.sloganText"
          class="modal-foot"
        >
          <span
            v-if="opts.brandText"
            class="brand-logo"
          >
            <span class="brand-name">{{ opts.brandText }}</span>
          </span>
          <span
            v-if="opts.sloganText"
            class="brand-slogan"
          >{{ opts.sloganText }}</span>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import SliderCaptcha from './SliderCaptcha.vue';
import ClickCaptcha from './ClickCaptcha.vue';
import RotateCaptcha from './RotateCaptcha.vue';
import CurveCaptcha from './CurveCaptcha.vue';
import SlideCurveCaptcha from './SlideCurveCaptcha.vue';
import { useCaptchaOptions } from './options';
import type { VerifyResult } from './api';
import type { CaptchaMode } from './types';

interface Props {
  /** 是否显示弹窗 */
  visible?: boolean
  /** 验证模式：slider / click */
  mode?: CaptchaMode | string
  /** 自定义 API 客户端 */
  api?: object | null
  /** 后端接口前缀 */
  baseUrl?: string | null
  /** 自定义请求函数 */
  request?: unknown
  /** 验证图片宽度（px） */
  width?: number | null
  /** 验证图片高度（px） */
  height?: number | null
  /** 滑块初始形状 */
  shape?: string | null
  /** 形状选择器白名单 */
  shapes?: string[] | null
  /** 形状显示名覆盖 */
  shapeLabels?: Record<string, string> | null
  /** 是否显示形状选择器 */
  showShapePicker?: boolean | null
  /** 是否请求调试答案 */
  debug?: boolean | null
  /** 失败后自动刷新 */
  autoReload?: boolean | null
  /** 滑块手柄宽度（px） */
  handleWidth?: number | null
  /** 点选提示前缀文案 */
  promptPrefix?: string | null
  /** 点选去重最小间距（px） */
  markMinDistance?: number | null
  /** 形状选择器标题文案 */
  shapeLabel?: string | null
  /** 随机按钮文案 */
  randomLabel?: string | null
  /** 拖拽提示文案 */
  sliderTip?: string | null
  /** 旋转提示文案 */
  rotateTip?: string | null
  /** 滑动曲线提示文案 */
  slideCurveTip?: string | null
  /** 滑动曲线摆动曲线颜色 */
  slideCurveColor?: string | null
  /** 曲线绘制提示文案 */
  curveTip?: string | null
  /** 用户绘制笔迹颜色 */
  curveColor?: string | null
  /** 用户绘制笔迹宽度（px） */
  curveWidth?: number | null
  /** 加载提示文案 */
  loadingText?: string | null
  /** 图片 alt 文案 */
  imageAlt?: string | null
  /** 验证成功后弹窗是否自动关闭 */
  closeOnSuccess?: boolean | null
  /** 验证成功后自动关闭/回调延迟（ms） */
  successDelay?: number | null
  /** 弹窗标题文案 */
  title?: string | null
  /** 刷新按钮 title 文案 */
  refreshTitle?: string | null
  /** 关闭按钮 title 文案 */
  closeTitle?: string | null
  /** 左下角品牌文案，空串隐藏 */
  brandText?: string | null
  /** 左下角标语文案，空串隐藏 */
  sloganText?: string | null
}

const props = withDefaults(defineProps<Props>(), {
  visible: false,
  mode: 'slider',
  // 布尔可选 props 统一用 null 作为“未传”标记，避免 Vue 默认 false 覆盖全局配置
  showShapePicker: null,
  debug: null,
  autoReload: null,
  closeOnSuccess: null,
});

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'success', result: VerifyResult): void
  (e: 'fail', result: VerifyResult): void
}>();

const opts = useCaptchaOptions(props);
const refreshKey = ref(0);

watch(
  () => props.visible,
  (value) => {
    if (value) refreshKey.value++;
  }
);

function refresh() {
  refreshKey.value++;
}

function close() {
  emit('close');
}

function onCaptchaSuccess(result: VerifyResult) {
  setTimeout(() => {
    // 把验证结果（含一次性票据 ticket）透传给宿主业务代码
    emit('success', result);
    if (opts.closeOnSuccess) {
      emit('close');
    }
  }, opts.successDelay);
}

/** 把验证失败结果转发给宿主，便于清理上一次的成功状态 */
function onCaptchaFail(result: VerifyResult) {
  emit('fail', result);
}
</script>
