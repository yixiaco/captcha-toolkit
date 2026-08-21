// 设备指纹采集：组合稳定的设备信号（UA、屏幕、时区、Canvas、WebGL 等），
// 哈希后作为同一设备的稳定标识；只在浏览器环境可用，异常时回退到基础信号。

/** 指纹缓存：同一页面生命周期内只计算一次，保证多次请求指纹一致 */
let cachedFingerprint: Promise<string> | null = null;

/** 部分浏览器提供 deviceMemory（设备内存），类型声明中缺失，这里补充 */
interface NavigatorWithMemory extends Navigator {
  deviceMemory?: number
}

/**
 * 返回当前设备的指纹哈希（SHA-256，非安全上下文回退 FNV-1a）。
 */
export function getDeviceFingerprint(): Promise<string> {
  if (!cachedFingerprint) {
    cachedFingerprint = collectFingerprint();
  }
  return cachedFingerprint;
}

/** 采集全部信号并哈希 */
async function collectFingerprint(): Promise<string> {
  const signals = [basicSignals(), canvasFingerprint(), webglFingerprint()]
    .filter(Boolean);
  return hash(signals.join('|'));
}

/** 基础信号：UA、语言、屏幕、时区、触摸能力等，任何环境都可用 */
function basicSignals(): string {
  const rawNav = typeof navigator !== 'undefined' ? navigator : null;
  const nav = rawNav as NavigatorWithMemory | null;
  const screenInfo = typeof screen !== 'undefined' ? screen : null;
  const timezone = typeof Intl !== 'undefined'
    ? Intl.DateTimeFormat().resolvedOptions().timeZone
    : '';
  return [
    nav?.userAgent || '',
    nav?.language || '',
    nav?.languages?.join(',') || '',
    nav?.platform || '',
    nav?.hardwareConcurrency || '',
    nav?.deviceMemory || '',
    nav?.maxTouchPoints || '',
    screenInfo ? `${screenInfo.width}x${screenInfo.height}x${screenInfo.colorDepth}` : '',
    screenInfo ? `${screenInfo.availWidth}x${screenInfo.availHeight}` : '',
    timezone,
    typeof window !== 'undefined' && 'ontouchstart' in window ? 'touch' : '',
  ].join('|');
}

/** Canvas 指纹：不同设备/浏览器对同一绘制的栅格化结果存在细微差异 */
function canvasFingerprint(): string {
  if (typeof document === 'undefined') {
    return '';
  }
  try {
    const canvas = document.createElement('canvas');
    canvas.width = 240;
    canvas.height = 60;
    const ctx = canvas.getContext('2d');
    if (!ctx) {
      return '';
    }
    // 用不同字体、颜色和几何图形制造可辨识的渲染差异
    ctx.textBaseline = 'top';
    ctx.font = '14px Arial';
    ctx.fillStyle = '#f60';
    ctx.fillRect(0, 0, 240, 60);
    ctx.fillStyle = '#069';
    ctx.fillText('captcha-toolkit', 8, 8);
    ctx.font = '16px "Times New Roman"';
    ctx.fillStyle = '#0a0';
    ctx.fillText('device-fingerprint', 8, 32);
    return canvas.toDataURL();
  } catch {
    return '';
  }
}

/** WebGL 指纹：显卡驱动/渲染器字符串，设备区分度较高 */
function webglFingerprint(): string {
  if (typeof document === 'undefined') {
    return '';
  }
  try {
    const canvas = document.createElement('canvas');
    const gl = canvas.getContext('webgl');
    if (!gl) {
      return '';
    }
    const debugInfo = gl.getExtension('WEBGL_debug_renderer_info');
    if (!debugInfo) {
      return '';
    }
    return [
      String(gl.getParameter(debugInfo.UNMASKED_VENDOR_WEBGL) || ''),
      String(gl.getParameter(debugInfo.UNMASKED_RENDERER_WEBGL) || ''),
    ].join('|');
  } catch {
    return '';
  }
}

/** 优先 SHA-256；非安全上下文或异常时回退 FNV-1a */
async function hash(text: string): Promise<string> {
  try {
    if (globalThis.crypto?.subtle) {
      const data = new TextEncoder().encode(text);
      const digest = await crypto.subtle.digest('SHA-256', data);
      return Array.from(new Uint8Array(digest))
        .map((byte) => byte.toString(16).padStart(2, '0'))
        .join('');
    }
  } catch {
    // 忽略加密失败，走非加密回退
  }
  return fnv1a(text);
}

/** FNV-1a 32 位非加密哈希，仅作为 crypto.subtle 不可用时的回退 */
function fnv1a(text: string): string {
  let hashValue = 0x811c9dc5;
  for (let i = 0; i < text.length; i++) {
    hashValue ^= text.charCodeAt(i);
    hashValue = Math.imul(hashValue, 0x01000193);
  }
  return (hashValue >>> 0).toString(16);
}
