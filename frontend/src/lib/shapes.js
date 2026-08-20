// 滑块形状注册表：与后端 enabled-shapes 保持一致，可扩展自定义形状

export const PUZZLE_SHAPES = {
  classic: { label: '经典' },
  leaf: { label: '叶子' },
  triangle: { label: '三角' },
  circle: { label: '圆形' },
  diamond: { label: '菱形' },
  star: { label: '星星' },
  heart: { label: '爱心' },
}

/** 注册自定义形状（key 需与后端 shape 名称一致） */
export function registerShape(key, config) {
  PUZZLE_SHAPES[key] = config
}

/** 把形状列表转成选择器需要的选项数组，支持用 shapeLabels 覆盖显示名 */
export function getShapeOptions(shapes, shapeLabels = {}) {
  const keys = Array.isArray(shapes) ? shapes : Object.keys(PUZZLE_SHAPES)
  return keys
    .filter((key) => PUZZLE_SHAPES[key])
    .map((key) => ({
      key,
      label: shapeLabels[key] || PUZZLE_SHAPES[key].label,
    }))
}
