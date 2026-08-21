<template>
  <Teleport to="body">
    <div
      class="floating-widget"
      :class="[opts.floatingPosition, { expanded: open }]"
    >
      <button
        v-if="!open"
        class="floating-trigger"
        :title="opts.floatingText"
        @click="open = true"
      >
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
          <path d="M12 3l7 3v5c0 4.5-3 8.2-7 10-4-1.8-7-5.5-7-10V6l7-3z" />
          <path d="M9 12l2 2 4-4" />
        </svg>
        <span>{{ opts.floatingText }}</span>
      </button>

      <Transition name="float-expand">
        <div
          v-if="open"
          class="floating-content"
        >
          <div class="floating-head">
            <div class="floating-title">
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
                @click="closePanel"
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

          <div class="floating-body">
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
              v-else-if="mode === 'rotate'"
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
              v-else-if="mode === 'curve'"
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
          </div>

          <div
            v-if="opts.brandText || opts.sloganText"
            class="floating-foot"
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
      </Transition>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { provide, ref } from 'vue';
import SliderCaptcha from './SliderCaptcha.vue';
import ClickCaptcha from './ClickCaptcha.vue';
import RotateCaptcha from './RotateCaptcha.vue';
import CurveCaptcha from './CurveCaptcha.vue';
import { CaptchaOptionsKey, useCaptchaOptions } from './options';
import type { CaptchaMessages } from './i18n';
import type { VerifyResult } from './api';
import type { CaptchaMode } from './types';

interface Props {
  /** 验证模式：slider / click / rotate / curve */
  mode?: CaptchaMode | string
  /** 自定义 API 客户端 */
  api?: object | null
  /** 后端接口前缀 */
  baseUrl?: string | null
  /** 自定义请求函数 */
  request?: unknown
  /** 提示语言：zh-CN / en，随请求携带 lang 与后端联动 */
  locale?: string | null
  /** 自定义提示文案（按消息键覆盖语言默认值） */
  messages?: Partial<CaptchaMessages> | null
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
  /** 浮动按钮文案 */
  floatingText?: string | null
  /** 浮动位置：bottom-right / bottom-left */
  floatingPosition?: string | null
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

// 布尔可选 props 统一用 null 作为“未传”标记，避免 Vue 默认 false 覆盖全局配置
const props = withDefaults(defineProps<Props>(), {
  mode: 'slider',
  showShapePicker: null,
  debug: null,
  autoReload: null,
  closeOnSuccess: null,
});

const emit = defineEmits<{
  (e: 'success', result: VerifyResult): void
  (e: 'fail', result: VerifyResult): void
  (e: 'close'): void
  (e: 'error', error: unknown): void
}>();

const opts = useCaptchaOptions(props);
// 让浮动面板内的验证组件自动继承 locale / messages / 自定义文案
provide(CaptchaOptionsKey, opts);
const open = ref(false);
const refreshKey = ref(0);

/** 切换浮动面板开合 */
function toggle() {
  open.value = !open.value;
}

/** 关闭浮动面板 */
function closePanel() {
  open.value = false;
  emit('close');
}

/** 手动刷新面板内验证码 */
function refresh() {
  refreshKey.value++;
}

/** 验证成功：透传结果并按配置延迟关闭面板 */
function onCaptchaSuccess(result: VerifyResult) {
  setTimeout(() => {
    emit('success', result);
    if (opts.closeOnSuccess) {
      open.value = false;
    }
  }, opts.successDelay);
}

/** 把验证失败结果转发给宿主，便于清理上一次的成功状态 */
function onCaptchaFail(result: VerifyResult) {
  emit('fail', result);
}

defineExpose({
  openPanel: () => {
    open.value = true;
  },
  close: closePanel,
  toggle,
  refresh,
});
</script>
