package com.captcha.toolkit.behavior;

import com.captcha.toolkit.config.ClientBehaviorConfig;

/**
 * 行为风险评分策略：从轨迹中提取统计特征，输出综合异常分数。
 *
 * <p>与硬规则校验不同，风险评分不设单特征否决，而是把多个弱信号
 * 加权汇总，超过画像阈值才判定异常，降低正常用户误伤。</p>
 */
public interface BehaviorRiskScorer {

    /**
     * 计算轨迹的风险分数。
     *
     * @param trace   已通过硬规则校验的行为轨迹
     * @param profile 客户端画像（含分端拒绝阈值）
     * @return 风险评分结果
     */
    BehaviorRiskResult score(BehaviorTrace trace, ClientBehaviorConfig profile);
}
