import { fileURLToPath, URL } from 'node:url';
import vue from '@vitejs/plugin-vue';
import { defineConfig } from 'vite';
import dts from 'vite-plugin-dts';

export default defineConfig(({ mode }) => {
  // 默认构建为可发布组件库；--mode demo 时构建演示站点
  const isDemo = mode === 'demo';
  return {
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
          target: 'http://localhost:18080',
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
