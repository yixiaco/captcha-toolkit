package com.captcha.toolkit.util;

import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;

/**
 * 轻量 SVG 路径解析器。
 *
 * <p>支持 M/L/H/V/C/S/Q/T/A/Z 及对应小写相对命令、命令省略重复参数组、
 * 椭圆弧（A/a）自动转换为三次贝塞尔分段；用于把参考 SVG 的图形路径
 * 引入内置形状，避免引入完整 SVG 解析依赖。</p>
 */
public final class SvgPathParser {

    /** 数值容差 */
    private static final double EPS = 1e-9;

    private SvgPathParser() {
    }

    /**
     * 解析 SVG path 的 d 属性为 {@link Path2D}。
     *
     * @param d SVG 路径数据
     * @return 解析后的路径（坐标系与 SVG 一致，Y 轴向下）
     */
    public static Path2D parse(String d) {
        List<String> tokens = tokenize(d);
        Path2D path = new Path2D.Double();
        Cursor cursor = new Cursor();
        int i = 0;
        char current = 0;
        while (i < tokens.size()) {
            String token = tokens.get(i);
            char cmd;
            if (isCommand(token)) {
                cmd = token.charAt(0);
                i++;
                current = cmd;
            } else {
                if (current == 0) {
                    throw new IllegalArgumentException("SVG 路径缺少命令: " + token);
                }
                cmd = current;
            }
            boolean relative = Character.isLowerCase(cmd);
            switch (Character.toUpperCase(cmd)) {
                case 'M' -> {
                    double x = number(tokens, i++);
                    double y = number(tokens, i++);
                    if (relative) {
                        x += cursor.x;
                        y += cursor.y;
                    }
                    path.moveTo(x, y);
                    cursor.moveTo(x, y);
                    // 后续坐标对视为隐式直线
                    current = relative ? 'l' : 'L';
                }
                case 'L' -> {
                    double x = number(tokens, i++);
                    double y = number(tokens, i++);
                    if (relative) {
                        x += cursor.x;
                        y += cursor.y;
                    }
                    path.lineTo(x, y);
                    cursor.lineTo(x, y);
                }
                case 'H' -> {
                    double x = number(tokens, i++);
                    if (relative) {
                        x += cursor.x;
                    }
                    path.lineTo(x, cursor.y);
                    cursor.x = x;
                }
                case 'V' -> {
                    double y = number(tokens, i++);
                    if (relative) {
                        y += cursor.y;
                    }
                    path.lineTo(cursor.x, y);
                    cursor.y = y;
                }
                case 'C' -> {
                    double x1 = number(tokens, i++);
                    double y1 = number(tokens, i++);
                    double x2 = number(tokens, i++);
                    double y2 = number(tokens, i++);
                    double x = number(tokens, i++);
                    double y = number(tokens, i++);
                    if (relative) {
                        x1 += cursor.x;
                        y1 += cursor.y;
                        x2 += cursor.x;
                        y2 += cursor.y;
                        x += cursor.x;
                        y += cursor.y;
                    }
                    path.curveTo(x1, y1, x2, y2, x, y);
                    cursor.curveTo(x, y, x2, y2);
                }
                case 'S' -> {
                    double x2 = number(tokens, i++);
                    double y2 = number(tokens, i++);
                    double x = number(tokens, i++);
                    double y = number(tokens, i++);
                    if (relative) {
                        x2 += cursor.x;
                        y2 += cursor.y;
                        x += cursor.x;
                        y += cursor.y;
                    }
                    double x1 = cursor.lastCubic ? 2 * cursor.x - cursor.controlX : cursor.x;
                    double y1 = cursor.lastCubic ? 2 * cursor.y - cursor.controlY : cursor.y;
                    path.curveTo(x1, y1, x2, y2, x, y);
                    cursor.curveTo(x, y, x2, y2);
                }
                case 'Q' -> {
                    double x1 = number(tokens, i++);
                    double y1 = number(tokens, i++);
                    double x = number(tokens, i++);
                    double y = number(tokens, i++);
                    if (relative) {
                        x1 += cursor.x;
                        y1 += cursor.y;
                        x += cursor.x;
                        y += cursor.y;
                    }
                    path.quadTo(x1, y1, x, y);
                    cursor.quadTo(x, y, x1, y1);
                }
                case 'T' -> {
                    double x = number(tokens, i++);
                    double y = number(tokens, i++);
                    if (relative) {
                        x += cursor.x;
                        y += cursor.y;
                    }
                    double x1 = cursor.lastQuad ? 2 * cursor.x - cursor.controlX : cursor.x;
                    double y1 = cursor.lastQuad ? 2 * cursor.y - cursor.controlY : cursor.y;
                    path.quadTo(x1, y1, x, y);
                    cursor.quadTo(x, y, x1, y1);
                }
                case 'A' -> {
                    double rx = Math.abs(number(tokens, i++));
                    double ry = Math.abs(number(tokens, i++));
                    double rotation = number(tokens, i++);
                    boolean largeArc = number(tokens, i++) != 0;
                    boolean sweep = number(tokens, i++) != 0;
                    double x = number(tokens, i++);
                    double y = number(tokens, i++);
                    if (relative) {
                        x += cursor.x;
                        y += cursor.y;
                    }
                    arcTo(path, cursor, rx, ry, rotation, largeArc, sweep, x, y);
                    cursor.arcTo(x, y);
                }
                case 'Z' -> {
                    path.closePath();
                    cursor.closePath();
                }
                default -> throw new IllegalArgumentException("不支持的 SVG 命令: " + cmd);
            }
        }
        return path;
    }

    /** 解析过程中的当前点与平滑控制点状态 */
    private static final class Cursor {
        double x;
        double y;
        double startX;
        double startY;
        double controlX;
        double controlY;
        boolean lastCubic;
        boolean lastQuad;

        void moveTo(double x, double y) {
            this.x = x;
            this.y = y;
            startX = x;
            startY = y;
            lastCubic = false;
            lastQuad = false;
        }

        void lineTo(double x, double y) {
            this.x = x;
            this.y = y;
            lastCubic = false;
            lastQuad = false;
        }

        void curveTo(double x, double y, double controlX, double controlY) {
            this.x = x;
            this.y = y;
            this.controlX = controlX;
            this.controlY = controlY;
            lastCubic = true;
            lastQuad = false;
        }

        void quadTo(double x, double y, double controlX, double controlY) {
            this.x = x;
            this.y = y;
            this.controlX = controlX;
            this.controlY = controlY;
            lastCubic = false;
            lastQuad = true;
        }

        void arcTo(double x, double y) {
            this.x = x;
            this.y = y;
            lastCubic = false;
            lastQuad = false;
        }

        void closePath() {
            x = startX;
            y = startY;
            lastCubic = false;
            lastQuad = false;
        }
    }

    /**
     * SVG 椭圆弧（端点参数化）转换为三次贝塞尔分段。
     * 参考 W3C SVG 实现注记的“endpoint → center”算法。
     */
    private static void arcTo(Path2D path, Cursor cursor,
                              double rx, double ry, double rotationDeg,
                              boolean largeArc, boolean sweep,
                              double x, double y) {
        double x0 = cursor.x;
        double y0 = cursor.y;
        if (Math.abs(x - x0) < EPS && Math.abs(y - y0) < EPS) {
            return;
        }
        double phi = Math.toRadians(rotationDeg);
        double cosPhi = Math.cos(phi);
        double sinPhi = Math.sin(phi);

        double dx2 = (x0 - x) / 2;
        double dy2 = (y0 - y) / 2;
        double x1p = cosPhi * dx2 + sinPhi * dy2;
        double y1p = -sinPhi * dx2 + cosPhi * dy2;

        double lambda = x1p * x1p / (rx * rx) + y1p * y1p / (ry * ry);
        if (lambda > 1) {
            double scale = Math.sqrt(lambda);
            rx *= scale;
            ry *= scale;
        }

        double sign = largeArc == sweep ? -1 : 1;
        double numerator = rx * rx * ry * ry
                - rx * rx * y1p * y1p
                - ry * ry * x1p * x1p;
        double denominator = rx * rx * y1p * y1p + ry * ry * x1p * x1p;
        double coefficient = denominator > 0
                ? sign * Math.sqrt(Math.max(0, numerator / denominator))
                : 0;
        double cxp = coefficient * rx * y1p / ry;
        double cyp = coefficient * -ry * x1p / rx;
        double cx = cosPhi * cxp - sinPhi * cyp + (x0 + x) / 2;
        double cy = sinPhi * cxp + cosPhi * cyp + (y0 + y) / 2;

        double ux = (x1p - cxp) / rx;
        double uy = (y1p - cyp) / ry;
        double vx = (-x1p - cxp) / rx;
        double vy = (-y1p - cyp) / ry;
        double startAngle = Math.atan2(uy, ux);
        double delta = Math.atan2(ux * vy - uy * vx, ux * vx + uy * vy);
        if (!sweep && delta > 0) {
            delta -= Math.PI * 2;
        } else if (sweep && delta < 0) {
            delta += Math.PI * 2;
        }

        // 每段不超过 90°，保证贝塞尔拟合误差可控
        int segments = Math.max(1, (int) Math.ceil(Math.abs(delta) / (Math.PI / 2)));
        double step = delta / segments;
        for (int i = 0; i < segments; i++) {
            double theta1 = startAngle + i * step;
            double theta2 = theta1 + step;
            double tangent = (4.0 / 3.0) * Math.tan((theta2 - theta1) / 4);

            double sin1 = Math.sin(theta1);
            double cos1 = Math.cos(theta1);
            double sin2 = Math.sin(theta2);
            double cos2 = Math.cos(theta2);

            double px1 = cx + rx * cos1 * cosPhi - ry * sin1 * sinPhi;
            double py1 = cy + rx * cos1 * sinPhi + ry * sin1 * cosPhi;
            double px2 = cx + rx * cos2 * cosPhi - ry * sin2 * sinPhi;
            double py2 = cy + rx * cos2 * sinPhi + ry * sin2 * cosPhi;

            double tan1x = -rx * sin1 * cosPhi - ry * cos1 * sinPhi;
            double tan1y = -rx * sin1 * sinPhi + ry * cos1 * cosPhi;
            double tan2x = -rx * sin2 * cosPhi - ry * cos2 * sinPhi;
            double tan2y = -rx * sin2 * sinPhi + ry * cos2 * cosPhi;

            path.curveTo(
                    px1 + tangent * tan1x, py1 + tangent * tan1y,
                    px2 - tangent * tan2x, py2 - tangent * tan2y,
                    px2, py2);
        }
    }

    /** 读取一个数值 token */
    private static double number(List<String> tokens, int index) {
        if (index >= tokens.size()) {
            throw new IllegalArgumentException("SVG 路径参数不足");
        }
        return Double.parseDouble(tokens.get(index));
    }

    /** 是否为命令 token */
    private static boolean isCommand(String token) {
        return token.length() == 1 && isCommand(token.charAt(0));
    }

    private static boolean isCommand(char c) {
        return Character.isLetter(c) && c != 'e' && c != 'E';
    }

    /** 把 SVG d 切分为命令与数字 token */
    private static List<String> tokenize(String d) {
        List<String> tokens = new ArrayList<>();
        int n = d.length();
        int i = 0;
        while (i < n) {
            char c = d.charAt(i);
            if (Character.isWhitespace(c) || c == ',') {
                i++;
                continue;
            }
            if (isCommand(c)) {
                tokens.add(String.valueOf(c));
                i++;
                continue;
            }
            int start = i;
            boolean seenDot = false;
            boolean seenExponent = false;
            while (i < n) {
                char ch = d.charAt(i);
                if (Character.isDigit(ch)) {
                    i++;
                    continue;
                }
                if (ch == '.' && !seenDot) {
                    seenDot = true;
                    i++;
                    continue;
                }
                if ((ch == 'e' || ch == 'E') && !seenExponent) {
                    seenExponent = true;
                    i++;
                    if (i < n && (d.charAt(i) == '+' || d.charAt(i) == '-')) {
                        i++;
                    }
                    continue;
                }
                if (ch == '+' || ch == '-') {
                    if (i == start) {
                        i++;
                        continue;
                    }
                    break;
                }
                break;
            }
            tokens.add(d.substring(start, i));
        }
        return tokens;
    }
}
