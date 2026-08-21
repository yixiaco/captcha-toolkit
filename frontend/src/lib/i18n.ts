// 前端多语言与自定义提示：内置中/英词典，支持按消息键覆盖

/** 支持的语言 */
export type CaptchaLocale = 'zh-CN' | 'en'

/** 全部用户可见提示文案（消息键即配置键） */
export interface CaptchaMessages {
  /** 形状选择器标题文案 */
  shapeLabel: string
  /** “随机”按钮文案 */
  randomLabel: string
  /** 滑块轨道提示文案 */
  sliderTip: string
  /** 旋转提示文案 */
  rotateTip: string
  /** 滑动曲线提示文案 */
  slideCurveTip: string
  /** 滑块摆动图块提示文案 */
  swingTileTip: string
  /** 曲线绘制提示文案 */
  curveTip: string
  /** 点选提示前缀文案 */
  promptPrefix: string
  /** 图片加载中提示文案 */
  loadingText: string
  /** 验证图片 alt 文案 */
  imageAlt: string
  /** 弹窗/浮动面板标题文案 */
  title: string
  /** 刷新按钮 title 文案 */
  refreshTitle: string
  /** 关闭按钮 title 文案 */
  closeTitle: string
  /** 浮动按钮文案 */
  floatingText: string
}

/** 全部消息键（用于跨语言切换时清理注入层携带的旧语言文案） */
export const CAPTCHA_MESSAGE_KEYS: Array<keyof CaptchaMessages> = [
  'shapeLabel',
  'randomLabel',
  'sliderTip',
  'rotateTip',
  'slideCurveTip',
  'swingTileTip',
  'curveTip',
  'promptPrefix',
  'loadingText',
  'imageAlt',
  'title',
  'refreshTitle',
  'closeTitle',
  'floatingText',
];

/** 简体中文默认文案 */
export const zhCNMessages: CaptchaMessages = {
  shapeLabel: '拼图形状',
  randomLabel: '随机',
  sliderTip: '按住滑块，拖动完成拼图',
  rotateTip: '拖动滑块旋转图片，使其对齐',
  slideCurveTip: '按住滑块，将曲线滑入对应凹槽',
  swingTileTip: '按住滑块，沿曲线把图块摆入目标凹槽',
  curveTip: '请按住并沿虚线从绿色起点描绘到红色终点',
  promptPrefix: '请依次点选',
  loadingText: '图片加载中...',
  imageAlt: '验证图片',
  title: '安全验证',
  refreshTitle: '换一张',
  closeTitle: '关闭',
  floatingText: '安全验证',
};

/** 英文默认文案 */
export const enMessages: CaptchaMessages = {
  shapeLabel: 'Shape',
  randomLabel: 'Random',
  sliderTip: 'Press and drag the slider to complete the puzzle',
  rotateTip: 'Drag the slider to rotate the image into place',
  slideCurveTip: 'Press and drag to swing the curve into the groove',
  swingTileTip: 'Press and drag to move the tile along the curve into the groove',
  curveTip: 'Press and trace the dashed curve from the green start to the red end',
  promptPrefix: 'Please click in order',
  loadingText: 'Loading image...',
  imageAlt: 'Captcha image',
  title: 'Security Verification',
  refreshTitle: 'Refresh',
  closeTitle: 'Close',
  floatingText: 'Security Verification',
};

/** 按语言返回默认文案 */
export function defaultMessagesFor(locale: CaptchaLocale): CaptchaMessages {
  return locale === 'en' ? enMessages : zhCNMessages;
}

/**
 * 解析最终文案：语言默认值 < 自定义 messages 覆盖。
 * 单个文案 prop（如 sliderTip）仍可在组件/插件层继续覆盖。
 */
export function resolveCaptchaMessages(
  locale: CaptchaLocale,
  customMessages: Partial<CaptchaMessages> = {},
): CaptchaMessages {
  return { ...defaultMessagesFor(locale), ...customMessages };
}
