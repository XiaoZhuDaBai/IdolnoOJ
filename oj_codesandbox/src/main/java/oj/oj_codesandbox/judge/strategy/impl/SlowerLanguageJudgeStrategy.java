package oj.oj_codesandbox.judge.strategy.impl;

import oj.oj_codesandbox.judge.entity.JudgeContext;
import oj.oj_codesandbox.judge.entity.JudgeInfo;
import oj.oj_codesandbox.judge.strategy.AbstractJudgeStrategy;
import oj.oj_codesandbox.model.dto.Question;
import org.springframework.stereotype.Component;

/**
 * 慢速语言判题策略
 * 适用于编译和执行速度较慢的语言（如Java等）
 * 在时间限制上给予额外的1000ms缓冲时间
 */
@Component
public class SlowerLanguageJudgeStrategy extends AbstractJudgeStrategy {
    
    private static final int EXTRA_TIME = 1000; // ms

    /**
     * 重写时间限制检查，为慢速语言提供额外的时间缓冲
     */
    @Override
    protected long getTimeLimit(Question question) {
        return question.getTimeLimit() + EXTRA_TIME;
    }
}
