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

    /** 滑块答案：归一化位移（0~1，相对轨道/图片宽度），滑块类型必填 */
    private Double xNorm;

    /** 点选答案：按点击顺序排列的归一化坐标（0~1），点选类型必填 */
    private List<NormalizedPoint> points;

    /** 旋转答案：用户旋转的角度（度），图片旋转类型必填 */
    private Double angle;

    /** 行为轨迹报文（td），格式见 BehaviorTraceCodec；开启行为校验后必填 */
    private String td;

    /** 客户端类型：web / h5 / mini_program，用于选择对应的行为校验画像 */
    private String clientType;

    /**
     * 构造滑块答案。
     *
     * @param xNorm 归一化位移（0~1，相对轨道/图片宽度）
     * @return 滑块答案
     */
    public static CaptchaAnswer slider(Double xNorm) {
        CaptchaAnswer answer = new CaptchaAnswer();
        answer.xNorm = xNorm;
        return answer;
    }

    /**
     * 构造点选答案。
     *
     * @param points 按点击顺序排列的归一化坐标（0~1）
     * @return 点选答案
     */
    public static CaptchaAnswer click(List<NormalizedPoint> points) {
        CaptchaAnswer answer = new CaptchaAnswer();
        answer.points = points;
        return answer;
    }

    /**
     * 构造旋转答案。
     *
     * @param angle 用户旋转的角度（度）
     * @return 旋转答案
     */
    public static CaptchaAnswer rotate(Double angle) {
        CaptchaAnswer answer = new CaptchaAnswer();
        answer.angle = angle;
        return answer;
    }
}
