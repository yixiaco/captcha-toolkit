// 组件库配置：支持 app.use(plugin, options) 全局提供，也支持组件 props 覆盖

import { inject, provide, reactive, watch } from 'vue';
import type { InjectionKey, Reactive } from 'vue';
import { createCaptchaApi } from './api';
import type { CaptchaApi, RequestFunction } from './api';
import { CAPTCHA_MESSAGE_KEYS, resolveCaptchaMessages } from './i18n';
import type { CaptchaLocale, CaptchaMessages } from './i18n';
import type { ClientType } from './types';

export interface CaptchaOptions {
  /** 自定义 API 客户端；不传则按 baseUrl/request 自动创建 */
  api: CaptchaApi | null
  /** 后端接口前缀 */
  baseUrl: string
  /** 自定义请求函数，兼容 fetch(url, { method, query, json }) */
  request: RequestFunction | null
  /** 验证图片宽度（px）；组件加载后会以后端返回的 width 为准 */
  width: number
  /** 验证图片高度（px）；组件加载后会以后端返回的 height 为准 */
  height: number
  /** 提示语言：zh-CN / en */
  locale: CaptchaLocale
  /** 自定义提示文案（按消息键覆盖语言默认值） */
  messages: Partial<CaptchaMessages>
  /** 滑块初始形状，空串表示随机 */
  shape: string
  /** 形状选择器白名单 */
  shapes: string[]
  /** 形状显示名覆盖，如 { classic: 'Classic', leaf: 'Leaf' } */
  shapeLabels: Record<string, string>
  /** 是否显示形状选择器 */
  showShapePicker: boolean
  /** 滑块手柄宽度（px） */
  handleWidth: number
  /** 形状选择器标题文案 */
  shapeLabel: string
  /** “随机”按钮文案 */
  randomLabel: string
  /** 滑块轨道提示文案 */
  sliderTip: string
  /** 旋转提示文案 */
  rotateTip: string
  /** 角度验证提示文案 */
  angleTip: string
  /** 刮刮乐提示文案 */
  scratchTip: string
  /** 滑动曲线提示文案 */
  slideCurveTip: string
  /** 滑块摆动图块提示文案 */
  swingTileTip: string
  /** 滑动曲线摆动曲线颜色 */
  slideCurveColor: string
  /** 曲线绘制提示文案 */
  curveTip: string
  /** 用户绘制笔迹颜色 */
  curveColor: string
  /** 用户绘制笔迹宽度（px） */
  curveWidth: number
  /** 点选提示前缀文案 */
  promptPrefix: string
  /** 点选去重最小间距（px），防止重复点击同一位置 */
  markMinDistance: number
  /** 客户端类型：web / h5 / mini_program，影响后端行为校验画像 */
  clientType: ClientType
  /** 图片加载中提示文案 */
  loadingText: string
  /** 加载失败提示文案 */
  loadFailedText: string
  /** 重试按钮文案 */
  retryText: string
  /** 验证图片 alt 文案 */
  imageAlt: string
  /** 是否请求调试答案（仅本地联调开启） */
  debug: boolean
  /** 验证失败后是否自动刷新换一张 */
  autoReload: boolean
  /** 验证成功后弹窗是否自动关闭 */
  closeOnSuccess: boolean
  /** 验证成功后自动关闭/回调的延迟（ms） */
  successDelay: number
  /** 浮动按钮文案 */
  floatingText: string
  /** 浮动位置：bottom-right / bottom-left */
  floatingPosition: 'bottom-right' | 'bottom-left'
  /** 弹窗标题文案 */
  title: string
  /** 刷新按钮 title 文案 */
  refreshTitle: string
  /** 关闭按钮 title 文案 */
  closeTitle: string
  /** 左下角品牌文案，空串隐藏 */
  brandText: string
  /** 左下角标语文案，空串隐藏 */
  sloganText: string
}

export const CaptchaOptionsKey: InjectionKey<Partial<CaptchaOptions>> =
  Symbol('captcha-options');

/** 按设备能力自动判断客户端类型；小程序等特殊环境可显式传入覆盖 */
function detectClientType(): ClientType {
  if (typeof window === 'undefined' || typeof navigator === 'undefined') {
    return 'web';
  }
  if (navigator.maxTouchPoints > 0) {
    const finePointer = window.matchMedia && window.matchMedia('(pointer: fine)').matches;
    if (!finePointer) {
      return 'h5';
    }
  }
  return 'web';
}

export const defaultCaptchaOptions: CaptchaOptions = {
  api: null,
  baseUrl: '/api/captcha',
  request: null,
  width: 340,
  height: 190,
  locale: 'zh-CN',
  messages: {},
  shape: '',
  shapes: ['classic', 'leaf', 'triangle', 'circle', 'diamond', 'star', 'heart'],
  shapeLabels: {},
  showShapePicker: true,
  handleWidth: 44,
  shapeLabel: '拼图形状',
  randomLabel: '随机',
  sliderTip: '按住滑块，拖动完成拼图',
  rotateTip: '拖动滑块旋转图片，使其对齐',
  angleTip: '按住滑块，旋转圆盘使箭头对准顶部',
  scratchTip: '按住涂刮，刮出提示中的图形',
  slideCurveTip: '按住滑块，将曲线滑入对应凹槽',
  swingTileTip: '按住滑块，沿曲线把图块摆入目标凹槽',
  slideCurveColor: '#3b7cff',
  curveTip: '请按住并沿虚线从绿色起点描绘到红色终点',
  curveColor: '#3b7cff',
  curveWidth: 3,
  promptPrefix: '请依次点选',
  markMinDistance: 16,
  clientType: detectClientType(),
  loadingText: '图片加载中...',
  loadFailedText: '加载失败，请重试',
  retryText: '重试',
  imageAlt: '验证图片',
  debug: false,
  autoReload: true,
  closeOnSuccess: true,
  successDelay: 750,
  floatingText: '安全验证',
  floatingPosition: 'bottom-right',
  title: '安全验证',
  refreshTitle: '换一张',
  closeTitle: '关闭',
  brandText: '',
  sloganText: '',
};

function defined(obj: object): Record<string, unknown> {
  const result: Record<string, unknown> = {};
  for (const [key, value] of Object.entries(obj)) {
    if (value !== undefined && value !== null) {
      result[key] = value;
    }
  }
  return result;
}

/**
 * 解析提供给组件树的配置：先按 locale/messages 生成对应语言文案，
 * 再让显式传入的单个文案 prop 覆盖，避免默认中文文案覆盖英文。
 */
export function resolveProvidedCaptchaOptions(
  options: Partial<CaptchaOptions> = {},
): Partial<CaptchaOptions> {
  const locale = (options.locale || defaultCaptchaOptions.locale) as CaptchaLocale;
  const localized = resolveCaptchaMessages(locale, options.messages);
  return { ...defaultCaptchaOptions, ...localized, ...defined(options) };
}

/** 在组件树中提供全局配置 */
export function provideCaptchaOptions(options: Partial<CaptchaOptions> = {}): void {
  provide(CaptchaOptionsKey, resolveProvidedCaptchaOptions(options));
}

/**
 * 合并配置优先级：全局默认值 < 局部默认值 < provide 注入 < 组件 props。
 * 返回带 api 客户端的响应式配置对象。
 */
export function useCaptchaOptions(
  props: object = {},
  localDefaults: Partial<CaptchaOptions> = {},
): Reactive<CaptchaOptions & { api: CaptchaApi }> {
  const injected = inject(CaptchaOptionsKey, {});
  const rawProps = props as Record<string, unknown>;
  const rawInjected = injected as Record<string, unknown>;

  /** 按当前 props / 注入配置计算一份完整的合并结果（不含 api） */
  function buildMerged(): CaptchaOptions {
    const locale = (rawProps.locale || rawInjected.locale
      || defaultCaptchaOptions.locale) as CaptchaLocale;
    const messages = {
      ...((rawInjected.messages || {}) as Partial<CaptchaMessages>),
      ...((rawProps.messages || {}) as Partial<CaptchaMessages>),
    };
    const localized = resolveCaptchaMessages(locale, messages);
    const injectedDefined = defined(injected);
    // 组件级显式切换了语言时，注入层携带的是旧语言文案，必须丢弃，
    // 否则默认中文会覆盖组件级英文
    if (rawProps.locale && rawInjected.locale && rawProps.locale !== rawInjected.locale) {
      for (const key of CAPTCHA_MESSAGE_KEYS) {
        delete injectedDefined[key];
      }
    }
    return {
      ...defaultCaptchaOptions,
      ...localized,
      ...localDefaults,
      ...injectedDefined,
      ...defined(props),
    } as CaptchaOptions;
  }

  /** 按合并结果创建 API 客户端（携带当前语言对应的 Accept-Language） */
  function buildApi(merged: CaptchaOptions): CaptchaApi {
    return merged.api || createCaptchaApi({
      baseUrl: merged.baseUrl,
      request: merged.request,
      locale: merged.locale,
    });
  }

  const merged = reactive<CaptchaOptions & { api: CaptchaApi }>({
    ...buildMerged(),
    api: buildApi(buildMerged()),
  });

  // locale / messages 变化时重建配置与 API 客户端，支持运行时切换语言
  watch(
    () => [rawProps.locale, rawProps.messages, rawInjected.locale, rawInjected.messages],
    () => {
      const next = buildMerged();
      Object.assign(merged, next);
      merged.api = buildApi(next);
    },
    { deep: true },
  );

  return merged;
}
