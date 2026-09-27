import { getCurrentInstance } from 'vue';

/**
 * 读取当前组件收到的透传属性（attrs）。
 *
 * <p>Vue 3 的 {@code useAttrs()} 是 3.3 才有的 API，Vue 2.7 没有；
 * 这里统一走 {@code getCurrentInstance()}：Vue 3 直接用实例上的 attrs，
 * Vue 2.7 退回实例代理上的 {@code $attrs}，同一份源码两个版本都能跑。</p>
 */
export function useAttrsCompat(): Record<string, unknown> {
  const instance = getCurrentInstance() as unknown as {
    attrs?: Record<string, unknown>
    proxy?: { $attrs?: Record<string, unknown> } | null
  } | null;
  return instance?.attrs ?? instance?.proxy?.$attrs ?? {};
}
