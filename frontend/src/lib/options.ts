// 组件库配置：支持 app.use(plugin, options) 全局提供，也支持组件 props 覆盖

import { inject, provide, reactive } from 'vue';
import type { InjectionKey, Reactive } from 'vue';
import { createCaptchaApi } from './api';
import type { CaptchaApi, RequestFunction } from './api';
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
  /** 点选提示前缀文案 */
  promptPrefix: string
  /** 点选去重最小间距（px），防止重复点击同一位置 */
  markMinDistance: number
  /** 客户端类型：web / h5 / mini_program，影响后端行为校验画像 */
  clientType: ClientType
  /** 图片加载中提示文案 */
  loadingText: string
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
  shape: '',
  shapes: ['classic', 'leaf', 'triangle', 'circle', 'diamond', 'star', 'heart'],
  shapeLabels: {},
  showShapePicker: true,
  handleWidth: 44,
  shapeLabel: '拼图形状',
  randomLabel: '随机',
  sliderTip: '按住滑块，拖动完成拼图',
  rotateTip: '拖动滑块旋转图片，使其对齐',
  promptPrefix: '请依次点选',
  markMinDistance: 16,
  clientType: detectClientType(),
  loadingText: '图片加载中...',
  imageAlt: '验证图片',
  debug: false,
  autoReload: true,
  closeOnSuccess: true,
  successDelay: 750,
  title: '安全验证',
  refreshTitle: '换一张',
  closeTitle: '关闭',
  brandText: '',
  sloganText: '',
};

/** 在组件树中提供全局配置 */
export function provideCaptchaOptions(options: Partial<CaptchaOptions> = {}): void {
  provide(CaptchaOptionsKey, { ...defaultCaptchaOptions, ...options });
}

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
 * 合并配置优先级：全局默认值 < 局部默认值 < provide 注入 < 组件 props。
 * 返回带 api 客户端的响应式配置对象。
 */
export function useCaptchaOptions(
  props: object = {},
  localDefaults: Partial<CaptchaOptions> = {},
): Reactive<CaptchaOptions & { api: CaptchaApi }> {
  const injected = inject(CaptchaOptionsKey, {});
  const merged = {
    ...defaultCaptchaOptions,
    ...localDefaults,
    ...defined(injected),
    ...defined(props),
  } as CaptchaOptions & { api: CaptchaApi };
  const api = merged.api || createCaptchaApi({
    baseUrl: merged.baseUrl,
    request: merged.request,
  });
  return reactive({ ...merged, api });
}
