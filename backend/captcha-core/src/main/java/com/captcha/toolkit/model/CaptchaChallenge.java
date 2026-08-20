package com.captcha.toolkit.model;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 验证码下发载荷（接口响应模型）。
 */
@Data
public class CaptchaChallenge {

    /** 验证码会话 id，前端提交答案时原样带回 */
    private String id;

    /** 验证码类型：slider / click */
    private String type;

    /** 大图（滑块背景图 / 点选背景图），Base64 Data URI */
    private String image1;

    /** 小图（滑块拼图块），仅滑块类型返回 */
    private String image2;

    /** 图片宽度（服务端像素坐标系） */
    private Integer width;

    /** 图片高度（服务端像素坐标系） */
    private Integer height;

    /** 滑块拼图形状名 */
    private String shape;

    /** 点选提示文字（词组模式为词组，单字模式为单个汉字） */
    private List<String> prompt;
    /** 小图（拼图块）内部左侧留白，前端定位时使用 */
    private Integer pieceOffsetX;
    /** 调试字段：仅 debug=1 且引擎开启调试时返回滑块答案 x */
    private Integer debugX;
    /** 调试字段：仅 debug=1 且引擎开启调试时返回点选目标坐标 */
    private List<PointVo> debugTargets;
    /** 调试字段：仅 debug=1 且引擎开启调试时返回滑块假目标坐标 */
    private List<PointVo> debugFakeTargets;
    /** 扩展元数据：自定义验证码可携带任意附加信息 */
    private Map<String, Object> metadata;

}
