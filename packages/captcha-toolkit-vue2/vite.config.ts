import { fileURLToPath, URL } from 'node:url';
import process from 'node:process';
import vue2 from '@vitejs/plugin-vue2';
import { defineConfig } from 'vite';

// Vue 2.7 包与 Vue 3 包共用 captcha-toolkit-vue/src 下的同一份实现源码，
// 区别只在编译器（@vitejs/plugin-vue2）与 portal 实现（Vue 2 没有 Teleport）。
export default defineConfig({
  plugins: [vue2()],
  resolve: {
    alias: {
      '@captcha-portal': fileURLToPath(new URL('../captcha-toolkit-vue/src/lib/portal-vue2.vue', import.meta.url)),
      // 共享源码位于 captcha-toolkit-vue 包内，从那里往上找会命中根目录提升的 Vue 3；
      // 这里强制解析到本包自带的 Vue 2.7（构建时 vue 是 external，别名只影响 dev / 本地解析）
      vue: fileURLToPath(new URL('./node_modules/vue/dist/vue.runtime.esm.js', import.meta.url)),
    },
  },
  server: {
    port: 5175,
    open: false,
    proxy: {
      '/api': {
        // 三个后端演示（Boot 2.7 / 3 / 4）统一监听 18080，换演示无需改前端；
        // 需要指向别的地址时用 VITE_API_TARGET=http://localhost:xxxx npm run dev
        target: process.env.VITE_API_TARGET ?? 'http://localhost:18080',
        changeOrigin: true,
      },
    },
  },
  build: {
    outDir: 'dist',
    sourcemap: true,
    lib: {
      entry: fileURLToPath(new URL('./src/index.ts', import.meta.url)),
      name: 'CaptchaToolkitVue2',
      fileName: 'captcha-toolkit-vue2',
    },
    rollupOptions: {
      // Vue 作为 peer dependency，不打进产物
      external: ['vue'],
      output: {
        globals: { vue: 'Vue' },
      },
    },
  },
});
