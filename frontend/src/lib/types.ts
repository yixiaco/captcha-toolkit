// 组件库共享类型

/** 验证码类型 */
export type CaptchaMode = 'slider' | 'click' | 'rotate' | 'curve' | 'slide-curve' | 'swing-tile'

/** 客户端类型：决定后端行为校验画像 */
export type ClientType = 'web' | 'h5' | 'mini_program'

/** 验证码加载/交互状态 */
export type CaptchaStatus = 'loading' | 'idle' | 'success'
