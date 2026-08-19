// 验证码前后端接口
const BASE = '/api/captcha'

/**
 * 获取验证码
 * @param {{type?: 'slider'|'click', shape?: string, debug?: string}} params
 */
export async function getCaptcha(params = {}) {
  const query = new URLSearchParams()
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== null && value !== '') {
      query.set(key, String(value))
    }
  }
  const response = await fetch(`${BASE}?${query.toString()}`)
  if (!response.ok) {
    throw new Error(`获取验证码失败：${response.status}`)
  }
  return response.json()
}

/**
 * 提交验证
 * @param {{id: string, type: string, x?: number, width?: number, points?: Array<{x:number,y:number}>}} payload
 */
export async function verifyCaptcha(payload) {
  const response = await fetch(`${BASE}/verify`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  })
  if (!response.ok) {
    throw new Error(`验证请求失败：${response.status}`)
  }
  return response.json()
}
