// Vue 2 冒烟测试：在 jsdom 里挂载打包产物，验证
//   1) 组件能被 Vue 2.7 正常渲染、卸载；
//   2) 一次挂载只请求一张验证码（受控模式没有重复请求）；
//   3) Vue 2 的 portal 兼容层确实把弹窗搬到了 body。
// 运行前先构建：npm run build:lib && npm run smoke

import { JSDOM } from 'jsdom';

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

let calls = 0;
const api = {
  getCaptcha: async () => {
    calls += 1;
    return {
      id: 'smoke-1',
      type: 'slider',
      image1: 'data:image/png;base64,iVBORw0KGgo=',
      image2: '',
      width: 340,
      height: 190,
      data: { pieceOffsetX: 8 },
    };
  },
  verify: async () => ({ success: true, ticket: 'smoke-ticket' }),
  getTypes: async () => ({ types: ['slider'], shapes: {} }),
};

const vm = new Vue({
  render: (h) => h('div', [h(lib.CaptchaModal, { props: { visible: true, api, locale: 'zh-CN' } })]),
}).$mount(dom.window.document.getElementById('app'));

await new Promise((resolve) => setTimeout(resolve, 150));

const html = dom.window.document.body.innerHTML;
const checks = [
  ['只请求一次验证码', calls === 1],
  ['渲染出滑块组件', html.includes('slider-captcha')],
  ['portal 已挂载到 body', !!dom.window.document.body.querySelector('.captcha-portal')],
  ['导出名称数量为 24', Object.keys(lib).length === 24],
];

vm.$destroy();

let failed = 0;
for (const [name, ok] of checks) {
  console.log(`${ok ? 'PASS' : 'FAIL'}  ${name}`);
  if (!ok) {
    failed += 1;
  }
}
console.log(failed === 0 ? 'smoke: all checks passed' : `smoke: ${failed} check(s) failed`);
process.exit(failed === 0 ? 0 : 1);
