package com.captcha.toolkit.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * 滑块拼图验证码的类型特定化载荷。
 *
 * <p>仅包含前端渲染和调试需要的信息；拼图形状属于服务端答案的一部分，
 * 由会话在服务端保存，不会下发给前端，避免前端通过形状名推断凹槽特征。</p>
 *
 * @param pieceOffsetX     拼图块内部左侧留白（像素），前端据此修正拼图块相对滑块的偏移
 * @param debugX           调试：滑块正确答案 x（像素坐标，仅 debug 模式返回）
 * @param debugFakeTargets 调试：假目标凹槽坐标（仅 debug 模式返回）
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SliderChallengeData(
        Integer pieceOffsetX,
        Integer debugX,
        List<PointVo> debugFakeTargets) {
}
