<template>
  <div class="vue2-demo">
    <h1>captcha-toolkit-vue2</h1>
    <p>Vue 2.7 适配演示：验证码类型由后端决定，弹窗形态。</p>
    <button
      class="demo-btn"
      @click="open"
    >
      开始验证
    </button>
    <p v-if="ticket">
      一次性票据：{{ ticket }}
    </p>

    <CaptchaModal
      :visible="visible"
      :debug="true"
      title="安全验证"
      @success="onSuccess"
      @close="visible = false"
    />
  </div>
</template>

<script lang="ts">
import Vue from 'vue';
import { CaptchaModal } from '../src/index';

export default Vue.extend({
  name: 'DemoApp',
  components: { CaptchaModal },
  data() {
    return {
      visible: false,
      ticket: '',
    };
  },
  methods: {
    open() {
      this.ticket = '';
      this.visible = true;
    },
    onSuccess(result: { ticket?: string }) {
      this.ticket = result?.ticket || '';
    },
  },
});
</script>

<style>
body {
  margin: 0;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
  background: #f5f7fb;
}

.vue2-demo {
  max-width: 520px;
  margin: 80px auto;
  padding: 32px;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.08);
  text-align: center;
}

.demo-btn {
  height: 40px;
  padding: 0 24px;
  border: none;
  border-radius: 6px;
  background: #3b7cff;
  color: #fff;
  font-size: 14px;
  cursor: pointer;
}
</style>
