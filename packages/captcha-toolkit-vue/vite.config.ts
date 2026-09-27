import { fileURLToPath, URL } from 'node:url';
import process from 'node:process';
import vue from '@vitejs/plugin-vue';
import { defineConfig } from 'vite';
import dts from 'vite-plugin-dts';

export default defineConfig(({ mode }) => {
  // 默认构建为可发布组件库；--mode demo 时构建演示站点
  const isDemo = mode === 'demo';
  return {
    resolve: {
      alias: {
        // 弹窗/浮动组件的“传送门”按目标版本替换实现（Vue 3 用 Teleport，Vue 2.7 用挂载后搬移节点）
        '@captcha-portal': fileURLToPath(new URL('./src/lib/portal-vue3.vue', import.meta.url)),
      },
    },
    plugins: [
      vue(),
      // 组件库构建时生成 .d.ts 类型声明；演示站构建不需要
      ...(isDemo ? [] : [dts({ include: ['src/lib'], insertTypesEntry: true })]),
    ],
    server: {
      port: 5173,
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
    build: isDemo
      ? {
          outDir: 'dist-demo',
          sourcemap: true,
        }
      : {
          lib: {
            entry: fileURLToPath(new URL('./src/lib/index.ts', import.meta.url)),
            name: 'CaptchaToolkit',
            fileName: 'captcha-toolkit',
          },
          rollupOptions: {
            // Vue 作为 peer dependency，不打进产物
            external: ['vue'],
            output: {
              globals: { vue: 'Vue' },
            },
          },
          sourcemap: true,
        },
  };
});
