// 通用验证码组件库入口：Vue 3 插件 + 具名导出

import type { App } from 'vue';
import CaptchaModal from './CaptchaModal.vue';
import FloatingCaptcha from './FloatingCaptcha.vue';
import Captcha from './Captcha.vue';
import SliderCaptcha from './SliderCaptcha.vue';
import ClickCaptcha from './ClickCaptcha.vue';
import RotateCaptcha from './RotateCaptcha.vue';
import CurveCaptcha from './CurveCaptcha.vue';
import { createCaptchaApi, defaultRequest } from './api';
import type {
  CaptchaApi,
  CaptchaChallenge,
  CaptchaTypes,
  ChallengePoint,
  SliderChallengeData,
  ClickChallengeData,
  RotateChallengeData,
  CurveChallengeData,
  RequestFunction,
  RequestOptions,
  VerifyResult,
} from './api';
import { PUZZLE_SHAPES, getShapeOptions, registerShape } from './shapes';
import type { ShapeConfig, ShapeMap } from './shapes';
import {
  CaptchaOptionsKey,
  defaultCaptchaOptions,
  provideCaptchaOptions,
} from './options';
import type { CaptchaOptions } from './options';
import type { CaptchaMode, CaptchaStatus, ClientType } from './types';
import './style.css';

export {
  CaptchaModal,
  FloatingCaptcha,
  Captcha,
  SliderCaptcha,
  ClickCaptcha,
  RotateCaptcha,
  CurveCaptcha,
  createCaptchaApi,
  defaultRequest,
  PUZZLE_SHAPES,
  getShapeOptions,
  registerShape,
  provideCaptchaOptions,
  defaultCaptchaOptions,
  CaptchaOptionsKey,
};

export type {
  CaptchaApi,
  CaptchaChallenge,
  CaptchaTypes,
  ChallengePoint,
  SliderChallengeData,
  ClickChallengeData,
  RotateChallengeData,
  CurveChallengeData,
  RequestFunction,
  RequestOptions,
  VerifyResult,
  ShapeConfig,
  ShapeMap,
  CaptchaOptions,
  CaptchaMode,
  CaptchaStatus,
  ClientType,
};

const CaptchaToolkit = {
  install(app: App, options: Partial<CaptchaOptions> = {}) {
    app.provide(CaptchaOptionsKey, { ...defaultCaptchaOptions, ...options });
  },
};

export default CaptchaToolkit;
