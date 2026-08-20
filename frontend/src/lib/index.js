// 通用验证码组件库入口：Vue 3 插件 + 具名导出

import CaptchaModal from './CaptchaModal.vue'
import Captcha from './Captcha.vue'
import SliderCaptcha from './SliderCaptcha.vue'
import ClickCaptcha from './ClickCaptcha.vue'
import RotateCaptcha from './RotateCaptcha.vue'
import { createCaptchaApi, defaultRequest } from './api'
import { PUZZLE_SHAPES, getShapeOptions, registerShape } from './shapes'
import {
  CaptchaOptionsKey,
  defaultCaptchaOptions,
  provideCaptchaOptions,
} from './options'
import './style.css'

export {
  CaptchaModal,
  Captcha,
  SliderCaptcha,
  ClickCaptcha,
  RotateCaptcha,
  createCaptchaApi,
  defaultRequest,
  PUZZLE_SHAPES,
  getShapeOptions,
  registerShape,
  provideCaptchaOptions,
  defaultCaptchaOptions,
  CaptchaOptionsKey,
}

const CaptchaToolkit = {
  install(app, options = {}) {
    app.provide(CaptchaOptionsKey, { ...defaultCaptchaOptions, ...options })
  },
}

export default CaptchaToolkit
