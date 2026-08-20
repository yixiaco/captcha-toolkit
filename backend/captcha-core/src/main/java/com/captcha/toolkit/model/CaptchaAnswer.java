package com.captcha.toolkit.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 验证码答案（前端提交的载荷）。
 */
@Data
public class CaptchaAnswer {

    /** 验证码会话 id，下发验证码时返回，必填 */
    @NotNull(message = "缺少验证码 id")
    private String id;

    /** 验证码类型：slider / click，必填 */
    @NotBlank(message = "缺少验证码类型")
    private String type;

    /** 滑块答案：拖动位移（像素），滑块类型必填 */
    private Double x;

    /** 前端实际渲染宽度（px），用于把客户端坐标换算回服务端坐标 */
    private Integer clientWidth;

    /** 前端实际渲染高度（px），配合 clientWidth 按图片比例做双轴换算 */
    private Integer clientHeight;

    /** 点选答案：按点击顺序排列的坐标列表，点选类型必填；坐标使用前端实际渲染尺寸 */
    private List<PointVo> points;

    /** 旋转答案：用户旋转的角度（度），图片旋转类型必填 */
    private Double angle;

    public static CaptchaAnswer slider(Double x, Integer clientWidth) {
        return slider(x, clientWidth, null);
    }

    public static CaptchaAnswer slider(Double x, Integer clientWidth, Integer clientHeight) {
        CaptchaAnswer answer = new CaptchaAnswer();
        answer.x = x;
        answer.clientWidth = clientWidth;
        answer.clientHeight = clientHeight;
        return answer;
    }

    public static CaptchaAnswer click(List<PointVo> points) {
        return click(points, null, null);
    }

    public static CaptchaAnswer click(List<PointVo> points,
                                      Integer clientWidth,
                                      Integer clientHeight) {
        CaptchaAnswer answer = new CaptchaAnswer();
        answer.points = points;
        answer.clientWidth = clientWidth;
        answer.clientHeight = clientHeight;
        return answer;
    }

    public static CaptchaAnswer rotate(Double angle) {
        CaptchaAnswer answer = new CaptchaAnswer();
        answer.angle = angle;
        return answer;
    }
}
