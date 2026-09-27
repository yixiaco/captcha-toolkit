import Vue from 'vue';
import DemoApp from '../../frontend/src/demo/App.vue';
import '../../frontend/src/demo/demo.css';

// 演示页与 Vue 3 包完全共用同一份源码（frontend/src/demo/App.vue），
// 这里只是把启动方式换成 Vue 2：Vue 3 用 createApp(App).mount('#app')，Vue 2 用下面这两行。
// 组件库的入口同样共用（demo 里的 '../lib' 会被 @vitejs/plugin-vue2 按 Vue 2 编译）。
new Vue({
  render: (h) => h(DemoApp),
}).$mount('#app');
