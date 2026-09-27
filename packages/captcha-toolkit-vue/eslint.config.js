// ESLint 9 flat config：Vue 3 + TypeScript
import js from '@eslint/js'
import pluginVue from 'eslint-plugin-vue'
import tseslint from 'typescript-eslint'
import globals from 'globals'

export default tseslint.config(
  {
    ignores: ['dist/**', 'dist-demo/**', 'node_modules/**'],
  },
  js.configs.recommended,
  ...tseslint.configs.recommended,
  ...pluginVue.configs['flat/recommended'],
  {
    files: ['**/*.{ts,vue}'],
    languageOptions: {
      globals: {
        ...globals.browser,
      },
      parserOptions: {
        parser: tseslint.parser,
        extraFileExtensions: ['.vue'],
        sourceType: 'module',
      },
    },
    rules: {
      // 与项目现有风格保持一致：单引号 + 分号
      'semi': ['error', 'always'],
      'quotes': ['error', 'single'],
      'no-console': 'off',
      '@typescript-eslint/no-explicit-any': 'off',
      'vue/multi-word-component-names': 'off',
      'vue/block-lang': ['error', { script: { lang: 'ts' } }],
      'vue/require-default-prop': 'off',
    },
  },
)
