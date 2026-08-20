import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig(({ mode }) => {
  // 默认构建为可发布组件库；--mode demo 时构建演示站点
  const isDemo = mode === 'demo'
  return {
    plugins: [vue()],
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
            entry: fileURLToPath(new URL('./src/lib/index.js', import.meta.url)),
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
  }
})
