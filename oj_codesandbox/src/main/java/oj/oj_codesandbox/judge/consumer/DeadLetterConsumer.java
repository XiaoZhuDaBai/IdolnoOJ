package oj.oj_codesandbox.judge.consumer;

import oj.oj_codesandbox.model.ExecuteCodeRequest;
import oj.oj_codesandbox.model.JudgeResultMessage;
import oj.oj_codesandbox.judge.rabbitmq.RabbitMQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 死信队列消费者 - 处理异常消息
 */
@Component
public class DeadLetterConsumer {
    
    private static final Logger log = LoggerFactory.getLogger(DeadLetterConsumer.class);
    
    /**
     * 处理判题任务的死信消息
     */
    @RabbitListener(queues = RabbitMQConfig.DLX_QUEUE)
    public void handleDeadLetter(Object message) {
        log.error("收到死信消息: {}", message);
        
        // 根据消息类型进行不同处理
        if (message instanceof ExecuteCodeRequest) {
            handleJudgeTaskDeadLetter((ExecuteCodeRequest) message);
        } else if (message instanceof JudgeResultMessage) {
            handleResultDeadLetter((JudgeResultMessage) message);
        } else {
            log.error("未知类型的死信消息: {}", message.getClass().getName());
        }
    }
    
    /**
     * 处理判题任务的死信
     */
    private void handleJudgeTaskDeadLetter(ExecuteCodeRequest request) {
        String commitId = request.getCommitId();
        log.error("判题任务进入死信队列，commitId: {}, problemId: {}", 
                 commitId, request.getProblemId());
        
        // 示例：记录到日志，实际项目中可以发送到监控系统
        log.error("判题任务处理失败，需要人工介入，commitId: {}", commitId);
    }
    
    /**
     * 处理结果消息的死信
     */
    private void handleResultDeadLetter(JudgeResultMessage result) {
        String commitId = result.getCommitId();
        log.error("判题结果进入死信队列，commitId: {}, problemId: {}", 
                 commitId, result.getProblemId());
        
        log.error("判题结果处理失败，需要人工介入，commitId: {}", commitId);
    }
}
