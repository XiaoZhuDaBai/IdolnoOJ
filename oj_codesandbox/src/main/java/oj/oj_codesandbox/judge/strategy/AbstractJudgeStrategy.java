package oj.oj_codesandbox.judge.strategy;

import oj.oj_codesandbox.judge.entity.JudgeContext;
import oj.oj_codesandbox.judge.entity.JudgeInfo;
import oj.oj_codesandbox.judge.myenum.CommitStatusEnum;
import oj.oj_codesandbox.model.dto.Question;
import oj.oj_codesandbox.service.ErrorClassificationService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * 抽象判题策略基类
 * 提取公共的判题逻辑，减少代码重复
 */
public abstract class AbstractJudgeStrategy implements JudgeStrategy {

    @Autowired
    protected ErrorClassificationService errorClassificationService;

    @Override
    public JudgeInfo doJudge(JudgeContext judgeContext) {
        JudgeInfo judgeInfo = judgeContext.getJudgeInfo();
        Long memory = judgeInfo.getMemory();
        Long time = judgeInfo.getTime();
        boolean[] correct = judgeInfo.getCorrect();
        Question question = judgeContext.getQuestion();
        long exitCode = judgeContext.getExitCode();

        JudgeInfo judgeInfoResponse = new JudgeInfo();
        judgeInfoResponse.setMemory(memory);
        judgeInfoResponse.setTime(time);

        // 处理错误情况
        JudgeInfo errorResult = handleErrorCase(judgeContext, judgeInfoResponse);
        if (errorResult != null) {
            return errorResult;
        }

        // 处理错误消息
        List<String> errorMessages = judgeInfo.getErrorMessages();
        if (!errorMessages.isEmpty()) {
            judgeInfoResponse.setErrorMessages(errorMessages);
        }

        // 检查答案正确性
        JudgeInfo answerResult = checkAnswerCorrectness(correct, judgeInfoResponse);
        if (answerResult != null) {
            return answerResult;
        }

        // 检查内存限制
        JudgeInfo memoryResult = checkMemoryLimit(memory, question, judgeInfoResponse);
        if (memoryResult != null) {
            return memoryResult;
        }

        // 检查时间限制
        JudgeInfo timeResult = checkTimeLimit(time, question, judgeInfoResponse);
        if (timeResult != null) {
            return timeResult;
        }

        // 所有检查通过
        judgeInfoResponse.setCnMessage(CommitStatusEnum.ACCEPTED.getCnMessage());
        judgeInfoResponse.setEnMessage(CommitStatusEnum.ACCEPTED.getEnMessage());
        return judgeInfoResponse;
    }

    @Override
    public JudgeInfo doTest(JudgeContext judgeContext) {
        JudgeInfo judgeInfo = judgeContext.getJudgeInfo();
        Long memory = judgeInfo.getMemory();
        Long time = judgeInfo.getTime();
        Question question = judgeContext.getQuestion();
        long exitCode = judgeContext.getExitCode();

        JudgeInfo judgeInfoResponse = new JudgeInfo();
        judgeInfoResponse.setMemory(memory);
        judgeInfoResponse.setTime(time);

        // 处理错误情况
        JudgeInfo errorResult = handleErrorCase(judgeContext, judgeInfoResponse);
        if (errorResult != null) {
            return errorResult;
        }

        // 检查内存限制
        JudgeInfo memoryResult = checkMemoryLimit(memory, question, judgeInfoResponse);
        if (memoryResult != null) {
            return memoryResult;
        }

        // 检查时间限制
        JudgeInfo timeResult = checkTimeLimit(time, question, judgeInfoResponse);
        if (timeResult != null) {
            return timeResult;
        }

        return judgeInfoResponse;
    }

    /**
     * 处理错误情况
     */
    private JudgeInfo handleErrorCase(JudgeContext judgeContext, JudgeInfo judgeInfoResponse) {
        long exitCode = judgeContext.getExitCode();
        if (exitCode != 0) {
            List<String> errorMessages = judgeContext.getJudgeInfo().getErrorMessages();
            String errorOutput = errorMessages.get(0);
            String language = judgeContext.getLanguage();
            String errorType = errorClassificationService.classifyError(errorOutput, exitCode, language);
            
            if ("COMPILE_ERROR".equals(errorType)) {
                judgeInfoResponse.setCnMessage(CommitStatusEnum.COMPILE_ERROR.getCnMessage());
                judgeInfoResponse.setEnMessage(errorOutput);
            } else if ("RUNTIME_ERROR".equals(errorType)) {
                judgeInfoResponse.setCnMessage(CommitStatusEnum.RUNTIME_ERROR.getCnMessage());
                judgeInfoResponse.setEnMessage(errorOutput);
            } else {
                judgeInfoResponse.setCnMessage(CommitStatusEnum.NON_ZERO_ERROR.getCnMessage());
                judgeInfoResponse.setEnMessage(errorOutput);
            }
            judgeInfoResponse.setExitCode(exitCode);
            return judgeInfoResponse;
        }
        return null;
    }

    /**
     * 检查答案正确性
     */
    private JudgeInfo checkAnswerCorrectness(boolean[] correct, JudgeInfo judgeInfoResponse) {
        for (boolean c : correct) {
            if (!c) {
                judgeInfoResponse.setCnMessage(CommitStatusEnum.WRONG_ANSWER.getCnMessage());
                judgeInfoResponse.setEnMessage(CommitStatusEnum.WRONG_ANSWER.getEnMessage());
                return judgeInfoResponse;
            }
        }
        return null;
    }

    /**
     * 检查内存限制
     */
    private JudgeInfo checkMemoryLimit(Long memory, Question question, JudgeInfo judgeInfoResponse) {
        long memoryLimit = question.getMemoryLimit();
        if (memory > memoryLimit) {
            judgeInfoResponse.setCnMessage(CommitStatusEnum.MEMORY_LIMIT_EXCEEDED.getCnMessage());
            judgeInfoResponse.setEnMessage(CommitStatusEnum.MEMORY_LIMIT_EXCEEDED.getEnMessage());
            return judgeInfoResponse;
        }
        return null;
    }

    /**
     * 检查时间限制
     * 子类可以重写此方法来提供不同的时间限制逻辑
     */
    protected JudgeInfo checkTimeLimit(Long time, Question question, JudgeInfo judgeInfoResponse) {
        long timeLimit = getTimeLimit(question);
        if (time > timeLimit) {
            judgeInfoResponse.setCnMessage(CommitStatusEnum.TIME_OUT.getCnMessage());
            judgeInfoResponse.setEnMessage(CommitStatusEnum.TIME_OUT.getEnMessage());
            return judgeInfoResponse;
        }
        return null;
    }

    /**
     * 获取时间限制
     * 子类可以重写此方法来提供不同的时间限制计算
     */
    protected long getTimeLimit(Question question) {
        return question.getTimeLimit();
    }
}
