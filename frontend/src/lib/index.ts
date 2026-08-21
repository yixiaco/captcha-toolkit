// 通用验证码组件库入口：Vue 3 插件 + 具名导出

import type { App } from 'vue';
import CaptchaModal from './CaptchaModal.vue';
import FloatingCaptcha from './FloatingCaptcha.vue';
import Captcha from './Captcha.vue';
import SliderCaptcha from './SliderCaptcha.vue';
import ClickCaptcha from './ClickCaptcha.vue';
import RotateCaptcha from './RotateCaptcha.vue';
import CurveCaptcha from './CurveCaptcha.vue';
import SlideCurveCaptcha from './SlideCurveCaptcha.vue';
import SwingTileCaptcha from './SwingTileCaptcha.vue';
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
  SlideCurveChallengeData,
  SwingTileChallengeData,
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
  resolveProvidedCaptchaOptions,
} from './options';
import type { CaptchaOptions } from './options';
import {
  defaultMessagesFor,
  resolveCaptchaMessages,
} from './i18n';
import type { CaptchaLocale, CaptchaMessages } from './i18n';
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
  SlideCurveCaptcha,
  SwingTileCaptcha,
  createCaptchaApi,
  defaultRequest,
  PUZZLE_SHAPES,
  getShapeOptions,
  registerShape,
  provideCaptchaOptions,
  resolveProvidedCaptchaOptions,
  defaultMessagesFor,
  resolveCaptchaMessages,
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
  SlideCurveChallengeData,
  SwingTileChallengeData,
  RequestFunction,
  RequestOptions,
  VerifyResult,
  ShapeConfig,
  ShapeMap,
  CaptchaOptions,
  CaptchaLocale,
  CaptchaMessages,
  CaptchaMode,
  CaptchaStatus,
  ClientType,
};

const CaptchaToolkit = {
  install(app: App, options: Partial<CaptchaOptions> = {}) {
    app.provide(CaptchaOptionsKey, resolveProvidedCaptchaOptions(options));
  },
};

export default CaptchaToolkit;
