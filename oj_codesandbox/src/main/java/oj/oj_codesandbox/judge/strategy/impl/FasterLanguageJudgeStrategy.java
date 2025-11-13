package oj.oj_codesandbox.judge.strategy.impl;

import oj.oj_codesandbox.judge.entity.JudgeContext;
import oj.oj_codesandbox.judge.entity.JudgeInfo;
import oj.oj_codesandbox.judge.strategy.AbstractJudgeStrategy;
import oj.oj_codesandbox.model.dto.Question;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 快速语言判题策略
 * 适用于编译和执行速度较快的语言（如C、C++、Python、JavaScript等）
 */
@Component
public class FasterLanguageJudgeStrategy extends AbstractJudgeStrategy {

    @Override
    public JudgeInfo doTest(JudgeContext judgeContext) {
        JudgeInfo judgeInfo = judgeContext.getJudgeInfo();
        Long memory = judgeInfo.getMemory();
        Long time = judgeInfo.getTime();

        JudgeInfo judgeInfoResponse = new JudgeInfo();
        judgeInfoResponse.setMemory(memory);
        judgeInfoResponse.setTime(time);

        // 处理特殊的非零异常情况
        List<String> errorMessages = judgeInfo.getErrorMessages();
        if (!errorMessages.isEmpty() && errorMessages.get(0).contains("非零异常")) {
            judgeInfoResponse.setCnMessage(errorMessages.get(0));
            judgeInfoResponse.setEnMessage(errorMessages.get(0));
            judgeInfoResponse.setErrorMessages(errorMessages);
            return judgeInfoResponse;
        }

        // 使用父类的通用逻辑处理其他情况
        return super.doTest(judgeContext);
    }
}
