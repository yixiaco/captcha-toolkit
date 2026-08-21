package com.captcha.toolkit.behavior;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * 行为报文（td）编解码。
 *
 * <p>为避免核心引擎引入 JSON 依赖，采用紧凑的管道分隔文本协议；</p>
 * <pre>
 * m|w|h|s|e|p
 * p = timeMs,x,y,type;timeMs,x,y,type;...
 * </pre>
 * 示例：<code>1|300.6|262.0|1787290676714|1787290683694|0,0.0,0.5,0;21,0.1,0.5,1;...</code>
 *
 * <p>轨迹点较多时建议用 {@link #encodeCompressed} 压缩（gzip + base64url，与极验一致）；
 * 解码时自动识别压缩/明文两种格式。</p>
 */
public final class BehaviorTraceCodec {

    /** 报文顶层字段分隔符 */
    private static final String FIELD_SEPARATOR = "\\|";

    /** 轨迹点之间的分隔符 */
    private static final String POINT_SEPARATOR = ";";

    /** 单个轨迹点内部值的分隔符 */
    private static final String VALUE_SEPARATOR = ",";

    /** 报文顶层字段数量：m|w|h|s|e|p */
    private static final int EXPECTED_FIELDS = 6;

    /** 单个轨迹点的值数量：timeMs,x,y,type */
    private static final int EXPECTED_POINT_VALUES = 4;

    private BehaviorTraceCodec() {
    }

    /**
     * 编码为 td 文本。
     */
    public static String encode(BehaviorTrace trace) {
        StringBuilder points = new StringBuilder();
        for (BehaviorPoint point : trace.points()) {
            if (!points.isEmpty()) {
                points.append(POINT_SEPARATOR);
            }
            points.append(point.timeMs())
                    .append(VALUE_SEPARATOR)
                    .append(point.x())
                    .append(VALUE_SEPARATOR)
                    .append(point.y())
                    .append(VALUE_SEPARATOR)
                    .append(point.type().code());
        }
        return trace.protocol() + "|"
                + trace.viewportWidth() + "|"
                + trace.viewportHeight() + "|"
                + trace.startTime() + "|"
                + trace.endTime() + "|"
                + points;
    }

    /**
     * 编码为 gzip + base64url 压缩文本，体积远小于明文。
     */
    public static String encodeCompressed(BehaviorTrace trace) {
        String text = encode(trace);
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            try (GZIPOutputStream gzip = new GZIPOutputStream(buffer)) {
                gzip.write(text.getBytes(StandardCharsets.UTF_8));
            }
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(buffer.toByteArray());
        } catch (IOException e) {
            throw new IllegalStateException("压缩行为轨迹失败", e);
        }
    }

    /**
     * 解码 td（自动识别 gzip+base64url 压缩格式与明文格式）；
     * 格式非法时抛出 {@link IllegalArgumentException}。
     */
    public static BehaviorTrace decode(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("行为轨迹为空");
        }
        if (text.startsWith("H4sI")) {
            return decodeCompressed(text);
        }
        return decodePlain(text);
    }

    /** 解析明文格式：m|w|h|s|e|p */
    private static BehaviorTrace decodePlain(String text) {
        String[] fields = text.split(FIELD_SEPARATOR, -1);
        if (fields.length != EXPECTED_FIELDS) {
            throw new IllegalArgumentException("行为轨迹字段数量不正确");
        }
        int protocol = Integer.parseInt(fields[0].trim());
        double width = Double.parseDouble(fields[1].trim());
        double height = Double.parseDouble(fields[2].trim());
        long startTime = Long.parseLong(fields[3].trim());
        long endTime = Long.parseLong(fields[4].trim());
        return new BehaviorTrace(protocol, width, height, startTime, endTime,
                parsePoints(fields[5]));
    }

    /** 解析 gzip + base64url 压缩格式：先解压再走明文解析 */
    private static BehaviorTrace decodeCompressed(String text) {
        try {
            byte[] bytes = Base64.getUrlDecoder().decode(text);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(bytes))) {
                gzip.transferTo(output);
            }
            return decodePlain(output.toString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalArgumentException("解压行为轨迹失败", e);
        }
    }

    /** 解析轨迹点文本：timeMs,x,y,type;... */
    private static List<BehaviorPoint> parsePoints(String text) {
        List<BehaviorPoint> points = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return points;
        }
        for (String segment : text.split(POINT_SEPARATOR, -1)) {
            if (segment.isBlank()) {
                continue;
            }
            String[] values = segment.split(VALUE_SEPARATOR, -1);
            if (values.length != EXPECTED_POINT_VALUES) {
                throw new IllegalArgumentException("轨迹点字段数量不正确: " + segment);
            }
            points.add(new BehaviorPoint(
                    Integer.parseInt(values[0].trim()),
                    Double.parseDouble(values[1].trim()),
                    Double.parseDouble(values[2].trim()),
                    BehaviorEventType.fromCode(Integer.parseInt(values[3].trim()))));
        }
        return points;
    }
}
