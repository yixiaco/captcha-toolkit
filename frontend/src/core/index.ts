// 框架无关的核心层：协议调用、行为轨迹、设备指纹、图形与文案资源。
// Vue 层（src/lib）与后续其它框架适配层都从这里取能力，不重复实现。

export * from './api';
export * from './device';
export * from './i18n';
export * from './shapes';
export * from './trace';
export * from './types';
