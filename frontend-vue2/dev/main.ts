import Vue from 'vue';
import CaptchaToolkit from '../src/index';
import DemoApp from './DemoApp.vue';

// 与 Vue 3 包相同的插件用法（install 内部会按 Vue 版本选择 provide 方式）
Vue.use(CaptchaToolkit, {
  baseUrl: '/api/captcha',
  locale: 'zh-CN',
});

new Vue({
  render: (h) => h(DemoApp),
}).$mount('#app');
