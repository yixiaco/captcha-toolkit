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

    /** 滑块答案：前端图片宽度，用于服务端做缩放换算；缺省时按服务端宽度处理 */
    private Integer clientWidth;

    /** 点选答案：按点击顺序排列的坐标列表，点选类型必填 */
    private List<PointVo> points;

    public static CaptchaAnswer slider(Double x, Integer clientWidth) {
        CaptchaAnswer answer = new CaptchaAnswer();
        answer.x = x;
        answer.clientWidth = clientWidth;
        return answer;
    }

    public static CaptchaAnswer click(List<PointVo> points) {
        CaptchaAnswer answer = new CaptchaAnswer();
        answer.points = points;
        return answer;
    }
}
