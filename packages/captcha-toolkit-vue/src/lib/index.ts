// 通用验证码组件库入口：Vue 3 插件 + 具名导出

import type { App } from 'vue';
import CaptchaModal from './CaptchaModal.vue';
import FloatingCaptcha from './FloatingCaptcha.vue';
import Captcha from './Captcha.vue';
import AutoCaptcha from './AutoCaptcha.vue';
import SliderCaptcha from './SliderCaptcha.vue';
import ClickCaptcha from './ClickCaptcha.vue';
import RotateCaptcha from './RotateCaptcha.vue';
import AngleCaptcha from './AngleCaptcha.vue';
import ScratchCaptcha from './ScratchCaptcha.vue';
import CurveCaptcha from './CurveCaptcha.vue';
import SlideCurveCaptcha from './SlideCurveCaptcha.vue';
import SwingTileCaptcha from './SwingTileCaptcha.vue';
import { createCaptchaApi, defaultRequest } from '../core/api';
import type {
  CaptchaApi,
  CaptchaChallenge,
  CaptchaTypes,
  ChallengePoint,
  SliderChallengeData,
  ClickChallengeData,
  RotateChallengeData,
  AngleChallengeData,
  ScratchChallengeData,
  ScratchDebugPattern,
  CurveChallengeData,
  SlideCurveChallengeData,
  SwingTileChallengeData,
  RequestFunction,
  RequestOptions,
  VerifyResult,
} from '../core/api';
import { PUZZLE_SHAPES, getShapeOptions, registerShape } from '../core/shapes';
import type { ShapeConfig, ShapeMap } from '../core/shapes';
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
} from '../core/i18n';
import type { CaptchaLocale, CaptchaMessages } from '../core/i18n';
import type { CaptchaMode, CaptchaStatus, ClientType } from '../core/types';
import './style.css';

export {
  CaptchaModal,
  FloatingCaptcha,
  Captcha,
  AutoCaptcha,
  SliderCaptcha,
  ClickCaptcha,
  RotateCaptcha,
  AngleCaptcha,
  ScratchCaptcha,
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
  AngleChallengeData,
  ScratchChallengeData,
  ScratchDebugPattern,
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
    const resolved = resolveProvidedCaptchaOptions(options);
    // Vue 3 有应用级 provide；Vue 2.7 没有，用全局 mixin 提供同一份配置
    const host = app as unknown as {
      provide?: (key: unknown, value: unknown) => void
      mixin?: (options: unknown) => void
    };
    if (typeof host.provide === 'function') {
      host.provide(CaptchaOptionsKey, resolved);
    } else if (typeof host.mixin === 'function') {
      host.mixin({ provide: { [CaptchaOptionsKey]: resolved } });
    }
  },
};

export default CaptchaToolkit;
