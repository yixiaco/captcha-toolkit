// 行为轨迹采集：与后端 BehaviorTraceCodec 的 td 文本协议一一对应。
//
// 报文格式：m|w|h|s|e|p
//   m = 协议版本（当前 1）
//   w / h = 采集容器尺寸（用于说明归一化坐标系）
//   s / e = 起始 / 结束时间戳（毫秒）
//   p = timeMs,x,y,type;timeMs,x,y,type;...
// 坐标使用 0~1 归一化，事件类型与极验对齐：0=起点 1=移动 2=松开 3=按下

/** 轨迹事件类型：0=起点 1=移动 2=松开 3=按下 */
export type TraceEventType = 0 | 1 | 2 | 3

/** 单个轨迹点：[timeMs, x, y, type] */
export type TracePoint = [number, number, number, number]

/** 行为轨迹对象（与后端 td 报文字段一一对应） */
export interface BehaviorTrace {
  /** 协议版本 */
  m: number
  /** 容器宽度 */
  w: number
  /** 容器高度 */
  h: number
  /** 起始时间戳（毫秒） */
  s: number
  /** 结束时间戳（毫秒） */
  e: number
  /** 轨迹点列表 */
  p: TracePoint[]
}

/** 创建一个新的轨迹对象 */
export function createTrace(container: Element | null): BehaviorTrace {
  const rect = container ? container.getBoundingClientRect() : null;
  return {
    m: 1,
    w: rect ? rect.width : window.innerWidth,
    h: rect ? rect.height : window.innerHeight,
    s: Date.now(),
    e: Date.now(),
    p: [],
  };
}

/** 追加一个轨迹点（坐标自动归一化到容器内 0~1） */
export function pushPoint(
  trace: BehaviorTrace,
  clientX: number,
  clientY: number,
  type: TraceEventType,
  container: Element,
): void {
  const rect = container.getBoundingClientRect();
  const x = clamp01((clientX - rect.left) / rect.width);
  const y = clamp01((clientY - rect.top) / rect.height);
  trace.p.push([nextTime(trace), x, y, type]);
  trace.e = Date.now();
}

/** 追加一个已经归一化好的轨迹点（用于滑块等以控件位置为坐标的场景） */
export function pushNormalizedPoint(
  trace: BehaviorTrace,
  x: number,
  y: number,
  type: TraceEventType,
): void {
  trace.p.push([nextTime(trace), x, y, type]);
  trace.e = Date.now();
}

/** 移除最近一个指定类型的事件（用于取消未完成的点击） */
export function removeLastEvent(trace: BehaviorTrace, type: TraceEventType): boolean {
  for (let i = trace.p.length - 1; i >= 0; i--) {
    if (trace.p[i][3] === type) {
      trace.p.splice(i, 1);
      return true;
    }
  }
  return false;
}

/** 编码为 td 文本 */
export function buildTrace(trace: BehaviorTrace): string {
  return [
    trace.m,
    trace.w,
    trace.h,
    trace.s,
    trace.e,
    trace.p.map(([t, x, y, type]) => `${t},${x},${y},${type}`).join(';'),
  ].join('|');
}

/**
 * 编码为 gzip + base64url 压缩文本（与后端 BehaviorTraceCodec 对齐）。
 * 浏览器不支持 CompressionStream 时回退为明文，后端两种格式都能识别。
 */
export async function buildCompressedTrace(trace: BehaviorTrace): Promise<string> {
  const text = buildTrace(trace);
  if (typeof CompressionStream === 'undefined') {
    return text;
  }
  try {
    const stream = new Blob([text])
      .stream()
      .pipeThrough(new CompressionStream('gzip'));
    const buffer = await new Response(stream).arrayBuffer();
    return bytesToBase64Url(new Uint8Array(buffer));
  } catch {
    return text;
  }
}

/** 把坐标限制在 0~1 区间，避免指针移出容器时产生越界点 */
function clamp01(value: number): number {
  return Math.min(1, Math.max(0, value));
}

/** 计算下一个轨迹点的时间：首点固定 0，其余相对起点 */
function nextTime(trace: BehaviorTrace): number {
  return trace.p.length === 0 ? 0 : Date.now() - trace.s;
}

/** 字节数组转 base64url（去掉 +/ 与末尾 =） */
function bytesToBase64Url(bytes: Uint8Array): string {
  let binary = '';
  for (const byte of bytes) {
    binary += String.fromCharCode(byte);
  }
  return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/g, '');
}
