package com.captcha.toolkit.shape;

import com.captcha.toolkit.util.SvgPathParser;

import java.awt.geom.Arc2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

/**
 * 内置拼图形状集合。
 *
 * <p>经典形状严格参考 puzzle_captcha：3x3 方块，上边内凹、右边外凸、左边内凹。</p>
 */
public final class PuzzleShapes {

    /** 工具类不可实例化 */
    private PuzzleShapes() {
    }

    /** 经典 3x3 拼图块 */
    public static PuzzleShape classic() {
        return named("classic", "经典", PuzzleShapes::createClassic);
    }

    /** 叶子形状 */
    public static PuzzleShape leaf() {
        return named("leaf", "叶子", (x, y, size) -> {
            Path2D path = new Path2D.Double();
            path.append(LEAF_TEMPLATE.getPathIterator(null), false);
            fitToBox(path, x, y, size);
            return path;
        });
    }

    /** 参考 Leaf.svg 的树叶路径（viewBox 1024×1024，解析一次后复用） */
    private static final Path2D LEAF_TEMPLATE = SvgPathParser.parse(
            "M853.333333 128h-171.264 "
                    + "C316.16 128 133.802667 281.301333 127.573333 542.250667 "
                    + "l-0.128 21.973333 "
                    + "c0.597333 74.538667 17.621333 138.325333 68.266667 201.216 "
                    + "a726.186667 726.186667 0 0 0-24.746667 125.866667 "
                    + "42.666667 42.666667 0 1 0 84.778667 9.386666 "
                    + "c3.541333-31.744 8.832-61.738667 16-90.026666 "
                    + "H384 "
                    + "c309.717333 0 490.624-180.992 511.914667-552.234667 "
                    + "L896 170.666667 "
                    + "a42.666667 42.666667 0 0 0-42.666667-42.666667 "
                    + "z");

    /** 三角形 */
    public static PuzzleShape triangle() {
        return named("triangle", "三角", (x, y, size) -> {
            Path2D path = new Path2D.Double();
            path.moveTo(x + size * 0.5, y + size * 0.04);
            path.lineTo(x + size * 0.96, y + size * 0.92);
            path.lineTo(x + size * 0.04, y + size * 0.92);
            path.closePath();
            return path;
        });
    }

    /** 圆形 */
    public static PuzzleShape circle() {
        return named("circle", "圆形", (x, y, size) ->
                new Path2D.Double(new Ellipse2D.Double(x, y, size, size)));
    }

    /** 菱形 */
    public static PuzzleShape diamond() {
        return named("diamond", "菱形", (x, y, size) -> {
            Path2D path = new Path2D.Double();
            path.moveTo(x + size * 0.5, y + size * 0.04);
            path.lineTo(x + size * 0.96, y + size * 0.5);
            path.lineTo(x + size * 0.5, y + size * 0.96);
            path.lineTo(x + size * 0.04, y + size * 0.5);
            path.closePath();
            return path;
        });
    }

    /** 五角星 */
    public static PuzzleShape star() {
        return named("star", "星星", (x, y, size) -> {
            double cx = x + size * 0.5;
            double cy = y + size * 0.5;
            double outer = size * 0.5;
            double inner = size * 0.22;
            Path2D path = new Path2D.Double();
            for (int i = 0; i < 10; i++) {
                double radius = i % 2 == 0 ? outer : inner;
                double angle = -Math.PI / 2 + i * Math.PI / 5;
                double px = cx + Math.cos(angle) * radius;
                double py = cy + Math.sin(angle) * radius;
                if (i == 0) {
                    path.moveTo(px, py);
                } else {
                    path.lineTo(px, py);
                }
            }
            path.closePath();
            return path;
        });
    }

    /** 爱心（参数方程生成后居中缩放） */
    public static PuzzleShape heart() {
        return named("heart", "爱心", (x, y, size) -> {
            Path2D path = new Path2D.Double();
            // 经典参数方程爱心：
            // x = 16 * sin^3(t)
            // y = 13*cos(t) - 5*cos(2t) - 2*cos(3t) - cos(4t)
            // 曲线横向 32 个单位，等比缩放到 size，再按实际边界居中
            double scale = size / 32.0;
            int steps = 256;
            for (int i = 0; i <= steps; i++) {
                double t = i * 2 * Math.PI / steps;
                double sinT = Math.sin(t);
                double px = 16 * sinT * sinT * sinT * scale;
                // 屏幕坐标系 y 向下，参数方程的数学 y 需取反
                double py = -(13 * Math.cos(t) - 5 * Math.cos(2 * t)
                        - 2 * Math.cos(3 * t) - Math.cos(4 * t)) * scale;
                if (i == 0) {
                    path.moveTo(px, py);
                } else {
                    path.lineTo(px, py);
                }
            }
            path.closePath();
            // 等比缩放并居中到 (x, y, size, size)
            fitToBox(path, x, y, size);
            return path;
        });
    }

    /** 月亮（参考 月亮_moon.svg 的月牙轮廓） */
    public static PuzzleShape moon() {
        return named("moon", "月亮", (x, y, size) -> {
            Path2D path = new Path2D.Double();
            path.append(MOON_TEMPLATE.getPathIterator(null), false);
            fitToBox(path, x, y, size);
            return path;
        });
    }

    /** 参考 月亮_moon.svg 的月牙路径（viewBox 48×48） */
    private static final Path2D MOON_TEMPLATE = SvgPathParser.parse(
            "M28.0527 4.41085 "
                    + "C22.5828 5.83695 18.5455 10.8106 18.5455 16.7273 "
                    + "C18.5455 23.7564 24.2436 29.4545 31.2727 29.4545 "
                    + "C37.1894 29.4545 42.1631 25.4172 43.5891 19.9473 "
                    + "C43.8585 21.256 44 22.6115 44 24 "
                    + "C44 35.0457 35.0457 44 24 44 "
                    + "C12.9543 44 4 35.0457 4 24 "
                    + "C4 12.9543 12.9543 4 24 4 "
                    + "C25.3885 4 26.744 4.14149 28.0527 4.41085 "
                    + "z");

    /** 六边形 */
    public static PuzzleShape hexagon() {
        return named("hexagon", "六边形", (x, y, size) -> {
            Path2D path = new Path2D.Double();
            double cx = x + size * 0.5;
            double cy = y + size * 0.5;
            double r = size * 0.5;
            for (int i = 0; i < 6; i++) {
                double angle = -Math.PI / 2 + i * Math.PI / 3;
                double px = cx + Math.cos(angle) * r;
                double py = cy + Math.sin(angle) * r;
                if (i == 0) {
                    path.moveTo(px, py);
                } else {
                    path.lineTo(px, py);
                }
            }
            path.closePath();
            return path;
        });
    }

    /** 蝙蝠 */
    public static PuzzleShape bat() {
        return fromSvg("bat", "蝙蝠", "M21.6188 8.98551L22.095 11.4493H25.905L26.3812 8.98551C26.8574 10.628 27.8097 14.2087 27.8097 15.3913C29.5558 15.2271 32.9526 14.0116 32.5716 10.4638C32.5716 9.97101 32.1905 8.78841 30.6667 8C34.9524 8.98551 43.619 13.7159 44 24.7536C41.1429 22.9469 35.2381 21.6 34.4762 30.6667C32.7303 27.7101 28.6671 23.3739 26.3812 29.6812C25.5875 32.4734 23.9998 38.8464 23.9998 42C23.9998 38.8464 22.4125 32.4734 21.6188 29.6812C19.3329 23.3739 15.2697 27.7101 13.5238 30.6667C12.7619 21.6 6.85714 22.9469 4 24.7536C4.38095 13.7159 13.0476 8.98551 17.3333 8C15.8095 8.78841 15.4284 9.97101 15.4284 10.4638C15.0474 14.0116 18.4442 15.2271 20.1903 15.3913C20.1903 14.2087 21.1426 10.628 21.6188 8.98551Z");
    }

    /** 大象 */
    public static PuzzleShape elephant() {
        return fromSvg("elephant", "大象", "M16.7942 12C12.7417 11.1116 4 12.1925 4 21.7874V40H9.86912V33.7811H26.638V40H32.5071V28.8949C33.2058 28.3026 34.4075 33.0231 36.6994 34.2253C38.0521 34.9349 39.2147 35.3968 40.4724 35.1137C43.1948 34.5009 44.4969 32.715 43.8261 28.4507C42.5685 29.9313 38.7955 31.5429 38.3763 28.4507C38.3763 24.8798 38.3763 15.5687 38.3763 15.5687C37.957 12.9035 35.5255 7.66183 29.1534 8.01719H22.2842C19.3497 8 14.9898 12.0148 15.9959 17.3452C16.3082 19 17.5 22.3045 21.7035 22.6757C23.3804 22.8238 26.1534 22.1427 27.8302 18.2336");
    }

    /** 海豚 */
    public static PuzzleShape dolphin() {
        return fromSvg("dolphin", "海豚", "M24.0002 7.00002C14.2305 1.61313 9.68601 8.63202 8.00016 11.0004C2.33569 12.2184 5.14579 14.3237 7.00023 15.0004C8.21404 15.4064 11.1458 16.3233 13.0002 16.9999C13.4048 20.248 14.6631 23.1541 15.0002 23.9999C15.0002 23.1879 16.3259 20.3533 17.0002 18.9999C25.0923 22.248 30.7971 30.6015 34.0002 34.9999C32.7864 37.436 31.506 41.3082 31.0002 42.9999L37.0002 40L44.0002 42C44.0002 38.7519 39.8547 35.3534 38.0002 34C38.8095 21.0076 32.7091 13.1993 29.0002 11C29.4048 9.37598 30.1573 6.67671 31.0002 6.00002C27.7634 4.37598 25.1803 6.15418 24.0002 7.00002Z");
    }

    /** 蝴蝶 */
    public static PuzzleShape butterfly() {
        return fromSvg("butterfly", "蝴蝶", "M5.0002 12.0003C8.66389 7.70613 19.0814 18.8191 24.0002 25C28.9191 18.8191 39.3366 7.70599 43.0002 12.0001C43.6787 12.6507 44.4427 14.877 42.0002 18C41.3217 18.9759 40.186 21.7059 41.0002 26C41.0002 27.1386 39.8852 28.9519 35.0002 27C37.3749 28.6266 41.8498 33.0962 39.0002 37C36.2864 40.4158 29.9649 44.4569 26.0002 35L24.0002 31L22.0002 35C18.0356 44.4569 11.7141 40.4158 9.00024 37C6.15071 33.0962 10.6255 28.6268 13.0001 27.0003C8.1152 28.9521 7.00011 27.1389 7.00011 26.0003C7.81438 21.7061 6.67867 18.9762 6.00012 18.0003C3.55766 14.8773 4.32174 12.6509 5.0002 12.0003Z");
    }

    /** 鲸鱼 */
    public static PuzzleShape whale() {
        return fromSvg("whale", "鲸鱼", "M16.0691 13C6.25336 13.3494 4.67583 21.0064 5.11403 24.7913C3.36116 27.9356 3.89597 28.7217 5.2104 30.0319C11.7835 36.1459 24.0534 35.7092 29.75 33.5256C35.7096 31.08 38.7486 26.4747 39.0407 24C44.2991 20.5063 44.4063 14.6013 43.676 13C42.896 14.3101 40.6474 15.2721 39.0407 16C37.2879 16.3494 34.7659 15.038 34.0355 13.8734C33.5343 16.5 34.0355 18.5 35.3501 19.5507C37.6376 21.2976 36.4754 23.0633 36.0372 23.5C34.1405 25.3902 30.5308 24.4905 28.7771 21.7343C24.0533 14.3101 18.9905 13 16.0691 13Z");
    }

    /** 猫头鹰 */
    public static PuzzleShape owl() {
        return fromSvg("owl", "猫头鹰", "M6.35826 7.30954C9.01959 11.1582 12.544 12.4023 14.2182 12.5245C17.0733 11.1273 20.4207 10.3232 24 10.3232C27.5793 10.3232 30.9267 11.1272 33.7818 12.5245C35.456 12.4023 38.9804 11.1582 41.6417 7.30954C42.3988 6.4886 44.6721 6.8713 43.8063 13.8831C43.5173 15.6385 42.7186 19.4684 42.1897 21.3732C42.6781 22.7341 42.9399 24.1731 42.9399 25.6616C42.9399 34.1328 34.4602 41 24 41C13.5398 41 5.06009 34.1328 5.06009 25.6616C5.06009 24.1731 5.3219 22.7341 5.81026 21.3732C5.28138 19.4684 4.48265 15.6385 4.19369 13.8831C3.32787 6.8713 5.60125 6.4886 6.35826 7.30954Z");
    }

    /** 鸟 */
    public static PuzzleShape bird() {
        return fromSvg("bird", "鸟", "M9 14.0003L4 20.0689C4 20.0689 5.84913 27.035 11 32.0006C20.8896 41.5344 35.3341 35.304 41 31.0006C46.3568 26.6309 43.7169 25.6695 42 26.0006L37 27.0006C46.0654 12.6997 43.5754 11.173 41 12.0006L32 16.0006C26.2311 19.1785 23.5 17.5006 22 16.0006L19 13.0002C14.5 9 10.0302 12.8417 9 14.0003Z");
    }

    /** 青蛙 */
    public static PuzzleShape frog() {
        return fromSvg("frog", "青蛙", "M19.1015 10.8942C19.5261 11.6689 19.7431 12.5226 19.772 13.4061C20.9787 13.2453 22.2252 13.1611 23.5003 13.1611C25.1295 13.1611 26.7122 13.2986 28.2249 13.5577C28.2328 12.6203 28.4497 11.7131 28.8985 10.8942C30.6575 7.68502 35.3132 7.04762 39.2973 9.47057C43.2814 11.8935 45.0852 16.4593 43.3262 19.6685C42.8986 20.4486 42.2999 21.0768 41.5812 21.5459C42.4961 23.1006 43 24.8001 43 26.5806C43 33.9919 34.2697 40 23.5003 40C12.7308 40 4.00052 33.9919 4.00052 26.5806C4.00052 24.5994 4.62433 22.7186 5.74416 21.026C5.32121 20.6426 4.95932 20.1894 4.67378 19.6685C2.91478 16.4593 4.71859 11.8935 8.7027 9.47057C12.6868 7.04762 17.3425 7.68502 19.1015 10.8942Z");
    }

    /** 熊 */
    public static PuzzleShape bear() {
        return fromSvg("bear", "熊", "M5 13.0606C5 15.5392 6.29171 17.5214 8 19C6.92442 21.1464 6 23.4737 6 26C6 35.2215 14.1238 43 24 43C33.8762 43 42 35.2215 42 26C42 23.4737 41.0756 21.1464 40 19C41.7083 17.5214 43 15.4786 43 13C43 8.54825 39.3208 5 35 5C31.7266 5 29.167 7.06019 28 10C26.7347 9.73491 25.351 9.60606 24 9.60606C22.649 9.60606 21.2653 9.73491 20 10C18.833 7.06019 16.2734 5 13 5C8.67918 5 5 8.60886 5 13.0606Z");
    }

    /** 鸭子 */
    public static PuzzleShape duck() {
        return fromSvg("duck", "鸭子", "M12 7.99964C8.94774 12.5171 6.01307 13.8091 4 12.9992C4.54274 14.8333 8.44257 17.3755 10.9274 18.838C11.9978 19.4679 12.3095 20.9029 11.5649 21.897C10.2021 23.7164 8.31564 26.289 8 27.0001C0.954271 39.7887 16.482 44.6479 24 44.0001C46.1437 42.0918 45.3551 24.8027 42 18.0001C33.9477 31.9941 21.5186 23.9154 22 21C22.4814 18.0846 25.7922 18.6647 27 14C29.0131 4.76782 16.3737 0.398938 12 7.99964Z");
    }

    /** 鹰 */
    public static PuzzleShape eagle() {
        return fromSvg("eagle", "鹰", "M5.99968 23.0008C1.96038 15.9568 9.62382 11.8637 13.9997 11.0004C28.541 -1.84434 40.485 10.7132 41.9998 19C43.5145 27.2868 43.1582 33.8931 43.9997 37.0008C37.5368 28.2995 33.1225 29.8415 31.9997 32.0008C29.98 36.1442 26.6863 36.2519 24.9997 35.0008C20.9604 31.686 14.5242 38.2023 11.9997 42.0005C16.847 33.2992 17.5047 27.7272 16.9997 26.0008C14.98 17.7138 8.69261 20.5837 5.99968 23.0008Z");
    }

    /** 鱼 */
    public static PuzzleShape fish() {
        return fromSvg("fish", "鱼", "M44 24C42.7848 28.6903 36.038 32.4667 33 32.9997C30.5696 38.9691 24.038 39.5327 21 38.9997L25 32.9997C20.5443 32.5733 15.0253 27.9544 13 26.0001C10.3861 28.8504 6.19409 31.0805 4 31.9688C7.64557 24.2939 5.51899 17.3097 4 15.0001C6.83544 15.0001 11.1435 18.2235 13 20.0001C15.0253 17.8681 21.962 14.8879 25 13.9997L21 8.99979C28.6962 8.147 32.1561 11.868 33 14C40.6962 15.7056 43.6624 21.6904 44 24Z");
    }

    /** 猪 */
    public static PuzzleShape pig() {
        return fromSvg("pig", "猪", "M14.0538 9.64415C14.5962 10.1855 15.0733 10.8148 15.4678 11.4894C17.927 9.95189 20.8351 9.06302 23.9511 9.06302C27.0975 9.06302 30.0317 9.96926 32.5061 11.5345C32.9055 10.8428 33.3917 10.1975 33.9462 9.64418C36.4673 7.12825 40.8924 6.02041 42.937 8.06083C44.9816 10.1012 43.8715 14.5172 41.3504 17.0332C40.6016 17.7805 39.6848 18.4035 38.7122 18.8568C39.511 20.7563 39.9524 22.8424 39.9524 25.0315C39.9524 33.8507 32.7884 41 23.9511 41C15.1138 41 7.94978 33.8507 7.94978 25.0315C7.94978 22.8277 8.39715 20.7281 9.20621 18.8183C8.26404 18.3685 7.37746 17.7595 6.64959 17.0331C4.12849 14.5172 3.01837 10.1012 5.06298 8.06079C7.10758 6.02038 11.5327 7.12822 14.0538 9.64415Z");
    }

    /** 飞机 */
    public static PuzzleShape airplane() {
        return fromSvg("airplane", "飞机", "M20.5 10.5372C20.5 6.5143 22.8333 4.50286 24 4C25.1667 4.50286 27.5 6.5143 27.5 10.5372V18.0801L43 31V35L27 27V36L32 44L24 41L16 44L21 36V27L5 35V31L20.5 18.0801V10.5372Z");
    }

    /** 火热 */
    public static PuzzleShape fire() {
        return fromSvg("fire", "火热", "M24 44C32.2347 44 38.9998 37.4742 38.9998 29.0981C38.9998 27.0418 38.8953 24.8375 37.7555 21.4116C36.6157 17.9858 36.3861 17.5436 35.1809 15.4279C34.666 19.7454 31.911 21.5448 31.2111 22.0826C31.2111 21.5231 29.5445 15.3359 27.0176 11.6339C24.537 8 21.1634 5.61592 19.1853 4C19.1853 7.06977 18.3219 11.6339 17.0854 13.9594C15.8489 16.2849 15.6167 16.3696 14.0722 18.1002C12.5278 19.8308 11.8189 20.3653 10.5274 22.4651C9.23596 24.565 9 27.3618 9 29.4181C9 37.7942 15.7653 44 24 44Z");
    }

    /** 学校 */
    public static PuzzleShape school() {
        return fromSvg("school", "学校", "M4 33C4 31.8954 4.89543 31 6 31H12V24L24 16L36 24V31H42C43.1046 31 44 31.8954 44 33V42C44 43.1046 43.1046 44 42 44H4V33Z");
    }

    /** 从参考 SVG 路径创建内置形状（等比适配方块） */
    private static PuzzleShape fromSvg(String name, String label, String pathData) {
        Path2D template = SvgPathParser.parse(pathData);
        return named(name, label, (x, y, size) -> {
            Path2D path = new Path2D.Double();
            path.append(template.getPathIterator(null), false);
            fitToBox(path, x, y, size);
            return path;
        });
    }

    /** 返回全部内置形状 */
    public static List<PuzzleShape> all() {
        return List.of(classic(), leaf(), triangle(), circle(), diamond(), star(), heart(),
                moon(), hexagon(),
                bat(), elephant(), dolphin(), butterfly(), whale(), owl(), bird(),
                frog(), bear(), duck(), eagle(), fish(), pig(),
                airplane(), fire(), school());
    }

    /** 包装名称、标签与绘制工厂为一个不可变形状 */
    private static PuzzleShape named(String name, String label, ShapeFactory factory) {
        return new PuzzleShape() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public String getLabel() {
                return label;
            }

            @Override
            public Path2D create(double x, double y, double size) {
                return factory.create(x, y, size);
            }
        };
    }

    /**
     * puzzle_captcha 经典图形：上边内凹、右边外凸、左边内凹。
     */
    private static Path2D createClassic(double x, double y, double size) {
        double u = size / 3.0;
        Path2D path = new Path2D.Double();
        path.moveTo(x, y);
        path.lineTo(x + u, y);
        // 上边内凹半圆
        path.append(new Arc2D.Double(x + u, y - u / 2, u, u, 180, -180, Arc2D.OPEN), true);
        path.lineTo(x + size, y);
        path.lineTo(x + size, y + u);
        // 右边外凸半圆
        path.append(new Arc2D.Double(x + size - u / 2, y + u, u, u, 90, -180, Arc2D.OPEN), true);
        path.lineTo(x + size, y + size);
        path.lineTo(x, y + size);
        path.lineTo(x, y + 2 * u);
        // 左边内凹半圆
        path.append(new Arc2D.Double(x - u / 2, y + u, u, u, -90, 180, Arc2D.OPEN), true);
        path.lineTo(x, y);
        path.closePath();
        return path;
    }

    /**
     * 将路径等比缩放并居中到 (x, y, size, size) 方块内。
     */
    private static void fitToBox(Path2D path, double x, double y, double size) {
        Rectangle2D bounds = path.getBounds2D();
        double scale = Math.min(size / bounds.getWidth(), size / bounds.getHeight());
        path.transform(AffineTransform.getScaleInstance(scale, scale));
        Rectangle2D scaled = path.getBounds2D();
        double dx = x + (size - scaled.getWidth()) / 2.0 - scaled.getMinX();
        double dy = y + (size - scaled.getHeight()) / 2.0 - scaled.getMinY();
        path.transform(AffineTransform.getTranslateInstance(dx, dy));
    }

    @FunctionalInterface
    private interface ShapeFactory {

        /** 在 (x, y) 处绘制边长为 size 的形状路径 */
        Path2D create(double x, double y, double size);
    }
}
