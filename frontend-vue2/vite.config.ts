import { fileURLToPath, URL } from 'node:url';
import process from 'node:process';
import vue2 from '@vitejs/plugin-vue2';
import { defineConfig } from 'vite';

// Vue 2.7 包与 Vue 3 包共用 frontend/src 下的同一份实现源码，
// 区别只在编译器（@vitejs/plugin-vue2）与 portal 实现（Vue 2 没有 Teleport）。
export default defineConfig({
  plugins: [vue2()],
  resolve: {
    alias: {
      '@captcha-portal': fileURLToPath(new URL('../frontend/src/lib/portal-vue2.vue', import.meta.url)),
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
