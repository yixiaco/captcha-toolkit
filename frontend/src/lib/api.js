// 验证码请求适配器：默认走 fetch，宿主可传入自己的 request 函数

/**
 * 默认请求实现：GET 拼接 query，POST 发送 JSON。
 * @param {string} url
 * @param {{method?: string, query?: object, json?: object}} options
 */
export async function defaultRequest(url, { method = 'GET', query, json } = {}) {
  let target = url
  if (query) {
    const qs = new URLSearchParams()
    for (const [key, value] of Object.entries(query)) {
      if (value !== undefined && value !== null && value !== '') {
        qs.set(key, String(value))
      }
    }
    const queryString = qs.toString()
    if (queryString) {
      target += (target.includes('?') ? '&' : '?') + queryString
    }
  }
  const response = await fetch(target, {
    method,
    headers: json ? { 'Content-Type': 'application/json' } : undefined,
    body: json ? JSON.stringify(json) : undefined,
  })
  if (!response.ok) {
    throw new Error(`验证码请求失败：${response.status}`)
  }
  return response.json()
}

/**
 * 创建验证码 API 客户端。
 * @param {{baseUrl?: string, request?: Function}} options
 */
export function createCaptchaApi({ baseUrl = '/api/captcha', request = defaultRequest } = {}) {
  // request 可能被上层配置显式传成 null，统一回退到默认实现
  const requestFn = request || defaultRequest
  return {
    /** 获取验证码 */
    getCaptcha(params = {}) {
      return requestFn(baseUrl, { method: 'GET', query: params })
    },
    /** 提交答案 */
    verify(payload) {
      return requestFn(`${baseUrl}/verify`, { method: 'POST', json: payload })
    },
    /** 查询后端支持的类型与形状（通用前端可用它动态渲染） */
    getTypes() {
      return requestFn(`${baseUrl}/types`, { method: 'GET' })
    },
  }
}
