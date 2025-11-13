package oj.oj_codesandbox.judge.consumer;

import oj.oj_codesandbox.CodeSandbox;
import oj.oj_codesandbox.CodeSandboxProxy;
import oj.oj_codesandbox.codesandbox.ConfigurableCodeSandbox;
import oj.oj_codesandbox.judge.JudgeManager;
import oj.oj_codesandbox.judge.entity.JudgeContext;
import oj.oj_codesandbox.judge.entity.JudgeInfo;
import oj.oj_codesandbox.judge.myenum.CommitStatusEnum;
import oj.oj_codesandbox.model.ExecuteCodeRequest;
import oj.oj_codesandbox.model.ExecuteCodeResponse;
import oj.oj_codesandbox.model.JudgeResultMessage;
import oj.oj_codesandbox.model.dto.CommitCase;
import oj.oj_codesandbox.model.dto.Question;
import oj.oj_codesandbox.model.dto.UserCommit;
import oj.oj_codesandbox.judge.rabbitmq.RabbitMQConfig;
import oj.oj_codesandbox.service.CommitCaseService;
import oj.oj_codesandbox.service.QuestionService;
import oj.oj_codesandbox.service.UserCommitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.List;

/**
 * 判题任务消费者 - 专门负责判题逻辑
 */
@Component
public class JudgeTaskConsumer {
    
    private static final Logger log = LoggerFactory.getLogger(JudgeTaskConsumer.class);
    
    @Resource
    private UserCommitService userCommitService;
    @Resource
    private QuestionService questionService;
    @Resource
    private CommitCaseService commitCaseService;
    @Resource
    private JudgeManager judgeManager;
    @Resource
    private RabbitTemplate rabbitTemplate;
    
    @Value("${codesandbox.type}")
    private String type;
    
    @Autowired
    private ConfigurableCodeSandbox configurableCodeSandbox;
    
    private CodeSandbox codeSandboxProxy;
    
    @PostConstruct
    public void init() {
        if (type == null || type.trim().isEmpty()) {
            log.warn("codesandbox.type 配置为空，使用默认值 'native'");
            type = "native";
        }
        
        // 直接使用注入的 ConfigurableCodeSandbox
        this.codeSandboxProxy = new CodeSandboxProxy(configurableCodeSandbox);
        log.info("判题消费者初始化完成，沙箱类型: {}", type);
    }
    
    /**
     * 消费判题任务 - 只负责判题逻辑
     */
    @RabbitListener(queues = RabbitMQConfig.JUDGE_QUEUE)
    public void processJudgeTask(ExecuteCodeRequest executeCodeRequest) {
        String commitId = executeCodeRequest.getCommitId();
        String problemId = executeCodeRequest.getProblemId();
        String userId = executeCodeRequest.getUuid();
        long startTime = System.currentTimeMillis();
        
        log.info("开始处理判题任务，commitId: {}, problemId: {}", commitId, problemId);
        
        try {
            // 1. 更新状态为"判题中"
            updateCommitStatus(commitId, CommitStatusEnum.RUNNING);
            
            // 2. 获取题目信息
            Question question = getQuestion(problemId);
            
            // 3. 获取用户提交信息
            UserCommit userCommit = getUserCommit(commitId);
            
            // 4. 执行判题
            JudgeResultMessage result = executeJudge(executeCodeRequest, question, userCommit, startTime);
            
            // 5. 发送判题结果到结果队列
            sendJudgeResult(result);
            
            log.info("判题任务完成，commitId: {}, 耗时: {}ms", commitId, result.getProcessTime());
            
        } catch (Exception e) {
            log.error("判题任务失败，commitId: {}", commitId, e);
            
            // 发送失败结果到结果队列
            JudgeResultMessage errorResult = JudgeResultMessage.error(
                commitId, problemId, userId, e.getMessage(), 
                System.currentTimeMillis() - startTime
            );
            sendJudgeResult(errorResult);
        }
    }
    
    /**
     * 更新提交状态
     */
    private void updateCommitStatus(String commitId, CommitStatusEnum status) {
        CommitCase commitCaseUpdate = new CommitCase();
        commitCaseUpdate.setCommitId(commitId);
        commitCaseUpdate.setCnName(status.getCnMessage());
        commitCaseUpdate.setEnglishName(status.getEnMessage());
        
        boolean updated = commitCaseService.updateById(commitCaseUpdate);
        if (!updated) {
            throw new RuntimeException("更新CommitCase状态失败，commitId: " + commitId);
        }
    }
    
    /**
     * 获取题目信息
     */
    private Question getQuestion(String problemId) {
        Question question = questionService.getOneQuestion(problemId);
        if (question == null) {
            throw new RuntimeException("题目找不到，problemId: " + problemId);
        }
        return question;
    }
    
    /**
     * 获取用户提交信息
     */
    private UserCommit getUserCommit(String commitId) {
        UserCommit userCommit = userCommitService.getByCommitId(commitId);
        if (userCommit == null) {
            throw new RuntimeException("提交记录不存在，commitId: " + commitId);
        }
        return userCommit;
    }
    
    /**
     * 执行判题
     */
    private JudgeResultMessage executeJudge(ExecuteCodeRequest executeCodeRequest, 
                                         Question question, UserCommit userCommit, 
                                         long startTime) {
        // 调用沙箱执行代码
        ExecuteCodeResponse executeCodeResponse = codeSandboxProxy.executeCode(executeCodeRequest);
        List<String> outputList = executeCodeResponse.getOutputList();
        JudgeInfo exeJudgeInfo = executeCodeResponse.getJudgeInfo();
        
        // 构建判题上下文
        JudgeContext judgeContext = JudgeContext.builder()
                .exitCode(executeCodeResponse.getExitCode())
                .question(question)
                .outputList(outputList)
                .judgeInfo(exeJudgeInfo)
                .userCommit(userCommit)
                .language(executeCodeRequest.getLanguage())
                .build();
        
        // 执行判题
        JudgeInfo judgeInfo = judgeManager.doJudge(judgeContext);
        
        long processTime = System.currentTimeMillis() - startTime;
        
        return JudgeResultMessage.success(
            executeCodeRequest.getCommitId(),
            executeCodeRequest.getProblemId(),
            executeCodeRequest.getUuid(),
            judgeInfo,
            executeCodeResponse,
            processTime
        );
    }
    
    /**
     * 发送判题结果到结果队列
     */
    private void sendJudgeResult(JudgeResultMessage result) {
        try {
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.RESULT_EXCHANGE, 
                RabbitMQConfig.RESULT_ROUTING_KEY, 
                result
            );
            log.debug("判题结果已发送到结果队列，commitId: {}", result.getCommitId());
        } catch (Exception e) {
            log.error("发送判题结果失败，commitId: {}", result.getCommitId(), e);
            throw new RuntimeException("发送判题结果失败", e);
        }
    }
}
