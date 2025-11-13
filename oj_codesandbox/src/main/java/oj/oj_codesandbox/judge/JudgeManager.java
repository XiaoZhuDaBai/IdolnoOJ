package oj.oj_codesandbox.judge;


import oj.oj_codesandbox.judge.entity.JudgeContext;
import oj.oj_codesandbox.judge.entity.JudgeInfo;
import oj.oj_codesandbox.judge.strategy.JudgeStrategy;
import oj.oj_codesandbox.judge.strategy.impl.FasterLanguageJudgeStrategy;
import oj.oj_codesandbox.judge.strategy.impl.SlowerLanguageJudgeStrategy;
import oj.oj_codesandbox.model.dto.UserCommit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class JudgeManager {

    @Autowired
    private FasterLanguageJudgeStrategy fasterLanguageJudgeStrategy;

    @Autowired
    private SlowerLanguageJudgeStrategy slowerLanguageJudgeStrategy;
    public JudgeInfo doJudge(JudgeContext judgeContext) {
        UserCommit userCommit = judgeContext.getUserCommit();
        String language = userCommit.getLanguage();
        JudgeStrategy judgeStrategy = getStrategyByLanguage(language);
        return judgeStrategy.doJudge(judgeContext);
    }

    public JudgeInfo doTest(JudgeContext judgeContext) {
        String language = judgeContext.getLanguage();
        JudgeStrategy judgeStrategy = getStrategyByLanguage(language);
        return judgeStrategy.doTest(judgeContext);
    }

    //todo 补充语言种类的
    private JudgeStrategy getStrategyByLanguage(String language) {
        if ("java".equals(language)) {
            return slowerLanguageJudgeStrategy;
        } else {
            return fasterLanguageJudgeStrategy;
        }
    }
}
