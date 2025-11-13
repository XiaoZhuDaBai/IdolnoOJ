package oj.oj_codesandbox.judge.consumer;

import oj.oj_codesandbox.judge.entity.JudgeInfo;
import oj.oj_codesandbox.judge.myenum.CommitStatusEnum;
import oj.oj_codesandbox.model.ExecuteCodeResponse;
import oj.oj_codesandbox.model.JudgeResultMessage;
import oj.oj_codesandbox.model.dto.CommitCase;
import oj.oj_codesandbox.judge.rabbitmq.RabbitMQConfig;
import oj.oj_codesandbox.service.CommitCaseService;
import oj.oj_codesandbox.service.QuestionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Arrays;

/**
 * 数据库更新消费者 - 专门负责数据库更新操作
 */
@Component
public class DatabaseUpdateConsumer {
    
    private static final Logger log = LoggerFactory.getLogger(DatabaseUpdateConsumer.class);
    
    @Resource
    private CommitCaseService commitCaseService;
    @Resource
    private QuestionService questionService;
    
    /**
     * 消费判题结果 - 只负责数据库更新
     */
    @RabbitListener(queues = RabbitMQConfig.RESULT_QUEUE)
    public void updateDatabase(JudgeResultMessage result) {
        String commitId = result.getCommitId();
        String problemId = result.getProblemId();
        
        log.info("开始更新数据库，commitId: {}, problemId: {}", commitId, problemId);
        
        try {
            if (result.isSuccess()) {
                // 更新判题结果
                updateJudgeResult(result);
                
                // 更新题目统计信息
                updateQuestionStatistics(result);
                
                log.info("数据库更新成功，commitId: {}", commitId);
            } else {
                // 更新为失败状态
                updateFailureResult(result);
                log.warn("判题失败，已更新数据库，commitId: {}, 错误: {}", commitId, result.getErrorMessage());
            }
            
        } catch (Exception e) {
            log.error("数据库更新失败，commitId: {}", commitId, e);
            // 这里可以发送到死信队列或重试队列
            handleUpdateFailure(result, e);
        }
    }
    
    /**
     * 更新判题结果
     */
    private void updateJudgeResult(JudgeResultMessage result) {
        JudgeInfo judgeInfo = result.getJudgeInfo();
        ExecuteCodeResponse executeCodeResponse = result.getExecuteCodeResponse();
        
        CommitCase commitCaseUpdate = new CommitCase();
        commitCaseUpdate.setCommitId(result.getCommitId());
        commitCaseUpdate.setCnName(judgeInfo.getCnMessage());
        commitCaseUpdate.setEnglishName(judgeInfo.getEnMessage());
        commitCaseUpdate.setTime(judgeInfo.getTime());
        commitCaseUpdate.setMemory(judgeInfo.getMemory());
        
        // 设置输出结果
        long exitCode = judgeInfo.getExitCode();
        if (exitCode == 0 && executeCodeResponse.getJudgeInfo() != null) {
            commitCaseUpdate.setOutput(Arrays.toString(executeCodeResponse.getJudgeInfo().getCorrect()));
        }
        
        boolean updated = commitCaseService.updateById(commitCaseUpdate);
        if (!updated) {
            throw new RuntimeException("更新判题结果失败，commitId: " + result.getCommitId());
        }
    }
    
    /**
     * 更新题目统计信息
     */
    private void updateQuestionStatistics(JudgeResultMessage result) {
        String problemId = result.getProblemId();
        JudgeInfo judgeInfo = result.getJudgeInfo();
        
        try {
            // 更新提交总数
            int commitResult = questionService.updateCommitCountById(problemId);
            if (commitResult <= 0) {
                log.warn("更新提交总数失败，problemId: {}", problemId);
            }
            
            // 更新通过数（只有成功才更新）
            if (judgeInfo.getCnMessage().equals(CommitStatusEnum.ACCEPTED.getCnMessage())) {
                int acResult = questionService.updateAcCountById(problemId);
                if (acResult <= 0) {
                    log.warn("更新通过数失败，problemId: {}", problemId);
                }
            }
            
        } catch (Exception e) {
            log.error("更新题目统计信息失败，problemId: {}, commitId: {}", problemId, result.getCommitId(), e);
            // 不影响主流程，只记录日志
        }
    }
    
    /**
     * 更新失败结果
     */
    private void updateFailureResult(JudgeResultMessage result) {
        CommitCase commitCaseError = new CommitCase();
        commitCaseError.setCommitId(result.getCommitId());
        commitCaseError.setCnName(CommitStatusEnum.FAIL.getCnMessage());
        commitCaseError.setEnglishName(CommitStatusEnum.FAIL.getEnMessage());
        
        boolean updated = commitCaseService.updateById(commitCaseError);
        if (!updated) {
            throw new RuntimeException("更新失败状态失败，commitId: " + result.getCommitId());
        }
    }
    
    /**
     * 处理更新失败的情况
     */
    private void handleUpdateFailure(JudgeResultMessage result, Exception e) {
        // 这里可以实现重试逻辑或发送到死信队列
        log.error("数据库更新失败，需要人工处理，commitId: {}, 错误: {}", 
                 result.getCommitId(), e.getMessage());
        
        // 可以发送到死信队列进行人工处理
        // 或者实现重试机制
    }
}
