package oj.oj_codesandbox.judge;


import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import oj.oj_codesandbox.CodeSandbox;
import oj.oj_codesandbox.CodeSandboxFactor;
import oj.oj_codesandbox.CodeSandboxProxy;
import oj.oj_codesandbox.comm.ResponseResult;
import oj.oj_codesandbox.judge.entity.JudgeContext;
import oj.oj_codesandbox.judge.entity.JudgeInfo;
import oj.oj_codesandbox.judge.myenum.CommitStatusEnum;
import oj.oj_codesandbox.model.ExecuteCodeRequest;
import oj.oj_codesandbox.model.ExecuteCodeResponse;
import oj.oj_codesandbox.model.dto.CommitCase;
import oj.oj_codesandbox.model.dto.Question;
import oj.oj_codesandbox.model.dto.UserCommit;
import oj.oj_codesandbox.model.vo.CommitResultVo;
import oj.oj_codesandbox.judge.rabbitmq.RabbitMQConfig;
import oj.oj_codesandbox.service.CommitCaseService;
import oj.oj_codesandbox.service.QuestionService;
import oj.oj_codesandbox.service.UserCommitService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Collections;
import java.util.Date;
import java.util.List;


/**
 *  实现判题逻辑
 */
@Service
public class JudgeServiceImpl implements JudgeService {
    @Resource
    private UserCommitService userCommitService;
    @Resource
    private QuestionService questionService;
    @Resource
    private CommitCaseService commitCaseService;
    @Value("${codesandbox.type}")
    private String type;
    @Resource
    private JudgeManager judgeManager;
    @Resource
    private RabbitTemplate rabbitTemplate;
    private CodeSandbox codeSandboxProxy;
    
    @PostConstruct
    public void init() {
        if (type == null || type.trim().isEmpty()) {
            log.warn("codesandbox.type 配置为空，使用默认值 'remote'");
            type = "remote";
        }
        CodeSandbox codeSandbox = CodeSandboxFactor.newInstance(type);
        this.codeSandboxProxy = new CodeSandboxProxy(codeSandbox);
        log.info("代码沙箱初始化完成，类型: {}", type);
    }
    
    private static final Logger log = LoggerFactory.getLogger(JudgeServiceImpl.class);

    /**
     * 在进入队列前发生错误允许回滚
     * @param executeCodeRequest
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResponseResult<CommitResultVo> doJudge(ExecuteCodeRequest executeCodeRequest) {
        // 1. 参数验证
        validateExecuteCodeRequest(executeCodeRequest);
        
        // 2. 保存用户提交记录
        String commitId = saveUserCommit(executeCodeRequest);
        
        // 3. 保存判题状态为"等待中"
        saveCommitCase(commitId, CommitStatusEnum.WAITING);
        
        // 4. 发送判题任务到队列
        sendJudgeTask(executeCodeRequest, commitId);
        
        // 5. 返回提交成功响应
        return buildSuccessResponse(commitId);
    }

    /**
     * 验证执行代码请求参数
     */
    private void validateExecuteCodeRequest(ExecuteCodeRequest request) {
        if (request == null) {
            throw new RuntimeException("请求参数不能为空");
        }
        if (StrUtil.isEmptyIfStr(request.getCode())) {
            throw new RuntimeException("代码不能为空");
        }
        if (StrUtil.isEmptyIfStr(request.getLanguage())) {
            throw new RuntimeException("编程语言不能为空");
        }
        if (StrUtil.isEmptyIfStr(request.getProblemId())) {
            throw new RuntimeException("题目ID不能为空");
        }
        if (StrUtil.isEmptyIfStr(request.getUuid())) {
            throw new RuntimeException("用户ID不能为空");
        }
    }
    
    /**
     * 保存用户提交记录
     */
    private String saveUserCommit(ExecuteCodeRequest request) {
        UserCommit commit = new UserCommit();
        String commitId = UUID.randomUUID().toString();
        commit.setUid(request.getUuid());
        commit.setQid(request.getProblemId());
        commit.setCommitId(commitId);
        commit.setCode(request.getCode());
        commit.setLanguage(request.getLanguage());
        commit.setCreateTime(new Date());
        
        int save = userCommitService.insert(commit);
        if (save <= 0) {
            throw new RuntimeException("提交保存失败");
        }
        return commitId;
    }
    
    /**
     * 保存判题状态
     */
    private void saveCommitCase(String commitId, CommitStatusEnum status) {
        CommitCase commitCase = new CommitCase();
        commitCase.setCommitId(commitId);
        commitCase.setCnName(status.getCnMessage());
        commitCase.setEnglishName(status.getEnMessage());
        
        int insert = commitCaseService.insert(commitCase);
        if (insert <= 0) {
            throw new RuntimeException("提交状态保存失败");
        }
    }
    
    /**
     * 发送判题任务到队列
     */
    private void sendJudgeTask(ExecuteCodeRequest request, String commitId) {
        request.setCommitId(commitId);
        try {
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.JUDGE_EXCHANGE, 
                RabbitMQConfig.JUDGE_ROUTING_KEY, 
                request
            );
            log.info("判题任务已发送到队列，commitId: {}", commitId);
        } catch (Exception e) {
            log.error("发送判题任务失败，commitId: {}", commitId, e);
            throw new RuntimeException("发送判题任务失败", e);
        }
    }
    
    /**
     * 构建成功响应
     */
    private ResponseResult<CommitResultVo> buildSuccessResponse(String commitId) {
        CommitResultVo resultVo = new CommitResultVo();
        resultVo.setMessage("提交成功，正在判题中");
        resultVo.setCommitId(commitId);
        return ResponseResult.success(resultVo);
    }

    /**
     * 根据提交ID查询判题结果
     */
    public ResponseResult<CommitResultVo> getJudgeResult(String commitId) {
        // 查询判题结果
        CommitCase commitCase = commitCaseService.getByCommitId(commitId);
        if (commitCase == null) {
            return ResponseResult.fail("提交记录不存在");
        }

        CommitResultVo resultVo = new CommitResultVo();
        String judgeResult = commitCase.getCnName();
        resultVo.setCommitId(commitId);
        resultVo.setMessage(judgeResult);
        resultVo.setTime(commitCase.getTime());
        resultVo.setMemory(commitCase.getMemory());
        resultVo.setOutput(Collections.singletonList(commitCase.getOutput()));
        //做错误信息返回
        if (judgeResult.equals(CommitStatusEnum.COMPILE_ERROR.getCnMessage()) ||
                judgeResult.equals(CommitStatusEnum.RUNTIME_ERROR.getCnMessage()) ||
                judgeResult.equals(CommitStatusEnum.NON_ZERO_ERROR.getCnMessage())) {

            resultVo.setErrorMessages(commitCase.getEnglishName());
        }
        // 如果状态是等待中或运行中，前端可以继续轮询
        resultVo.setProcessing(CommitStatusEnum.WAITING.getCnMessage().equals(commitCase.getCnName()) ||
                CommitStatusEnum.RUNNING.getCnMessage().equals(commitCase.getCnName()));

        return ResponseResult.success(resultVo);
    }

    @Override
    public ResponseResult<CommitResultVo> userTest(ExecuteCodeRequest executeCodeRequest) {
        String input = executeCodeRequest.getUserInput();
        String language = executeCodeRequest.getLanguage();
        String problemId = executeCodeRequest.getProblemId();

        if (StrUtil.isEmptyIfStr(input)) {
            // 无输入，提示输入
            CommitResultVo resultVo = new CommitResultVo();
            resultVo.setOutput(null);
            resultVo.setTime(0L);
            resultVo.setMemory(0L);
            resultVo.setErrorMessages("请在输入行完成输入!");
            return ResponseResult.fail(resultVo);
        }

        Question question = questionService.getOneQuestion(problemId);
        if (question == null){
            throw new RuntimeException("题目找不到");
        }

        // 调用沙箱 - 使用已初始化的代理对象
        CodeSandbox codeSandbox = codeSandboxProxy;

        ExecuteCodeResponse executeCodeResponse = codeSandbox.userTestCode(executeCodeRequest);

        List<String> outputList = executeCodeResponse.getOutputList();
        // 沙箱执行代码获取返回对象
        JudgeContext judgeContext = JudgeContext.builder()
                .exitCode(executeCodeResponse.getExitCode())
                .question(question)
                .language(language)
                .outputList(outputList)
                .judgeInfo(executeCodeResponse.getJudgeInfo())
                .build();
        // 获取判题结果
        JudgeInfo judgeInfo = judgeManager.doTest(judgeContext);

        // 返回结果对象
        long exitCode = judgeInfo.getExitCode();
        CommitResultVo resultVo = new CommitResultVo();
        if (exitCode != 0) {
            resultVo.setErrorMessages(judgeInfo.getCnMessage() + " : " + judgeInfo.getEnMessage());
        } else {
            resultVo.setMessage(judgeInfo.getCnMessage());
        }
        resultVo.setOutput(outputList);
        resultVo.setTime(judgeInfo.getTime());
        resultVo.setMemory(judgeInfo.getMemory());

        return ResponseResult.success(resultVo);
    }
}
