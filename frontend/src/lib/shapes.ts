// 滑块形状注册表：与后端 enabled-shapes 保持一致，可扩展自定义形状

export interface ShapeConfig {
  label: string
}

export type ShapeMap = Record<string, ShapeConfig>

export const PUZZLE_SHAPES: ShapeMap = {
  classic: { label: '经典' },
  leaf: { label: '叶子' },
  triangle: { label: '三角' },
  circle: { label: '圆形' },
  diamond: { label: '菱形' },
  star: { label: '星星' },
  heart: { label: '爱心' },
  moon: { label: '月亮' },
  hexagon: { label: '六边形' },
  bat: { label: '蝙蝠' },
  elephant: { label: '大象' },
  dolphin: { label: '海豚' },
  butterfly: { label: '蝴蝶' },
  whale: { label: '鲸鱼' },
  owl: { label: '猫头鹰' },
  bird: { label: '鸟' },
  frog: { label: '青蛙' },
  bear: { label: '熊' },
  duck: { label: '鸭子' },
  eagle: { label: '鹰' },
  fish: { label: '鱼' },
  pig: { label: '猪' },
  airplane: { label: '飞机' },
  fire: { label: '火热' },
  school: { label: '学校' },
};

/** 注册自定义形状（key 需与后端 shape 名称一致） */
export function registerShape(key: string, config: ShapeConfig): void {
  PUZZLE_SHAPES[key] = config;
}

/** 把形状列表转成选择器需要的选项数组，支持用 shapeLabels 覆盖显示名 */
export function getShapeOptions(
  shapes?: string[] | null,
  shapeLabels: Record<string, string> = {},
): Array<{ key: string; label: string }> {
  const keys = Array.isArray(shapes) ? shapes : Object.keys(PUZZLE_SHAPES);
  return keys
    .filter((key) => PUZZLE_SHAPES[key])
    .map((key) => ({
      key,
      label: shapeLabels[key] || PUZZLE_SHAPES[key].label,
    }));
}
