// 组件库配置：支持 app.use(plugin, options) 全局提供，也支持组件 props 覆盖

import { inject, provide, reactive } from 'vue'
import { createCaptchaApi } from './api'

export const CaptchaOptionsKey = Symbol('captcha-options')

export const defaultCaptchaOptions = {
  // 接口
  api: null,
  baseUrl: '/api/captcha',
  request: null,
  // 尺寸
  width: 340,
  height: 190,
  // 滑块
  shape: '',
  shapes: ['classic', 'leaf', 'triangle', 'circle', 'diamond', 'star', 'heart'],
  showShapePicker: true,
  handleWidth: 44,
  // 点选
  promptPrefix: '请依次点选',
  markMinDistance: 16,
  // 行为
  debug: false,
  autoReload: true,
  closeOnSuccess: true,
  successDelay: 750,
  // 文案
  title: '安全验证',
  refreshTitle: '换一张',
  closeTitle: '关闭',
  brandText: 'GeeTest 极验',
  sloganText: '安全、智能、高效',
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
