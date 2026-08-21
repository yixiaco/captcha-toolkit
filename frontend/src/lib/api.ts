// 验证码请求适配器：默认走 fetch，宿主可传入自己的 request 函数

import { getDeviceFingerprint } from './device';

export interface RequestOptions {
  method?: 'GET' | 'POST'
  query?: Record<string, unknown>
  json?: unknown
}

/** 宿主可替换的请求函数，兼容 fetch 风格 */
export type RequestFunction = (url: string, options?: RequestOptions) => Promise<any>

/** 验证码下发载荷 */
export interface CaptchaChallenge {
  id: string
  type: string
  image1: string
  image2?: string
  width: number
  height: number
  shape?: string
  prompt?: string[]
  pieceOffsetX?: number
  debugX?: number
  debugTargets?: Array<{ x: number; y: number }>
  debugAngle?: number
  metadata?: Record<string, unknown>
}

/** 校验结果 */
export interface VerifyResult {
  success: boolean
  done?: boolean
  message?: string
  code?: string
  ticket?: string
}

/** 后端支持的类型与形状 */
export interface CaptchaTypes {
  types: string[]
  shapes: {
    slider: string[]
  }
}

/** 验证码 API 客户端 */
export interface CaptchaApi {
  getCaptcha(params?: Record<string, unknown>): Promise<CaptchaChallenge>
  verify(payload: Record<string, unknown>): Promise<VerifyResult>
  getTypes(): Promise<CaptchaTypes>
}

/**
 * 自动携带设备指纹：请求方显式传了 deviceFingerprint 时优先保留，
 * 否则用本地采集的指纹；采集失败时跳过该字段。
 */
async function attachDeviceFingerprint(
  payload?: Record<string, unknown>,
): Promise<Record<string, unknown>> {
  const merged = { ...(payload || {}) };
  if (!merged.deviceFingerprint) {
    merged.deviceFingerprint = await getDeviceFingerprint();
  }
  return merged;
}

/**
 * 默认请求实现：GET 拼接 query，POST 发送 JSON。
 */
export async function defaultRequest(
  url: string,
  { method = 'GET', query, json }: RequestOptions = {},
): Promise<any> {
  let target = url;
  if (query) {
    const qs = new URLSearchParams();
    for (const [key, value] of Object.entries(query)) {
      if (value !== undefined && value !== null && value !== '') {
        qs.set(key, String(value));
      }
    }
    const queryString = qs.toString();
    if (queryString) {
      target += (target.includes('?') ? '&' : '?') + queryString;
    }
  }
  const response = await fetch(target, {
    method,
    headers: json ? { 'Content-Type': 'application/json' } : undefined,
    body: json ? JSON.stringify(json) : undefined,
  });
  if (!response.ok) {
    throw new Error(`验证码请求失败：${response.status}`);
  }
  return response.json();
}

/**
 * 创建验证码 API 客户端。
 */
export function createCaptchaApi({
  baseUrl = '/api/captcha',
  request = defaultRequest,
}: {
  baseUrl?: string
  request?: RequestFunction | null
} = {}): CaptchaApi {
  // request 可能被上层配置显式传成 null，统一回退到默认实现
  const requestFn = request || defaultRequest;
  return {
    /** 获取验证码 */
    async getCaptcha(params = {}) {
      const query = await attachDeviceFingerprint(params);
      return requestFn(baseUrl, { method: 'GET', query });
    },
    /** 提交答案 */
    async verify(payload) {
      const json = await attachDeviceFingerprint(payload);
      return requestFn(`${baseUrl}/verify`, { method: 'POST', json });
    },
    /** 查询后端支持的类型与形状（通用前端可用它动态渲染） */
    getTypes() {
      return requestFn(`${baseUrl}/types`, { method: 'GET' });
    },
  };
}
