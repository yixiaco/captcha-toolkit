// 组件库配置：支持 app.use(plugin, options) 全局提供，也支持组件 props 覆盖

import { inject, provide, reactive } from 'vue'
import { createCaptchaApi } from './api'

export const CaptchaOptionsKey = Symbol('captcha-options')

export const defaultCaptchaOptions = {
  /** 自定义 API 客户端；不传则按 baseUrl/request 自动创建 */
  api: null,
  /** 后端接口前缀 */
  baseUrl: '/api/captcha',
  /** 自定义请求函数，兼容 fetch(url, { method, query, json }) */
  request: null,
  /** 验证图片宽度（px），同时也是前端坐标换算基准 */
  width: 340,
  /** 验证图片高度（px） */
  height: 190,
  /** 滑块初始形状，空串表示随机 */
  shape: '',
  /** 形状选择器白名单 */
  shapes: ['classic', 'leaf', 'triangle', 'circle', 'diamond', 'star', 'heart'],
  /** 形状显示名覆盖，如 { classic: 'Classic', leaf: 'Leaf' } */
  shapeLabels: {},
  /** 是否显示形状选择器 */
  showShapePicker: true,
  /** 滑块手柄宽度（px） */
  handleWidth: 44,
  /** 形状选择器标题文案 */
  shapeLabel: '拼图形状',
  /** “随机”按钮文案 */
  randomLabel: '随机',
  /** 滑块轨道提示文案 */
  sliderTip: '按住滑块，拖动完成拼图',
  /** 旋转提示文案 */
  rotateTip: '拖动滑块旋转图片，使其对齐',
  /** 点选提示前缀文案 */
  promptPrefix: '请依次点选',
  /** 点选去重最小间距（px），防止重复点击同一位置 */
  markMinDistance: 16,
  /** 图片加载中提示文案 */
  loadingText: '图片加载中...',
  /** 验证图片 alt 文案 */
  imageAlt: '验证图片',
  /** 是否请求调试答案（仅本地联调开启） */
  debug: false,
  /** 验证失败后是否自动刷新换一张 */
  autoReload: true,
  /** 验证成功后弹窗是否自动关闭 */
  closeOnSuccess: true,
  /** 验证成功后自动关闭/回调的延迟（ms） */
  successDelay: 750,
  /** 弹窗标题文案 */
  title: '安全验证',
  /** 刷新按钮 title 文案 */
  refreshTitle: '换一张',
  /** 关闭按钮 title 文案 */
  closeTitle: '关闭',
  /** 左下角品牌文案，空串隐藏 */
  brandText: '',
  /** 左下角标语文案，空串隐藏 */
  sloganText: '',
}

/** 在组件树中提供全局配置 */
export function provideCaptchaOptions(options = {}) {
  provide(CaptchaOptionsKey, { ...defaultCaptchaOptions, ...options })
}

function defined(obj) {
  const result = {}
  for (const [key, value] of Object.entries(obj)) {
    if (value !== undefined && value !== null) {
      result[key] = value
    }
  }
  return result
}

/**
 * 合并配置优先级：全局默认值 < 局部默认值 < provide 注入 < 组件 props。
 * 返回带 api 客户端的响应式配置对象。
 */
export function useCaptchaOptions(props = {}, localDefaults = {}) {
  const injected = inject(CaptchaOptionsKey, {})
  const merged = {
    ...defaultCaptchaOptions,
    ...localDefaults,
    ...defined(injected),
    ...defined(props),
  }
  const api = merged.api || createCaptchaApi({
    baseUrl: merged.baseUrl,
    request: merged.request,
  })
  return reactive({ ...merged, api })
}
