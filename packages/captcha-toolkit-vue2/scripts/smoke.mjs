// Vue 2 冒烟测试：在 jsdom 里挂载打包产物，验证
//   1) 组件能被 Vue 2.7 正常渲染、卸载；
//   2) 一次挂载只请求一张验证码（受控模式没有重复请求）；
//   3) Vue 2 的 portal 兼容层确实把弹窗搬到了 body。
// 运行前先构建：npm run build:lib && npm run smoke

import { readFileSync } from 'node:fs';
import { JSDOM } from 'jsdom';

// index.html 由浏览器 / Vite 直接解析：非空元素写成 <div /> 这种自闭合形式不会闭合，
// parse5 会报 eof-in-element-that-can-contain-only-text，dev server 直接 500。
const indexHtml = readFileSync(new URL('../index.html', import.meta.url), 'utf8');
const VOID_TAGS = new Set(['br', 'hr', 'img', 'input', 'meta', 'link', 'source', 'area',
  'base', 'col', 'embed', 'param', 'track', 'wbr']);
const badSelfClosing = [...indexHtml.matchAll(/<([a-zA-Z][\w-]*)([^>]*?)\/>/g)]
  .map((match) => match[1].toLowerCase())
  .filter((tag) => !VOID_TAGS.has(tag));

const dom = new JSDOM('<!doctype html><html><body><div id="app"></div></body></html>', {
  pretendToBeVisual: true,
  url: 'http://localhost/',
});

/** Node 22 的 globalThis 上有只读内建，统一用 defineProperty 覆盖 */
const define = (key, value) => Object.defineProperty(globalThis, key, {
  value, configurable: true, writable: true,
});
define('window', dom.window);
define('self', dom.window);
for (const key of ['document', 'HTMLElement', 'Element', 'Node', 'CustomEvent', 'Event',
  'getComputedStyle', 'requestAnimationFrame', 'cancelAnimationFrame', 'MutationObserver']) {
  define(key, key === 'getComputedStyle'
    ? dom.window.getComputedStyle.bind(dom.window)
    : dom.window[key]);
}

const Vue = (await import('vue')).default;
const lib = await import('../dist/captcha-toolkit-vue2.js');

const PIXEL = 'data:image/png;base64,iVBORw0KGgo=';

let calls = 0;
const sliderChallenge = {
  id: 'smoke-1',
  type: 'slider',
  image1: PIXEL,
  image2: '',
  width: 340,
  height: 190,
  data: { pieceOffsetX: 8 },
};
const api = {
  getCaptcha: async () => {
    calls += 1;
    return sliderChallenge;
  },
  verify: async () => ({ success: true, ticket: 'smoke-ticket' }),
  getTypes: async () => ({ types: ['slider'], shapes: {} }),
};

const modalHost = dom.window.document.createElement('div');
dom.window.document.body.appendChild(modalHost);
const vm = new Vue({
  render: (h) => h('div', [h(lib.CaptchaModal, { props: { visible: true, api, locale: 'zh-CN' } })]),
}).$mount(modalHost);

await new Promise((resolve) => setTimeout(resolve, 150));

const html = dom.window.document.body.innerHTML;
const checks = [
  [`Vue 版本为 2.7.x（实际 ${Vue.version}）`, String(Vue.version).startsWith('2.7')],
  ['只请求一次验证码', calls === 1],
  ['渲染出滑块组件', html.includes('slider-captcha')],
  ['portal 已挂载到 body', !!dom.window.document.body.querySelector('.captcha-portal')],
  ['导出名称数量为 24', Object.keys(lib).length === 24],
  [`index.html 无非空自闭合标签（发现 ${badSelfClosing.join(', ') || '无'}）`, badSelfClosing.length === 0],
];

vm.$destroy();

// 回归：swing-tile 的图块必须真的渲染出来并带上位置/尺寸
// （Vue 2.7 不会把普通 let 绑定包成响应式，曾导致该图块完全不渲染）
const swingHost = dom.window.document.createElement('div');
dom.window.document.body.appendChild(swingHost);
const swingApi = {
  ...api,
  getCaptcha: async () => ({
    id: 'smoke-swing',
    type: 'swing-tile',
    image1: PIXEL,
    image2: PIXEL,
    width: 340,
    height: 190,
    data: {
      path: [{ x: 20, y: 150 }, { x: 110, y: 60 }, { x: 250, y: 60 }, { x: 320, y: 150 }],
      startRotation: 10,
      endRotation: -8,
      swingAmplitude: 45,
      pieceSize: 50,
    },
  }),
};
const swingVm = new Vue({
  render: (h) => h(lib.SwingTileCaptcha, { props: { api: swingApi, locale: 'zh-CN' } }),
}).$mount(swingHost);

await new Promise((resolve) => setTimeout(resolve, 150));

const piece = dom.window.document.querySelector('.swing-tile-piece');
const pieceStyle = piece?.getAttribute('style') || '';
checks.push(['swing-tile 图块已渲染', !!piece]);
checks.push([`swing-tile 图块样式完整（${pieceStyle}）`,
  /width: 50px/.test(pieceStyle) && /left:/.test(pieceStyle) && /top:/.test(pieceStyle)]);

swingVm.$destroy();

let failed = 0;
for (const [name, ok] of checks) {
  console.log(`${ok ? 'PASS' : 'FAIL'}  ${name}`);
  if (!ok) {
    failed += 1;
  }
}
console.log(failed === 0 ? 'smoke: all checks passed' : `smoke: ${failed} check(s) failed`);
process.exit(failed === 0 ? 0 : 1);
