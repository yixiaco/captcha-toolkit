// Canvas 动态生成验证码底图、拼图缺口与拼图块

export function rand(min, max) {
  return min + Math.random() * (max - min)
}

export function pick(arr) {
  return arr[Math.floor(Math.random() * arr.length)]
}

export function shuffle(arr) {
  const result = [...arr]
  for (let i = result.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1))
    ;[result[i], result[j]] = [result[j], result[i]]
  }
  return result
}

// 绘制一张随机的卡通风景底图（天空 / 太阳 / 云 / 山 / 草地 / 树）
export function drawScene(ctx, width, height) {
  const hue = Math.floor(Math.random() * 360)

  const sky = ctx.createLinearGradient(0, 0, 0, height)
  sky.addColorStop(0, `hsl(${hue} 65% 78%)`)
  sky.addColorStop(0.65, `hsl(${(hue + 35) % 360} 72% 88%)`)
  sky.addColorStop(1, `hsl(${(hue + 55) % 360} 65% 94%)`)
  ctx.fillStyle = sky
  ctx.fillRect(0, 0, width, height)

  // 太阳
  const sunX = rand(width * 0.18, width * 0.82)
  const sunY = rand(height * 0.1, height * 0.32)
  const sunR = rand(13, 24)
  const sun = ctx.createRadialGradient(sunX, sunY, 2, sunX, sunY, sunR * 2.6)
  sun.addColorStop(0, 'rgba(255,244,179,0.95)')
  sun.addColorStop(0.45, 'rgba(255,224,130,0.75)')
  sun.addColorStop(1, 'rgba(255,224,130,0)')
  ctx.fillStyle = sun
  ctx.fillRect(sunX - sunR * 2.6, sunY - sunR * 2.6, sunR * 5.2, sunR * 5.2)
  ctx.fillStyle = 'rgba(255,244,179,0.92)'
  ctx.beginPath()
  ctx.arc(sunX, sunY, sunR, 0, Math.PI * 2)
  ctx.fill()

  // 云
  for (let i = 0; i < 4; i++) {
    const cx = rand(width * 0.08, width * 0.92)
    const cy = rand(height * 0.08, height * 0.42)
    const r = rand(9, 16)
    ctx.fillStyle = 'rgba(255,255,255,0.55)'
    ctx.beginPath()
    ctx.arc(cx, cy, r, 0, Math.PI * 2)
    ctx.arc(cx + r * 0.9, cy - r * 0.35, r * 0.75, 0, Math.PI * 2)
    ctx.arc(cx + r * 1.8, cy, r * 0.65, 0, Math.PI * 2)
    ctx.fill()
  }

  // 远山与近丘
  ridge(ctx, width, height, `hsl(${(hue + 140) % 360} 38% 62%)`, height * 0.62, height * 0.13, 5)
  ridge(ctx, width, height, `hsl(${(hue + 152) % 360} 42% 52%)`, height * 0.78, height * 0.09, 7)

  // 草地
  const ground = ctx.createLinearGradient(0, height * 0.72, 0, height)
  ground.addColorStop(0, `hsl(${(hue + 160) % 360} 46% 58%)`)
  ground.addColorStop(1, `hsl(${(hue + 170) % 360} 40% 44%)`)
  ctx.fillStyle = ground
  ctx.fillRect(0, height * 0.72, width, height * 0.28)

  // 树木
  for (let i = 0; i < 9; i++) {
    const tx = rand(width * 0.04, width * 0.96)
    const baseY = rand(height * 0.76, height * 0.96)
    const size = rand(7, 14)
    ctx.fillStyle = 'rgba(92,64,38,0.85)'
    ctx.fillRect(tx - size * 0.08, baseY - size * 0.7, size * 0.16, size * 0.9)
    ctx.fillStyle = `hsl(${(hue + 150) % 360} 48% 42%)`
    ctx.beginPath()
    ctx.arc(tx, baseY - size * 0.9, size * 0.5, 0, Math.PI * 2)
    ctx.arc(tx - size * 0.45, baseY - size * 0.55, size * 0.38, 0, Math.PI * 2)
    ctx.arc(tx + size * 0.45, baseY - size * 0.55, size * 0.38, 0, Math.PI * 2)
    ctx.fill()
  }
}

function ridge(ctx, width, height, color, baseY, amp, stepCount) {
  ctx.fillStyle = color
  ctx.beginPath()
  ctx.moveTo(0, height)
  ctx.lineTo(0, baseY)
  const step = width / (stepCount || 6)
  const phase = rand(0, Math.PI * 2)
  for (let x = 0; x <= width + step; x += step) {
    const y = baseY + Math.sin(x * 0.02 + phase) * amp + rand(-3, 3)
    ctx.lineTo(x, y)
  }
  ctx.lineTo(width, height)
  ctx.closePath()
  ctx.fill()
}

// 可选拼图形状：标准极验款 / 叶子 / 三角形 / 圆形 / 菱形 / 星星 / 爱心
export const PUZZLE_SHAPES = {
  classic: { label: '经典', draw: puzzlePathClassic },
  leaf: { label: '叶子', draw: puzzlePathLeaf },
  triangle: { label: '三角', draw: puzzlePathTriangle },
  circle: { label: '圆形', draw: puzzlePathCircle },
  diamond: { label: '菱形', draw: puzzlePathDiamond },
  star: { label: '星星', draw: puzzlePathStar },
  heart: { label: '爱心', draw: puzzlePathHeart },
}

// 经典极验造型：右侧半圆凸起，上边与左边各有一个向内凹陷的半圆
function puzzlePathClassic(ctx, x, y, size) {
  const r = size * 0.09
  const knob = size * 0.14

  ctx.beginPath()
  ctx.moveTo(x + r, y)

  // 上边：向内凹
  ctx.lineTo(x + size * 0.38, y)
  ctx.arc(x + size * 0.5, y, knob, Math.PI, 0, true)
  ctx.lineTo(x + size - r, y)

  ctx.arc(x + size - r, y + r, r, -Math.PI / 2, 0, false)

  // 右边：向外凸
  ctx.lineTo(x + size, y + size * 0.38)
  ctx.arc(x + size, y + size * 0.5, knob, -Math.PI / 2, Math.PI / 2, false)
  ctx.lineTo(x + size, y + size - r)

  ctx.arc(x + size - r, y + size - r, r, 0, Math.PI / 2, false)
  ctx.lineTo(x + r, y + size)
  ctx.arc(x + r, y + size - r, r, Math.PI / 2, Math.PI, false)

  // 左边：向内凹
  ctx.lineTo(x, y + size * 0.62)
  ctx.arc(x, y + size * 0.5, knob, Math.PI / 2, -Math.PI / 2, true)
  ctx.lineTo(x, y + r)

  ctx.arc(x + r, y + r, r, Math.PI, -Math.PI / 2, true)
  ctx.closePath()
}

// 叶子：两端尖、两侧鼓起的叶片轮廓
function puzzlePathLeaf(ctx, x, y, size) {
  ctx.beginPath()
  ctx.moveTo(x + size * 0.5, y + size * 0.02)
  ctx.bezierCurveTo(
    x + size * 0.98, y + size * 0.16,
    x + size * 0.98, y + size * 0.72,
    x + size * 0.5, y + size * 0.98
  )
  ctx.bezierCurveTo(
    x + size * 0.02, y + size * 0.72,
    x + size * 0.02, y + size * 0.16,
    x + size * 0.5, y + size * 0.02
  )
  ctx.closePath()
}

// 三角形：上尖下平的等边三角形
function puzzlePathTriangle(ctx, x, y, size) {
  ctx.beginPath()
  ctx.moveTo(x + size * 0.5, y + size * 0.04)
  ctx.lineTo(x + size * 0.96, y + size * 0.92)
  ctx.lineTo(x + size * 0.04, y + size * 0.92)
  ctx.closePath()
}

// 圆形
function puzzlePathCircle(ctx, x, y, size) {
  ctx.beginPath()
  ctx.arc(x + size * 0.5, y + size * 0.5, size * 0.48, 0, Math.PI * 2)
  ctx.closePath()
}

// 菱形
function puzzlePathDiamond(ctx, x, y, size) {
  ctx.beginPath()
  ctx.moveTo(x + size * 0.5, y + size * 0.04)
  ctx.lineTo(x + size * 0.96, y + size * 0.5)
  ctx.lineTo(x + size * 0.5, y + size * 0.96)
  ctx.lineTo(x + size * 0.04, y + size * 0.5)
  ctx.closePath()
}

// 五角星
function puzzlePathStar(ctx, x, y, size) {
  const cx = x + size * 0.5
  const cy = y + size * 0.5
  const outer = size * 0.5
  const inner = size * 0.22
  ctx.beginPath()
  for (let i = 0; i < 10; i++) {
    const radius = i % 2 === 0 ? outer : inner
    const angle = -Math.PI / 2 + (i * Math.PI) / 5
    const px = cx + Math.cos(angle) * radius
    const py = cy + Math.sin(angle) * radius
    if (i === 0) ctx.moveTo(px, py)
    else ctx.lineTo(px, py)
  }
  ctx.closePath()
}

// 爱心
function puzzlePathHeart(ctx, x, y, size) {
  ctx.beginPath()
  ctx.moveTo(x + size * 0.5, y + size * 0.92)
  ctx.bezierCurveTo(
    x + size * 0.06, y + size * 0.58,
    x + size * 0.04, y + size * 0.16,
    x + size * 0.3, y + size * 0.06
  )
  ctx.bezierCurveTo(
    x + size * 0.44, y + size * 0.01,
    x + size * 0.5, y + size * 0.14,
    x + size * 0.5, y + size * 0.24
  )
  ctx.bezierCurveTo(
    x + size * 0.5, y + size * 0.14,
    x + size * 0.56, y + size * 0.01,
    x + size * 0.7, y + size * 0.06
  )
  ctx.bezierCurveTo(
    x + size * 0.96, y + size * 0.16,
    x + size * 0.94, y + size * 0.58,
    x + size * 0.5, y + size * 0.92
  )
  ctx.closePath()
}

// 生成滑块拼图所需的三张素材：底图、带缺口的图、可拖动的拼图块
export function createSliderAssets(width, height, shape) {
  const shapeName = shape && PUZZLE_SHAPES[shape] ? shape : pick(Object.keys(PUZZLE_SHAPES))
  const shapeDraw = PUZZLE_SHAPES[shapeName].draw
  const bg = document.createElement('canvas')
  bg.width = width
  bg.height = height
  drawScene(bg.getContext('2d'), width, height)

  const pieceSize = Math.min(54, Math.round(height * 0.32))
  const margin = Math.ceil(pieceSize * 0.2 + 4)
  const pieceCanvasSize = pieceSize + margin * 2
  const targetX = Math.round(rand(pieceSize + 22, width - pieceSize - 22))
  const targetY = Math.round(rand(18, height - pieceSize - 18))

  // 带缺口的底图
  const hole = document.createElement('canvas')
  hole.width = width
  hole.height = height
  const holeCtx = hole.getContext('2d')
  holeCtx.drawImage(bg, 0, 0)
  holeCtx.save()
  shapeDraw(holeCtx, targetX, targetY, pieceSize)
  holeCtx.globalCompositeOperation = 'destination-out'
  holeCtx.fill()
  holeCtx.restore()

  // 可拖动的拼图块（画布比块本身大一圈，容纳外凸卡榫和阴影）
  const piece = document.createElement('canvas')
  piece.width = pieceCanvasSize
  piece.height = pieceCanvasSize
  const pieceCtx = piece.getContext('2d')
  pieceCtx.save()
  shapeDraw(pieceCtx, margin, margin, pieceSize)
  pieceCtx.clip()
  pieceCtx.drawImage(bg, -targetX + margin, -targetY + margin)
  pieceCtx.restore()

  pieceCtx.save()
  shapeDraw(pieceCtx, margin, margin, pieceSize)
  pieceCtx.shadowColor = 'rgba(0,0,0,0.35)'
  pieceCtx.shadowBlur = 7
  pieceCtx.strokeStyle = 'rgba(255,255,255,0.92)'
  pieceCtx.lineWidth = 2
  pieceCtx.stroke()
  pieceCtx.restore()

  return {
    hole: hole.toDataURL('image/png'),
    piece: piece.toDataURL('image/png'),
    targetX,
    targetY,
    pieceSize,
    margin,
    shape: shapeName,
  }
}
